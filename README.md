# KP QA Automation — Java + Selenium

## Prerequisites
- Java 17+
- Maven 3.8+
- Internet access (for WebDriverManager to download drivers). Works on Windows/Linux/macOS.

## Run
```bash
mvn -q -DBASE_URL=https://www.kupujemprodajem.com test
```
Specific tests:
```bash
# Scenario 1
mvn -q \
  -DBASE_URL=https://www.kupujemprodajem.com \
  -Dtest=SearchFiltersTest test
# Scenario 2 (requires a public ad URL)
mvn -q \
  -DBASE_URL=https://www.kupujemprodajem.com \
  -DKP_SAMPLE_AD_URL="https://www.kupujemprodajem.com/.../oglas/..." \
  -Dtest=AdresarLoginTest test
```

## Env/System properties
- `BASE_URL` (default KP URL)
- `DIRECT_URL` (optional direct category URL if nav text changes)
- `KP_SAMPLE_AD_URL` (required for Adresar login test)
- `BROWSER` = `chrome` (default) or `firefox`
