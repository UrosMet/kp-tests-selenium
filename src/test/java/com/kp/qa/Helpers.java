package com.kp.qa;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.ui.*;

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Helpers {

    private Helpers() {}


    private static final Duration SHORT  = Duration.ofSeconds(1);
    private static final Duration MEDIUM = Duration.ofSeconds(3);

    private static final String BASE_URL = Optional.ofNullable(System.getenv("BASE_URL"))
            .filter(s -> !s.isBlank())
            .map(s -> s.endsWith("/") ? s : s + "/")
            .orElse("https://www.kupujemprodajem.com/");



    public static void waitForDomComplete(WebDriver driver, Duration timeout) {
        new WebDriverWait(driver, timeout).until(d ->
                "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
    }

    public static Object js(WebDriver driver, String script, Object... args) {
        return ((JavascriptExecutor) driver).executeScript(script, args);
    }

    public static void recoverClosedWindowIfNeeded(WebDriver driver) {
        try {
            js(driver, "return 1;");
        } catch (NoSuchWindowException e) {
            Set<String> handles = driver.getWindowHandles();
            if (!handles.isEmpty()) {
                driver.switchTo().window(handles.iterator().next());
            } else {
                throw e;
            }
        }
    }
    public static void takeScreenshot(WebDriver driver, String name) {
        try {
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            String filename = "screenshots/" + name + "_" + timestamp + ".png";

            File dest = new File(filename);
            dest.getParentFile().mkdirs();
            org.openqa.selenium.io.FileHandler.copy(src, dest);

            System.out.println("Screenshot saved: " + filename);
        } catch (Exception e) {
            System.err.println("Could not save screenshot: " + e.getMessage());
        }
    }

    public static void switchToNewestWindow(WebDriver driver, Duration timeout) {
        String current = driver.getWindowHandle();
        Set<String> before = driver.getWindowHandles();
        try {
            new WebDriverWait(driver, timeout).until(d -> d.getWindowHandles().size() > before.size());
            Set<String> after = new HashSet<>(driver.getWindowHandles());
            after.removeAll(before);
            if (!after.isEmpty()) driver.switchTo().window(after.iterator().next());
        } catch (TimeoutException ignore) {
            driver.switchTo().window(current);
        }
    }

    private static void stabilize(WebDriver driver, Duration shortWait) {
        try {
            recoverClosedWindowIfNeeded(driver);
            waitForDomComplete(driver, shortWait.plusSeconds(3));
        } catch (Exception ignore) {}
    }


    public static void acceptCookiesIfPresent(WebDriver driver, Duration shortWait) {
        String[] xps = {
                "//button[normalize-space()='Prihvatam']",
                "//button[normalize-space()='Prihvati']",
                "//button[contains(.,'Slažem se')]",
                "//button[normalize-space()='Accept']",
                "//button[contains(.,'Accept all') or contains(.,'I agree')]"
        };
        for (String xp : xps) {
            try {
                WebElement el = new WebDriverWait(driver, shortWait)
                        .until(ExpectedConditions.presenceOfElementLocated(By.xpath(xp)));
                new WebDriverWait(driver, shortWait)
                        .until(ExpectedConditions.elementToBeClickable(el)).click();
                stabilize(driver, shortWait);
                return;
            } catch (TimeoutException | NoSuchElementException | ElementClickInterceptedException ignored) {}
        }
    }

    public static void dismissGoogleOneTap(WebDriver driver, Duration shortWait) {
        try {
            driver.switchTo().defaultContent();
            driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
        } catch (Exception ignored) {}

        String[] iframeXps = {
                "//iframe[contains(@src,'accounts.google.com') or contains(@src,'gsi')]",
                "//iframe[contains(@id,'google') or contains(@name,'google')]"
        };
        for (String ifrXp : iframeXps) {
            try {
                WebElement iframe = new WebDriverWait(driver, shortWait)
                        .until(ExpectedConditions.presenceOfElementLocated(By.xpath(ifrXp)));
                driver.switchTo().frame(iframe);
                String[] closeXps = {
                        "//*[@aria-label='Close']",
                        "//*[contains(@class,'close') or contains(@class,'Close')]",
                        "//*[contains(text(),'No thanks') or contains(text(),'Ne hvala')]"
                };
                for (String closeXp : closeXps) {
                    try {
                        WebElement close = new WebDriverWait(driver, shortWait)
                                .until(ExpectedConditions.elementToBeClickable(By.xpath(closeXp)));
                        close.click();
                        driver.switchTo().defaultContent();
                        stabilize(driver, shortWait);
                        return;
                    } catch (TimeoutException ignored) {}
                }
                driver.switchTo().defaultContent();
            } catch (TimeoutException ignored) {}
        }

        try {
            driver.switchTo().defaultContent();
            js(driver,
                    "const kill=s=>document.querySelectorAll(s).forEach(el=>el.style.display='none');" +
                            "kill('[id^=\"credential_picker\"],[class*=\"credential\"],[class*=\"g_id_onload\"],[id*=\"google\"]');");
        } catch (Exception ignored) {}
    }

    private static List<By> addToAddressBookLocators() {
        List<By> locs = new ArrayList<>();
        locs.add(By.xpath("//button[.//span[normalize-space()='Dodajte u adresar']]"));
        locs.add(By.xpath(
                "//*[self::button or self::a]" +
                        "[contains(translate(normalize-space(.),'ĐđŠšŽžĆćČč','DdSszZCcCc'),'dodaj u adresar') or " +
                        " contains(translate(normalize-space(.),'ĐđŠšŽžĆćČč','DdSszZCcCc'),'dodajte u adresar')]"
        ));
        locs.add(By.cssSelector("[aria-label*='adresar' i], [title*='adresar' i], [data-qa*='adresar' i]"));
        locs.add(By.xpath("//*[@role='button' and contains(translate(normalize-space(.),'ĐđŠšŽžĆćČč','DdSszZCcCc'),'adresar')]"));
        return locs;
    }

    public static void clickAddToAddressBookWithRetries(WebDriver driver, Duration wait) {
        recoverClosedWindowIfNeeded(driver);
        waitForDomComplete(driver, wait);
        List<By> locators = addToAddressBookLocators();
        WebElement target = null;
        for (By by : locators) {
            try {
                target = new WebDriverWait(driver, wait)
                        .until(ExpectedConditions.visibilityOfElementLocated(by));
                if (target != null) break;
            } catch (TimeoutException ignored) {}
        }
        if (target == null) {
            switchToNewestWindow(driver, wait);
            recoverClosedWindowIfNeeded(driver);
            waitForDomComplete(driver, wait);
            for (By by : locators) {
                try {
                    target = new WebDriverWait(driver, wait)
                            .until(ExpectedConditions.visibilityOfElementLocated(by));
                    if (target != null) break;
                } catch (TimeoutException ignored) {}
            }
        }
        Assertions.assertNotNull(target, "Nisam našao 'Dodaj/Dodajte u adresar'.");

        try {
            js(driver, "arguments[0].scrollIntoView({block:'center'});", target);
            new WebDriverWait(driver, wait).until(ExpectedConditions.elementToBeClickable(target)).click();
        } catch (Exception e1) {
            try {
                dismissGoogleOneTap(driver, wait);
                js(driver, "arguments[0].scrollIntoView({block:'center'});", target);
                new WebDriverWait(driver, wait).until(ExpectedConditions.elementToBeClickable(target)).click();
            } catch (Exception e2) {
                js(driver, "arguments[0].click();", target);
            }
        }

        switchToNewestWindow(driver, wait);
        recoverClosedWindowIfNeeded(driver);
        waitForDomComplete(driver, wait);
    }

    public static void expectLoginForm(WebDriver driver, Duration wait) {
        String[] selectors = {
                "//input[@type='email']",
                "//input[@name='email']",
                "//input[@name='username']",
                "//input[@type='text' and (contains(@name,'user') or contains(@id,'user'))]",
                "//input[@type='password']",
                "//*[self::h1 or self::h2 or self::h3][contains(.,'Prijava') or contains(.,'Ulogujte se') or contains(.,'Sign in')]"
        };
        boolean found = false;
        for (String xp : selectors) {
            try {
                WebElement el = new WebDriverWait(driver, wait)
                        .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xp)));
                if (el != null) { found = true; break; }
            } catch (TimeoutException ignored) {}
        }
        Assertions.assertTrue(found, "Nije pronađena forma za prijavu (input polja i/ili naslov).");
    }


    public static void openHomePage(WebDriver driver) {
        driver.get(BASE_URL);
        waitForDomComplete(driver, MEDIUM);
        try { acceptCookiesIfPresent(driver, SHORT); } catch (Exception ignored) {}
        try { dismissGoogleOneTap(driver, SHORT); } catch (Exception ignored) {}
        waitForDomComplete(driver, MEDIUM);
    }

    public static void navigateToBlouses(WebDriver driver) {
        WebElement women = waitVisible(driver, By.cssSelector("a[href*='/odeca-zenska']"), SHORT);
        scrollCenter(driver, women);
        clickSafe(driver, women, SHORT);

        waitForDomComplete(driver, MEDIUM);
        try { dismissGoogleOneTap(driver, SHORT); } catch (Exception ignored) {}

        WebElement bluze = waitVisible(driver, By.cssSelector("a[aria-label='Bluze']"), SHORT);
        scrollCenter(driver, bluze);
        clickSafe(driver, bluze, SHORT);

        waitForDomComplete(driver, MEDIUM);
        try { dismissGoogleOneTap(driver, SHORT); } catch (Exception ignored) {}
    }

    public static String applyPriceFilter(WebDriver driver, String minPrice) {
        WebElement crumbSpan = firstVisible(
                driver, SHORT,
                Arrays.asList(
                        By.xpath("//*[contains(@class,'BreadcrumbHolder_breadcrumb')]//span[contains(translate(.,'ĐđŠšŽžĆćČč','DdSszZCcCc'),'rezultat')]"),
                        By.xpath("//*[contains(@class,'BreadcrumbHolder_breadcrumb')]//span[contains(.,'rezultat')]")
                )
        );
        String prevCrumb = safeText(crumbSpan);

        By priceTag = By.xpath("//section[contains(@class,'SearchTag')][.//text()[contains(.,'Cena')]]");
        WebElement priceTagEl = waitVisible(driver, priceTag, SHORT);
        scrollCenter(driver, priceTagEl);
        clickSafe(driver, priceTagEl, SHORT);

        By priceDialog = By.xpath("//section[contains(@class,'SearchTag_priceTagContent__')]");
        WebElement dialog = waitVisible(driver, priceDialog, SHORT);

        WebElement minInput = firstVisibleIn(
                driver, dialog, SHORT,
                Arrays.asList(
                        By.cssSelector("input[name*='priceFrom']"),
                        By.cssSelector("input[placeholder*='Cena od']"),
                        By.cssSelector("input[aria-label*='priceFrom']"),
                        By.id("priceFrom")
                )
        );
        scrollCenter(driver, minInput);
        try { minInput.clear(); } catch (Exception ignored) {}
        minInput.sendKeys(minPrice);

        WebElement onlyWithPrice = waitClickableIn(dialog,
                By.cssSelector("label[for='hasPriceyes'] div[class*='Checkbox_checkmark']"), SHORT);
        scrollCenter(driver, onlyWithPrice);
        clickSafe(driver, onlyWithPrice, SHORT);

        List<WebElement> rsd = driver.findElements(By.cssSelector("input[aria-label='rsd']"));
        if (!rsd.isEmpty()) {
            WebElement rsdRadio = rsd.get(0);
            if (!rsdRadio.isSelected()) {
                scrollCenter(driver, rsdRadio);
                clickSafe(driver, rsdRadio, SHORT);
            }
        }

        WebElement applyBtn = firstVisible(
                driver, SHORT,
                Collections.singletonList(By.xpath("//button[contains(.,'Primeni filtere') or contains(.,'Primeni') or contains(.,'Prikaži') or contains(.,'Filtriraj')]"))
        );
        scrollCenter(driver, applyBtn);
        clickSafe(driver, applyBtn, SHORT);

        new WebDriverWait(driver, SHORT).until(ExpectedConditions.invisibilityOfElementLocated(priceDialog));
        waitForDomComplete(driver, MEDIUM);
        try { dismissGoogleOneTap(driver, SHORT); } catch (Exception ignored) {}
        new WebDriverWait(driver, SHORT).until(textChanged(crumbSpan, prevCrumb));

        return prevCrumb;
    }

    public static void applyConditionFilter(WebDriver driver) {
        openConditionDropdown(driver);
        pickConditionOption(driver, Pattern.compile("^Novo$", Pattern.CASE_INSENSITIVE));

        openConditionDropdown(driver);
        pickConditionOption(driver, Pattern.compile("(?i)nekori\\S+", Pattern.CASE_INSENSITIVE));
    }

    public static int submitSearchAndGetResultCount(WebDriver driver, String prevCrumb) {
        WebElement search = waitVisible(driver, By.cssSelector("button[aria-label='Pretražite']"), MEDIUM);
        scrollCenter(driver, search);
        clickSafe(driver, search, SHORT);

        waitForDomComplete(driver, MEDIUM);
        try { dismissGoogleOneTap(driver, SHORT); } catch (Exception ignored) {}

        WebElement coutSpan = firstVisible(
                driver, SHORT,
                Arrays.asList(
                        By.xpath("//*[contains(@class,'BreadcrumbHolder_breadcrumb')]//span[contains(translate(.,'ĐđŠšŽžĆćČč','DdSszZCcCc'),'rezultat')]"),
                        By.xpath("//*[contains(@class,'BreadcrumbHolder_breadcrumb')]//span")
                )
        );
        new WebDriverWait(driver, SHORT).until(textChanged(coutSpan, prevCrumb));

        String txt = safeText(coutSpan);
        int total = extractResultsCount(txt);
        if (total <= 0) {
            WebElement allCrumbs = waitVisible(driver, By.cssSelector("[class*='BreadcrumbHolder_breadcrumb']"), MEDIUM);
            total = extractMaxNumber(safeText(allCrumbs));
        }
        return total;
    }


    private static WebElement waitVisible(WebDriver driver, By by, Duration timeout) {
        return new WebDriverWait(driver, timeout).until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    private static WebElement waitClickableIn(WebElement scope, By by, Duration timeout) {
        WebDriver driver = ((WrapsDriver) scope).getWrappedDriver();
        return new WebDriverWait(driver, timeout)
                .until(ExpectedConditions.elementToBeClickable(scope.findElement(by)));
    }

    private static WebElement firstVisible(WebDriver driver, Duration timeout, List<By> candidates) {
        for (By by : candidates) {
            try {
                WebElement el = new WebDriverWait(driver, timeout)
                        .until(ExpectedConditions.visibilityOfElementLocated(by));
                if (el != null) return el;
            } catch (TimeoutException ignored) {}
        }
        throw new NoSuchElementException("Nijedan lokator nije našao element: " + candidates);
    }

    private static WebElement firstVisibleIn(WebDriver driver, WebElement scope, Duration timeout, List<By> candidates) {
        WebDriverWait w = new WebDriverWait(driver, timeout);
        for (By by : candidates) {
            try {
                WebElement el = w.until(d -> {
                    try {
                        WebElement s = scope.findElement(by);
                        return (s != null && s.isDisplayed()) ? s : null;
                    } catch (NoSuchElementException e) { return null; }
                });
                if (el != null) return el;
            } catch (TimeoutException ignored) {}
        }
        throw new NoSuchElementException("Nijedan lokator nije našao element u scope-u: " + candidates);
    }

    private static void clickSafe(WebDriver driver, WebElement el, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(ExpectedConditions.elementToBeClickable(el)).click();
        } catch (Exception e1) {
            try {
                dismissGoogleOneTap(driver, SHORT);
                scrollCenter(driver, el);
                new WebDriverWait(driver, timeout).until(ExpectedConditions.elementToBeClickable(el)).click();
            } catch (Exception e2) {
                js(driver, "arguments[0].click();", el);
            }
        }
    }

    private static void scrollCenter(WebDriver driver, WebElement el) {
        try { js(driver, "arguments[0].scrollIntoView({block:'center', inline:'center'});", el); }
        catch (Exception ignored) {}
    }

    private static ExpectedCondition<Boolean> textChanged(WebElement element, String prev) {
        return d -> {
            try {
                String now = element.getText().replace('\u00a0',' ').trim();
                return !now.equals(prev);
            } catch (StaleElementReferenceException e) {
                return true;
            }
        };
    }

    private static String safeText(WebElement el) {
        try {
            return el.getText().replace('\u00a0',' ').trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static int extractResultsCount(String txt) {
        if (txt == null) return 0;
        String norm = txt.replace('\u00a0',' ').trim();
        Matcher m = Pattern.compile("(\\d[\\d\\.\\s]*)\\s*(rezultat)", Pattern.CASE_INSENSITIVE).matcher(norm);
        if (m.find()) {
            try {
                String num = m.group(1).replace(".", " ").trim().replace(" ", "");
                return Integer.parseInt(num);
            } catch (Exception ignored) {}
        }
        return 0;
    }

    private static int extractMaxNumber(String txt) {
        if (txt == null) return 0;
        String norm = txt.replace('\u00a0',' ').replace(".", " ");
        Matcher m = Pattern.compile("\\d+").matcher(norm);
        int max = 0;
        while (m.find()) {
            try {
                int v = Integer.parseInt(m.group());
                if (v > max) max = v;
            } catch (Exception ignored) {}
        }
        return max;
    }

    public static void openConditionDropdown(WebDriver driver) {
        Duration T = Duration.ofSeconds(12);

        By placeholder = By.id("react-select-condition-placeholder");
        By input       = By.cssSelector("input[id^='react-select-condition-'][id$='-input']");
        By control     = By.cssSelector("div[id^='react-select-condition-'][class*='-control'], [aria-labelledby='react-select-condition-placeholder']");
        By listbox     = By.cssSelector("div[id^='react-select-condition-'][id$='-listbox']");

        if (isVisible(driver, listbox)) return;

        try {
            WebElement ph = new WebDriverWait(driver, T)
                    .until(ExpectedConditions.visibilityOfElementLocated(placeholder));
            scrollCenter(driver, ph);
            clickSafe(driver, ph, T);
            if (isVisible(driver, listbox)) return;
        } catch (Exception ignored) {}

        try {
            WebElement inp = new WebDriverWait(driver, T)
                    .until(ExpectedConditions.presenceOfElementLocated(input));
            scrollCenter(driver, inp);
            try { inp.click(); } catch (Exception ignored) {}
            try { inp.sendKeys(Keys.ARROW_DOWN); } catch (Exception ignored) {}
            try { if (!isVisible(driver, listbox)) inp.sendKeys(Keys.SPACE); } catch (Exception ignored) {}
            if (isVisible(driver, listbox)) return;
        } catch (Exception ignored) {}

        try {
            WebElement ctl = new WebDriverWait(driver, T)
                    .until(ExpectedConditions.visibilityOfElementLocated(control));
            scrollCenter(driver, ctl);
            clickSafe(driver, ctl, T);
        } catch (Exception ignored) {}

        new WebDriverWait(driver, T).until(ExpectedConditions.visibilityOfElementLocated(listbox));
        try { Thread.sleep(80); } catch (InterruptedException ignored) {}
    }

    public static void pickConditionOption(WebDriver driver, java.util.regex.Pattern name) {
        Duration T = Duration.ofSeconds(12);
        By listbox = By.cssSelector("div[id^='react-select-condition-'][id$='-listbox']");

        openConditionDropdown(driver);

        WebElement lb = new WebDriverWait(driver, T)
                .until(ExpectedConditions.visibilityOfElementLocated(listbox));

        List<WebElement> opts = lb.findElements(By.cssSelector("div[role='option']"));
        WebElement match = null;
        for (WebElement o : opts) {
            String txt = safeText(o);
            if (name.matcher(txt).find()) {
                match = o;
                break;
            }
        }
        if (match == null) {
            js(driver, "arguments[0].scrollTop = 0;", lb);
            for (int i = 0; i < 20 && match == null; i++) {
                opts = lb.findElements(By.cssSelector("div[role='option']"));
                for (WebElement o : opts) {
                    String txt = safeText(o);
                    if (name.matcher(txt).find()) { match = o; break; }
                }
                js(driver, "arguments[0].scrollTop = arguments[0].scrollTop + 200;", lb);
            }
        }
        Assertions.assertNotNull(match, "Nisam našao opciju u 'Stanje': " + name);

        scrollCenter(driver, match);
        try {
            new WebDriverWait(driver, T).until(ExpectedConditions.elementToBeClickable(match)).click();
        } catch (Exception e) {
            js(driver, "arguments[0].click();", match);
        }
        try { Thread.sleep(60); } catch (InterruptedException ignored) {}
    }

    private static boolean isVisible(WebDriver driver, By by) {
        try {
            WebElement e = driver.findElement(by);
            return e != null && e.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

}
