# Products and Inventory

This document describes the integrated product catalogue and inventory functionality. Cart and checkout details are in [Cart & Checkout](CART-CHECKOUT.md).

## Catalogue

The public catalogue is served at `/products`; `/search` remains an alias to the same catalogue handler. Product details use `/products/{id}` with an optional `storeId` query parameter. Search and filters operate on catalogue data and can be combined:

- Product name and description query (`q`).
- Existing product category.
- Minimum and maximum price, using the effective promotional price where one applies.
- Availability.
- Sorting by name, price low to high, or price high to low.

Category values come from products in the selected store. The application does not add sample products or invent categories. Customer catalogue results omit inactive products; product records and their identifiers remain available to authorized management flows.

## Stores and stock

Products have shared details (name, category, description, base price, icon and active state). Inventory quantities and low-stock thresholds are specific to a store. A store can be added through Inventory. Adding a store creates a zero-quantity inventory row for each existing product. Adding a product creates an inventory row for every store; the entered quantity and threshold apply to the selected store, while other stores start with zero stock.

Inventory is managed at `/inventory` by STAFF and ADMIN users. The product form is available at `/inventory/new?storeId={id}` and `/inventory/{id}/edit?storeId={id}`. Product and inventory edits use version checks so stale forms cannot overwrite newer changes. Stock cannot be negative, and the management form accepts quantities from 0 to 1,000,000.

The low-stock threshold defaults to 10 and can be configured per product/store from 0 to 1,000,000. Quantity at or below the configured threshold is considered low stock. The management page reports active products at or below threshold, including out-of-stock products. Alerts are shown in the application; no email or SMS alert is sent.

## Stock movement history

`StockMovement` stores immutable snapshots of product/store IDs and names, before/after quantities, movement type, reason, actor and timestamp. History is available from `/inventory/{id}/history?storeId={id}` with recent-first pagination. Product creation, new-store stock initialization, stock adjustments, restocks and checkout deductions are recorded. Quantity changes and their history entries are committed together; a failed checkout rolls back its stock movement as part of the shared transaction. Historical activity from before stock history was introduced is not reconstructed.

Checkout and other stock-changing integrations should use `CatalogueService` operations so stock locking and history behavior are preserved. Orders & Payments owns the coordinated restoration of stock on eligible order cancellation.

## Product promotions

STAFF and ADMIN can manage scheduled percentage promotions at `/inventory/promotions`. A promotion applies to one product across its stores; its base price remains unchanged. Active dates are inclusive in Pacific/Fiji time. Supported discounts range from 0.01% to 99%, with prices rounded HALF_UP to two decimal places and a minimum effective price of FJ$ 0.01. Enabled date ranges for one product cannot overlap, including endpoints. Scheduled, active, expired, disabled and hidden-product states are shown in management. Stale promotion edits are rejected.

The catalogue displays effective prices, and its price filters and the cart integration use the same active product promotion pricing. Cart promo codes and their stacking/eligibility rules are documented separately in [Cart & Checkout](CART-CHECKOUT.md).

## Security and database

Product and inventory management routes require STAFF or ADMIN authorization and retain Spring Security CSRF protection. Product browsing and details are available through public catalogue routes. Inventory changes are performed through the shared application services rather than direct writes from other modules.

The project uses Hibernate `ddl-auto=update` for local development. The application creates/updates mapped product, inventory, promotion and stock-history schema on startup when the configured MySQL account has the required privileges. This is not a production database migration strategy. No product sample-data initializer is included; create a store and catalogue records through authorized management flows.

## Tests

Relevant automated coverage includes `ProductsInventoryTest` and `ProductsPhaseTwoTest`, as well as the integrated `CheckoutIntegrationTest`. These exercise catalogue rendering and filtering, per-store stock, stock deduction and rollback, stale updates, low-stock thresholds, promotion schedules and effective pricing, authorization, CSRF and checkout integration. Tests use H2 (including MySQL compatibility mode where configured); they do not replace manual checks against the target MySQL schema. Run the full suite with `mvn test` or `mvn clean verify` from the project root.