-- Keeps sys_audit_log bounded.
--
-- Whose job this is matters: the application role can only SELECT and INSERT on this table
-- (sql/02-app-role.sh revokes UPDATE and DELETE), so the application cannot delete the evidence
-- it produced. The price of that property is that nothing in the application trims the table.
-- Run this as the database owner, nightly:
--
--     docker compose exec -T postgres \
--       psql -U biw -d biw -v retention_days=365 -f - < sql/03-audit-retention.sql
--
-- or from a host cron entry. `retention_days` defaults to 365 when it is not supplied.
--
-- An unbounded table is its own outage: this is not optional housekeeping, it is part of running
-- the audit sink.

\if :{?retention_days}
\else
\set retention_days 365
\endif

DELETE FROM sys_audit_log
WHERE occurred_at < now() - make_interval(days => :retention_days);
