(ns rebujito.security.jwt-test
  (:require [rebujito.security.jwt :refer (jws-sign claims jws-unsign)]))

(jws-sign (claims {:hola "juan" } "id" 3) "aa")
(jws-unsign (jws-sign (claims {:hola "juan" :scopes :hola } "rebuj" 3) "key-secret") "key-secret")
