# rebujito
<img src="https://raw.githubusercontent.com/naartjie/rebujito/master/src/main/resources/rebujito.jpg?token=AAsqtc4zNpTssdFZabxNOVPc9i2JEFuiks5XWnqzwA%3D%3D" width="250">

A clojure starbucks api ([wiki](https://github.com/naartjie/rebujito/wiki)).

# config files
+ `src/main/resources/config.edn`
+ `~/.secrets.edn`

# mock vs prod components

+ Using **mock-store**, this is the default option, but you can explicitly change the system using `(set-env! :+mock-store)`  & `(reset)`

```
 lein repl
 user> (dev)
 dev> (set-env! :+mock-store)
 dev> (reset)
 => :reloading ()
 => using :+mock-store profile in dev-system
 => using :mock-store profile in dependency dev-system
 => :ready

```

+ Using **prod-store** `(set-env!)`  & `(reset)`


```
 dev> (set-env!)
 dev> (reset)
 => :ready 
``` 


# auto-increment sequence in mongo

The rebujito/system now includes :counter-store

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


```


# development flow


```
lein repl
user> (dev)
dev> (reset)
dev> (open-app)
```


# Copyright and License

Copyright © 2016 swarmloyalty.co.za
