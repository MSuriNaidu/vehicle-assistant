package com.vigidrive.vehicleassistant.knowledge.repository.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vigidrive.vehicleassistant.knowledge.ModelInfo;
import com.vigidrive.vehicleassistant.knowledge.repository.ModelInfoRepository;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Repository;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Repository
public class JsonModelInfoRepository implements ModelInfoRepository {

    private final Map<String, ModelInfo> models;

    public JsonModelInfoRepository(ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        this.models = Map.copyOf(JsonDataReader.read(
                resourceLoader, objectMapper, "data/model-segments.json", new TypeReference<>() {
                }));
    }

    @Override
    public Optional<ModelInfo> findByModel(String model) {
        if (model == null || model.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(models.get(model.trim().toLowerCase(Locale.ROOT)));
    }
}
