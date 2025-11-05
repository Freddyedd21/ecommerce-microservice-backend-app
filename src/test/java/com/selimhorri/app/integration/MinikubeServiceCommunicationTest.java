package com.selimhorri.app.integration;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Verifies that each Minikube-hosted microservice enriches its payload with data obtained from other services.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MinikubeServiceCommunicationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final RestTemplate REST_TEMPLATE = createRestTemplate();
    private static final int MAX_SERVICE_REACHABILITY_ATTEMPTS = 5;
    private static final Duration SERVICE_RETRY_DELAY = Duration.ofSeconds(5);

    @Test
    @Order(4)
    void favouriteServiceShouldResolveUserDetails() throws Exception {
        assertServiceReachable(ServiceTarget.FAVOURITE);

        ArrayNode favourites = fetchCollection(ServiceTarget.FAVOURITE, "/api/favourites");
        for (JsonNode favourite : favourites) {
            JsonNode userNode = favourite.get("user");
            assertNotNull(userNode, "Expected favourite entry to include resolved user data");
            assertTrue(userNode.isObject(), "Expected user payload to be a JSON object");
            assertEquals(
                    favourite.path("userId").asInt(),
                    userNode.path("userId").asInt(),
                    "Favourite entry must expose the same user id received from user-service");
            assertTrue(
                    hasText(userNode.path("firstName").asText(null)),
                    "User payload should include the firstName coming from user-service");
        }
    }

    @Test
    @Order(5)
    void favouriteServiceShouldResolveProductDetails() throws Exception {
        assertServiceReachable(ServiceTarget.FAVOURITE);

        ArrayNode favourites = fetchCollection(ServiceTarget.FAVOURITE, "/api/favourites");
        for (JsonNode favourite : favourites) {
            JsonNode productNode = favourite.get("product");
            assertNotNull(productNode, "Expected favourite entry to include resolved product data");
            assertTrue(productNode.isObject(), "Expected product payload to be a JSON object");
            assertEquals(
                    favourite.path("productId").asInt(),
                    productNode.path("productId").asInt(),
                    "Favourite entry must expose the same product id received from product-service");
            assertTrue(
                    hasText(productNode.path("productTitle").asText(null)),
                    "Product payload should include the productTitle coming from product-service");
        }
    }

    @Test
    @Order(6)
    void shippingServiceShouldResolveProductDetails() throws Exception {
        assertServiceReachable(ServiceTarget.SHIPPING);

        ArrayNode orderItems = fetchCollection(ServiceTarget.SHIPPING, "/api/shippings");
        for (JsonNode orderItem : orderItems) {
            JsonNode productNode = orderItem.get("product");
            assertNotNull(productNode, "Expected order item entry to include resolved product data");
            assertTrue(productNode.isObject(), "Expected product payload to be a JSON object");
            assertEquals(
                    orderItem.path("productId").asInt(),
                    productNode.path("productId").asInt(),
                    "Order item must expose the same product id received from product-service");
            assertTrue(
                    hasText(productNode.path("productTitle").asText(null)),
                    "Product payload should include the productTitle coming from product-service");
        }
    }

    @Test
    @Order(7)
    void shippingServiceShouldResolveOrderDetails() throws Exception {
        assertServiceReachable(ServiceTarget.SHIPPING);

        ArrayNode orderItems = fetchCollection(ServiceTarget.SHIPPING, "/api/shippings");
        for (JsonNode orderItem : orderItems) {
            JsonNode orderNode = orderItem.get("order");
            assertNotNull(orderNode, "Expected order item entry to include resolved order data");
            assertTrue(orderNode.isObject(), "Expected order payload to be a JSON object");
            assertEquals(
                    orderItem.path("orderId").asInt(),
                    orderNode.path("orderId").asInt(),
                    "Order item must expose the same order id received from order-service");
            assertTrue(
                    hasText(orderNode.path("orderDesc").asText(null)),
                    "Order payload should include the orderDesc coming from order-service");
        }
    }

    @Test
    @Order(8)
    void paymentServiceShouldResolveOrderDetails() throws Exception {
        assertServiceReachable(ServiceTarget.PAYMENT);

        ArrayNode payments = fetchCollection(ServiceTarget.PAYMENT, "/api/payments");
        for (JsonNode payment : payments) {
            JsonNode orderNode = payment.get("order");
            assertNotNull(orderNode, "Expected payment entry to include resolved order data");
            assertTrue(orderNode.isObject(), "Expected order payload to be a JSON object");
            int orderId = orderNode.path("orderId").asInt();
            assertTrue(
                    orderId > 0,
                    "Order payload should include a valid orderId coming from order-service");
            assertTrue(
                    hasText(orderNode.path("orderDesc").asText(null)),
                    "Order payload should include the orderDesc coming from order-service");
        }
    }

    private static ArrayNode fetchCollection(ServiceTarget target, String path) throws Exception {
        ResponseEntity<String> response = REST_TEMPLATE.getForEntity(buildUrl(target, path), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode(), () -> "Expected 200 from " + buildUrl(target, path));
        String body = response.getBody();
        assertNotNull(body, "Response body must not be null");
        JsonNode root = OBJECT_MAPPER.readTree(body);
        JsonNode collectionNode = root.get("collection");
        assertNotNull(collectionNode, "Response should wrap payload within a 'collection' field");
        assertTrue(collectionNode.isArray(), "Expected 'collection' to be a JSON array");
        ArrayNode collection = (ArrayNode) collectionNode;
        assertTrue(collection.size() > 0, "Expected collection to return at least one element");
        return collection;
    }

    private static boolean isServiceReachable(ServiceTarget target) {
        try {
            ResponseEntity<String> response = REST_TEMPLATE.getForEntity(buildUrl(target, target.healthPath), String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                return false;
            }
            String body = response.getBody();
            if (!hasText(body)) {
                return true;
            }
            JsonNode root = OBJECT_MAPPER.readTree(body);
            JsonNode status = root.get("status");
            return status != null ? "UP".equalsIgnoreCase(status.asText()) : true;
        } catch (RestClientException | JsonProcessingException ex) {
            return false;
        }
    }

    private static void assertServiceReachable(ServiceTarget target) {
        for (int attempt = 1; attempt <= MAX_SERVICE_REACHABILITY_ATTEMPTS; attempt++) {
            if (isServiceReachable(target)) {
                return;
            }
            if (attempt < MAX_SERVICE_REACHABILITY_ATTEMPTS) {
                try {
                    Thread.sleep(SERVICE_RETRY_DELAY.toMillis());
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        assertTrue(false, target.unreachableMessage());
    }

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(20).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(20).toMillis());
        return new RestTemplate(factory);
    }

    private static String buildUrl(ServiceTarget target, String relativePath) {
        String baseUrl = resolveBaseUrl(target);
        String cleanPath = relativePath.startsWith("/") ? relativePath : "/" + relativePath;
        return baseUrl + cleanPath;
    }

    private static String resolveBaseUrl(ServiceTarget target) {
        String value = System.getProperty(target.systemProperty);
        if (!hasText(value)) {
            value = System.getenv(target.envVar);
        }
        if (!hasText(value)) {
            value = target.defaultBaseUrl;
        }
        return stripTrailingSlash(value);
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

    @Test
    @Order(1)
    void productServiceShouldListProducts() throws Exception {
        assertServiceReachable(ServiceTarget.PRODUCT);

        ArrayNode products = fetchCollection(ServiceTarget.PRODUCT, "/api/products");
        for (JsonNode product : products) {
            assertTrue(product.path("productId").asInt() > 0, "Expected productId to be populated");
            assertTrue(hasText(product.path("productTitle").asText(null)), "Product should expose a productTitle");
        }
    }

    @Test
    @Order(2)
    void userServiceShouldListUsers() throws Exception {
        assertServiceReachable(ServiceTarget.USER);

        ArrayNode users = fetchCollection(ServiceTarget.USER, "/api/users");
        for (JsonNode user : users) {
            assertTrue(user.path("userId").asInt() > 0, "Expected userId to be populated");
            JsonNode credential = user.get("credential");
            assertNotNull(credential, "User payload should embed credential data");
            assertTrue(credential.isObject(), "Credential payload should be a JSON object");
            assertTrue(
                    hasText(credential.path("username").asText(null)),
                    "User should expose a credential.username field");
        }
    }

    @Test
    @Order(3)
    void orderServiceShouldListOrders() throws Exception {
        assertServiceReachable(ServiceTarget.ORDER);

        ArrayNode orders = fetchCollection(ServiceTarget.ORDER, "/api/orders");
        for (JsonNode order : orders) {
            assertTrue(order.path("orderId").asInt() > 0, "Expected orderId to be populated");
            assertTrue(hasText(order.path("orderDesc").asText(null)), "Order should expose an orderDesc");
        }
    }

    private enum ServiceTarget {
        PRODUCT("PRODUCT_SERVICE_BASE_URL", "product.service.base-url", "http://product-service.ecommerce.svc.cluster.local:8500/product-service"),
        USER("USER_SERVICE_BASE_URL", "user.service.base-url", "http://user-service.ecommerce.svc.cluster.local:8700/user-service"),
        ORDER("ORDER_SERVICE_BASE_URL", "order.service.base-url", "http://order-service.ecommerce.svc.cluster.local:8300/order-service"),
        FAVOURITE("FAVOURITE_SERVICE_BASE_URL", "favourite.service.base-url", "http://favourite-service.ecommerce.svc.cluster.local:8800/favourite-service"),
        SHIPPING("SHIPPING_SERVICE_BASE_URL", "shipping.service.base-url", "http://shipping-service.ecommerce.svc.cluster.local:8600/shipping-service"),
        PAYMENT("PAYMENT_SERVICE_BASE_URL", "payment.service.base-url", "http://payment-service.ecommerce.svc.cluster.local:8400/payment-service");

        private final String envVar;
        private final String systemProperty;
        private final String defaultBaseUrl;
        private final String healthPath = "/actuator/health";

        ServiceTarget(String envVar, String systemProperty, String defaultBaseUrl) {
            this.envVar = envVar;
            this.systemProperty = systemProperty;
            this.defaultBaseUrl = defaultBaseUrl;
        }

        String unreachableMessage() {
            return describe() + " not reachable at " + resolveBaseUrl(this);
        }

        String describe() {
            switch (this) {
                case PRODUCT:
                    return "Product service";
                case USER:
                    return "User service";
                case ORDER:
                    return "Order service";
                case FAVOURITE:
                    return "Favourite service";
                case SHIPPING:
                    return "Shipping service";
                case PAYMENT:
                    return "Payment service";
                default:
                    return name();
            }
        }
    }
}
