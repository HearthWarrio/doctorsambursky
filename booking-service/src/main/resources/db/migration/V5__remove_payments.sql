-- Remove payment-related columns and legacy PENDING state.

-- If there are legacy rows created by the old web flow, move them into the new doctor-approval flow.
UPDATE appointments
SET status = 'PENDING_DOCTOR'
WHERE status = 'PENDING';

ALTER TABLE appointments
    DROP COLUMN IF EXISTS payment_id,
    DROP COLUMN IF EXISTS paid_amount;
