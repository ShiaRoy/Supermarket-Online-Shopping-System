# BulaBasket (SOSS)

BulaBasket is the CS241 Supermarket Online Shopping System (SOSS), a web-based grocery shopping application for customers in Suva, Fiji. It brings customer accounts, a product catalogue, store-specific inventory, cart and checkout, orders and simulated payments, and delivery tracking into one Spring Boot application.

## Technologies

- Java 25
- Spring Boot 3.5.16 and Maven
- Spring MVC/Web, Thymeleaf and Spring Security
- Spring Data JPA with Hibernate
- MySQL Connector/J for the application database
- Jakarta Bean Validation
- HTML, CSS and JavaScript; Bootstrap 5 CSS is loaded from jsDelivr by the templates
- Spring Boot Test, JUnit 5, Mockito and H2 for automated tests

The Maven coordinates are `com.soss:soss:0.0.1-SNAPSHOT`; the main Java package is `com.soss`. The application is packaged as a JAR.

## System features

### Authentication & Accounts

Registration, login/logout, profile editing, password change and password-reset requests are implemented. Passwords are stored as BCrypt hashes. Accounts can maintain saved delivery addresses and submit customer-support requests; Staff/Admin can review and reply to support queries. Reset email delivery requires SMTP configuration.

### Products & Inventory

The catalogue supports stores, products, details, search and filters. Inventory is tracked per store, with stock controls, low-stock thresholds/alerts and stock-movement history. Staff/Admin can manage products, stock, restocks and scheduled product promotions. Catalogue and checkout prices use active product promotions where applicable.

### Cart & Checkout

Authenticated customers get a cart on first visit. The cart supports quantity changes, current catalogue pricing and the stackable `BULA10`, `SAVE5` and `FREEDEL` codes. Checkout validates a current basket token, stock, supported Fiji delivery slots, a supported payment-method choice, and an owned saved/profile address or structured new address. Successful checkout coordinates order creation, simulated payment, inventory deduction and delivery creation transactionally.

### Orders & Payments

Customers can view their order and payment history, order confirmation, cancel eligible orders, and reorder previous items into their cart. Cancellation checks delivery progress, restores checkout-deducted inventory once, and records a simulated refund for an eligible successful payment. Admin has read-only order/payment oversight. Payment and refund records are application records only; no real payment service is connected.

### Delivery & Tracking

Staff/Admin can create deliveries and assign delivery personnel. Staff handle the preparation stage; assigned Delivery users progress their own deliveries. Customers can track their own orders and confirm receipt after delivery, which completes the order. Delivery progress, timestamps, failed attempts and cancellation state are recorded.

## Module documentation

- [Products and Inventory](PRODUCTS-AND-INVENTORY.md)
- [Cart and Checkout](CART-CHECKOUT.md)

## User roles

Authorization is enforced by Spring Security on the server as well as reflected in page navigation.

| Role | Main permissions |
| --- | --- |
| `CUSTOMER` | Browse products; manage cart and checkout; view own orders and payment history; cancel eligible own orders, reorder and track own deliveries; manage profile, addresses and support requests. |
| `STAFF` | Manage products and inventory; process the staff order queue; prepare deliveries and assign drivers; manage customer-support replies. Staff do not have customer cart/checkout access. |
| `ADMIN` | Admin dashboard and user/role management, plus authorized inventory, order/payment oversight, delivery and support operations. Admin order/payment oversight is read-only; refunds follow the Orders module's cancellation rules. |
| `DELIVERY` | View deliveries assigned to their account and perform permitted delivery progress actions. Delivery users cannot access customer shopping or manage unassigned deliveries. |

Public routes include the home page, product catalogue/details, login and registration. Registration creates a `CUSTOMER` account. There are no seeded accounts or credentials. The Admin management screen cannot create an Admin account; the group must provision its initial Admin account through its agreed secure setup process before using Admin-only pages.

## Installation and setup

### Prerequisites

- JDK 25
- Apache NetBeans with Maven project support and a Java 25 platform configured
- MySQL Server and MySQL Workbench
- Maven 3.6.3 or later, either installed or available through NetBeans

### Open in NetBeans

1. Ensure the project folder is in a writable development location. The project root is the folder containing `pom.xml`.
2. Configure the database environment variables below before launching NetBeans. Restart NetBeans after changing Windows user environment variables so the IDE inherits them.
3. In NetBeans, choose **File > Open Project** and select the existing project root. Do not create a new project.
4. Select JDK 25 as the project's Java Platform and ensure Maven also runs with JDK 25. Check with `mvn -version` if using a terminal.
5. Right-click the project and choose **Clean and Build**. Wait for `BUILD SUCCESS`.
6. Run the project from NetBeans, or use the Maven command below.

### Create and configure MySQL

In MySQL Workbench, connect to the intended MySQL server and create the application schema if needed:

```sql
CREATE DATABASE IF NOT EXISTS soss_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

Configure a MySQL account for the application and grant it access to `soss_db`, including the DDL privileges needed by the development schema-update setting. Supply its username and password through environment variables; do not place credentials in source files or commit them.

The application reads these database settings from `src/main/resources/application.properties`:

| Environment variable | Required | Default / purpose |
| --- | --- | --- |
| `DB_HOST` | No | `localhost` |
| `DB_PORT` | No | `3306` |
| `DB_USERNAME` | Yes | MySQL account name |
| `DB_PASSWORD` | Yes | MySQL account password |

The database name is `soss_db` and the web server uses port `8080`. Hibernate uses `spring.jpa.hibernate.ddl-auto=update` for development, but this does not replace the manual compatibility migration below. The repository contains no sample-data initializer: a real shopping checkout needs a registered customer, a store, products and available inventory.

### Existing database migration and inventory version check

The repository includes the manual, non-destructive MySQL compatibility script [`V2__payment_and_order_status_varchar.sql`](src/main/resources/db/manual/V2__payment_and_order_status_varchar.sql). For an existing database whose `payments.status` or `orders.status` columns are MySQL `ENUM` columns from an earlier version, select `soss_db` in Workbench and run this script before using refund and cancellation features. It changes those status columns to `VARCHAR` and preserves existing rows. The script is manual; Spring Boot does not automatically run it. A fresh schema uses the current JPA mappings, but check the actual schema before assuming an older database is compatible.

`Inventory.version` is a JPA optimistic-lock field. Rows created through the current application receive a version value. If upgrading an older database or importing inventory rows, check for legacy NULL versions after Hibernate has created/updated the column:

```sql
SELECT COUNT(*) AS null_inventory_versions
FROM inventory
WHERE version IS NULL;
```

If the count is nonzero, initialize only those missing version values once, before using inventory edits or checkout against those rows:

```sql
UPDATE inventory
SET version = 0
WHERE version IS NULL;
```

This conditional repair does not delete or replace inventory records. No separate inventory-version migration script is currently tracked. Do not drop or recreate `soss_db` to apply either change.

For password-reset email, optional SMTP settings are `APP_BASE_URL`, `MAIL_FROM`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_AUTH` and `MAIL_STARTTLS`. Configure them only if testing email delivery, and keep all credentials private. Mail is not required for the other application modules.

## Run the application

From the project root, with JDK 25 and the database environment variables available to the process:

```text
mvn spring-boot:run
```

Alternatively, build and run the packaged JAR:

```text
mvn clean package
java -jar target/soss-0.0.1-SNAPSHOT.jar
```

Open **http://localhost:8080** in a browser. Stop the process using NetBeans' Stop action or Ctrl+C in the terminal. No external Tomcat server is required.

## Testing

Run the complete test suite from NetBeans using the project's **Test** action, or from a terminal:

```text
mvn test
```

For a clean verification build:

```text
mvn clean verify
```

Tests use H2, often in MySQL compatibility mode, as configured in test properties. They do not validate the local MySQL credentials, execute the manual V2 migration against MySQL, or replace browser testing. Verify the migration and inventory-version condition against a backed-up local database, then manually exercise the app with MySQL before submission.

Current test coverage is organized in classes including `AuthenticationTest`, `AdminAccessTest`, `AdminManagementServiceTest`, `ProductsInventoryTest`, `ProductsPhaseTwoTest`, `Phase2AccountsTest`, `Phase2CartCheckoutTest`, `CheckoutIntegrationTest`, `OrderCancellationServiceTest`, `PaymentServiceTest`, `DeliveryServiceTest`, `DeliveryConfirmationFlowTest`, `WebFlowTest`, and `PromoCodesTest`, plus model and delivery-status tests. These cover authentication and role restrictions, product/inventory operations, cart/checkout and promotions, cancellation/refunds, payment history behavior and delivery workflows.

## Team contributions

The group allocation identifies these module responsibilities:

- **Shasmita:** Accounts and Authentication
- **Divit:** Products and Inventory
- **Vaibhav:** Cart and Checkout
- **Shreya:** Orders and Payments
- **Taniya:** Delivery and Tracking

This allocation describes module ownership; it does not claim individual contribution percentages or exclusive authorship of the integrated application.

## Limitations

- Payments, payment failures and refunds are simulated. The application does not connect to a bank, card network or mobile-money gateway.
- Delivery-address checks validate structure and supported areas, not a real-world address or map location. Current checkout areas are Suva and Nausori.
- Loyalty points are deferred and are not implemented.
- The development Hibernate schema-update setting and manual SQL script are not a production database migration/deployment strategy.

### AI Usage Acknowledgment

Generative AI tools were utilized as supplementary resources during the development of this project. Their use involved technical guidance, assistance with troubleshooting, and recommendations for documentation improvements. Relevant outputs were reviewed and evaluated, and the completed system was tested in accordance with the project requirements.