package com.onescan.app.services;

import io.github.cdimascio.dotenv.Dotenv;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.onescan.app.Entity.Commande;
import com.onescan.app.Entity.Plateforme;
import com.onescan.app.repository.CommandeRepository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class DexisIsConnectSeleniumService extends BaseSeleniumService {

    // Chargement des variables d'environnement (.env)
    private final Dotenv dotenv = Dotenv.load();

    @Autowired
    private CommandeRepository commandeRepository;

    /**
     * Connexion à la plateforme Dexis.
     */
    @Override
    public String login() {
        initializeDriver();

        // Vérifie si l'utilisateur est déjà connecté
        if (isLoggedIn && verifyLoggedIn()) {
            return "Déjà connecté.";
        }

        // Récupération des identifiants depuis le fichier .env
        String email = dotenv.get("ISCONNECT_USERNAME");
        String password = dotenv.get("ISCONNECT_PASSWORD");

        try {
            // Étape 1 : Ouverture de la page d'accueil
            driver.get("https://dentalconnect.dexis.com/");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            // Clic sur le bouton de connexion
            WebElement loginLink = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("a#login")));
            loginLink.click();

            // Étape 2 : Saisie de l'email
            WebElement emailField = wait
                    .until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input#email")));
            emailField.clear();
            emailField.sendKeys(email);

            // Clic sur "Continuer"
            WebElement continueButton = wait
                    .until(ExpectedConditions.elementToBeClickable(By.cssSelector("button#continue")));
            continueButton.click();

            // Étape 3 : Saisie du mot de passe
            WebElement passwordField = wait
                    .until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input#password")));
            passwordField.clear();
            passwordField.sendKeys(password);

            // Clic sur "Se connecter"
            WebElement loginButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button#next")));
            loginButton.click();

            // Vérification de la redirection après connexion
            wait.until(ExpectedConditions.urlContains("main.php"));

            isLoggedIn = true;
            return "Connexion réussie.";

        } catch (Exception e) {
            handleError(e);
            return "Échec de la connexion: " + e.getMessage();
        }
    }

    /**
     * Récupération des commandes/patients depuis Dexis.
     */
    @Override
    public List<Commande> fetchCommandes() {
        List<Commande> commandes = new ArrayList<>();

        // Vérifie que l'utilisateur est connecté avant de continuer
        if (!ensureLoggedIn()) {
            System.err.println("Erreur : Impossible de se connecter à Dexis");
            return commandes;
        }

        try {
            driver.navigate().to("https://dentalconnect.dexis.com/main.php");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(40));

            // Attente du chargement de la liste des cas patients
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("li[id^='caseMaster_']")));

            List<WebElement> caseElements = driver.findElements(By.cssSelector("li[id^='caseMaster_']"));

            for (WebElement caseElement : caseElements) {
                try {
                    // Extraction du nom du patient
                    WebElement patientElement = caseElement.findElement(By.cssSelector("mark[id^='casePatient_']"));
                    String refPatient = patientElement.getText().trim();

                    // Extraction de l'ID du cas
                    WebElement caseIdElement = caseElement.findElement(By.cssSelector("h2[id^='caseId_']"));
                    String externalId = caseIdElement.getText().trim();

                    // S'assurer que les données sont valides
                    if (refPatient.isEmpty() || externalId.isEmpty())
                        continue;

                    // Création de l'objet commande
                    Commande commande = new Commande();
                    commande.setRefPatient(refPatient);
                    commande.setVu(false);
                    commande.setPlateforme(Plateforme.DEXIS);

                    commandes.add(commande);

                } catch (Exception e) {
                    System.err.println("Erreur lors de l'extraction d'un patient: " + e.getMessage());
                }
            }

            // Sauvegarde en base si commandes trouvées
            if (!commandes.isEmpty()) {
                commandeRepository.saveAll(commandes);
            } else {
                System.out.println("Aucune commande trouvée sur Dexis.");
            }

        } catch (Exception e) {
            handleError(e);
            System.err.println("Erreur lors de la récupération des commandes Dexis : " + e.getMessage());
        }

        return commandes;
    }

    /**
     * Déconnexion de la plateforme Dexis.
     */
    @Override
    public String logout() {
        if (driver != null) {
            try {
                // Navigation vers la page de logout
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

    /**
     * Vérifie que l'utilisateur est bien connecté (vérification de l'URL
     * principale).
     */
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
