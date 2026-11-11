package com.biw.config;

import com.biw.common.security.AdminGuardLock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * A PostgreSQL transaction-level advisory lock for the admin-count invariant.
 * <p>
 * {@code pg_advisory_xact_lock} is held until the transaction ends, needs no table, and cannot
 * deadlock here because there is exactly one key and therefore one acquisition order — a
 * property the previous {@code SELECT ... FOR UPDATE OF u} only had by convention.
 * <p>
 * The key is the ASCII of "biw_adm"; a derived project changes it together with the package
 * rename so two services sharing a database do not serialise against each other by accident.
 */
@Component
public class PostgresAdminGuardLock implements AdminGuardLock {

    /** ASCII of "biw_adm" as a 64-bit key. */
    private static final long LOCK_KEY = 0x6269775f61646dL;

    private final JdbcTemplate jdbc;

    public PostgresAdminGuardLock(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void acquire() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException(
                    "AdminGuardLock must be acquired inside a transaction; otherwise the lock is "
                            + "released before the write it is meant to protect");
        }
        jdbc.query("SELECT pg_advisory_xact_lock(?)", rs -> {
        }, LOCK_KEY);
    }
}
