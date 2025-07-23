package com.onescan.app.Entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "commandes")
public class Commande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "external_id", unique = true)
    private Long externalId;

    @Column(name = "ref_patient")
    private String refPatient;

    @Column(name = "date_reception")
    private LocalDate dateReception;

    @Column(name = "file_3d")
    private String file3d;

    @Column(name = "plateforme")
    @Enumerated(EnumType.STRING)
    private Plateforme plateforme;

    @Column(name = "cabinet")
    private String cabinet;

    @Column(name = "commentaire")
    private String commentaire;

    @Column(name = "date_echeance")
    private LocalDate dateEcheance;

    @Column(name = "vu")
    private Boolean vu;

    public Commande() {

    }

    public Commande(Long externalId, String refPatient, LocalDate dateReception, String file3d,
            Plateforme plateforme, String cabinet, String commentaire, LocalDate dateEcheance,
            Boolean vu) {
        this.externalId = externalId;
        this.refPatient = refPatient;
        this.dateReception = dateReception;
        this.file3d = file3d;
        this.plateforme = plateforme;
        this.cabinet = cabinet;
        this.commentaire = commentaire;
        this.dateEcheance = dateEcheance;
        this.vu = vu;
    }

    // Getters et Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getExternalId() {
        return externalId;
    }

    public void setExternalId(Long externalId) {
        this.externalId = externalId;
    }

    public String getRefPatient() {
        return refPatient;
    }

    public void setRefPatient(String refPatient) {
        this.refPatient = refPatient;
    }

    public LocalDate getDateReception() {
        return dateReception;
    }

    public void setDateReception(LocalDate dateReception) {
        this.dateReception = dateReception;
    }

    public String getFile3d() {
        return file3d;
    }

    public void setFile3d(String file3d) {
        this.file3d = file3d;
    }

    public Plateforme getPlateforme() {
        return plateforme;
    }

    public void setPlateforme(Plateforme plateforme) {
        this.plateforme = plateforme;
    }

    public String getCabinet() {
        return cabinet;
    }

    public void setCabinet(String cabinet) {
        this.cabinet = cabinet;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public LocalDate getDateEcheance() {
        return dateEcheance;
    }

    public void setDateEcheance(LocalDate dateEcheance) {
        this.dateEcheance = dateEcheance;
    }

    public Boolean getVu() {
        return vu;
    }

    public void setVu(Boolean vu) {
        this.vu = vu;
    }
}
