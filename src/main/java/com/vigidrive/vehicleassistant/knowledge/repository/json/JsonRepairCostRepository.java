package com.vigidrive.vehicleassistant.knowledge.repository.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vigidrive.vehicleassistant.knowledge.RepairCost;
import com.vigidrive.vehicleassistant.knowledge.VehicleSegment;
import com.vigidrive.vehicleassistant.knowledge.repository.RepairCostRepository;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public class JsonRepairCostRepository implements RepairCostRepository {

    private final List<RepairCost> costs;

    public JsonRepairCostRepository(ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        this.costs = List.copyOf(JsonDataReader.read(
                resourceLoader, objectMapper, "data/repair-costs.json", new TypeReference<>() {
                }));
    }

    @Override
    public Optional<RepairCost> findByRepairAndSegment(String repair, VehicleSegment segment) {
        if (repair == null || repair.isBlank() || segment == null) {
            return Optional.empty();
        }
        String query = normalize(repair);
        return costs.stream()
                .filter(cost -> cost.segment() == segment)
                .filter(cost -> matchesRepair(normalize(cost.repair()), query))
                .findFirst();
    }

    private static boolean matchesRepair(String repair, String query) {
        return repair.contains(query) || query.contains(repair);
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("\\b(replacement|replace|repair|service)\\b", "")
                .replaceAll("\\bplugs\\b", "plug")
                .replaceAll("\\bpads\\b", "pad")
                .replaceAll("\\bbrakes\\b", "brake")
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
