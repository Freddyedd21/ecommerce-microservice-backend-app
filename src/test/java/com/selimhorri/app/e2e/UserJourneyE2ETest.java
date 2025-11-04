package com.selimhorri.app.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Exercises user-facing journeys end-to-end through the API Gateway while the
 * microservices run inside Minikube and are exposed locally via port-forwarding.
 *
 * Each test simulates a complete user flow and asserts on meaningful business
 * data rather than infrastructure-only signals.
 */
public class UserJourneyE2ETest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final RestTemplate REST_TEMPLATE = createRestTemplate();
    private static final int MAX_GATEWAY_ATTEMPTS = 3;
    private static final Duration GATEWAY_RETRY_DELAY = Duration.ofSeconds(5);
    private static final String GATEWAY_BASE_URL = resolveGatewayBaseUrl();

    @Test
    void userCanBrowseProductCatalogue() throws Exception {
        ArrayNode products = fetchCollection("/product-service/api/products");
        assertTrue(products.size() > 0, "Expected product catalogue to contain items");
        JsonNode sample = products.get(0);
        assertTrue(sample.path("productId").asInt() > 0, "Product must expose an id");
        assertTrue(hasText(sample.path("productTitle").asText(null)), "Product must expose a title");
    }

    @Test
    void userCanOpenProductDetailsFromCatalogue() throws Exception {
        ArrayNode products = fetchCollection("/product-service/api/products");
        JsonNode first = products.get(0);
        int productId = first.path("productId").asInt();
        JsonNode product = fetchObject("/product-service/api/products/" + productId);
        assertEquals(productId, product.path("productId").asInt(), "Product endpoint must return requested id");
        assertTrue(hasText(product.path("productTitle").asText(null)), "Product details should include a title");
    }

    @Test
    void userCanReviewFavouriteListWithResolvedDetails() throws Exception {
        ArrayNode favourites = fetchCollection("/favourite-service/api/favourites");
        assertTrue(favourites.size() > 0, "Expected at least one favourite item");
        for (JsonNode favourite : favourites) {
            JsonNode userNode = favourite.get("user");
            assertNotNull(userNode, "Favourite entry must include resolved user data");
            assertTrue(hasText(userNode.path("firstName").asText(null)), "Resolved user should include firstName");

            JsonNode productNode = favourite.get("product");
            assertNotNull(productNode, "Favourite entry must include resolved product data");
            assertTrue(hasText(productNode.path("productTitle").asText(null)), "Resolved product should include productTitle");
        }
    }

    @Test
    void userCanInspectShippingSummaryForOrders() throws Exception {
        ArrayNode shippings = fetchCollection("/shipping-service/api/shippings");
        assertTrue(shippings.size() > 0, "Expected at least one shipping entry");
        for (JsonNode shipping : shippings) {
            JsonNode orderNode = shipping.get("order");
            assertNotNull(orderNode, "Shipping entry must embed order data");
            assertTrue(orderNode.path("orderId").asInt() > 0, "Embedded order should expose orderId");
            assertTrue(hasText(orderNode.path("orderDesc").asText(null)), "Embedded order should expose orderDesc");

            JsonNode productNode = shipping.get("product");
            assertNotNull(productNode, "Shipping entry must embed product data");
            assertTrue(productNode.path("productId").asInt() > 0, "Embedded product should expose productId");
            assertTrue(hasText(productNode.path("productTitle").asText(null)), "Embedded product should expose productTitle");
        }
    }

    @Test
    void userCanCheckPaymentStatusForOrders() throws Exception {
        ArrayNode payments = fetchCollection("/payment-service/api/payments");
        assertTrue(payments.size() > 0, "Expected at least one payment record");
        for (JsonNode payment : payments) {
            assertTrue(hasText(payment.path("paymentStatus").asText(null)), "Payment must expose its status");
            JsonNode orderNode = payment.get("order");
            assertNotNull(orderNode, "Payment should embed the associated order");
            assertTrue(orderNode.path("orderId").asInt() > 0, "Embedded order should expose orderId");
        }
    }

    private static ArrayNode fetchCollection(String relativePath) throws Exception {
    ResponseEntity<String> response = executeGatewayGet(relativePath);
        assertEquals(HttpStatus.OK, response.getStatusCode(), () -> "Expected 200 from " + relativePath);
        String body = response.getBody();
        assertTrue(hasText(body), "Response body must not be empty");
        JsonNode root = OBJECT_MAPPER.readTree(body);
        JsonNode collectionNode = root.get("collection");
        assertNotNull(collectionNode, "Response must include a 'collection' field");
        assertTrue(collectionNode.isArray(), "'collection' field must be an array");
        return (ArrayNode) collectionNode;
    }

    private static JsonNode fetchObject(String relativePath) throws Exception {
    ResponseEntity<String> response = executeGatewayGet(relativePath);
        assertEquals(HttpStatus.OK, response.getStatusCode(), () -> "Expected 200 from " + relativePath);
        String body = response.getBody();
        assertTrue(hasText(body), "Response body must not be empty");
        JsonNode node = OBJECT_MAPPER.readTree(body);
        assertTrue(node.isObject(), "Expected the response to be a JSON object");
        return node;
    }

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(30).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(45).toMillis());
        return new RestTemplate(factory);
    }

    // Retries help mask the occasional pause while port-forwarded gateway routes warm up.
    private static ResponseEntity<String> executeGatewayGet(String relativePath) throws Exception {
        ResourceAccessException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_GATEWAY_ATTEMPTS; attempt++) {
            try {
                return REST_TEMPLATE.getForEntity(buildUrl(relativePath), String.class);
            } catch (ResourceAccessException ex) {
                lastFailure = ex;
                if (attempt == MAX_GATEWAY_ATTEMPTS) {
                    break;
                }
                Thread.sleep(GATEWAY_RETRY_DELAY.toMillis());
            }
        }
        throw lastFailure;
    }

    private static String resolveGatewayBaseUrl() {
        String candidate = System.getProperty("API_GATEWAY_BASE_URL");
        if (!hasText(candidate)) {
            candidate = System.getenv("API_GATEWAY_BASE_URL");
        }
        if (!hasText(candidate)) {
            candidate = "http://api-gateway-service.ecommerce.svc.cluster.local:8080";
        }
        return stripTrailingSlash(candidate);
    }

    private static String buildUrl(String relativePath) {
        String cleanPath = relativePath.startsWith("/") ? relativePath : "/" + relativePath;
        return GATEWAY_BASE_URL + cleanPath;
    }

    private static String stripTrailingSlash(String value) {
        if (value == null) {
            return null;
        }
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
