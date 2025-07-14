// Ajoutez cette méthode dans votre CommandeRepository
package com.onescan.app.repository;

import com.onescan.app.Entity.Commande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long> {

    // Méthode pour trouver une commande par son external_id
    Optional<Commande> findByExternalId(Long externalId);

    boolean existsByExternalId(Long externalId);
}