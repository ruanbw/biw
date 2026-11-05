-- Runs once, on first start of an empty PostgreSQL volume.
-- The database itself (biw) and the role (biw) are created by the
-- official image through POSTGRES_DB / POSTGRES_USER.

-- gen_random_uuid(), digest(), ...
CREATE EXTENSION IF NOT EXISTS pgcrypto;
-- uuid_generate_v4()
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Schema objects go here (or use a migration tool later).
