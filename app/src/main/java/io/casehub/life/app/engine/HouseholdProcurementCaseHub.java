/*
 * Copyright 2026-Present The Case Hub Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.casehub.life.app.engine;

import io.casehub.api.model.CaseDefinition;
import io.casehub.life.api.LifeCaseType;
import io.casehub.life.app.engine.agent.AnalyseNeedResult;
import io.casehub.life.app.engine.agent.FulfilOrderResult;
import io.casehub.life.app.engine.agent.SearchProductsResult;
import io.casehub.life.app.engine.agent.TrackDeliveryResult;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HouseholdProcurementCaseHub extends LifeTypedCaseHub {

    public HouseholdProcurementCaseHub() {
        super("life/household-procurement.yaml", LifeAgent.HOME);
    }

    @Override
    public LifeCaseType lifeCaseType() {
        return LifeCaseType.HOUSEHOLD_PROCUREMENT;
    }

    @Override
    protected void configureCase(CaseDefinition definition) {
        definition.getWorkers().add(agentWorker("analyse-need", """
                You are a household procurement agent. Analyse the procurement request to
                determine what items are needed, the budget, urgency, and whether this is
                a gift purchase or a household supply reorder.
                If this is a gift, note the recipient and occasion.
                If cbrCalibration is provided, use featureStats for typical costs and
                quantities in similar purchases.""", AnalyseNeedResult.class));
        definition.getWorkers().add(agentWorker("search-products", """
                You are a household procurement agent. Search the product catalogue for
                items matching the needs analysis.
                Use search_products to find matching items by category and description.
                Use list_orders to check recent order history for reorder candidates.
                Recommend the best option with price and reasoning.""", SearchProductsResult.class));
        definition.getWorkers().add(agentWorker("fulfil-order", """
                You are a household procurement agent. Place the approved order.
                Use add_to_cart to add the selected items.
                Use checkout to complete the purchase.
                Use send_chat to send an order confirmation to the household.
                Include the order ID, status, and notification message ID in your response.""", FulfilOrderResult.class));
        definition.getWorkers().add(agentWorker("track-delivery", """
                You are a household procurement agent. Check the delivery status of the order.
                Use list_orders to look up the current order status and tracking information.
                Use send_chat to notify the household when delivery is expected or completed.
                Include the delivery status and notification message ID in your response.""", TrackDeliveryResult.class));
    }
}
