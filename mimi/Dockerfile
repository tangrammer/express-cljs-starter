FROM mhart/alpine-node:6.1

WORKDIR /src

RUN apk add --no-cache openssh git

WORKDIR /src

# add deploy key for npm 'private' modules on github
COPY deploy.ssh /root/.ssh/
RUN chmod 600 /root/.ssh/*

# dependencies before code trick: http://bit.ly/1QfnlUR
COPY package.json /tmp/package.json
RUN cd /tmp && NODE_ENV=production npm install
RUN cp -a /tmp/node_modules /src/node_modules

COPY . .

# remove deploy key from image
RUN rm -rf /root/.ssh/

# version info
RUN \
  VERSION=$(git rev-parse --short HEAD) && \
  DATE=$(date +%Y-%m-%dT%H:%M:%S) && \
  if ! [[ -z "`git status -s`" ]]; then VERSION="!! DIRTY ${VERSION}"; fi && \
  sed -i "s/@@__VERSION__@@/${VERSION}/g;s/@@__BUILT__@@/${DATE}/g" package.json

EXPOSE 3000
CMD ["npm", "start"]
