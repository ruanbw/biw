package com.biw.common.security;

/**
 * Serialises the operations that decide whether the installation still has an administrator.
 * <p>
 * The invariant is "at least one enabled admin", which is a property of a <em>set</em> of rows in
 * {@code sys_user_role}, not of any single row. Row locks on {@code sys_user} happen to work today
 * only because every update also touches {@code updated_at}; that is an accident, not a guarantee.
 * <p>
 * Acquire it as the first statement of any transaction that changes a user's role set or its
 * enabled state, before any row lock. Implementations must fail when called outside a
 * transaction, because the lock is supposed to be released by the transaction commit.
 */
public interface AdminGuardLock {

    void acquire();
}
