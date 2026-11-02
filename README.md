# UCO Realty

Simplified UCO Bank Approved Housing Project Portal.
Built on **Java 1.8**, **Spring Boot 2.7.18**, and standard **JSP** (`WEB-INF/jsp/`).
Single executable WAR running on port 8080 with zero Node.js / React dependencies.

## Key Features
- **Public Customer Portal (`/projects`):** Search and filter approved projects by State and City, view project specifications, photos, and submit Home Loan inquiries.
- **Maker-Checker Workflow (`/admin/projects`):** Zone Makers create project drafts and upload photos; Zone Checkers review, approve, reject, or return for correction.
- **Inventory Control:** Head Office Retail can reduce unsold units when flats are booked.
- **User Management (`/admin/users`):** DIT Superadmin and HO Admin manage staff accounts and roles.
- **Lead Management (`/admin/leads`):** View customer leads and export to Excel (`.xlsx`).

---

## Running Locally (CLI)
Requires Docker (for local Oracle container), Java 8 (or 11/17 in Java 8 target mode), and Maven.

```bash
scripts/dev-start.sh          # Starts Oracle, applies schema, creates admins, launches Spring Boot on 8080
scripts/dev-stop.sh           # Stops the application
scripts/dev-start.sh --reset  # Wipes database and restarts from scratch
```

### URLs
- **Public Portal:** http://localhost:8080/projects
- **Staff Login:** http://localhost:8080/login
- **Maker / Checker Dashboard:** http://localhost:8080/admin/projects
- **User Management:** http://localhost:8080/admin/users
- **Leads & Export:** http://localhost:8080/admin/leads

---

## Running in Eclipse IDE
To run directly in Eclipse (identical to your standard bank workspace like `emailSender`):

1. **Import Project:**
   - Open Eclipse.
   - Select **File** -> **Import...** -> **Existing Maven Projects**.
   - Browse to the project root folder (where `pom.xml` is located) and click **Finish**.
2. **Java Runtime:**
   - Right-click project -> **Build Path** -> **Configure Build Path...**.
   - Verify **JRE System Library** is set to **JavaSE-1.8**.
3. **Run Application:**
   - Right-click `com.ucorealty.UcoRealtyApplication.java`.
   - Select **Run As** -> **Java Application** (or **Spring Boot App**).
   - Set Environment Variable / VM Argument: `-Dspring.profiles.active=dev`.
   - Access http://localhost:8080/projects in your browser.

---

## Testing & Packaging
```bash
# Run unit tests
mvn test

# Build executable WAR
mvn package -DskipTests

# Package release bundle (for DBA & deployment)
scripts/package-release.sh [uat|prod]
```
