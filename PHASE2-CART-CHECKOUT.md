# Phase 2 Cart & Checkout

## Scope

The cart and checkout use the authenticated customer's email as the cart owner. A cart is provisioned on first access. Product names, availability and prices come from the catalogue for the default store; active product promotions managed by the Products module are reflected in the server-calculated product price. The browser does not supply trusted prices or totals.

The cart shows the grocery subtotal, delivery fee, each applied promo discount and the estimated total. Delivery costs FJ$ 3.00 for a non-empty cart and FJ$ 0.00 for an empty cart. Amounts use `BigDecimal` and two decimal places.

## Promo codes and stacking

Customers can apply the following cart codes, one at a time through the Apply form. Codes are trimmed and case-insensitive, then stored in uppercase.

| Code | Discount | Eligibility, based on grocery subtotal before promo discounts |
| --- | --- | --- |
| `BULA10` | 10% off groceries, rounded to cents | Cart must contain groceries (subtotal greater than FJ$ 0.00) |
| `SAVE5` | FJ$ 5.00 off groceries | FJ$ 50.00 or more, inclusive |
| `FREEDEL` | Removes the delivery fee | FJ$ 30.00 or more, inclusive |

Different eligible codes can be stacked. The deterministic calculation is:

1. Calculate `BULA10` against the pre-promo grocery subtotal and round the percentage discount to cents.
2. Apply `SAVE5` against the grocery value remaining after `BULA10`, capped at that remaining value. Its FJ$ 50.00 threshold is still checked against the pre-promo subtotal.
3. Apply `FREEDEL` to the delivery fee when the pre-promo grocery subtotal is at least FJ$ 30.00.
4. Cap total discounts at the grocery subtotal plus delivery fee. The payable total cannot be negative.

The cart lists each applied code and its own discount. A code can be removed individually. Applying a duplicate, unknown/expired code, or a code below its eligibility threshold is rejected with an explanatory message. If a quantity change makes an already-applied code ineligible, the cart preview recalculates it as zero and marks it ineligible; checkout is blocked until the basket qualifies again or that code is removed. Applying/removing a code or changing quantity rotates the checkout token so a stale checkout form cannot place an order against a changed basket.

## Cart and checkout validation

Cart quantity controls change one unit at a time, permit quantities from 1 to 99, and reject increments for inactive or unavailable products. Existing unavailable items can still be decremented or removed. Availability and stock are checked again during checkout; the checkout transaction fails and rolls back if required stock is unavailable.

Checkout requires:

- A non-empty cart and a current checkout token matching the basket.
- Available delivery slot selected from the options generated in the Pacific/Fiji time zone.
- One of the supported payment-method choices: `M-PAiSA`, `MyCash`, or `Visa / Mastercard`.
- A complete delivery address, either selected from the customer's own saved/profile addresses or entered as a new structured address.
- All applied promo codes to remain eligible.

The owner and checkout token are used for idempotency. Replaying a successfully used token for the same customer returns the existing order rather than creating another order, payment or stock deduction. The cart is cleared only after the shared order integration succeeds. Validation and application errors are shown on checkout; entered form values are retained where the form is re-rendered.

## Delivery addresses

The checkout form supports a saved address, the account's profile address, or a new structured address with:

- Delivery area
- Street/road
- House, lot or building number
- Optional delivery instructions (up to 160 characters)

The currently configured areas are **Suva** and **Nausori**. Other locations are not accepted by this checkout policy unless the application configuration/code is updated. A new address is composed as `number street, area`, with instructions appended when supplied. Validation rejects missing/incomplete fields and a small set of obvious placeholder values; it cannot prove that an address physically exists. Saved addresses are loaded for the authenticated account and looked up with owner scoping. Selecting a saved address uses it for this order; entering a new checkout address does not automatically add it to the address book.

The delivery slot list is generated for Fiji time: three future options (tomorrow morning, tomorrow afternoon, and the following afternoon). The server accepts only a slot in the current generated list.

## Orders, payments, stock and delivery integration

`CartService` recalculates the basket using server-side catalogue prices and passes the item lines, address, slot, selected payment method and final total to `OrderCheckoutOrders`. The adapter uses the existing customer, order, payment, catalogue and delivery services; it does not create parallel cart/order/product entities.

For a valid checkout, the shared transaction performs these steps:

1. Create the shared order and order items for the authenticated customer. The order total is set to the checkout payable total, including delivery and cart promo discounts.
2. Record a **simulated successful payment** for that full total, with the selected method stored on the payment record.
3. Deduct the ordered quantities from the default store's inventory and record the stock movement.
4. Create the associated delivery with the validated address and selected delivery window.
5. Clear the cart and its promo codes after the operation succeeds.

These operations join the checkout transaction; a failure such as insufficient stock rolls back the order/payment/stock changes and leaves the basket available for correction. Checkout payment methods are prototype choices only. No card details or wallet credentials are collected, and no real payment gateway, bank transfer or financial transaction is performed. The Orders & Payments module also has simulated success/failure handling and records payment history/refunds; cancellation/refund rules remain owned by that module.

## MySQL setup for manual checkout

The application connects to the MySQL database `soss_db`. `src/main/resources/application.properties` uses:

- `DB_HOST` (defaults to `localhost`)
- `DB_PORT` (defaults to `3306`)
- `DB_USERNAME` (required)
- `DB_PASSWORD` (required)

Create the database once in MySQL Workbench if it does not already exist:

```sql
CREATE DATABASE IF NOT EXISTS soss_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

Set `DB_USERNAME` and `DB_PASSWORD` in the environment used to launch Spring Boot; do not put credentials in source control. For local development, Hibernate is configured with `spring.jpa.hibernate.ddl-auto=update`, so it updates mapped tables on startup and the database user needs the corresponding schema privileges. The project currently has no separate Cart/Checkout migration script. Do not drop or recreate the database to run checkout. To complete a real checkout manually, the database must also contain a registered customer, at least one store and an active product with available inventory in the default store; these are application data and are not fabricated by checkout.

Automated cart/checkout tests use H2, including MySQL compatibility mode, and do not verify a developer's live MySQL credentials or existing schema. A manual local MySQL run is still needed to verify that setup.

## Tests

Relevant current test classes include:

- `src/test/java/com/soss/service/PromoCodesTest.java` — rounding, inclusive thresholds, stacked discount order/caps, duplicate/invalid codes, individual removal and eligibility after basket changes.
- `src/test/java/com/soss/Phase2CartCheckoutTest.java` — empty/stale baskets, promo stacking and recalculation, minimum-spend validation, malformed requests and server-side pricing.
- `src/test/java/com/soss/CheckoutIntegrationTest.java` — order/payment/stock integration, automatic delivery creation, checkout redirect, structured-address validation, transaction rollback, idempotency/concurrency, stacked totals/refund amount and reorder regressions.
- `src/test/java/com/soss/service/DeliveryAddressPolicyTest.java` — structured address formatting, supported-area checks and rejection of incomplete/placeholder input.
- `src/test/java/com/soss/Phase2AccountsTest.java` — saved-address ownership and profile synchronization.
- `src/test/java/com/soss/DeliveryConfirmationFlowTest.java` — delivery progress and customer confirmation integration.

Run the suite with `mvn test`. Test results depend on the current run; consult Maven output rather than treating an earlier test count as current.

## Deferred

Loyalty points are not implemented and remain deferred.
