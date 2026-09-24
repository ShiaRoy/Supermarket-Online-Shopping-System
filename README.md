# SOSS — Supermarket Online Shopping System

Shared MAIN Maven project for all five group members. This is the foundation only:
one homepage, a configured MySQL datasource and empty architectural packages.
No authentication, business entities, sample data or shopping modules are implemented.

## Technology and prerequisites

- JDK **25** (not just a JRE).
- Spring Boot **3.5.16**, a stable release compatible with Java 25:
  https://docs.spring.io/spring-boot/3.5/system-requirements.html
- Maven **3.6.3+** (the Maven bundled with NetBeans can be used).
- MySQL **8.0+**, running locally by default, and MySQL Workbench.
- Apache NetBeans with Java/Maven support and support for JDK 25.
- Internet access for the initial Maven download and Bootstrap **5.3.8** CDN CSS.

Coordinates: `com.soss:soss:0.0.1-SNAPSHOT`; package: `com.soss`; packaging: JAR.
Spring Web MVC uses `spring-boot-starter-web`, with versions managed by the Spring Boot parent.
The only direct application dependencies are Web MVC, Thymeleaf, Spring Data JPA,
MySQL Connector/J and Validation. Spring Boot Test is included with test scope only. Spring Security is not included.

## 1. Put the project in a normal development directory

Extract or clone the whole `soss` folder into a writable location such as
`C:\Users\<you>\Documents\NetBeansProjects\soss`.
Do not work inside the NetBeans installation directory under Program Files.
The project root is the folder containing `pom.xml`.

## 2. Create the database with MySQL Workbench

1. Open Workbench and connect to your local MySQL server using an administrator account.
2. Open a new SQL tab and execute:

```sql
CREATE DATABASE IF NOT EXISTS soss_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
```

3. Refresh the Schemas panel; `soss_db` should appear.
4. In **Server > Users and Privileges**, add a local development user named
   `soss_app`, host `localhost`, and choose your own password.
   Under **Schema Privileges**, add `soss_db` and grant SELECT, INSERT, UPDATE,
   DELETE, CREATE, ALTER, DROP, INDEX and REFERENCES, then Apply.
   These DDL privileges allow Hibernate's development schema updates.
   No global administrator privileges are needed.
5. Create a separate Workbench connection for this user. Test that it connects.

Keep the chosen password local; never put it in source code or Git.
This base has no entities, so an empty schema with no application tables is expected.

## 3. Set environment variables

| Variable | Required | Meaning / default |
| --- | --- | --- |
| `DB_USERNAME` | Yes | Your local MySQL user, e.g. `soss_app` |
| `DB_PASSWORD` | Yes | That user's password; no default |
| `DB_HOST` | No | `localhost` |
| `DB_PORT` | No | `3306` |
| `JAVA_HOME` | Recommended | JDK 25 installation directory |

The database name is fixed to `soss_db`; the HTTP port is `8080`.

For NetBeans on Windows: search for **Edit environment variables for your account**,
open **Environment Variables**, and add DB_USERNAME and DB_PASSWORD under **User variables**.
Set JAVA_HOME to your JDK 25 directory. Close all NetBeans instances and reopen the IDE
so it inherits the new variables. If using a launcher that was already open, restart it
or sign out and back in.

Alternatively, set variables in the same PowerShell session that launches Maven.
This prompts for the password without placing it in command history:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.2' # Adjust for your machine.
$env:DB_USERNAME = 'soss_app'
$sossCredential = Get-Credential -UserName $env:DB_USERNAME -Message 'Local SOSS MySQL credentials'
$env:DB_PASSWORD = $sossCredential.GetNetworkCredential().Password
mvn -version
mvn spring-boot:run
```

Check that `mvn -version` reports Java **25**. A JDK 25 `java -version`
alone is insufficient: Maven may use a different JAVA_HOME.
If Maven is not on PATH, replace `mvn` with the full path, for example:

```powershell
& 'C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd' -version
& 'C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd' spring-boot:run
```

Spring Boot does **not** automatically load a `.env` file. Set actual process/user
environment variables. Do not add passwords to pom.xml, nbactions.xml or properties files.

## 4. Open and run in NetBeans

1. Set the environment variables above and restart NetBeans.
2. Choose **Tools > Java Platforms > Add Platform > Java Standard Edition**.
   Select your JDK 25 installation and finish.
3. Choose **File > Open Project** and select the `soss` folder containing
   `pom.xml`. Open it as an existing Maven project; do not create another project.
4. Right-click **SOSS > Properties > Build > Compile** and select the JDK 25
   Java Platform (wording can vary by NetBeans version). The POM already sets
   the Java release to 25. Save.
5. Under **Tools > Options > Java > Maven**, use NetBeans' bundled Maven or
   a Maven 3.6.3+ installation.
6. Right-click **SOSS > Clean and Build**. Wait for **BUILD SUCCESS**.
7. Right-click **SOSS > Run** (or set it as the main project and press F6).
   The tracked `nbactions.xml` maps Run to `spring-boot:run`.
   No external Tomcat or application server is needed.
   If your NetBeans version does not load the mapping, use
   **Run Maven > Goals** and enter `spring-boot:run`.
8. In Output, wait for `Started SossApplication` and Tomcat on port 8080.
9. Visit **http://localhost:8080**.
10. Stop the application with the Stop button in NetBeans' Output window
    (Ctrl+C for a terminal run).

If compilation reports "release version 25 not supported", the build is running
with an older JDK: recheck the project Java Platform and JAVA_HOME.

CLI alternatives, from the project root:

```text
mvn clean verify
mvn spring-boot:run
```

Or build and run the executable JAR (with the same environment variables):

```text
mvn clean package
java -jar target/soss-0.0.1-SNAPSHOT.jar
```

## 5. Verify the homepage and database

- Open http://localhost:8080. Expect the green navbar with **SOSS** and
  **Supermarket Online Shopping System**, plus **Welcome to SOSS**.
- Open http://localhost:8080/css/style.css. It should return the custom stylesheet.
- Bootstrap CSS loads from jsDelivr. If external internet access is blocked,
  the HTML still renders, but the complete Bootstrap styling will be unavailable.
- Check the application log for Hikari adding a MySQL connection, successful
  EntityManagerFactory initialization, and `Started SossApplication`.
  There should be no authentication, communication or schema errors.
- With the same DB_USERNAME account in Workbench, run:

```sql
USE soss_db;
SELECT DATABASE() AS database_name, CURRENT_USER() AS authenticated_account;
SHOW TABLES;
```

The database name must be `soss_db`. No application tables exist yet.
A Workbench connection alone does not prove the application connected; check both
the Workbench result and the application's startup log.
No database status endpoint or fake entity has been added.

Common setup errors:

| Error | What to check |
| --- | --- |
| Missing DB_USERNAME / DB_PASSWORD | Variables must be inherited by the running IDE/process |
| Access denied for user | Username, password, account host and schema grants |
| Unknown database soss_db | Create the schema on the same server/port used by the app |
| Communications link failure | MySQL service, DB_HOST, DB_PORT and firewall |
| Public Key Retrieval is not allowed | Configure MySQL TLS or your trusted server RSA key; do not casually disable connection security |
| Port 8080 already in use | Stop the other process or earlier SOSS instance |
| Maven download failure | Network access, Maven proxy settings and repository access |

The standard application requires a reachable MySQL database and valid credentials.
There is no fallback database or datasource-disabled development mode.

## Structure and team conventions

```text
soss/
├── pom.xml
├── nbactions.xml
├── .gitignore
├── README.md
└── src/main/
    ├── java/com/soss/
    │   ├── SossApplication.java
    │   ├── controller/HomeController.java
    │   ├── service/package-info.java
    │   ├── repository/package-info.java
    │   ├── model/package-info.java
    │   └── config/package-info.java
    └── resources/
        ├── application.properties
        ├── templates/index.html
        └── static/
            ├── css/style.css
            ├── js/.gitkeep
            └── images/.gitkeep
```

Business request flow: **Controller → Service → Repository → MySQL**.

Controllers handle HTTP and validation, services own business rules and transaction
boundaries, repositories handle persistence, model holds domain entities, and config
holds shared configuration. Use constructor injection. Controllers must not directly
call repositories. The homepage only returns a view and needs no service or repository.

Keep new classes below `com.soss` so Spring discovers them. For larger modules,
use matching subpackages in each layer, e.g. `controller.product`,
`service.product`, `repository.product`. Use `jakarta.persistence` and
`jakarta.validation` imports when implementing entities and validation.

Planned module ownership (assign one member to each):

1. Authentication & User Accounts
2. Products & Inventory
3. Cart & Checkout
4. Orders & Payments
5. Delivery & Tracking

Reserved shared entity class names:
`User`, `Product`, `Store`, `Inventory`, `Cart`, `CartItem`,
`Order`, `OrderItem`, `Payment`, `Delivery`.

Agree on IDs, fields, relationships and service contracts before adding shared entities.
Create each entity only once and agree on ownership across modules.
Use explicit nonreserved table names such as `users` and `orders` when those
entities are implemented; SQL names such as ORDER can conflict with keywords.

Use one shared repository and main branch, feature branches per module, and reviewed
merges. Coordinate edits to pom.xml, application.properties and shared model classes.
Keep templates and static assets grouped by module when features are added.
Do not add empty business classes, placeholder endpoints or sample supermarket data.

Development settings: `ddl-auto=update`, SQL logging enabled, template cache disabled
and Open Session in View disabled. Services should load the data a page needs within
a transaction. Coordinate schema changes; later introduce reviewed migrations and
production configuration before deployment. The development schema-update setting
is not a production migration strategy.

The .gitignore excludes builds, IDE-local settings, environment files, credentials,
database dumps and logs. It deliberately tracks the shared NetBeans Run action.
Ignoring files does not remove secrets already committed: never commit them.

No automated business tests are included because no business behavior exists yet.
`mvn clean verify` checks compilation and packaging; it does not verify MySQL
connectivity or the running homepage. Add meaningful tests with future modules.

