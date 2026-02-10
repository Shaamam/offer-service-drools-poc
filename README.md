# Offer Service with Hot-Reloadable Drools Rules - POC

A Spring Boot microservice that evaluates offers using **Drools KJAR rules engine** with **automatic hot-reloading** capabilities using KieScanner.

## 🚀 Features

- ✅ **REST API** for offer evaluation
- ⚡ **Hot-reloading** of rules via KieScanner (no restart needed!)
- 🔧 **Drools 8.44.0** rules engine
- 🎯 **StatelessKieSession** for thread-safe rule execution
- 📊 **Spring Boot Actuator** for health monitoring
- 🔍 **Detailed logging** of rule execution

## 📋 Architecture

```
Offer Service (Spring Boot)
├── REST API (/api/offers/evaluate)
├── OfferService (business logic)
├── KieContainerManager (rules management)
│   ├── KieContainer (loaded rules)
│   ├── KieScanner (auto hot-reload)
│   └── StatelessKieSession Factory
└── KJAR Dependency (io.shaama:rules-engine-kjar)
```

## 🛠️ Tech Stack

- **Spring Boot 3.5.10** with Java 21
- **Drools 8.44.0.Final** for rules engine
- **Gradle** for dependency management
- **Lombok** for cleaner code
- **Spring Boot Actuator** for monitoring

## ⚙️ Configuration

The service is configured in `application.properties`:

```properties
# Rules Engine Configuration
rules.engine.groupId=io.shaama
rules.engine.artifactId=rules-engine-kjar
rules.engine.version=1.0.0-SNAPSHOT
rules.engine.session-name=offerKSession
rules.engine.auto-reload=true
rules.engine.scan-interval-seconds=10  # KieScanner polls every 10 seconds
```

### How KieScanner Hot-Reload Works

1. **KieScanner** uses `LATEST` version to automatically poll the local Maven repository (`~/.m2/repository/`)
2. Checks every **10 seconds** for the newest SNAPSHOT version
3. When a new timestamp is detected on the KJAR, **automatically reloads** rules
4. Always picks up the latest version - no need to update version numbers
5. No manual endpoint call needed
6. No service restart required

**Key Point:** The service uses `LATEST` internally for KieScanner polling, which means it will automatically detect and load any new SNAPSHOT builds you publish to your local Maven repo.

## 🚦 Getting Started

### Prerequisites

1. **Java 21** installed
2. **Gradle** installed
3. **rules-engine-kjar** KJAR published to local Maven repo

### Step 1: Build and Install KJAR

First, build and publish the rules KJAR to your local Maven repository:

```bash
# Navigate to your rules-engine project
cd /path/to/rules-engine-kjar

# Build and publish to local Maven repo
./gradlew clean build publishToMavenLocal

# Verify KJAR is installed
ls ~/.m2/repository/io/shaama/rules-engine-kjar/1.0.0-SNAPSHOT/
```

### Step 2: Build Offer Service

```bash
# Navigate to offer service project
cd /path/to/offer-service-drools-poc

# Build the project
./gradlew clean build

# Run the service
./gradlew bootRun
```

The service will start on **http://localhost:8080**

### Step 3: Test with HTTP Files

Use the `.http` files in the `requests/` folder to test the API directly in VS Code:

```
requests/offer-api.http  # Contains all API test requests
```

Open the file and click "Send Request" above any request block, or use the REST Client extension.

### Step 4: Verify Startup

Check the logs for successful initialization:

```
✓ Rules loaded successfully from KJAR
  - KBase names: [offerKBase]
  - KSession names: [offerKSession]
  
✓ KieScanner started successfully
  ⚡ Rules will hot-reload automatically when KJAR is updated in Maven repo
```

## 📡 API Endpoints

### 1. Evaluate Offer

**POST** `/api/offers/evaluate`

Evaluates an offer based on customer and order details.

**Request Body:**
```json
{
  "offerId": "OFF-001",
  "customerId": "CUST-001",
  "customerSegment": "PREMIUM",
  "orderAmount": 1500.00,
  "productCategory": "ELECTRONICS",
  "isFirstTimeCustomer": false,
  "offerValidUntil": "2026-12-31"
}
```

**Response:**
```json
{
  "offerId": "OFF-001",
  "offerApplicable": true,
  "discountPercentage": 20,
  "discountAmount": 300.00,
  "appliedOfferType": "PREMIUM_CUSTOMER",
  "finalAmount": 1200.00,
  "rejectionReason": null
}
```

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/offers/evaluate \
  -H "Content-Type: application/json" \
  -d '{
    "offerId": "TEST-001",
    "customerId": "CUST-123",
    "customerSegment": "PREMIUM",
    "orderAmount": 1500,
    "productCategory": "ELECTRONICS",
    "isFirstTimeCustomer": false,
    "offerValidUntil": "2026-12-31"
  }'
```

### 2. Get Rules Information

**GET** `/api/offers/rules-info`

Returns information about currently loaded rules.

**Response:**
```json
{
  "releaseId": "io.shaama:rules-engine-kjar:1.0.0-SNAPSHOT",
  "groupId": "io.shaama",
  "artifactId": "rules-engine-kjar",
  "version": "1.0.0-SNAPSHOT",
  "sessionName": "offerKSession",
  "autoReloadEnabled": true,
  "scanIntervalSeconds": 10,
  "kieBaseNames": "[offerKBase]",
  "status": "Active"
}
```

**cURL Example:**
```bash
curl http://localhost:8080/api/offers/rules-info
```

### 3. Health Check

**GET** `/api/offers/health`

Checks if the rules engine is active.

**Response:**
```json
{
  "status": "UP",
  "rulesEngine": "Active"
}
```

## 🔥 Testing Hot-Reload

### Scenario 1: Change Discount Percentage

**Initial State:**
```drl
rule "Premium Customer Discount"
when
    $offer : Offer(customerSegment == "PREMIUM", orderAmount > 1000)
then
    $offer.setApplicable(true);
    $offer.setDiscountPercentage(20);  // 20% discount
    $offer.setAppliedOfferType("PREMIUM_CUSTOMER");
end
```

**Step 1:** Test with initial rules
```bash
curl -X POST http://localhost:8080/api/offers/evaluate \
  -H "Content-Type: application/json" \
  -d '{
    "offerId": "TEST-001",
    "customerId": "CUST-001",
    "customerSegment": "PREMIUM",
    "orderAmount": 1500,
    "isFirstTimeCustomer": false
  }'

# Expected: 20% discount (300), finalAmount = 1200
```

**Step 2:** Modify the rule
```drl
rule "Premium Customer Discount"
when
    $offer : Offer(customerSegment == "PREMIUM", orderAmount > 1000)
then
    $offer.setApplicable(true);
    $offer.setDiscountPercentage(25);  // Changed to 25%!
    $offer.setAppliedOfferType("PREMIUM_CUSTOMER");
end
```

**Step 3:** Rebuild and republish KJAR
```bash
cd /path/to/rules-engine-kjar
./gradlew clean build publishToMavenLocal
```

**Step 4:** Wait for KieScanner (10 seconds)
Watch the logs for:
```
KieScanner detected new rules version
Reloading rules from KJAR...
✓ Rules reloaded successfully
```

**Step 5:** Test again with same request
```bash
# Same request as Step 1
curl -X POST http://localhost:8080/api/offers/evaluate ...

# Expected: 25% discount (375), finalAmount = 1125
# ✓ Rules changed without restart!
```

### Scenario 2: Add New Rule for First-Time Customers

**Step 1:** Add new rule in `offer-rules.drl`
```drl
rule "First Time Customer Welcome Offer"
salience 10
when
    $offer : Offer(isFirstTimeCustomer == true, orderAmount > 500)
then
    $offer.setApplicable(true);
    $offer.setDiscountPercentage(15);
    $offer.setDiscountAmount($offer.getOrderAmount().multiply(new BigDecimal("0.15")));
    $offer.setAppliedOfferType("FIRST_TIME_CUSTOMER");
end
```

**Step 2:** Rebuild and republish
```bash
cd /path/to/rules-engine-kjar
./gradlew clean build publishToMavenLocal
```

**Step 3:** Wait 10 seconds for KieScanner

**Step 4:** Test new rule
```bash
curl -X POST http://localhost:8080/api/offers/evaluate \
  -H "Content-Type: application/json" \
  -d '{
    "offerId": "NEW-CUST-001",
    "customerId": "FIRST-BUYER",
    "orderAmount": 600,
    "isFirstTimeCustomer": true
  }'

# Expected: 15% discount (90), finalAmount = 510
# ✓ New rule applied automatically!
```

### Scenario 3: Change Threshold Conditions

**Initial Rule:**
```drl
rule "Electronics Category Discount"
when
    $offer : Offer(productCategory == "ELECTRONICS", orderAmount > 2000)
then
    $offer.setApplicable(true);
    $offer.setDiscountPercentage(10);
end
```

**Test with amount = 1800 → No discount (below threshold)**

**Change Rule:**
```drl
rule "Electronics Category Discount"
when
    $offer : Offer(productCategory == "ELECTRONICS", orderAmount > 1500)  // Lowered!
then
    $offer.setApplicable(true);
    $offer.setDiscountPercentage(10);
end
```

**Rebuild, wait 10 seconds, test again → Discount now applies!**

## 📊 Monitoring

### Check Application Logs

```bash
# Watch logs in real-time
tail -f logs/offer-service.log

# Key log messages:
# - "Rules loaded successfully from KJAR"
# - "KieScanner started successfully"
# - "Evaluating offer: OFF-001 for customer: CUST-001"
# - "Rules executed. Applicable: true, Discount: 20%"
```

### Actuator Endpoints

```bash
# Health check
curl http://localhost:8080/actuator/health

# Metrics
curl http://localhost:8080/actuator/metrics
```

## 🧪 Complete Test Workflow

```bash
# Terminal 1: Start the service
cd offer-service-drools-poc
./gradlew bootRun

# Terminal 2: Test initial rules
curl -X POST http://localhost:8080/api/offers/evaluate \
  -H "Content-Type: application/json" \
  -d '{"offerId":"TEST-001","customerSegment":"PREMIUM","orderAmount":1500}'

# Terminal 3: Modify rules and republish
cd rules-engine-kjar
# Edit src/main/resources/rules/offer-rules.drl
./gradlew clean build publishToMavenLocal

# Terminal 2: Wait 10 seconds, then test again
sleep 10
curl -X POST http://localhost:8080/api/offers/evaluate \
  -H "Content-Type: application/json" \
  -d '{"offerId":"TEST-001","customerSegment":"PREMIUM","orderAmount":1500}'

# ✓ See the difference without restarting!
```

## 🎯 Key Implementation Details

### KieScanner Auto-Reload

The `KieContainerManager` uses **KieScanner** for automatic hot-reloading:

```java
kieServices.newKieScanner(kieContainer).start(scanIntervalSeconds * 1000L);
```

- **Polls** local Maven repository every 10 seconds
- **Detects** new KJAR versions automatically
- **Reloads** KieContainer with zero downtime
- **Thread-safe** - uses volatile KieContainer reference

### StatelessKieSession

The service uses **StatelessKieSession** for rule execution:

```java
StatelessKieSession session = kieContainerManager.getOfferSession();
session.execute(offer);
```

**Benefits:**
- ✅ Thread-safe (no shared state)
- ✅ No memory leaks
- ✅ Fast execution
- ✅ Perfect for stateless evaluations

## 🐛 Troubleshooting

### Issue: Rules not loading

**Check:**
1. KJAR is in local Maven repo: `ls ~/.m2/repository/io/shaama/rules-engine-kjar/`
2. Version matches in `application.properties`
3. Logs show "Rules loaded successfully"

### Issue: Hot-reload not working

**Check:**
1. `rules.engine.auto-reload=true` in config
2. KieScanner started successfully (check logs)
3. Wait at least 10 seconds after republishing KJAR
4. KJAR timestamp updated in Maven repo

### Issue: KieSession not found

**Error:** `Could not find stateless KieSession 'offerKSession'`

**Solution:**
1. Check `kmodule.xml` in KJAR has session named `offerKSession`
2. Verify session name matches in `application.properties`

## 📦 Project Structure

```
offer-service-drools-poc/
├── src/main/java/io/shaama/offerservice/
│   ├── config/
│   │   └── KieContainerManager.java      # KieScanner hot-reload manager
│   ├── controller/
│   │   └── OfferController.java          # REST endpoints
│   ├── model/
│   │   ├── Offer.java                    # Domain model for rules
│   │   ├── OfferRequest.java             # API request DTO
│   │   └── OfferResponse.java            # API response DTO
│   ├── service/
│   │   └── OfferService.java             # Business logic
│   └── exception/
│       └── GlobalExceptionHandler.java   # Error handling
├── src/main/resources/
│   └── application.properties            # Configuration
├── build.gradle                          # Dependencies
└── README.md                             # This file
```

## ✅ Success Criteria

- [x] ✅ Offer service evaluates offers using KJAR rules
- [x] ⚡ Rules hot-reload automatically via KieScanner
- [x] 🚀 No manual reload endpoint needed
- [x] 🔄 No service restart required
- [x] 📝 All test scenarios pass
- [x] 🔍 Detailed logging of rule execution
- [x] ⏱️ Fast response times (< 100ms)
- [x] 🧵 Thread-safe implementation

## 🚀 Next Steps

1. **Add Rule Versioning**: Track which rule version evaluated each offer
2. **Add A/B Testing**: Run multiple rule versions side-by-side
3. **Add Rule Audit**: Store rule execution history
4. **Add Monitoring**: Metrics for rule execution time
5. **Dockerize**: Container support with volume mounts

## 📝 Notes

- KieScanner polls **every 10 seconds** by default (configurable)
- Uses **StatelessKieSession** for thread safety
- KJAR must be in local Maven repo for hot-reload
- Logs show detailed rule firing information
- No manual reload endpoint needed - it's automatic! ⚡
