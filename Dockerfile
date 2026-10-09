# Etapa 1: Compilación de la aplicación
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copiar wrappers y configuración de Gradle
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Permisos de ejecución
RUN chmod +x gradlew

# Copiar código fuente y compilar
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# Etapa 2: Imagen ligera de ejecución
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copiar el artefacto compilado
COPY --from=builder /app/build/libs/*.jar app.jar

# Exponer puerto
EXPOSE 8081

# Comando de inicio
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
