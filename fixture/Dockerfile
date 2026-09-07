FROM eclipse-temurin:21-jre
ENV JAVA_OPTS="-Xmx512m -XX:+UseContainerSupport"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
