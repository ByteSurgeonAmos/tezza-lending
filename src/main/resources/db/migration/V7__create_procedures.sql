-- V7__create_procedures.sql
-- Stored procedures for business-critical DB operations

-- ============================================================
-- proc_allocate_repayment
-- Allocates payment to PENDING installments FIFO (oldest due date first).
-- Marks fully-paid installments as PAID.
-- Partial payments reduce the installment's OUTSTANDING_AMOUNT.
-- ============================================================
CREATE OR REPLACE PROCEDURE proc_allocate_repayment(
    p_loan_id       UUID,
    p_payment_amount NUMERIC(19,2)
)
LANGUAGE plpgsql AS $$
DECLARE
    rec             RECORD;
    v_remaining     NUMERIC(19,2) := p_payment_amount;
    v_apply         NUMERIC(19,2);
BEGIN
    FOR rec IN
        SELECT ID, OUTSTANDING_AMOUNT
        FROM LOAN_INSTALLMENTS
        WHERE LOAN_ID = p_loan_id
          AND STATUS = 'PENDING'
        ORDER BY DUE_DATE ASC
    LOOP
        EXIT WHEN v_remaining <= 0;

        IF v_remaining >= rec.OUTSTANDING_AMOUNT THEN
            -- FULLY pay this installment
            v_apply := rec.OUTSTANDING_AMOUNT;
            UPDATE LOAN_INSTALLMENTS
            SET OUTSTANDING_AMOUNT = 0,
                STATUS             = 'PAID',
                PAID_AT            = NOW(),
                UPDATED_AT         = NOW()
            WHERE ID = rec.ID;
        ELSE
            -- PARTIALLY pay this installment
            v_apply := v_remaining;
            UPDATE LOAN_INSTALLMENTS
            SET OUTSTANDING_AMOUNT = OUTSTANDING_AMOUNT - v_apply,
                UPDATED_AT         = NOW()
            WHERE ID = rec.ID;
        END IF;

        v_remaining := v_remaining - v_apply;
    END LOOP;
END;
$$;

-- ============================================================
-- proc_close_loan_if_paid
-- Sets loan STATUS to CLOSED and CLOSED_AT if OUTSTANDING_BALANCE = 0.
-- ============================================================
CREATE OR REPLACE PROCEDURE proc_close_loan_if_paid(
    p_loan_id UUID
)
LANGUAGE plpgsql AS $$
BEGIN
    UPDATE LOANS
    SET STATUS     = 'CLOSED',
        CLOSED_AT  = NOW(),
        UPDATED_AT = NOW()
    WHERE ID                 = p_loan_id
      AND OUTSTANDING_BALANCE = 0
      AND STATUS IN ('OPEN', 'OVERDUE');
END;
$$;

-- ============================================================
-- proc_apply_late_fee
-- Adds a late fee to a loan's OUTSTANDING_BALANCE.
-- ============================================================
CREATE OR REPLACE PROCEDURE proc_apply_late_fee(
    p_loan_id  UUID,
    p_fee_amount NUMERIC(19,2)
)
LANGUAGE plpgsql AS $$
BEGIN
    UPDATE LOANS
    SET OUTSTANDING_BALANCE = OUTSTANDING_BALANCE + p_fee_amount,
        UPDATED_AT          = NOW()
    WHERE ID = p_loan_id
      AND p_fee_amount > 0;
END;
$$;

-- ============================================================
-- proc_apply_daily_fee
-- Adds a daily accrual fee to a loan's OUTSTANDING_BALANCE.
-- ============================================================
CREATE OR REPLACE PROCEDURE proc_apply_daily_fee(
    p_loan_id  UUID,
    p_fee_amount NUMERIC(19,2)
)
LANGUAGE plpgsql AS $$
BEGIN
    UPDATE LOANS
    SET OUTSTANDING_BALANCE = OUTSTANDING_BALANCE + p_fee_amount,
        UPDATED_AT          = NOW()
    WHERE ID = p_loan_id
      AND p_fee_amount > 0;
END;
$$;

-- ============================================================
-- proc_run_overdue_sweep
-- Marks OPEN loans past their DUE_DATE as OVERDUE.
-- Marks PENDING installments past their DUE_DATE as OVERDUE.
-- Returns the count of loans transitioned via OUT param.
-- ============================================================
CREATE OR REPLACE PROCEDURE proc_run_overdue_sweep(
    OUT p_count INTEGER
)
LANGUAGE plpgsql AS $$
DECLARE
    v_today DATE := CURRENT_DATE;
BEGIN
    p_count := 0;

    -- Mark overdue installments for OPEN loans past due
    UPDATE LOAN_INSTALLMENTS li
    SET STATUS     = 'OVERDUE',
        UPDATED_AT = NOW()
    FROM LOANS l
    WHERE li.LOAN_ID   = l.ID
      AND l.STATUS     = 'OPEN'
      AND l.DUE_DATE   < v_today
      AND li.STATUS    = 'PENDING'
      AND li.DUE_DATE  < v_today;

    -- Mark loans as OVERDUE
    UPDATE LOANS
    SET STATUS     = 'OVERDUE',
        UPDATED_AT = NOW()
    WHERE STATUS   = 'OPEN'
      AND DUE_DATE < v_today;

    GET DIAGNOSTICS p_count = ROW_COUNT;
END;
$$;
