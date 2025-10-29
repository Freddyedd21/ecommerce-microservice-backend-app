#!/usr/bin/env bash
set -e

# 1. Namespace
kubectl apply -f k8s/namespace.yaml

# 2. ConfigMaps
kubectl apply -f k8s/config-maps.yaml

# 3. Infra services
kubectl apply -f k8s/service-discovery-deployment.yaml
kubectl apply -f k8s/cloud-config-deployment.yaml
kubectl apply -f k8s/zipkin-deployment.yaml

# 4. Business microservices
kubectl apply -f k8s/favourite-service-deployment.yaml
kubectl apply -f k8s/order-service-deployment.yaml
kubectl apply -f k8s/payment-service-deployment.yaml
kubectl apply -f k8s/product-service-deployment.yaml
kubectl apply -f k8s/shipping-service-deployment.yaml
kubectl apply -f k8s/user-service-deployment.yaml
kubectl apply -f k8s/api-gateway-deployment.yaml

echo "Despliegue completo."
