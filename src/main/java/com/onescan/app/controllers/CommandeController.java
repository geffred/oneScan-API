package com.onescan.app.controllers;

import java.util.List;

import org.apache.hc.core5.http.HttpStatus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.onescan.app.Entity.Commande;
import com.onescan.app.repository.CommandeRepository;

@Controller
@RequestMapping("/api/public/commandes")
public class CommandeController {

    @Autowired
    private CommandeRepository commandeRepository;

    // Define your request mappings here

    @GetMapping
    public ResponseEntity<?> getCommandes() {
        try {
            List<Commande> commandes = commandeRepository.findAll();
            return ResponseEntity.ok(commandes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SC_NOT_FOUND).body("error");
        }
    }
}
