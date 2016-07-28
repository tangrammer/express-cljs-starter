FROM naartjie/alpine-lein

WORKDIR /src

# preload dependencies in a separate layer
COPY project.clj /tmp/project.clj
RUN cd /tmp && lein deps && rm -rf /tmp/*

COPY . .

# version info
RUN \
  VERSION=$(git rev-parse --short HEAD) && \
  DATE=$(date +%Y-%m-%dT%H:%M:%S) && \
  if ! [[ -z "`git status -s`" ]]; then VERSION="!! DIRTY ${VERSION}"; fi && \
  sed -i "s/@@__VERSION__@@/${VERSION}/g;s/@@__BUILT__@@/${DATE}/g" ./src/main/resources/VERSION.edn

# RUN lein do clean, uberjar

EXPOSE 3000
# CMD ["java", "-cp", "/src/src/main/resources:/src/target/rebujito.jar", "ring.rebujito"]
CMD ["lein", "with-profiles", "production", "run"]
