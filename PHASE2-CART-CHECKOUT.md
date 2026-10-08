# Phase 2 cart and checkout

Promo codes are trimmed and matched without case sensitivity. Only one code applies:

| Code | Discount | Minimum groceries subtotal |
| --- | --- | --- |
| BULA10 | 10% of groceries, rounded once to cents | Non-empty cart |
| SAVE5 | FJ$ 5 | FJ$ 50 inclusive |
| FREEDEL | Delivery fee | FJ$ 30 inclusive |

Blank input removes the code. An invalid replacement preserves the existing code.
Applying the same code does not invalidate the checkout token. Quantity changes
recalculate discounts and invalidate the token. If a cart falls below a minimum,
its preview shows zero discount and checkout requires adding groceries or removing
the code. Discounts do not stack. Delivery remains FJ$ 3 for a non-empty cart.

All displayed and saved totals use server product prices and BigDecimal cents:
`total = groceries subtotal + delivery fee - discount total`.
The shared order now stores the subtotal, delivery, discount and normalized code,
and confirmation displays them. Existing orders keep null breakdown fields; no
historical values are invented. The existing development `ddl-auto=update` setting
adds the four nullable columns to MySQL; managed environments must apply equivalent
schema changes before deployment.

Checkout checks token format, cart freshness, address, current Fiji delivery slots,
supported payment method, item availability, quantity bounds and promo eligibility
in the service as well as validating HTTP forms. The order adapter checks line and
summary arithmetic before persisting or invoking payment and stock changes.
Existing owner-scoped idempotency, customer locks and transaction rollback remain.
Payments retain the existing simulation behavior; this phase adds no live payments.

Run with Java 25: `mvn test`. Tests use H2 (including checkout in MySQL compatibility
mode); live MySQL verification requires a locally configured database. Regression
coverage includes code normalization/replacement/removal, exact minimum spends,
rounding, edited carts, malformed API/service submissions, ignored client totals,
persisted breakdowns, insufficient-stock rollback and concurrent duplicate checkout.

Loyalty is deferred so this change stays focused on the required Phase 2 checkout work.
