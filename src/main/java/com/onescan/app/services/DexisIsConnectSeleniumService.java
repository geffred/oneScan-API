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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class DexisIsConnectSeleniumService extends BaseSeleniumService {

    // Formatter pour les dates au format dd/MM/yyyy
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
            System.err.println("[Dexis] Erreur : Impossible de se connecter à Dexis");
            return commandes;
        }

        try {
            driver.navigate().to("https://dentalconnect.dexis.com/main.php");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(40));

            // Attendre le chargement des sections de cas par jour
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("section.masterCaseListOfDay")));

            // Récupérer toutes les sections de jours
            List<WebElement> daySections = driver.findElements(By.cssSelector("section.masterCaseListOfDay"));

            System.out.println("[Dexis] " + daySections.size() + " sections de jours trouvées");

            for (WebElement daySection : daySections) {
                try {
                    // Récupérer la date de réception depuis l'en-tête de la section
                    LocalDate dateReception = extractDateFromSection(daySection);

                    // Récupérer tous les cas dans cette section
                    List<WebElement> caseElements = daySection.findElements(By.cssSelector("li[id^='caseMaster_']"));

                    System.out
                            .println("[Dexis] " + caseElements.size() + " cas trouvés pour la date: " + dateReception);

                    for (WebElement caseElement : caseElements) {
                        try {
                            Commande commande = extractCommandeFromCase(caseElement, dateReception);
                            if (commande != null) {
                                commandes.add(commande);
                            }
                        } catch (Exception e) {
                            System.err.println("[Dexis] Erreur lors de l'extraction d'un cas: " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[Dexis] Erreur lors du traitement d'une section de jour: " + e.getMessage());
                }
            }

            // Sauvegarde en base si commandes trouvées
            if (!commandes.isEmpty()) {
                commandeRepository.saveAll(commandes);
                System.out.println("[Dexis] " + commandes.size() + " commandes sauvegardées");
            } else {
                System.out.println("[Dexis] Aucune commande trouvée sur Dexis.");
            }

        } catch (Exception e) {
            handleError(e);
            System.err.println("[Dexis] Erreur lors de la récupération des commandes: " + e.getMessage());
        }

        return commandeRepository.findAll();
    }

    /**
     * Extrait la date de réception depuis l'en-tête de section
     */
    private LocalDate extractDateFromSection(WebElement daySection) {
        try {
            WebElement timeElement = daySection.findElement(By.cssSelector("header time"));
            String dateStr = timeElement.getAttribute("datetime");

            // Le datetime est au format "11/07/2025"
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            System.err.println("[Dexis] Erreur extraction date section: " + e.getMessage());
            return LocalDate.now(); // Valeur par défaut
        }
    }

    /**
     * Extrait les informations d'une commande depuis un élément de cas
     */
    private Commande extractCommandeFromCase(WebElement caseElement, LocalDate dateReception) {
        try {
            // Extraction du nom du patient
            WebElement patientElement = caseElement.findElement(By.cssSelector("mark[id^='casePatient_']"));
            String refPatient = patientElement.getText().trim();

            // Extraction de l'ID du cas et conversion pour externalId
            WebElement caseIdElement = caseElement.findElement(By.cssSelector("h2[id^='caseId_']"));
            String caseIdFull = caseIdElement.getText().trim(); // Ex: "NGO-6991"

            // Extraire la partie après le tiret
            String externalIdStr = null;
            if (caseIdFull.contains("-")) {
                externalIdStr = caseIdFull.split("-")[1];
            }

            // Extraction du partenaire/cabinet
            WebElement partnerElement = caseElement.findElement(By.cssSelector("mark[id^='casePartner_']"));
            String cabinet = partnerElement.getText().trim();

            // Validation des données essentielles
            if (refPatient.isEmpty() || externalIdStr == null || externalIdStr.isEmpty()) {
                System.err.println("[Dexis] Données manquantes pour un cas - Patient: " + refPatient + ", ExternalId: "
                        + externalIdStr);
                return null;
            }

            // Création de l'objet commande
            Commande commande = new Commande();
            commande.setRefPatient(refPatient);
            commande.setExternalId(Long.parseLong(externalIdStr));
            commande.setDateReception(dateReception);
            commande.setCabinet(cabinet);
            commande.setDateEcheance(null); // Défini à null comme demandé
            commande.setVu(false);
            commande.setPlateforme(Plateforme.DEXIS);

            System.out.println("[Dexis] Commande extraite - Patient: " + refPatient +
                    ", ID: " + externalIdStr +
                    ", Cabinet: " + cabinet +
                    ", Date: " + dateReception);

            return commande;

        } catch (NumberFormatException e) {
            System.err.println("[Dexis] Erreur conversion externalId: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("[Dexis] Erreur lors de l'extraction d'un cas: " + e.getMessage());
            return null;
        }
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
                return "Déconnexion réussie.";
            } catch (Exception e) {
                System.err.println("[Dexis] Erreur lors de la déconnexion: " + e.getMessage());
                return "Erreur déconnexion: " + e.getMessage();
            } finally {
                closeDriver();
                isLoggedIn = false;
            }
        }
        return "Déjà déconnecté.";
    }

    /**
     * Vérifie que l'utilisateur est bien connecté (vérification de l'URL
     * principale).
     */
    @Override
    protected boolean verifyLoggedIn() {
        if (driver == null)
            return false;

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