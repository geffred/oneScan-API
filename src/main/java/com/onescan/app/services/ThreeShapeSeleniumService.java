package com.onescan.app.services;

import io.github.cdimascio.dotenv.Dotenv;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class ThreeShapeSeleniumService extends BaseSeleniumService {
    private final Dotenv dotenv = Dotenv.load();

    @Override
    public String login() {
        initializeDriver();

        if (isLoggedIn && verifyLoggedIn()) {
            return "Déjà connecté.";
        }

        String email = dotenv.get("THREESHAPE_EMAIL");
        String password = dotenv.get("THREESHAPE_PASSWORD");

        try {
            driver.get("https://portal.3shapecommunicate.com/login");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(40));

            acceptCookiesIfPresent(wait);
            performLoginSteps(wait, email, password);
            wait.until(ExpectedConditions.urlContains("cases"));

            isLoggedIn = true;
            return "Connexion réussie.";
        } catch (Exception e) {
            handleError(e);
            return "Échec de la connexion: " + e.getMessage();
        }
    }

    @Override
    public List<String> fetchPatients() {
        List<String> patients = new ArrayList<>();

        // Tentative automatique de connexion si nécessaire
        if (!ensureLoggedIn()) {
            patients.add("Erreur : Impossible de se connecter à Itero");
            return patients;
        }

        try {
            driver.navigate().to("https://portal.3shapecommunicate.com/cases");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

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

    @Override
    public String logout() {
        if (driver != null) {
            closeDriver();
            isLoggedIn = false;
            return "Déconnexion réussie.";
        }
        return "Déjà déconnecté.";
    }

    @Override
    protected boolean verifyLoggedIn() {
        try {
            driver.navigate().to("https://portal.3shapecommunicate.com/cases");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("mat-cell.cdk-column-PatientName")));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void performLoginSteps(WebDriverWait wait, String email, String password) {
        try {
            WebElement signInBtn = wait
                    .until(ExpectedConditions.elementToBeClickable(By.cssSelector("a.nav-btn--login")));
            signInBtn.click();
        } catch (Exception ignored) {
            // Si bouton non présent, on continue
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

    private void acceptCookiesIfPresent(WebDriverWait wait) {
        try {
            WebElement acceptCookiesBtn = wait
                    .until(ExpectedConditions.elementToBeClickable(By.cssSelector("button.coi-banner__accept")));
            acceptCookiesBtn.click();
        } catch (Exception ignored) {
            // Popup absent, on ignore
        }
    }
}