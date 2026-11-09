#!/usr/bin/env bash
# この worktree のテスト用 DB（TEST_DB_NAME。なければ test_catticket）を読み取り専用で確認する。
# サブエージェントは権限の承認を受けられないため、docker compose exec を直接許可する代わりに
# 実行できる SQL をこのスクリプトの中に固定し、.claude/settings.json でこれだけを許可する。
#
#   bash scripts/db-inspect.sh tables            テーブル一覧
#   bash scripts/db-inspect.sh create {テーブル}   SHOW CREATE TABLE
#   bash scripts/db-inspect.sh history           Flyway の適用履歴
#
# [한국어] 이 worktree의 테스트용 DB(TEST_DB_NAME. 없으면 test_catticket)를 읽기 전용으로 확인한다.
# 서브 에이전트는 권한 승인을 받을 수 없으므로, docker compose exec를 직접 허용하는 대신
# 실행할 수 있는 SQL을 이 스크립트 안에 고정하고 .claude/settings.json에서 이것만 허용한다.
#   tables: 테이블 목록 / create {테이블}: SHOW CREATE TABLE / history: Flyway 적용 이력
set -euo pipefail

# worktree から実行しても、compose.yaml の name: catticket によりメインの MySQL を指す
cd "$(dirname "$0")/.."

usage() {
  echo "usage: $0 tables | create <table> | history" >&2
  exit 2
}

case "${1:-}" in
  tables)
    sql="SHOW TABLES"
    ;;
  create)
    table="${2:-}"
    # 任意の SQL を渡せないよう、テーブル名は英数字と _ だけ受け付ける
    [[ "$table" =~ ^[A-Za-z0-9_]+$ ]] || usage
    sql="SHOW CREATE TABLE \`$table\`\\G"
    ;;
  history)
    sql="SELECT installed_rank, version, description, success, installed_on FROM flyway_schema_history ORDER BY installed_rank"
    ;;
  *)
    usage
    ;;
esac

# DB 名は ./gradlew test と同じ順で決める: 環境変数 → この worktree の .env → test_catticket
db="${TEST_DB_NAME:-}"
if [[ -z "$db" && -f .env ]]; then
  db=$(sed -n 's/^[[:space:]]*TEST_DB_NAME[[:space:]]*=[[:space:]]*//p' .env | tail -n 1 | tr -d '' | sed 's/[[:space:]]*$//')
fi
db="${db:-test_catticket}"
if ! [[ "$db" =~ ^test_catticket(_[a-z0-9]+)*$ ]] || (( ${#db} > 64 )); then
  echo "TEST_DB_NAME は test_catticket または test_catticket_{英小文字・数字}（64 文字以下）にしてください: $db" >&2
  exit 2
fi

if ! docker compose ps --status running --services | grep -qx db; then
  echo "MySQL (db) が起動していません。メインのチェックアウトで docker compose up -d を実行してください（worktree では起動しない）。" >&2
  exit 1
fi

# 日本語・韓国語の COMMENT が ??? にならないよう、クライアントの文字コードを utf8mb4 にする
docker compose exec -T db sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --default-character-set=utf8mb4 "$2" -e "$1"' sh "$sql" "$db"
