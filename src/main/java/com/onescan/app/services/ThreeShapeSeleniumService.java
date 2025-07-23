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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ThreeShapeSeleniumService extends BaseSeleniumService {

    // Formatter pour les dates
    private static final DateTimeFormatter DATE_FORMATTER_DELIVERY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_FORMATTER_RECEPTION = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Pattern pour extraire la date de réception du case number
    private static final Pattern CASE_NUMBER_DATE_PATTERN = Pattern.compile("(\\d{4})(\\d{2})(\\d{2})_(\\d{4})_\\d{2}");

    private final Dotenv dotenv = Dotenv.load();

    @Autowired
    private CommandeRepository commandeRepository;

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
    public List<Commande> fetchCommandes() {
        List<Commande> commandes = new ArrayList<>();

        if (!ensureLoggedIn()) {
            System.err.println("[ThreeShape] Erreur de connexion");
            return commandes;
        }

        try {
            driver.navigate().to("https://portal.3shapecommunicate.com/cases");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            // Attendre que le tableau soit chargé
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("mat-table[role='table']")));

            // Récupérer toutes les lignes du tableau
            List<WebElement> rows = driver.findElements(By.cssSelector("mat-row"));

            System.out.println("[ThreeShape] " + rows.size() + " lignes trouvées");

            for (WebElement row : rows) {
                try {
                    Commande commande = extractCommandeFromRow(row);
                    if (commande != null) {
                        commandes.add(commande);
                    }
                } catch (Exception e) {
                    System.err.println("[ThreeShape] Erreur extraction ligne: " + e.getMessage());
                }
            }

            // Sauvegarde en base
            if (!commandes.isEmpty()) {
                commandeRepository.saveAll(commandes);
                System.out.println("[ThreeShape] " + commandes.size() + " commandes sauvegardées");
            } else {
                System.out.println("[ThreeShape] Aucune commande trouvée");
            }

        } catch (Exception e) {
            handleError(e);
            System.err.println("[ThreeShape] Erreur récupération commandes: " + e.getMessage());
        }

        return commandes;
    }

    /**
     * Extrait les informations d'une commande depuis une ligne du tableau
     */
    private Commande extractCommandeFromRow(WebElement row) {
        try {
            // Extraction du nom du patient
            String refPatient = null;
            try {
                WebElement patientElement = row.findElement(
                        By.cssSelector("mat-cell.cdk-column-PatientName div.mat-cell-inner--ellipsis"));
                refPatient = patientElement.getText().trim();
            } catch (Exception e) {
                System.err.println("[ThreeShape] Patient non trouvé dans la ligne");
                return null;
            }

            // Extraction du cabinet
            String cabinet = null;
            try {
                WebElement cabinetElement = row.findElement(
                        By.cssSelector("mat-cell.cdk-column-ClinicName div.mat-cell-inner--ellipsis"));
                cabinet = cabinetElement.getText().trim();
            } catch (Exception e) {
                System.err.println("[ThreeShape] Cabinet non trouvé pour patient: " + refPatient);
                cabinet = "N/A";
            }

            // Extraction de l'external ID (Case Number)
            String caseNumber = null;
            Long externalId = null;
            LocalDate dateReception = null;

            try {
                WebElement caseNumberElement = row.findElement(
                        By.cssSelector("mat-cell.cdk-column-CaseNumber div.mat-cell-inner--ellipsis"));
                caseNumber = caseNumberElement.getText().trim();

                if (!caseNumber.isEmpty()) {
                    // Extraire l'ID externe (partie avant le premier underscore)
                    String[] parts = caseNumber.split("_");
                    if (parts.length > 0) {
                        try {
                            externalId = Long.parseLong(parts[0]);
                        } catch (NumberFormatException e) {
                            System.err.println("[ThreeShape] Erreur parsing externalId: " + parts[0]);
                        }
                    }

                    // Extraire la date de réception du case number
                    dateReception = extractDateFromCaseNumber(caseNumber);
                }
            } catch (Exception e) {
                System.err.println("[ThreeShape] Case Number non trouvé pour patient: " + refPatient);
            }

            // Extraction de la date d'échéance
            LocalDate dateEcheance = null;
            try {
                WebElement deliveryElement = row.findElement(
                        By.cssSelector("mat-cell.cdk-column-DeliveryDate"));
                String deliveryDateStr = deliveryElement.getText().trim();

                if (!deliveryDateStr.isEmpty() && !deliveryDateStr.equals("-")) {
                    try {
                        dateEcheance = LocalDate.parse(deliveryDateStr, DATE_FORMATTER_DELIVERY);
                    } catch (Exception e) {
                        System.err.println("[ThreeShape] Erreur parsing date échéance: " + deliveryDateStr);
                    }
                }
            } catch (Exception e) {
                // Date d'échéance optionnelle
            }

            // Validation des données essentielles
            if (refPatient == null || refPatient.isEmpty()) {
                return null;
            }

            // Création de l'objet commande
            Commande commande = new Commande();
            commande.setRefPatient(refPatient);
            commande.setExternalId(externalId);
            commande.setDateReception(dateReception);
            commande.setCabinet(cabinet);
            commande.setDateEcheance(dateEcheance);
            commande.setVu(false);
            commande.setPlateforme(Plateforme.THREESHAPE);

            System.out.println("[ThreeShape] Commande extraite - Patient: " + refPatient +
                    ", ExternalId: " + externalId +
                    ", Cabinet: " + cabinet +
                    ", DateReception: " + dateReception +
                    ", DateEcheance: " + dateEcheance);

            return commande;

        } catch (Exception e) {
            System.err.println("[ThreeShape] Erreur extraction commande: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extrait la date de réception à partir du case number
     * Format attendu: XXXXXXXXX_YYYYMMDD_HHMM_XX
     * Exemple: 1417512720_20250718_1655_08 -> 18/07/2025
     */
    private LocalDate extractDateFromCaseNumber(String caseNumber) {
        try {
            Matcher matcher = CASE_NUMBER_DATE_PATTERN.matcher(caseNumber);

            if (matcher.find()) {
                String year = matcher.group(1); // 2025
                String month = matcher.group(2); // 07
                String day = matcher.group(3); // 18

                // Construire la date au format yyyy-MM-dd pour parsing
                String dateStr = year + "-" + month + "-" + day;
                return LocalDate.parse(dateStr);
            }

        } catch (Exception e) {
            System.err.println(
                    "[ThreeShape] Erreur extraction date du case number: " + caseNumber + " - " + e.getMessage());
        }

        return null;
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

    @Override
    public String logout() {
        if (!isLoggedIn) {
            return "Déjà déconnecté.";
        }

        try {
            driver.get("https://portal.3shapecommunicate.com/logout");
            isLoggedIn = false;
            return "Déconnexion réussie.";
        } catch (Exception e) {
            handleError(e);
            return "Échec de la déconnexion: " + e.getMessage();
        }
    }
}