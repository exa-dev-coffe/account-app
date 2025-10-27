# ====== Build Stage ======
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# 1️⃣ Copy POM dulu buat cache dependency
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 2️⃣ Copy source dan build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ====== Run Stage ======
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# 3️⃣ Copy JAR hasil build
COPY --from=build /app/target/*.jar app.jar

# 4️⃣ Tambahkan environment default agar JVM aware dengan container
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:+AlwaysPreTouch \
               -XX:+UseStringDeduplication \
               -XX:+HeapDumpOnOutOfMemoryError \
               -XX:HeapDumpPath=/tmp"

# 5️⃣ Expose port
EXPOSE 8080

# 6️⃣ Jalankan dengan arg JVM
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
