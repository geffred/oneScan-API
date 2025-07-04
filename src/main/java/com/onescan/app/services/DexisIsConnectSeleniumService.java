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
public class DexisIsConnectSeleniumService extends BaseSeleniumService {
    private final Dotenv dotenv = Dotenv.load();

    @Override
    public String login() {
        initializeDriver();

        if (isLoggedIn && verifyLoggedIn()) {
            return "Déjà connecté.";
        }

        String email = dotenv.get("ISCONNECT_USERNAME");
        String password = dotenv.get("ISCONNECT_PASSWORD");

        try {
            // Étape 1: Accès à la page de login
            driver.get("https://dentalconnect.dexis.com/");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            // Cliquer sur le bouton "Accès au service"
            WebElement loginLink = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("a#login")));
            loginLink.click();

            // Étape 2: Saisie de l'email
            WebElement emailField = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("input#email")));
            emailField.clear();
            emailField.sendKeys(email);

            // Cliquer sur "Continuer"
            WebElement continueButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("button#continue")));
            continueButton.click();

            // Étape 3: Saisie du mot de passe
            WebElement passwordField = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("input#password")));
            passwordField.clear();
            passwordField.sendKeys(password);

            // Cliquer sur "Se connecter"
            WebElement loginButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("button#next")));
            loginButton.click();

            // Vérification de la connexion réussie
            wait.until(ExpectedConditions.urlContains("main.php"));

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
            driver.navigate().to("https://dentalconnect.dexis.com/main.php");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            // Attendre que la liste des patients soit chargée
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("li[id^='caseMaster_']")));

            // Récupérer tous les éléments li représentant les patients
            List<WebElement> caseElements = driver.findElements(
                    By.cssSelector("li[id^='caseMaster_']"));

            for (WebElement caseElement : caseElements) {
                try {
                    // Récupérer le nom du patient depuis la balise <mark>
                    WebElement patientElement = caseElement.findElement(
                            By.cssSelector("mark[id^='casePatient_']"));
                    String patientName = patientElement.getText().trim();

                    // Récupérer l'ID du cas depuis la balise <h2>
                    WebElement caseIdElement = caseElement.findElement(
                            By.cssSelector("h2[id^='caseId_']"));
                    String caseId = caseIdElement.getText().trim();

                    // Récupérer le statut depuis la balise <em>
                    WebElement statusElement = caseElement.findElement(
                            By.cssSelector("em[id^='caseStatus_']"));
                    String status = statusElement.getText().trim();

                    // Formater les informations du patient
                    String patientInfo = String.format("%s (ID: %s, Statut: %s)",
                            patientName, caseId, status);

                    if (!patientName.isEmpty()) {
                        patients.add(patientInfo);
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors de l'extraction d'un patient: " + e.getMessage());
                }
            }

            if (patients.isEmpty()) {
                patients.add("Aucun patient trouvé dans la liste.");
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
            try {
                // Navigation vers la page de déconnexion si nécessaire
                driver.get("https://dentalconnect.dexis.com/logout.php");
            } catch (Exception e) {
                System.err.println("Erreur lors de la déconnexion: " + e.getMessage());
            } finally {
                closeDriver();
                isLoggedIn = false;
            }
            return "Déconnexion réussie.";
        }
        return "Déjà déconnecté.";
    }

    @Override
    protected boolean verifyLoggedIn() {
        try {
            driver.navigate().to("https://dentalconnect.dexis.com/main.php");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.urlContains("main.php"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}