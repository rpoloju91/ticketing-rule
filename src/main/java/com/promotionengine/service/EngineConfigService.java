package com.promotionengine.service;

import com.promotionengine.entity.EngineConfig;
import com.promotionengine.repository.EngineConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EngineConfigService {

    @Autowired
    EngineConfigRepository engineConfigRepository;

    // Get max promos per cart from DB
    // Default fallback = 2 if not found
    public int getMaxPromosPerCart() {
        return engineConfigRepository.findByConfigKey(EngineConfig.MAX_PROMOS_PER_CART).map(c -> Integer.parseInt(c.getConfigValue())).orElse(2);
    }

    // Get all config entries
    public List<EngineConfig> getAll() {
        return engineConfigRepository.findAll();
    }

    // Get config by key
    public EngineConfig getByKey(String key) {
        return engineConfigRepository.findByConfigKey(key).orElseThrow(() -> new RuntimeException("Config not found: " + key));
    }

    // Update config value
    @Transactional
    public EngineConfig update(String key, String value, String updatedBy) {
        EngineConfig config = getByKey(key);
        config.setConfigValue(value);
        config.setUpdatedBy(updatedBy);
        EngineConfig saved = engineConfigRepository.save(config);
        log.info("Config updated: {}={} by {}", key, value, updatedBy);
        return saved;
    }
}
