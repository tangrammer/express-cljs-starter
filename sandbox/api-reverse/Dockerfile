FROM mhart/alpine-node:5.10

WORKDIR /src
ADD ./target ./target
ADD ./node_modules ./node_modules

ENV NODE_ENV=production
EXPOSE 3000
CMD ["node", "target"]