FROM naartjie/alpine-lein

WORKDIR /src

# preload dependencies in a separate layer
COPY project.clj /tmp/project.clj
RUN cd /tmp && lein deps && rm -rf /tmp/*

COPY . .
RUN lein do clean, uberjar

# version info
RUN cd /src && \
  VERSION=$(git rev-parse --short HEAD) && \
  DATE=$(date +%Y-%m-%dT%H:%M:%S) && \
  if ! [[ -z "`git status -s`" ]]; then VERSION="!! DIRTY ${VERSION}"; fi && \
  printf "version=${VERSION}\ndate=${DATE}\n" > ./VERSION

EXPOSE 3000
CMD ["java", "-cp", "/src/src/main/resources:/src/target/rebujito.jar", "ring.rebujito"]
