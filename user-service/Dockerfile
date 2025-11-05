
FROM eclipse-temurin:11-jre
ARG PROJECT_VERSION=0.1.0
ENV SPRING_PROFILES_ACTIVE dev
WORKDIR /home/app
COPY ./user-service/target/user-service-v${PROJECT_VERSION}.jar ./user-service.jar
EXPOSE 8700
ENTRYPOINT ["java", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "user-service.jar"]


