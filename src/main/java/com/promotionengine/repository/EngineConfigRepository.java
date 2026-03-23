package com.promotionengine.repository;

import com.promotionengine.entity.EngineConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EngineConfigRepository extends JpaRepository<EngineConfig, Long> {

    Optional<EngineConfig> findByConfigKey(String configKey);
}