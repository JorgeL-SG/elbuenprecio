# Fase 1: Construcción con Maven y Java 21
# Usamos una imagen oficial que ya tiene todo lo necesario.
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean install -B -DskipTests

# Fase 2: Ejecución con solo el JRE de Java 21
# Usamos una imagen mínima para que sea más ligera.
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Copiamos solo el archivo .jar compilado desde la fase anterior.
COPY --from=build /app/target/*.jar app.jar
# Exponemos el puerto en el que corre Spring Boot.
EXPOSE 8080
# El comando para iniciar la aplicación.
ENTRYPOINT ["java", "-jar", "app.jar"]