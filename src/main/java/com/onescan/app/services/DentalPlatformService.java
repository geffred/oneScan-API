package com.onescan.app.services;

import java.util.List;

public interface DentalPlatformService {
    String login();

    List<String> fetchPatients();

    String logout();

    boolean isLoggedIn();
}