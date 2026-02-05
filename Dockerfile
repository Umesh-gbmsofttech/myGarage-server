# ---------- Stage 1: Build ----------
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# Copy jar
COPY --from=build /app/target/*.jar app.jar

# Copy Aiven CA cert
COPY ca.pem /app/ca.pem

# Import cert into Java truststore
RUN keytool -importcert -file /app/ca.pem \
    -alias aiven-ca \
    -keystore $JAVA_HOME/lib/security/cacerts \
    -storepass changeit -noprompt

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=$PORT"]
