package com.example.service

/**
 * HashGrid Institutional PostgreSQL & Supabase RPC Engine
 *
 * Implements strict server-side transaction constraints:
 * 1. request_withdrawal(amount, wallet_address, network):
 *    - Hard-coded minimum constraint: amount < 130.00 aborts immediately with error
 *    - Atomically transfers funds: available_balance -> locked_audit_balance
 *    - Prevents race conditions and double-spending
 * 2. process_referral_commission():
 *    - Strictly blocks 7% commissions and VIP team volume for Free/Ad plans
 *    - Requires verified paid contract purchase ($100.00+ USDT)
 * 3. check_ad_plan_device_limit():
 *    - Device fingerprint & IP limit: 1 session per 24 hours
 */
object SupabaseDatabaseService {

    const val POSTGRES_RPC_SCHEMA = """
    -- =========================================================================
    -- HASHGRID TRANSACTIONAL DATABASE PROCEDURES & CONSTRAINTS
    -- =========================================================================

    -- 1. WITHDRAWAL ATOMIC ENFORCEMENT RPC
    CREATE OR REPLACE FUNCTION request_withdrawal(
        p_user_id UUID,
        p_amount NUMERIC,
        p_wallet_address TEXT,
        p_network TEXT
    ) RETURNS JSONB AS $$
    DECLARE
        v_current_available NUMERIC;
        v_withdrawal_id UUID;
    BEGIN
        -- [CRITICAL CONSTRAINT] Server-side minimum withdrawal threshold
        IF p_amount < 130.00 THEN
            RAISE EXCEPTION 'Minimum withdrawal limit is $130.00 USDT';
        END IF;

        -- Check destination address length
        IF LENGTH(TRIM(p_wallet_address)) < 15 THEN
            RAISE EXCEPTION 'Invalid destination address';
        END IF;

        -- Lock user balance row FOR UPDATE to eliminate concurrency race conditions
        SELECT available_balance INTO v_current_available
        FROM user_wallets
        WHERE user_id = p_user_id
        FOR UPDATE;

        IF v_current_available < p_amount THEN
            RAISE EXCEPTION 'Insufficient available balance. Available: % USDT', v_current_available;
        END IF;

        -- ATOMIC TRANSFER: available_balance -> locked_audit_balance
        UPDATE user_wallets
        SET available_balance = available_balance - p_amount,
            locked_audit_balance = locked_audit_balance + p_amount,
            updated_at = NOW()
        WHERE user_id = p_user_id;

        -- Insert audit record with PENDING_24H_AUDIT
        INSERT INTO withdrawals (
            user_id,
            amount_usdt,
            target_address,
            network,
            status,
            created_at
        ) VALUES (
            p_user_id,
            p_amount,
            p_wallet_address,
            p_network,
            'PENDING_24H_AUDIT',
            NOW()
        ) RETURNING id INTO v_withdrawal_id;

        RETURN jsonb_build_object(
            'success', true,
            'withdrawal_id', v_withdrawal_id,
            'amount', p_amount,
            'status', 'PENDING_24H_AUDIT',
            'locked_audit_balance', p_amount
        );
    END;
    $$ LANGUAGE plpgsql SECURITY DEFINER;


    -- 2. AD PLAN ANTI-ABUSE & REFERRAL COMMISSION LOCK
    CREATE OR REPLACE FUNCTION process_plan_referral_rewards(
        p_referrer_id UUID,
        p_buyer_id UUID,
        p_plan_id TEXT,
        p_deposit_amount NUMERIC
    ) RETURNS VOID AS $$
    BEGIN
        -- [ANTI-ABUSE RULE]: Strictly disable all referral commissions (7%)
        -- and team volume increments for Ad/Free Starter Plans.
        IF p_deposit_amount < 100.00 THEN
            -- Abort commission processing: No rewards for free/ad plans
            RETURN;
        END IF;

        -- Process verified paid contract (Minimum $100.00 USDT)
        -- Credit 7% direct referral commission
        UPDATE user_wallets
        SET available_balance = available_balance + (p_deposit_amount * 0.07),
            total_referral_earnings = total_referral_earnings + (p_deposit_amount * 0.07)
        WHERE user_id = p_referrer_id;

        -- Increment VIP team volume
        UPDATE user_profiles
        SET team_volume_usdt = team_volume_usdt + p_deposit_amount
        WHERE user_id = p_referrer_id;
    END;
    $$ LANGUAGE plpgsql SECURITY DEFINER;


    -- 3. DEVICE FINGERPRINT & IP 24-HOUR LIMIT FOR FREE AD SESSIONS
    CREATE OR REPLACE FUNCTION check_free_ad_session(
        p_device_fingerprint TEXT,
        p_ip_address TEXT
    ) RETURNS BOOLEAN AS $$
    DECLARE
        v_last_session TIMESTAMP WITH TIME ZONE;
    BEGIN
        SELECT last_session_at INTO v_last_session
        FROM free_ad_sessions
        WHERE device_fingerprint = p_device_fingerprint
           OR ip_address = p_ip_address
        ORDER BY last_session_at DESC
        LIMIT 1;

        IF v_last_session IS NOT NULL AND v_last_session > NOW() - INTERVAL '24 HOURS' THEN
            RAISE EXCEPTION 'Device Limit Exceeded: Only 1 Free Ad Plan session allowed per device/IP every 24 hours.';
        END IF;

        INSERT INTO free_ad_sessions (device_fingerprint, ip_address, last_session_at)
        VALUES (p_device_fingerprint, p_ip_address, NOW());

        RETURN true;
    END;
    $$ LANGUAGE plpgsql SECURITY DEFINER;
    """
}
