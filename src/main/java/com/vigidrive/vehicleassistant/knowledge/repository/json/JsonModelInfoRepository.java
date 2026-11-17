package com.vigidrive.vehicleassistant.knowledge.repository.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vigidrive.vehicleassistant.knowledge.ModelInfo;
import com.vigidrive.vehicleassistant.knowledge.repository.ModelInfoRepository;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Repository;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
        String normalized = model.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
        ModelInfo exact = models.get(normalized.replace(" ", ""));
        if (exact != null) {
            return Optional.of(exact);
        }

        String[] parts = normalized.split("\\s+");
        for (int index = parts.length - 1; index >= 0; index--) {
            if (parts[index].matches("\\d{4}")) {
                continue;
            }
            ModelInfo match = models.get(parts[index]);
            if (match != null && isMakeCompatible(normalized, match)) {
                return Optional.of(match);
            }
        }
        return Optional.empty();
    }

    private boolean isMakeCompatible(String normalizedInput, ModelInfo modelInfo) {
        return models.values().stream()
                .map(ModelInfo::make)
                .distinct()
                .filter(make -> normalizedInput.contains(make.toLowerCase(Locale.ROOT)))
                .allMatch(make -> Objects.equals(make, modelInfo.make()));
    }
}
