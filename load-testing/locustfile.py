"""Locust scenarios that exercise real API Gateway user journeys.

Each virtual user completes the same flow that the end-to-end tests cover:
1. Browse the product catalogue.
2. Open the details of the first product returned.
3. Review the enriched favourites list.
4. Inspect the shipping summary.
5. Check payment status for orders.

Point Locust at the API Gateway base URL (for example http://127.0.0.1:18080)
and keep the Minikube port-forwards active while the load test runs.
"""

import os
from typing import Any, Dict, Optional

from locust import HttpUser, SequentialTaskSet, between, task

_TIMEOOUT_SECONDS = 45


def _extract_collection(payload: Dict[str, Any]) -> Optional[list]:
    collection = payload.get("collection") if isinstance(payload, dict) else None
    return collection if isinstance(collection, list) else None


class MarketplaceJourney(SequentialTaskSet):
    """Sequential user flow that mirrors the critical UI journey."""

    product_id: Optional[int] = None

    def on_start(self) -> None:
        self.product_id = None

    @task
    def browse_product_catalogue(self) -> None:
        with self.client.get(
            "/product-service/api/products",
            name="GET /product-service/api/products",
            timeout=_TIMEOOUT_SECONDS,
            catch_response=True,
        ) as response:
            if not response.ok:
                response.failure(f"Unexpected status {response.status_code}")
                return
            try:
                payload = response.json()
            except ValueError as exc:
                response.failure(f"Invalid JSON: {exc}")
                return

            products = _extract_collection(payload)
            if not products:
                response.failure("Product catalogue response did not contain data")
                return

            first_product = products[0]
            product_id = first_product.get("productId")
            if not isinstance(product_id, int):
                response.failure("Product collection missing numeric productId")
                return

            self.product_id = product_id
            response.success()

    @task
    def view_product_details(self) -> None:
        if self.product_id is None:
            # Previous step already recorded the failure.
            return

        with self.client.get(
            f"/product-service/api/products/{self.product_id}",
            name="GET /product-service/api/products/{id}",
            timeout=_TIMEOOUT_SECONDS,
            catch_response=True,
        ) as response:
            if not response.ok:
                response.failure(f"Unexpected status {response.status_code}")
                return
            try:
                payload = response.json()
            except ValueError as exc:
                response.failure(f"Invalid JSON: {exc}")
                return

            if payload.get("productId") != self.product_id:
                response.failure("Product endpoint did not return the expected id")
                return

            response.success()

    @task
    def review_favourites(self) -> None:
        self._validate_collection_endpoint(
            path="/favourite-service/api/favourites",
            name="GET /favourite-service/api/favourites",
            required_fields=("user", "product"),
        )

    @task
    def inspect_shipping_summary(self) -> None:
        self._validate_collection_endpoint(
            path="/shipping-service/api/shippings",
            name="GET /shipping-service/api/shippings",
            required_fields=("order", "product"),
        )

    @task
    def check_payment_status(self) -> None:
        self._validate_collection_endpoint(
            path="/payment-service/api/payments",
            name="GET /payment-service/api/payments",
            required_fields=("paymentStatus", "order"),
        )

    def _validate_collection_endpoint(
        self,
        *,
        path: str,
        name: str,
        required_fields: tuple[str, ...],
    ) -> None:
        with self.client.get(
            path,
            name=name,
            timeout=_TIMEOOUT_SECONDS,
            catch_response=True,
        ) as response:
            if not response.ok:
                response.failure(f"Unexpected status {response.status_code}")
                return

            try:
                payload = response.json()
            except ValueError as exc:
                response.failure(f"Invalid JSON: {exc}")
                return

            collection = _extract_collection(payload)
            if not collection:
                response.failure("Collection response did not contain elements")
                return

            first_item = collection[0]
            missing = [field for field in required_fields if field not in first_item]
            if missing:
                response.failure(f"Payload missing fields: {', '.join(missing)}")
                return

            response.success()


class EcommerceUser(HttpUser):
    tasks = [MarketplaceJourney]
    wait_time = between(1, 3)

    # Allow overriding via API_GATEWAY_BASE_URL to mirror the integration profile.
    host = os.getenv("API_GATEWAY_BASE_URL", "http://127.0.0.1:18080")
