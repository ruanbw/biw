#!/bin/bash
#
# Creates the role the application actually connects as.
#
# The PostgreSQL image creates POSTGRES_USER as a superuser, and using it from the application
# means any SQL injection or leaked connection string is full control of the database. This
# script runs once, right after 01-init.sql, and gives the application a role that can read and
# write rows and nothing else: no DDL, no role creation, no access to other schemas.
#
# Set APP_DB_PASSWORD (and optionally APP_DB_USERNAME) to match DB_USERNAME / DB_PASSWORD in the
# application configuration.

set -euo pipefail

app_user="${APP_DB_USERNAME:-biw_app}"
app_password="${APP_DB_PASSWORD:?set APP_DB_PASSWORD to create the application role}"
database="${POSTGRES_DB:-biw}"

echo "02-app-role: granting least privilege to role ${app_user}"

# The password is read from the environment inside psql (\getenv) instead of being passed with
# `-v`: a `-v` value is visible in the process list (`ps`, /proc/<pid>/cmdline) for the duration of
# the script, which is exactly long enough for another container to read it.
psql -v ON_ERROR_STOP=1 \
     -v app_user="${app_user}" \
     -v database="${database}" \
     --username "${POSTGRES_USER}" --dbname "${database}" <<'SQL'

\getenv app_password APP_DB_PASSWORD

-- CREATE ROLE has no IF NOT EXISTS, so build the statement conditionally and run it with \gexec:
-- an empty result set is a no-op, which keeps this re-runnable.
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE', :'app_user', :'app_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'app_user')
\gexec

-- Always reset the password, so changing APP_DB_PASSWORD and recreating the container works.
SELECT format('ALTER ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE', :'app_user', :'app_password')
\gexec

-- PUBLIC keeps CONNECT/TEMP on a fresh database and would let any role that can log in hold a
-- session here. Grant the one role that needs it, explicitly.
REVOKE ALL ON DATABASE :"database" FROM PUBLIC;
GRANT CONNECT ON DATABASE :"database" TO :"app_user";
GRANT USAGE ON SCHEMA public TO :"app_user";

-- DML on every table in the schema, and on nothing outside it. Listing the tables by name would
-- break the first derived project that adds one; the boundary that matters here is DDL and
-- cross-schema access, and the role does not get either.
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO :"app_user";
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO :"app_user";

-- One exception, and it is the point of the table: sys_audit_log is evidence. The application
-- writes rows and may read them back; it may not rewrite or delete what it wrote, because a
-- compromised application (or a SQL injection through it) is exactly the actor the record is
-- meant to survive. Retention is therefore an operator job — sql/03-audit-retention.sql, run as
-- the owner — not something this role can do.
REVOKE UPDATE, DELETE ON sys_audit_log FROM :"app_user";

-- Same grants for tables a later migration creates as the owner.
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO :"app_user";
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO :"app_user";

-- PUBLIC keeps its default privileges in older clusters; make sure the schema itself is not
-- writable by every role that can connect.
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

SQL
