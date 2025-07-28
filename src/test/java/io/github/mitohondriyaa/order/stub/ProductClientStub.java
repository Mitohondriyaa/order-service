package io.github.mitohondriyaa.order.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class ProductClientStub {
    public static void stubProductCall(String productId) {
        stubFor(get(urlEqualTo("/api/product/price/" + productId))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("500")));
    }
}