#!/bin/sh
# Runs once, when the MySQL volume is first created.
# Django's test runner creates and drops "test_<DB_NAME>", so the app user
# needs rights on that database too (it only gets <DB_NAME> by default).
set -e

mysql --protocol=socket -uroot -p"$MYSQL_ROOT_PASSWORD" <<SQL
GRANT ALL PRIVILEGES ON \`test_${MYSQL_DATABASE}\`.* TO '${MYSQL_USER}'@'%';
FLUSH PRIVILEGES;
SQL
