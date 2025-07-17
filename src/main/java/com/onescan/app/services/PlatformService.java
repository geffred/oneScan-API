package com.onescan.app.services;

import com.onescan.app.DTO.PlatformDTO;
import com.onescan.app.Entity.Platform;
import com.onescan.app.Entity.User;
import com.onescan.app.repository.PlatformRepository;
import com.onescan.app.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlatformService {

    private final PlatformRepository platformRepository;
    private final UserRepository userRepository;

    public PlatformDTO createPlatform(PlatformDTO dto) {
        User user = userRepository.findById(dto.userId())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Platform platform = dto.toEntity();
        platform.setUser(user);
        Platform savedPlatform = platformRepository.save(platform);

        return PlatformDTO.fromEntity(savedPlatform);
    }

    public List<PlatformDTO> getUserPlatforms(Long userId) {
        return platformRepository.findByUserId(userId).stream()
                .map(PlatformDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public PlatformDTO updatePlatform(Long id, PlatformDTO dto) {
        Platform platform = platformRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Platforme non trouvée"));

        platform.setName(dto.name());
        platform.setEmail(dto.email());
        // On ne met à jour le mot de passe que si fourni
        if (dto.password() != null && !dto.password().isBlank()) {
            platform.setPassword(dto.password());
        }

        Platform updatedPlatform = platformRepository.save(platform);
        return PlatformDTO.fromEntity(updatedPlatform);
    }

    public void deletePlatform(Long id) {
        platformRepository.deleteById(id);
    }

    public PlatformDTO getPlatformById(Long id) {
        return platformRepository.findById(id)
                .map(PlatformDTO::fromEntity)
                .orElseThrow(() -> new RuntimeException("Platforme non trouvée"));
    }
}