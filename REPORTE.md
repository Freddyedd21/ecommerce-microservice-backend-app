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

---

Este reporte documenta todos los pasos y configuraciones realizadas para correr y desplegar el sistema de microservicios de manera óptima y escalable.
