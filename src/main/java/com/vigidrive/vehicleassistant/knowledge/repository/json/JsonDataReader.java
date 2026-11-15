package com.vigidrive.vehicleassistant.knowledge.repository.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;

final class JsonDataReader {

    private JsonDataReader() {
    }

    static <T> T read(ResourceLoader resourceLoader, ObjectMapper objectMapper,
                      String resourcePath, TypeReference<T> type) {
        try (var input = resourceLoader.getResource("classpath:" + resourcePath).getInputStream()) {
            return objectMapper.readValue(input, type);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load knowledge data from " + resourcePath, exception);
        }
    }
}
