package io.casehub.life.app.cbr.describe;

import io.casehub.life.app.cbr.LifeCbrDescriptionProvider;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.UUID;

import static io.casehub.life.app.cbr.describe.ContractorCoordinationDescriptionProvider.asMap;
import static io.casehub.life.app.cbr.describe.ContractorCoordinationDescriptionProvider.str;

@ApplicationScoped
public class HouseholdProcurementDescriptionProvider implements LifeCbrDescriptionProvider {

    @Override
    public String caseType() {
        return "household-procurement";
    }

    @Override
    public String describeProblem(Map<String, Object> caseData) {
        var request = asMap(caseData.get("procurementRequest"));
        String category = str(request, "category", "general");
        String itemType = str(request, "itemType", "");
        String intent = str(request, "intent", "purchase");
        return "Procurement (%s): %s%s".formatted(
                intent,
                category,
                itemType.isEmpty() ? "" : " — " + itemType);
    }

    @Override
    public String describeSolution(Map<String, Object> caseData) {
        var order = asMap(caseData.get("orderResult"));
        String status = str(order, "orderStatus", "unknown");
        var delivery = asMap(caseData.get("deliveryStatus"));
        boolean delivered = Boolean.TRUE.equals(delivery.get("delivered"));
        return "Order %s%s".formatted(
                status,
                delivered ? " — delivered" : "");
    }

    @Override
    public String extractEntityId(Map<String, Object> caseData, UUID caseId) {
        var order = asMap(caseData.get("orderResult"));
        String orderId = str(order, "orderId", null);
        return orderId != null ? orderId : caseId.toString();
    }
}
