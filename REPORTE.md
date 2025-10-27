# Reporte de configuración y despliegue

## 1. Separación de archivos Compose
- Se dividió el archivo principal en tres grupos:
  - `core.yml`: servicios centrales (zipkin, cloud-config, service-discovery).
  - `compose-gateway.yml`: api-gateway y proxy-client.
  - `compose-business.yml`: microservicios de negocio (product-service, user-service, order-service, payment-service, favourite-service, shipping-service).
- Se recomendó levantar los servicios en el siguiente orden para evitar saturar la PC y asegurar dependencias:
  1. Servicios centrales
  2. Gateway y proxy
  3. Microservicios de negocio

![1761369714268](image/REPORTE/1761369714268.png)

## 2. Docker y Compose
- Se revisaron y adaptaron los Dockerfile de cada microservicio para asegurar consistencia y correcta configuración de variables de entorno.
- Se corrigieron los compose.yml individuales para que todos los servicios se conecten a la red `microservices_network` y tengan las variables necesarias para Eureka, Config Server y Zipkin.

## 3. GitHub Actions
- Se crearon workflows para los seis microservicios principales:
  - product-service
  - user-service
  - order-service
  - payment-service
  - proxy-client
  - api-gateway
- Cada workflow compila el código, construye la imagen Docker y la publica en Docker Hub.

## 4. Kubernetes
- Se generaron manifiestos de despliegue y servicio para los seis microservicios principales y los servicios centrales (Eureka, Config Server, Zipkin).
- Se recomendó reemplazar `<DOCKER_USERNAME>` por el usuario real de Docker Hub antes de desplegar.
- Se indicaron los comandos para aplicar los manifiestos en el clúster.

## 5. Recomendaciones de uso
- Mantener solo un archivo core para los servicios centrales.
- Levantar los servicios por grupos para evitar saturación de recursos.
- Verificar la salud de los servicios en Eureka y Zipkin.
- Usar los workflows de GitHub Actions para automatizar el build y despliegue.
- Usar los manifiestos de Kubernetes para el despliegue en clúster.

## 6. Automatización CI/CD y Despliegue en Kubernetes

### Workflows de GitHub Actions
- Se crearon dos workflows principales en `.github/workflows/`:
  - `core-services-pipeline.yml`: Despliega los servicios centrales (Zipkin, Cloud Config, Service Discovery) en el clúster de Kubernetes usando los manifiestos en `k8s/`.
  - `api-gateway-pipeline.yml`: Compila, construye y publica la imagen Docker del API Gateway, y luego la despliega en Kubernetes.
- El workflow de servicios centrales debe ejecutarse primero para asegurar que Zipkin, Eureka y Config Server estén disponibles antes de desplegar el API Gateway y los microservicios de negocio.
- Ambos workflows usan un runner self-hosted y requieren los secretos `DOCKER_USERNAME`, `DOCKER_PASSWORD` y `KUBECONFIG` para autenticación y acceso al clúster.

### Manifiestos de Kubernetes
- Los manifiestos para Zipkin, Cloud Config y Service Discovery se encuentran en la carpeta `k8s/`:
  - `zipkin-deployment.yaml`
  - `cloud-config-deployment.yaml`
  - `service-discovery-deployment.yaml`
- Cada manifiesto define un Deployment y un Service (NodePort) para exponer los servicios en el clúster.
- El API Gateway también cuenta con sus propios manifiestos (`api-gateway-deployment.yaml`, `api-gateway-service.yaml`).

### Orden de despliegue recomendado
1. Ejecutar el workflow `core-services-pipeline.yml` para desplegar los servicios centrales.
2. Verificar que los pods y servicios estén corriendo correctamente (`kubectl get pods`, `kubectl get services`).
3. Ejecutar el workflow `api-gateway-pipeline.yml` para compilar, construir, publicar y desplegar el API Gateway.
4. Validar el acceso al API Gateway y la integración con los servicios centrales.

### Notas adicionales
- Si se requiere que el workflow del API Gateway espere a que los servicios centrales estén listos, se puede configurar una dependencia entre workflows en GitHub Actions usando `needs` o ejecutando manualmente en el orden correcto.
- Se recomienda documentar cualquier cambio en los manifiestos y workflows en este reporte para mantener trazabilidad.

---

Este reporte documenta todos los pasos y configuraciones realizadas para correr y desplegar el sistema de microservicios de manera óptima y escalable.
