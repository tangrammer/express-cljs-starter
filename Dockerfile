FROM naartjie/alpine-lein

WORKDIR /src

# preload dependencies in a separate layer
COPY project.clj /tmp/project.clj
RUN cd /tmp && lein deps && rm -rf /tmp/*

COPY . .
RUN lein do clean, uberjar

EXPOSE 3000
CMD ["java", "-cp", "/src/src/main/resources:/src/target/rebujito.jar", "ring.rebujito"]
