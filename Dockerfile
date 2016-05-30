FROM naartjie/alpine-lein-node

WORKDIR /src
COPY . .
RUN lein do clean, uberjar

EXPOSE 3000
CMD ["java", "-cp", "/src/src/main/resources:/src/target/rebujito.jar", "ring.rebujito"]
