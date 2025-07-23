package com.onescan.app.services;

import io.github.cdimascio.dotenv.Dotenv;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
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
public class MeditLinkSeleniumService extends BaseSeleniumService {
    private static final String BASE_URL = "https://www.meditlink.com";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Dotenv dotenv = Dotenv.load();

    @Autowired
    private CommandeRepository commandeRepository;

    @Override
    public String login() {
        if (isLoggedIn && verifyLoggedIn()) {
            return "Déjà connecté.";
        }

        initializeDriver();
        String email = dotenv.get("MEDITLINK_USERNAME");
        String password = dotenv.get("MEDITLINK_PASSWORD");

        try {
            // 1. Accès à la page de login
            driver.get("https://www.meditlink.com/login");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            // 2. Saisie des identifiants avec vérification des champs
            WebElement emailField = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("input#input-login-id.text-box-input")));
            emailField.clear();
            if (emailField.getAttribute("maxlength") != null &&
                    Integer.parseInt(emailField.getAttribute("maxlength")) > 0) {
                email = email.substring(0, Integer.parseInt(emailField.getAttribute("maxlength")));
            }
            emailField.sendKeys(email);

            WebElement passwordField = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("input#input-login-password.text-box-input")));
            passwordField.clear();
            passwordField.sendKeys(password);

            // 3. Clique sur le bouton de connexion avec gestion des animations
            WebElement loginButton = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("button#btn-login")));

            // Attendre que l'animation soit terminée (si classe fade-out présente)
            wait.until(driver -> !loginButton.getAttribute("class").contains("fade-out"));

            // Clique via JavaScript pour éviter les problèmes d'interception
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", loginButton);

            // 4. Attente de la redirection vers l'inbox
            wait.until(ExpectedConditions.urlContains("inbox"));

            // 5. Fermeture du popup si présent
            try {
                WebElement closePopupButton = wait.until(ExpectedConditions.elementToBeClickable(
                        By.cssSelector("div.icon-wrapper.md-icon.xxs[rounded='false']")));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", closePopupButton);
                wait.until(ExpectedConditions.invisibilityOf(closePopupButton));
            } catch (Exception e) {
                System.out.println("Aucun popup à fermer détecté");
            }

            isLoggedIn = true;
            return "Connexion réussie.";

        } catch (Exception e) {
            handleError(e);
            return "Échec de la connexion: " + e.getMessage();
        }
    }

    @Override
    public List<Commande> fetchCommandes() {
        List<Commande> commandes = new ArrayList<>();

        if (!ensureLoggedIn()) {
            System.err.println("[MeditLink] Erreur de connexion");
            return commandes;
        }

        try {
            driver.navigate().to(BASE_URL + "/inbox");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(40));

            // Attente du chargement du tableau
            By tableRowLocator = By.cssSelector("tr.main-body-tr");
            wait.until(ExpectedConditions.presenceOfElementLocated(tableRowLocator));

            // Extraction des données
            List<WebElement> rows = driver.findElements(tableRowLocator);
            for (WebElement row : rows) {
                try {
                    Commande commande = extractCommandeFromRow(row);
                    if (commande != null) {
                        commandes.add(commande);
                    }
                } catch (Exception e) {
                    System.err.println("[MeditLink] Erreur extraction ligne: " + e.getMessage());
                }
            }

            // Sauvegarde en base
            if (!commandes.isEmpty()) {
                commandeRepository.saveAll(commandes);
            } else {
                System.out.println("[MeditLink] Aucune commande trouvée");
            }

        } catch (Exception e) {
            handleError(e);
            System.err.println("[MeditLink] Erreur récupération commandes: " + e.getMessage());
        }

        return commandes;
    }

    private Commande extractCommandeFromRow(WebElement row) {
        String patientName = row.findElement(By.cssSelector("td:nth-child(3) span")).getText().trim();
        String externalId = row.findElement(By.cssSelector("td:nth-child(7) span")).getText().trim();

        if (patientName.isEmpty() || externalId.isEmpty()) {
            return null;
        }

        Commande commande = new Commande();
        commande.setRefPatient(patientName);
        commande.setExternalId(Long.parseLong(externalId));
        commande.setVu(false);
        commande.setPlateforme(Plateforme.MEDITLINK);
        commande.setCabinet(row.findElement(By.cssSelector("td:nth-child(6) span")).getText().trim());

        try {
            String creationDateStr = row.findElement(By.cssSelector("td:nth-child(4) span")).getText().trim();
            String dueDateStr = row.findElement(By.cssSelector("td:nth-child(5) span")).getText().trim();
            commande.setDateReception(LocalDate.parse(creationDateStr, DATE_FORMATTER));
            commande.setDateEcheance(LocalDate.parse(dueDateStr, DATE_FORMATTER));
        } catch (Exception e) {
            System.err.println("[MeditLink] Erreur parsing dates: " + e.getMessage());
        }

        return commande;
    }

    @Override
    public String logout() {
        if (driver == null) {
            return "Déjà déconnecté.";
        }

        try {
            driver.get(BASE_URL + "/logout");
            return "Déconnexion réussie.";
        } catch (Exception e) {
            return "Erreur déconnexion: " + e.getMessage();
        } finally {
            closeDriver();
            isLoggedIn = false;
        }
    }

    @Override
    protected boolean verifyLoggedIn() {
        if (driver == null)
            return false;

        try {
            driver.navigate().to(BASE_URL + "/dashboard");
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.urlContains("dashboard"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Récupère le commentaire d'une commande spécifique
     * 
     * @param externalId L'ID externe de la commande
     * @return Le commentaire ou null si non trouvé/erreur
     */
    public String getCommentaire(Long externalId) {
        if (!ensureLoggedIn()) {
            System.err.println("[MeditLink] Erreur de connexion pour récupération commentaire");
            return null;
        }

        try {
            // Navigation vers la page de détail
            String detailUrl = BASE_URL + "/inbox/detail/" + externalId;
            driver.navigate().to(detailUrl);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            // Attendre le chargement de la page
            wait.until(ExpectedConditions.urlContains("/inbox/detail/"));

            // Rechercher le textarea avec les attributs spécifiés
            WebElement commentaireTextarea = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("textarea[data-v-8a2006a2][data-v-2adbe6cd-s].show-scrollbar[disabled]")));

            // Récupérer le texte du commentaire
            String commentaire = commentaireTextarea.getAttribute("value");
            if (commentaire == null || commentaire.trim().isEmpty()) {
                commentaire = commentaireTextarea.getText();
            }

            System.out.println("[MeditLink] Commentaire récupéré pour ID " + externalId + ": " +
                    (commentaire.isEmpty() ? "Aucun commentaire" : "Commentaire présent"));

            return commentaire.trim().isEmpty() ? null : commentaire.trim();

        } catch (Exception e) {
            System.err.println(
                    "[MeditLink] Erreur récupération commentaire pour ID " + externalId + ": " + e.getMessage());
            handleError(e);
            return null;
        }
    }

    /**
     * Récupère tous les commentaires pour toutes les commandes
     * 
     * @return Map avec externalId comme clé et commentaire comme valeur
     */
    public java.util.Map<Long, String> getAllCommentaires() {
        java.util.Map<Long, String> commentaires = new java.util.HashMap<>();

        if (!ensureLoggedIn()) {
            System.err.println("[MeditLink] Erreur de connexion pour récupération de tous les commentaires");
            return commentaires;
        }

        try {
            // Récupérer d'abord toutes les commandes pour avoir les IDs
            driver.navigate().to(BASE_URL + "/inbox");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(40));

            // Attente du chargement du tableau
            By tableRowLocator = By.cssSelector("tr.main-body-tr");
            wait.until(ExpectedConditions.presenceOfElementLocated(tableRowLocator));

            List<WebElement> rows = driver.findElements(tableRowLocator);
            List<Long> externalIds = new ArrayList<>();

            // Extraire tous les IDs externes
            for (WebElement row : rows) {
                try {
                    String externalIdStr = row.findElement(By.cssSelector("td:nth-child(7) span")).getText().trim();
                    if (!externalIdStr.isEmpty()) {
                        externalIds.add(Long.parseLong(externalIdStr));
                    }
                } catch (Exception e) {
                    System.err.println("[MeditLink] Erreur extraction ID: " + e.getMessage());
                }
            }

            System.out.println("[MeditLink] Récupération des commentaires pour " + externalIds.size() + " commandes");

            // Récupérer le commentaire pour chaque ID
            for (Long externalId : externalIds) {
                String commentaire = getCommentaire(externalId);
                if (commentaire != null && !commentaire.isEmpty()) {
                    commentaires.put(externalId, commentaire);
                }

                // Petite pause entre les requêtes pour éviter la surcharge
                Thread.sleep(1000);
            }

            System.out.println("[MeditLink] " + commentaires.size() + " commentaires récupérés sur "
                    + externalIds.size() + " commandes");

        } catch (Exception e) {
            System.err.println("[MeditLink] Erreur récupération tous commentaires: " + e.getMessage());
            handleError(e);
        }

        return commentaires;
    }

}