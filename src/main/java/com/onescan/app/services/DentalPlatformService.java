package com.onescan.app.services;

import java.util.List;

import com.onescan.app.Entity.Commande;

public interface DentalPlatformService {

    String login();

    List<Commande> fetchCommandes();

    String logout();

    boolean isLoggedIn();
}