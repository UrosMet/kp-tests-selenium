package com.kp.qa;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.WebDriver;

import java.util.stream.Stream;

import static com.kp.qa.Helpers.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FilterTest {

    private WebDriver driver;

    static Stream<Browser> browsers() {
        return Browser.isMac()
                ? Stream.of(Browser.CHROME, Browser.FIREFOX, Browser.SAFARI)
                : Stream.of(Browser.CHROME, Browser.FIREFOX);
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Order(1)
    @ParameterizedTest(name = "Filter test na browseru: {0}")
    @MethodSource("browsers")
    void filterBlousesMoreThan1000(Browser browser) {
        driver = Drivers.create(browser);

        openHomePage(driver);
        navigateToBlouses(driver);
        String prevCrumb = applyPriceFilter(driver, "100");
        applyConditionFilter(driver);
        int resultCount = submitSearchAndGetResultCount(driver, prevCrumb);

        System.out.println("Rezultat pretrage = " + resultCount);
        assertTrue(resultCount > 1000, "Očekujem više od 1000 rezultata, dobio sam: " + resultCount);
    }
}
