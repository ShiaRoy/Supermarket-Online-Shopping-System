# Phase 2 Products and Inventory

Divit's updated scope from `Phase 2.docx` is implemented on `phase2/products`,
starting from merged `origin/main` commit `529f9aa`. Existing entity names and the
team's shared authentication, navigation and checkout contracts are preserved.

## Features

### Low-stock alerts and thresholds

Each Inventory row has a configurable `lowStockThreshold` (0 to 1,000,000).
The default is 10, including the MySQL column default for existing rows.
Edit the threshold through the product form for the selected store. Other stores
keep their own settings. Quantity equal to the threshold is low stock.

The staff inventory page shows an alert count and a Needs restocking filter for
active products at or below the threshold, including out-of-stock products.
Threshold 0 alerts only at zero. Inactive products are excluded from alerts.
The ordinary Low stock filter remains limited to positive quantities; Out of stock
is separately available. Catalogue badges also use the configured threshold.
Alerts are displayed in the application; there are no email or SMS alerts.

### Stock and restock history

The new supporting entity `StockMovement` records product/store ID and name
snapshots, before/after quantities, movement type, reason, authenticated actor,
and UTC timestamp. It supplements Inventory and does not rename any shared entity.
Snapshot IDs intentionally have no foreign keys so historical entries survive
catalogue removal. There is no application endpoint to edit or delete history.

Inventory > Stock history provides recent-first pagination (30 entries per page)
and a restock form that adds a positive quantity under a row lock. Product creation,
new-store inventory initialization, stock edits, restocking and checkout deductions
are recorded. Quantity and history are committed in the same transaction; a failed
checkout transaction rolls back both. Metadata-only edits do not create stock events.
Historical changes made before this feature existed are not fabricated.

Other modules must use `CatalogueService.deductStock`, `restock`, or `save` rather
than writing Inventory directly, to preserve this audit trail. Refund/cancellation
stock restoration is owned by the order module and must be coordinated separately.

### Active promotion management

The shared entity name `Promotion` is used. Staff/admin can create, edit, enable
and disable scheduled percentage promotions for a product through
`/inventory/promotions`. The selected product is fixed after creation.

- Dates are inclusive calendar dates in Pacific/Fiji.
- States: Scheduled, Active, Expired, Disabled, or Product hidden.
- Percentage discount range: 0.01 to 99 percent, at most two decimal places.
- Prices round HALF_UP to two decimal places and have a FJ$0.01 minimum.
- Enabled date ranges cannot overlap for one product, including their endpoints.
- Promotion writes lock the product to prevent concurrent overlap races; version
  checks reject stale edits. Expired promotions remain available for inspection.
- A product promotion applies to that product in all stores; base prices are unchanged.
- Catalogue/detail pages display the effective price and original price. Price
  filters use the effective price. `CatalogueCheckoutProducts` supplies the same
  effective price through the existing cart integration contract.

Vaibhav still owns promo-code entry, code rules, discount totals and checkout
enhancements. Coordinate whether future codes can combine with product promotions;
this module does not implement code stacking or change the cart interface.

## Security and database

All management routes are below `/inventory/**` and use the team's existing
STAFF/ADMIN security rules and CSRF protection. The old Phase 1 optional in-memory
staff login is not reintroduced. Use a STAFF/ADMIN User account through `/login`.

Schema additions are `inventory.low_stock_threshold`, `promotion`, and
`stock_movement`. Existing development `ddl-auto=update` applies these locally.
No production data, passwords, local launch scripts or sample-data initializer is
included in the branch. The original Java 25 and Spring Boot 3.5.16 setup is retained.

## Verification

The full integrated Maven suite passes: 74 tests, including 9 new Phase 2 tests.
New coverage includes threshold boundaries, branch independence, stock audit
entries, rollback, invalid restocks, promotion dates/disabled states/overlap,
stale promotion edits, effective catalogue/search/checkout prices, staff access,
CSRF, validated posts and template rendering.

Local MySQL browser checks additionally cover the shared login, alert display,
threshold entry, restocking and history, active promotions, promotional catalogue
prices and desktop/mobile layouts. Temporary browser test records are removed.

Run `mvn test` with Java 25; run the application with the team's existing database
environment variables. Open `/inventory` for stock management and
`/inventory/promotions` for promotions.
