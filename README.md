<img src="http://1.bp.blogspot.com/-r1m0A2EBBY4/Tin_mKbCLQI/AAAAAAAAARQ/Bjna3dRK2wY/s640/rebujito.jpg" width="250">

# rebujito :: Starbucks API
Backend API (written in clojure) ([wiki](https://github.com/naartjie/rebujito/wiki)).

## config files
+ `src/main/resources/config.edn`
 
Environment values automatically switched based on [aero](https://github.com/juxt/aero)


# Main parts

## Webserver
[aleph](https://github.com/ztellman/aleph) => [netty](https://github.com/netty/netty)

## Database 
Mongo

## System
[component](https://github.com/stuartsierra/component)   
Production system components and their dependencies are defined in [src/main/clojure/rebujito/system.clj](https://github.com/naartjie/rebujito/blob/master/src/main/clojure/rebujito/system.clj)


## Async
The asynchronous communication is solved most of the time based in [ztellman/manifold](https://github.com/ztellman/manifold) specifically with its [deferred](https://github.com/ztellman/manifold/blob/master/docs/deferred.md) functionality

TODO: 


### development flow
Based on [stuartsierra reload pattern](http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded)

Following previous link documentation and to get into development you need to set/load `dev` namespace using `(dev)` fn from `user` namespace

```
lein repl
user> (dev)
dev> 
```

Then, to reload and start the system just type `(reset)`

```
dev> (reset)
```

Once that you get `(reset)` with no errors, you can use your system directly from your repl    

Example using the system: 

```
dev> (:keys system)
=> (:jquery :api :db :user-store :api-client-store :webserver :yada :token-store :swagger-ui :counter-store :mimi :mailer :crypto :authorizer :docsite-router :db-conn :payment-gateway :authenticator :webhook-store)
```

Example using system component

```
dev> (p/generate-token (:authenticator system) {:wow true} 5)
=> "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXUyJ9.eyJ3b3ciOnRydWUsImlzcyI6InJlYnVqaXRvLWF1dGgiLCJpYXQiOjE0Njg0MDAwNjksIm5iZiI6MTQ2ODQwMDA2OSwianRpIjoiOGZlZGQ2YTUtNDU1Ni00ZWNhLWFiOTgtZjU2ZTNiOGJhYzQ3In0.3w-IXV-a5Whyo8gje1EK68-HJYQzgETZz3AICaEm5xg"
```



And, if you got any error on `(reset)` call derived from code evaluation/compilation or start initialisation, type `(refresh)` and check the error trace in the repl, try to fix it, then `(refresh)` again and if the error dissapear then you can call again `(reset)` to get your system running again



To open swagger-ui in your default browser just type in the `(open-app)` in your repl from `dev` namespace

```
dev> (open-app)
```


### Development system configuration: mock vs prod components
You can change your system configuration in development with `(set-env! & modifiers)` fn

Currently there are the following [modifiers](https://github.com/naartjie/rebujito/blob/3ce3489da60e1db8be87dc9fe3c939dc02af1068/src/main/clojure/rebujito/system/dev_system.clj#L15-L47):` :+mock-mailer :+ephemeral-db :+mock-mimi :+mock-payment-gateway` 

Example of using mock-mailer   `(set-env! :+mock-mailer)`  & `(reset)`


```
 lein repl
 user> (dev)
 dev> (set-env! :+mock-mailer)
 dev> (reset)
 => :reloading ()
 => 16-07-13 08:57:11 WARN [rebujito.system.dev-system:19] - using :+mock-mailer profile in dev-system
 => :ready

```

To remove current env modifiers just type  `(set-env!)`  & `(reset)`


```
 dev> (set-env!)
 dev> (reset)
 => :ready 
``` 


# Logging
[com.taoensso/timbre](https://github.com/ptaoussanis/timbre)

You can configure `timbre` levels in the same way as you do with log4j in java

```clojure 
(taoensso.timbre/set-config!
 (rebujito.logging/log-config [["rebujito.*" :warn]
                               ["rebujito.security.*" :warn]
                               ["rebujito.mongo" :info]
                               ["rebujito.mimi" :info]
                               ["rebujito.mimi.*" :info]
                               ["rebujito.mongo.*" :info]
                               ["rebujito.api.*" :info]
                               ["rebujito.api.util" :warn]]))
```

# Bugs report
TODO: bugsnag


# OAUTH: authentication and authorisation

Security checks are implemented on top of [acccess_token query parameter](https://github.com/naartjie/rebujito/blob/7edde51b929b54faa7222877de01924dfb91aeec/src/main/clojure/rebujito/api/util.clj#L88-L92)

Previous fn dispatch to [verify](https://github.com/naartjie/rebujito/blob/7edde51b929b54faa7222877de01924dfb91aeec/src/main/clojure/rebujito/api/util.clj#L128-L135) definition in each yada resource

You can realise that the `verify` fn can return something or nothing. And depending [the result](https://github.com/naartjie/rebujito/blob/7edde51b929b54faa7222877de01924dfb91aeec/src/main/clojure/rebujito/api/util.clj#L99-L117) we return `401` in case we don't have a readable jwt result or `403` if this jwt result doesn't have enough roles for this resource


Besides that, we can double (double because we have time expiration limit in the jwt token) check the live  of this token making an [extra query](https://github.com/naartjie/rebujito/blob/7edde51b929b54faa7222877de01924dfb91aeec/src/main/clojure/rebujito/api/util.clj#L132) to the token-store to know if the token keeps valid. This extra check is added in `rebujito.api` resource definition with the extra flag `:check-valid-token-store true` as you can see [here](https://github.com/naartjie/rebujito/blob/7edde51b929b54faa7222877de01924dfb91aeec/src/main/clojure/rebujito/api.clj#L73) 


The jwt token can only be generated using **:authorizer** component  `(p/grant (:authorizer system) {:your-data ""})`




## :authenticator
JWT Authenticator Impl

```clojure
(defprotocol Authenticator
  (read-token [this token])
  (generate-token [this data minutes]))

```


## :authorizer 

Oauth impl

```clojure
(defprotocol Authorizer
  (grant
    [this data scopes]
    [this data scopes time-in-minutes])
  (invalidate! [this user-id])
  (protected-data [this refresh-token]))
```

** :authorizer depends_on/uses both :authenticator and :token-store**


Basically :authorizer functionality is used by following endpoints

+  `POST /oauth/token` impl in `rebujito.api.resources.oauth/token` 
+  `POST /account/create` impl in `rebujito.api.resources.account/create` 
+  `GET /me/logout` impl in `rebujito.api.resources.login/logout`


and in all 


# Components used directly from api/resources

## :mimi

This component wraps the http calls to the nodejs micro services 

```clojure
(defprotocol Mimi
  (create-account [this data])
  (update-account [this data])
  (register-physical-card [this data])
  (increment-balance! [this card-number amount type])
  (balances [this data])
  (get-history [this card-number])
  (transfer [this from to])
  (issue-coupon [this card-number coupon-type]))
```


## :payment-gateway

This component wraps the http calls to the webservices payment-gateway service 


```clojure
(defprotocol PaymentGateway
  (create-card-token [this data])
  (delete-card-token [this data])
  (execute-payment [this data]))
```

## :crypto
new-sha256-encrypter that implememts 

```clojure
(defprotocol Encrypter
  (sign [_ data])
  (check [this unhash hashed]))
```

## :mailer
A sendgrid impl mailer that accomplish following protocol

```clojure
(defprotocol MailService
  (send [this data]))
```

Global config for this component is inside `config.edn` 
```
:mailer {:from "info@swarmloyalty.co.za"
          :api {:url "https://api.sendgrid.com/v3/mail/send"
                :token "xxxx"}}
```

An schema validation for each data mail is included [too](https://github.com/naartjie/rebujito/blob/7edde51b929b54faa7222877de01924dfb91aeec/src/main/clojure/rebujito/mailer.clj#L14-L19)

Also there's a [mock](https://github.com/naartjie/rebujito/blob/7edde51b929b54faa7222877de01924dfb91aeec/src/main/clojure/rebujito/mailer.clj#L62-L72) version of this MailService protocol used in tests or in dev `(set-env! :+mock-mailer)`

Notice that there is an optional `hidden` [field](https://github.com/naartjie/rebujito/blob/7edde51b929b54faa7222877de01924dfb91aeec/src/main/clojure/rebujito/mailer.clj#L18) for testing purposes (i.e verifing token-links ). 

## :user-store
This mongo storage implements besides basic protocols/MutableStorate following ones:

```clojure
(defprotocol UserStore
  (add-autoreload-profile-card [this oid autoreload-profile-card])
  (disable-auto-reload [this oid card-id])
  (insert-card! [this oid card])
  (get-user-and-card [this card-number]
  (search-count [this firstName lastName emailAddress cardNumber])
  (search [this firstName lastName emailAddress cardNumber sort-by offset limit]))

(defprotocol UserPaymentMethodStore
  (add-new-payment-method [this oid p])
  (get-payment-method [this oid payment-method-id])
  (remove-payment-method [this oid payment-method])
  (update-payment-method [this oid payment-method])
  (get-payment-methods [this oid]))

(defprotocol UserAddressStore
  (insert-address [this oid address])
  (get-addresses [this oid])
  (get-address [this oid address-id])
  (remove-address [this oid address])
  (update-address [this oid address]))

(defprotocol UserCardStore
  (update-card-number [this oid old-card-number new-card-number]))

```

:user-store is the main tool for managing all the data related with persistent users thus it has so much protocols attached


## :api-client-store

Just used to manage "api-keys" mongo collection and following protocol (besides protocols/MutableStorate)

```clojure 
(defprotocol ApiClient
  (login [this id pw]))
```

## :webhook-store
This mongo storage implements besides basic protocols/MutableStorate , the protocols/WebhookStore to manage running webhooks process states in `rebujito.api.resources.card.reload/check` fn called from `GET /check-reload/{card-number}` 

```clojure
(defprotocol WebhookStore
  (webhook-uuid [this uuid])
  (change-state [this webhook-uuid state])
  (current [this webhook-uuid]))
```

Basically the internal logic for this webhook [card.reload/check](https://github.com/naartjie/rebujito/blob/7edde51b929b54faa7222877de01924dfb91aeec/src/main/clojure/rebujito/api/resources/card/reload.clj#L29-L135) runs only if the current state for the `webhook-uuid` (this uuid is based in the card-number) is equal to `done` or `ready`. If running the process we get into some problem the state is changed to `error`, on the contrary the state is changed to `done` when it finishs.



## :counter-store

**Safe auto-increment sequence in mongo**

```clojure

  :counter-store (new-counter-store (:auth config) false {:digital-card-number (read-string (format "96235709%05d" 0))})

```

This component has to include in the construcor a map with the counter-ids and counter-initial-values
 `{:digital-card-number 100 :other 35 :more 45}`
 
Then you can use the protocol/Counter with this component

```clojure 

(defprotocol Counter
  (increment! [this counter-name])
  (deref [this counter-name])) ;; idempotent

```

so ...

```clojure

dev> (p/increment! (:counter-store system) :digital-card-number)
9623570900001

dev> (p/deref (:counter-store system) :digital-card-number)
9623570900001

dev> (p/deref (:counter-store system) :digital-card-number)
9623570900001

dev> (p/increment! (:counter-store system) :digital-card-number)
9623570900002

dev> (p/deref (:counter-store system) :digital-card-number)
9623570900002

;; with "other" count-name-seq
dev> (p/increment! (:counter-store system) :other)
36

dev> (p/deref (:counter-store system) :other)
36


```




# Copyright and License

Copyright © 2016 swarmloyalty.co.za
