// ThreeShapeSeleniumService.java
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
public class ThreeShapeSeleniumService {

    private final Dotenv dotenv = Dotenv.load();
    private WebDriver driver;
    private boolean isLoggedIn = false;

    /**
     * Initialise ChromeDriver avec options si non encore fait.
     */
    public void initializeDriver() {
        if (driver == null) {
            WebDriverManager.chromedriver().setup();

            ChromeOptions options = new ChromeOptions();
            options.addArguments(
                    "--no-sandbox",
                    "--disable-dev-shm-usage",
                    "--remote-allow-origins=*",
                    "--disable-gpu",
                    "--window-size=1920,1080",
                    "--headless=new");

            System.setProperty("webdriver.chrome.silentOutput", "true");

            try {
                driver = new ChromeDriver(options);
                driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            } catch (Exception e) {
                throw new RuntimeException("Échec de l'initialisation de ChromeDriver: " + e.getMessage());
            }
        }
    }

    /**
     * Connexion au portail ThreeShape.
     * 
     * @return message de succès ou d'erreur.
     */
    public String login() {
        initializeDriver();

        // Si déjà connecté et session valide, on ne reconnecte pas
        if (isLoggedIn && verifyLoggedIn()) {
            return "Déjà connecté.";
        }

        String email = dotenv.get("THREESHAPE_EMAIL");
        String password = dotenv.get("THREESHAPE_PASSWORD");

        try {
            driver.get("https://portal.3shapecommunicate.com/login");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            // Accepter les cookies si popup présent
            acceptCookiesIfPresent(wait);
            // Remplir le formulaire de login
            performLoginSteps(wait, email, password);
            // Attendre la redirection vers la page cases (page principale)
            wait.until(ExpectedConditions.urlContains("cases"));

            isLoggedIn = true;
            return "Connexion réussie.";
        } catch (Exception e) {
            handleError(e);
            return "Échec de la connexion: " + e.getMessage();
        }
    }

    /**
     * Récupère la liste des patients uniquement si connecté.
     * 
     * @return liste des patients ou liste contenant un message d'erreur.
     */
    public List<String> fetchPatients() {
        List<String> patients = new ArrayList<>();

        // Vérification connexion
        if (!isLoggedIn || !verifyLoggedIn()) {
            patients.add("Erreur : Non connecté à ThreeShape");
            return patients;
        }

        try {
            driver.navigate().to("https://portal.3shapecommunicate.com/cases");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            // Sélecteur CSS des noms de patients dans la page
            List<WebElement> patientElements = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                    By.cssSelector("mat-cell.cdk-column-PatientName div.mat-cell-inner--ellipsis")));

            for (WebElement el : patientElements) {
                patients.add(el.getText().trim());
            }

        } catch (Exception e) {
            handleError(e);
            patients.add("Erreur lors de la récupération des patients: " + e.getMessage());
        }

        return patients;
    }

    /**
     * Déconnexion et fermeture du driver.
     * 
     * @return message de succès ou si déjà déconnecté.
     */
    public String logout() {
        if (driver != null) {
            closeDriver();
            isLoggedIn = false;
            return "Déconnexion réussie.";
        }
        return "Déjà déconnecté.";
    }

    /**
     * Vérifie si la session Selenium est encore valide.
     * 
     * @return true si connecté, false sinon.
     */
    private boolean verifyLoggedIn() {
        try {
            driver.navigate().to("https://portal.3shapecommunicate.com/cases");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("mat-cell.cdk-column-PatientName")));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Effectue les étapes de connexion : clique, remplissage, etc.
     */
    private void performLoginSteps(WebDriverWait wait, String email, String password) {
        try {
            WebElement signInBtn = wait
                    .until(ExpectedConditions.elementToBeClickable(By.cssSelector("a.nav-btn--login")));
            signInBtn.click();
        } catch (Exception ignored) {
            // Si bouton non présent, on continue (peut être déjà sur formulaire)
        }

        WebElement emailField = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[data-auto-qa-id='email-input']")));
        emailField.sendKeys(email);

        WebElement nextButton = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector("button[data-auto-qa-id='next-button']")));
        nextButton.click();

        WebElement passwordField = wait.until(ExpectedConditions
                .visibilityOfElementLocated(By.cssSelector("input[data-auto-qa-id='password-input']")));
        passwordField.sendKeys(password);

        WebElement signInButton = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector("button[data-auto-qa-id='sign-in-button']")));
        signInButton.click();
    }

    /**
     * Accepte le popup cookies s'il est présent.
     */
    private void acceptCookiesIfPresent(WebDriverWait wait) {
        try {
            WebElement acceptCookiesBtn = wait
                    .until(ExpectedConditions.elementToBeClickable(By.cssSelector("button.coi-banner__accept")));
            acceptCookiesBtn.click();
        } catch (Exception ignored) {
            // Popup absent, on ignore
        }
    }

    /**
     * Gestion centralisée des erreurs : reset de la session et fermeture du driver.
     */
    private void handleError(Exception e) {
        isLoggedIn = false;
        closeDriver();
        System.err.println("Erreur Selenium (ThreeShape): " + e.getMessage());
    }

    /**
     * Ferme proprement le driver Chrome.
     */
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

    /**
     * Permet de savoir si l'utilisateur est connecté.
     */
    public boolean isLoggedIn() {
        return isLoggedIn;
    }
}