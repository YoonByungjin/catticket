#!/bin/sh
# Runs once, when the MySQL volume is first created.
# Integration tests use a separate "test_<DB_NAME>" database so they never
# touch development data; the app user only gets <DB_NAME> by default.
set -e

mysql --protocol=socket -uroot -p"$MYSQL_ROOT_PASSWORD" <<SQL
GRANT ALL PRIVILEGES ON \`test_${MYSQL_DATABASE}\`.* TO '${MYSQL_USER}'@'%';
FLUSH PRIVILEGES;
SQL
