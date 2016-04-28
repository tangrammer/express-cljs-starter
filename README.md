# rebujito
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



# development flow


```
lein repl
user> (dev)
dev> (reset)
dev> (open-app)
```


# Copyright and License

Copyright © 2016 swarmloyalty.co.za
