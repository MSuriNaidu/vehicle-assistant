package com.vigidrive.vehicleassistant.knowledge.repository.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vigidrive.vehicleassistant.knowledge.DtcCode;
import com.vigidrive.vehicleassistant.knowledge.repository.DtcCodeRepository;
import org.springframework.stereotype.Repository;
import org.springframework.core.io.ResourceLoader;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public class JsonDtcCodeRepository implements DtcCodeRepository {

    private final Map<String, DtcCode> codes;

    public JsonDtcCodeRepository(ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        List<DtcCode> entries = JsonDataReader.read(
                resourceLoader, objectMapper, "data/dtc-codes.json", new TypeReference<>() {
                });
        this.codes = entries.stream().collect(Collectors.toUnmodifiableMap(
                entry -> entry.code().toUpperCase(Locale.ROOT), Function.identity()));
    }

    @Override
    public Optional<DtcCode> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(codes.get(code.trim().toUpperCase(Locale.ROOT)));
    }
}
