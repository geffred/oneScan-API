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
public class IteroSeleniumService extends BaseSeleniumService {
    private final Dotenv dotenv = Dotenv.load();

    @Override
    public String login() {
        initializeDriver();

        if (isLoggedIn && verifyLoggedIn()) {
            return "Déjà connecté.";
        }

        String email = dotenv.get("ITERO_USERNAME");
        String password = dotenv.get("ITERO_PASSWORD");

        try {
            driver.get("https://bff.cloud.myitero.com/login-legacy");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(40));

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

            wait.until(ExpectedConditions.urlContains("/labs/home"));

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
            driver.navigate().to("https://bff.cloud.myitero.com/labs/home");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

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
        if (!isDriverAlive())
            return false;

        try {
            driver.navigate().to("https://bff.cloud.myitero.com/labs/home");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector(".image-link")));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}