-- Creates the sequences required by the application.
--
-- This is intended for PostgreSQL.
--
-- Why: InvoiceRepository#getNextInvoiceSequence uses nextval('invoice_sequence')
-- and will fail if the sequence doesn't exist.

BEGIN;

-- Main invoice number sequence.
CREATE SEQUENCE IF NOT EXISTS invoice_sequence
    AS BIGINT
    INCREMENT BY 1
    MINVALUE 1
    START WITH 1
    CACHE 1;

COMMIT;

