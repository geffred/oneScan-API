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
import java.util.*;

@Service
public class IteroSeleniumService extends BaseSeleniumService {

    private final Dotenv dotenv = Dotenv.load();

    private static final String BASE_URL = "https://bff.cloud.myitero.com";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired
    private CommandeRepository commandeRepository;

    /**
     * Connexion à la plateforme Itero.
     */
    @Override
    public String login() {
        initializeDriver();

        if (isLoggedIn && verifyLoggedIn()) {
            return "Déjà connecté.";
        }

        String email = dotenv.get("ITERO_USERNAME");
        String password = dotenv.get("ITERO_PASSWORD");

        try {
            driver.get(BASE_URL + "/login-legacy");

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

            // Attendre la redirection vers la page d'accueil après connexion
            wait.until(ExpectedConditions.urlContains("/labs/home"));

            isLoggedIn = true;
            return "Connexion réussie.";
        } catch (Exception e) {
            handleError(e);
            return "Échec de la connexion: " + e.getMessage();
        }
    }

    /**
     * Extraction d’un texte depuis un élément avec gestion d’erreur.
     */
    private String getText(WebElement row, String selector) {
        try {
            return row.findElement(By.cssSelector(selector)).getText().trim();
        } catch (NoSuchElementException e) {
            return "";
        }
    }

    /**
     * Récupération des commandes depuis Itero.
     */
    @Override
    public List<Commande> fetchCommandes() {
        List<Commande> commandes = new ArrayList<>();

        if (!ensureLoggedIn()) {
            System.err.println("Erreur : Impossible de se connecter à Itero");
            return commandes;
        }

        try {
            driver.navigate().to(BASE_URL + "/labs/home");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

            List<WebElement> rows = wait.until(ExpectedConditions
                    .presenceOfAllElementsLocatedBy(By.cssSelector("tr[id^='tableRow_']")));

            for (WebElement row : rows) {
                try {
                    String dateReception = getText(row, ".col-received > div");
                    String externalId = getText(row, ".col-order-id > div");
                    String refPatient = getText(row, ".col-patient-name > div");
                    String cabinet = getText(row, ".col-practice-name > div");
                    String dateLivraison = getText(row, ".col-due-date");

                    // Ignorer les lignes incomplètes
                    if (externalId.isEmpty() || refPatient.isEmpty())
                        continue;

                    Commande commande = new Commande();
                    commande.setExternalId(Long.parseLong(externalId));
                    commande.setRefPatient(refPatient);
                    commande.setCabinet(cabinet);
                    commande.setDateReception(LocalDate.parse(dateReception, DATE_FORMATTER));
                    commande.setDateEcheance(LocalDate.parse(dateLivraison, DATE_FORMATTER));
                    commande.setVu(false);
                    commande.setPlateforme(Plateforme.ITERO);

                    commandes.add(commande);

                } catch (Exception e) {
                    System.err.println("⚠️ Ligne ignorée (erreur parsing): " + e.getMessage());
                }
            }

            if (!commandes.isEmpty()) {
                commandeRepository.saveAll(commandes);
            } else {
                System.out.println("Aucune commande trouvée ou contenu vide.");
            }

        } catch (Exception e) {
            handleError(e);
            System.err.println("Erreur lors de la récupération des commandes : " + e.getMessage());
        }

        return commandes;
    }

    /**
     * Déconnexion et fermeture du navigateur.
     */
    @Override
    public String logout() {
        if (driver != null) {
            closeDriver();
            isLoggedIn = false;
            return "Déconnexion réussie.";
        }
        return "Déjà déconnecté.";
    }

    /**
     * Vérifie si l'utilisateur est connecté à Itero.
     */
    @Override
    protected boolean verifyLoggedIn() {
        if (!isDriverAlive())
            return false;

        try {
            driver.navigate().to(BASE_URL + "/labs/home");
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".image-link")));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
