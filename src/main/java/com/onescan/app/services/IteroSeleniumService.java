package com.onescan.app.services;

import io.github.bonigarcia.wdm.WebDriverManager;
import io.github.cdimascio.dotenv.Dotenv;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class IteroSeleniumService {

    private final Dotenv dotenv = Dotenv.load();
    private WebDriver driver;
    private boolean isLoggedIn = false;

    public void initializeDriver() {
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
                driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(15));
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
            } catch (Exception e) {
                throw new RuntimeException("Échec de l'initialisation de ChromeDriver: " + e.getMessage());
            }
        }
    }

    public String login() {
        initializeDriver();

        if (isLoggedIn && verifyLoggedIn()) {
            return "Déjà connecté.";
        }

        String email = dotenv.get("ITERO_USERNAME");
        String password = dotenv.get("ITERO_PASSWORD");

        try {
            driver.get("https://bff.cloud.myitero.com/login-legacy");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

            WebElement emailField = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("input[formcontrolname='username']")));
            emailField.clear();
            emailField.sendKeys(email);

            WebElement passwordField = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("input[formcontrolname='password']")));
            passwordField.clear();
            passwordField.sendKeys(password);

            WebElement loginButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("input#btn-login")));
            loginButton.click();

            // Attendre une redirection ou un élément visible après login
            wait.until(ExpectedConditions.urlContains("/labs/home"));

            isLoggedIn = true;
            return "Connexion réussie.";

        } catch (Exception e) {
            handleError(e);
            return "Échec de la connexion: " + e.getMessage();
        }
    }

    public List<String> fetchPatients() {
        List<String> patients = new ArrayList<>();

        if (!isLoggedIn || !verifyLoggedIn()) {
            patients.add("Erreur : Non connecté à Itero");
            return patients;
        }

        try {
            driver.navigate().to("https://bff.cloud.myitero.com/labs/home");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            // Attendre que les lignes de patients apparaissent
            List<WebElement> rows = wait.until(
                    ExpectedConditions.presenceOfAllElementsLocatedBy(
                            By.cssSelector("tbody tr")));

            for (WebElement row : rows) {
                try {
                    WebElement nameCell = row.findElement(By.cssSelector(".col-patient-name"));
                    String name = nameCell.getText().trim();

                    if (!name.equalsIgnoreCase("Nom du patient") && !name.isEmpty()) {
                        patients.add(name);
                    }
                } catch (Exception e) {
                    System.err.println("Erreur avec une ligne: " + e.getMessage());
                }
            }

            if (patients.isEmpty()) {
                patients.add("Aucun patient trouvé ou contenu vide.");
            }

        } catch (Exception e) {
            handleError(e);
            patients.add("Erreur lors de la récupération des patients: " + e.getMessage());
        }

        return patients;
    }

    public String logout() {
        if (driver != null) {
            closeDriver();
            isLoggedIn = false;
            return "Déconnexion réussie.";
        }
        return "Déjà déconnecté.";
    }

    private boolean verifyLoggedIn() {
        if (!isDriverAlive())
            return false;

        try {
            driver.navigate().to("https://bff.cloud.myitero.com/labs/home");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector(".image-link"))); // à adapter si nécessaire

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void handleError(Exception e) {
        closeDriver();
        System.err.println("Erreur Selenium (Itero): " + e.getMessage());
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

    private boolean isDriverAlive() {
        try {
            driver.getTitle(); // essaie d'interroger une propriété pour détecter un crash
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }
}
