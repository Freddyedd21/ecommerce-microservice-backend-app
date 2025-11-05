
FROM eclipse-temurin:11-jre
ARG PROJECT_VERSION=0.1.0
ENV SPRING_PROFILES_ACTIVE dev
WORKDIR /home/app
COPY ./product-service/target/product-service-v${PROJECT_VERSION}.jar ./product-service.jar
EXPOSE 8500
ENTRYPOINT ["java", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "product-service.jar"]


