package com.onescan.app.services;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import java.time.Duration;

public abstract class BaseSeleniumService implements DentalPlatformService {
    protected WebDriver driver;
    protected boolean isLoggedIn = false;

    protected void initializeDriver() {
        if (driver == null || !isDriverAlive()) {
            WebDriverManager.chromedriver().setup();

            ChromeOptions options = new ChromeOptions();
            options.addArguments(
                    "--no-sandbox",
                    "--disable-dev-shm-usage",
                    "--remote-allow-origins=*",
                    "--disable-gpu",
                    "--window-size=1920,1080",
                    "--headless=new");

            try {
                driver = new ChromeDriver(options);
                driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            } catch (Exception e) {
                throw new RuntimeException("Échec de l'initialisation de ChromeDriver: " + e.getMessage());
            }
        }
    }

    protected boolean isDriverAlive() {
        try {
            driver.getTitle();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    protected void handleError(Exception e) {
        isLoggedIn = false;
        closeDriver();
        System.err.println("Erreur Selenium: " + e.getMessage());
    }

    public void closeDriver() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                System.err.println("Erreur lors de la fermeture du driver: " + e.getMessage());
            } finally {
                driver = null;
            }
        }
    }

    @Override
    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    protected abstract boolean verifyLoggedIn();
}