# Commie - SWARM microservice for sending user communications

Right now it's just push, but it is a placeholder for SMS / emails down the line too.

### Development

`> lein deps`

`> lein figwheel dev`

`> lein run`

### Production

`> lein do clean, deps, cljsbuild server once`

`> node target/prod`