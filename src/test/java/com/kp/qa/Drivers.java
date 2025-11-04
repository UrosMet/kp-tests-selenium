package com.kp.qa;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;

import java.awt.*;
import java.time.Duration;

public final class Drivers {

    private Drivers() {}

    public static WebDriver create(Browser browser) {
        boolean headless = Boolean.parseBoolean(System.getenv().getOrDefault("HEADLESS", "false"));

        switch (browser) {
            case FIREFOX: {
                WebDriverManager.firefoxdriver().setup();
                FirefoxOptions fo = new FirefoxOptions();

                if (headless) {
                    fo.addArguments("-headless");
                } else {
                    fo.addArguments("--start-maximized");
                }
                WebDriver d = new FirefoxDriver(fo);
                if (!headless) {
                    try {
                        d.manage().window().maximize();
                        d.manage().window().setSize(new Dimension(
                                Toolkit.getDefaultToolkit().getScreenSize().width,
                                Toolkit.getDefaultToolkit().getScreenSize().height
                        ));
                    } catch (Exception ignored) {}
                }

                commonTimeouts(d);
                return d;
            }
            case SAFARI: {
                SafariOptions so = new SafariOptions();
                WebDriver d = new SafariDriver(so);
                commonTimeouts(d);
                d.manage().window().maximize();
                return d;
            }
            case CHROME:
            default: {
                WebDriverManager.chromedriver().clearResolutionCache();
                WebDriverManager.chromedriver().setup();

                ChromeOptions co = new ChromeOptions();
                co.addArguments(
                        "--disable-notifications",
                        "--disable-infobars",
                        "--disable-features=FedCm,PrivacySandboxAdsAPIs,OptimizationHints,Translate",
                        "--no-first-run",
                        "--no-default-browser-check",
                        "--start-maximized",
                        "--disable-extensions",
                        "--user-data-dir=" + System.getProperty("java.io.tmpdir") + "/chrome-prof",
                        "--profile-directory=Default"
                );
                if (headless) co.addArguments("--headless=new");
                WebDriver d = new ChromeDriver(co);
                commonTimeouts(d);
                return d;
            }
        }
    }

    private static void commonTimeouts(WebDriver d) {
        d.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(15));
        d.manage().timeouts().scriptTimeout(Duration.ofSeconds(15));
    }
}
