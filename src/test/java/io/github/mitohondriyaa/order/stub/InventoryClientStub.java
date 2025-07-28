package io.github.mitohondriyaa.order.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class InventoryClientStub {
    public static void stubInventoryCall(String productId, Integer quantity) {
        stubFor(get(urlEqualTo("/api/inventory/check?productId=" + productId + "&quantity=" + quantity))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));
    }
}