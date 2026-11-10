#!/bin/sh
# Runs once, when the MySQL volume is first created.
# Integration tests use separate "test_<DB_NAME>" databases so they never
# touch development data; the app user only gets <DB_NAME> by default.
# Each worktree may use its own test DB (TEST_DB_NAME in .env), so the grant
# covers every name starting with "test_<DB_NAME>" ("_" escaped, "%" wildcard).
#
# For an already-running MySQL, scripts/grant-test-dbs.sh pipes this file into
# the db container. GRANT is idempotent, so running it again is harmless.
set -e

# "_" is a wildcard in database grants; escape it to match literally
db_pattern=$(printf 'test_%s' "$MYSQL_DATABASE" | sed 's/_/\\_/g')

MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --protocol=socket -uroot <<SQL
GRANT ALL PRIVILEGES ON \`${db_pattern}%\`.* TO '${MYSQL_USER}'@'%';
SQL
