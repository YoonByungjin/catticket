#!/usr/bin/env bash
# すでに動いているメインの MySQL に、worktree ごとのテスト用 DB（test_catticket%）の権限を付ける。
# 新しいボリュームでは docker/mysql/initdb/01-grant-test-db.sh が初回起動時に同じことをする。
# 権限を変える操作なので .claude/settings.json では許可しない（人間の確認のうえで実行する）。
# 何度実行してもよい（GRANT は冪等）。
#
#   bash scripts/grant-test-dbs.sh
#
# [한국어] 이미 돌고 있는 메인 MySQL에 worktree별 테스트용 DB(test_catticket%) 권한을 준다.
# 새 볼륨에서는 docker/mysql/initdb/01-grant-test-db.sh가 첫 실행 때 같은 일을 한다.
# 권한을 바꾸는 작업이므로 .claude/settings.json에서 허용하지 않는다(사람이 확인한 뒤 실행).
# 여러 번 실행해도 된다(GRANT는 멱등).
set -euo pipefail

# worktree から実行しても、compose.yaml の name: catticket によりメインの MySQL を指す
cd "$(dirname "$0")/.."

if ! docker compose ps --status running --services | grep -qx db; then
  echo "MySQL (db) が起動していません。メインのチェックアウトで docker compose up -d を実行してください（worktree では起動しない）。" >&2
  exit 1
fi

# マウントされた initdb ではなくこのチェックアウトのファイルを流す（コンテナの環境変数で動く）
docker compose exec -T db sh -s < docker/mysql/initdb/01-grant-test-db.sh

docker compose exec -T db sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -e "SHOW GRANTS FOR \`$MYSQL_USER\`@\`%\`"'
