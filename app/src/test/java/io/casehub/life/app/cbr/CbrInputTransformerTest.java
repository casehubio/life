package io.casehub.life.app.cbr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CbrInputTransformerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void apply_noExperiences_passThrough() {
        var        transformer = new CbrInputTransformer(new LifeCbrExperienceFormatter(), MAPPER);
        ObjectNode input       = MAPPER.createObjectNode().put("key", "value");
        JsonNode   result      = transformer.apply(input);
        assertEquals("value", result.get("key").asText());
        assertFalse(result.has("_cbrContext"));
    }

    @Test
    void apply_emptyExperiencesArray_passThrough() {
        var        transformer = new CbrInputTransformer(new LifeCbrExperienceFormatter(), MAPPER);
        ObjectNode input       = MAPPER.createObjectNode().put("key", "value");
        input.putArray("_experiences");
        JsonNode result = transformer.apply(input);
        assertFalse(result.has("_cbrContext"));
    }

    @Test
    void apply_withExperiences_mergesCbrContext() {
        var        transformer = new CbrInputTransformer(new LifeCbrExperienceFormatter(), MAPPER);
        ObjectNode input       = MAPPER.createObjectNode().put("key", "value");
        input.putArray("_experiences").add(
                MAPPER.createObjectNode().put("problem", "test problem").put("solution", "test solution"));
        JsonNode result = transformer.apply(input);
        assertTrue(result.has("_cbrContext"));
        assertTrue(result.get("_cbrContext").asText().contains("test problem"));
        assertFalse(result.has("_experiences"));
        assertEquals("value", result.get("key").asText());
    }

    @Test
    void apply_doesNotMutateOriginalInput() {
        var        transformer = new CbrInputTransformer(new LifeCbrExperienceFormatter(), MAPPER);
        ObjectNode input       = MAPPER.createObjectNode().put("key", "value");
        input.putArray("_experiences").add(
                MAPPER.createObjectNode().put("problem", "p").put("solution", "s"));
        transformer.apply(input);
        assertFalse(input.has("_cbrContext"));
        assertTrue(input.has("_experiences"));
    }

    @Test
    void apply_withAdaptedPlan_mergesIntoCbrContext() {
        var        transformer = new CbrInputTransformer(new LifeCbrExperienceFormatter(), MAPPER);
        ObjectNode adaptedPlan = MAPPER.createObjectNode();
        adaptedPlan.putArray("steps").addObject()
                   .put("bindingName", "b1")
                   .put("capabilityName", "request-quote")
                   .put("workerName", "w1")
                   .put("stepOutcome", "ok")
                   .put("priority", 8)
                   .put("action", "BOOSTED")
                   .put("reason", "Winter urgency")
                   .putObject("parameters").put("slaHours", 24);
        ObjectNode input = MAPPER.createObjectNode().put("key", "value");
        input.set("adaptedPlan", adaptedPlan);
        JsonNode result = transformer.apply(input);
        assertTrue(result.has("_cbrContext"));
        assertTrue(result.get("_cbrContext").asText().contains("request-quote"));
        assertTrue(result.get("_cbrContext").asText().contains("BOOSTED"));
        assertFalse(result.has("adaptedPlan"));
        assertEquals("value", result.get("key").asText());
    }

    @Test
    void apply_adaptedPlanAndExperiences_combinesBoth() {
        var        transformer = new CbrInputTransformer(new LifeCbrExperienceFormatter(), MAPPER);
        ObjectNode input       = MAPPER.createObjectNode();
        input.putArray("_experiences").add(
                MAPPER.createObjectNode().put("problem", "test").put("solution", "fix"));
        ObjectNode adaptedPlan = MAPPER.createObjectNode();
        adaptedPlan.putArray("steps").addObject()
                   .put("bindingName", "b1")
                   .put("capabilityName", "cap1")
                   .put("workerName", "w1")
                   .put("priority", 5)
                   .put("action", "RETAINED")
                   .putObject("parameters");
        input.set("adaptedPlan", adaptedPlan);
        JsonNode result = transformer.apply(input);
        String   ctx    = result.get("_cbrContext").asText();
        assertTrue(ctx.contains("test"));
        assertTrue(ctx.contains("Adapted Plan"));
    }
}
