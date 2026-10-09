-- SOSS Phase 2 Orders & Payments compatibility migration.
--
-- Hibernate's original MySQL enum mapping cannot accept enum values added by
-- later application versions. Converting these two status columns to VARCHAR
-- preserves every existing row while allowing REFUNDED and CANCELLED.
--
-- Run once against the target soss_db database before deploying Phase 2.

ALTER TABLE payments
    MODIFY COLUMN status VARCHAR(20) NULL;

ALTER TABLE orders
    MODIFY COLUMN status VARCHAR(30) NULL;
