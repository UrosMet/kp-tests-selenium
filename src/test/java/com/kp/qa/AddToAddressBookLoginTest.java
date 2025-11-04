package com.kp.qa;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.time.Duration;
import java.util.stream.Stream;

import static com.kp.qa.Helpers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AddToAddressBookLoginTest {

    private WebDriver driver;

    private static final Duration SHORT  = Duration.ofSeconds(1);

    static Stream<Browser> browsers() {
        if (Browser.isMac()) {
            return Stream.of(Browser.CHROME, Browser.FIREFOX, Browser.SAFARI);
        }
        return Stream.of(Browser.CHROME, Browser.FIREFOX);
    }

    @BeforeEach
    void openDriver(TestInfo info) {
        String override = System.getenv("BROWSER");
        Browser browser = override == null
                ? Browser.CHROME
                : Browser.valueOf(override.trim().toUpperCase());
        driver = null;
    }

    @AfterEach
    void closeDriver() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Order(1)
    @ParameterizedTest(name = "[{index}] Browser = {0}")
    @MethodSource("browsers")
    void shouldOpenLoginWhenAddingToAddressBook(Browser browser) {
        driver = Drivers.create(browser);

        String url = System.getenv("KP_SAMPLE_AD_URL");
        Assertions.assertNotNull(url, "Postavi KP_SAMPLE_AD_URL (npr: https://www.kupujemprodajem.com/...)");

        driver.get(url);
        waitForDomComplete(driver, SHORT);
        ((JavascriptExecutor) driver).executeScript("return true;");

        try { acceptCookiesIfPresent(driver, SHORT); } catch (Exception ignored) {}
        waitForDomComplete(driver, SHORT);
        try { dismissGoogleOneTap(driver, SHORT); } catch (Exception ignored) {}
        waitForDomComplete(driver, SHORT);

        clickAddToAddressBookWithRetries(driver, SHORT);

        System.out.println("Prikazana login forma");
        expectLoginForm(driver, SHORT);
    }
}
