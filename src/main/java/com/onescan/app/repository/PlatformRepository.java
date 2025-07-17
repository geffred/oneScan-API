package com.onescan.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.onescan.app.Entity.Platform;

public interface PlatformRepository extends JpaRepository<Platform, Long> {

    List<Platform> findByUserId(Long id);
}