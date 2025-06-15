FROM flink:1.16.3-scala_2.12-java11

# Install AWS SDK and DynamoDB connector
RUN wget -P /opt/flink/lib/ https://repo1.maven.org/maven2/org/apache/flink/flink-connector-dynamodb/4.0.0-1.16/flink-connector-dynamodb-4.0.0-1.16.jar
RUN wget -P /opt/flink/lib/ https://repo1.maven.org/maven2/software/amazon/awssdk/dynamodb/2.20.56/dynamodb-2.20.56.jar
RUN wget -P /opt/flink/lib/ https://repo1.maven.org/maven2/software/amazon/awssdk/aws-core/2.20.56/aws-core-2.20.56.jar
RUN wget -P /opt/flink/lib/ https://repo1.maven.org/maven2/software/amazon/awssdk/auth/2.20.56/auth-2.20.56.jar
RUN wget -P /opt/flink/lib/ https://repo1.maven.org/maven2/software/amazon/awssdk/regions/2.20.56/regions-2.20.56.jar
RUN wget -P /opt/flink/lib/ https://repo1.maven.org/maven2/software/amazon/awssdk/sdk-core/2.20.56/sdk-core-2.20.56.jar
RUN wget -P /opt/flink/lib/ https://repo1.maven.org/maven2/software/amazon/awssdk/http-client-spi/2.20.56/http-client-spi-2.20.56.jar
RUN wget -P /opt/flink/lib/ https://repo1.maven.org/maven2/software/amazon/awssdk/apache-client/2.20.56/apache-client-2.20.56.jar
RUN wget -P /opt/flink/lib/ https://repo1.maven.org/maven2/software/amazon/awssdk/profiles/2.20.56/profiles-2.20.56.jar
RUN wget -P /opt/flink/lib/ https://repo1.maven.org/maven2/software/amazon/awssdk/json-utils/2.20.56/json-utils-2.20.56.jar
RUN wget -P /opt/flink/lib/ https://repo1.maven.org/maven2/software/amazon/awssdk/third-party-jackson-core/2.20.56/third-party-jackson-core-2.20.56.jar
RUN wget -P /opt/flink/lib/ https://repo1.maven.org/maven2/software/amazon/awssdk/utils/2.20.56/utils-2.20.56.jar
RUN wget -P /opt/flink/lib/ https://repo1.maven.org/maven2/software/amazon/awssdk/metrics-spi/2.20.56/metrics-spi-2.20.56.jar

# Set AWS region
ENV AWS_REGION=ap-southeast-1

# Copy SQL scripts
COPY ./flink-job/src/main/resources/flink-sql /opt/flink/sql

WORKDIR /opt/flink