(ns starbucks.schema
  (:require [schema.core :as s]))

(s/defschema StarbucksEntry {:username String :userid String :password String})

(s/defschema Starbucks {s/Int StarbucksEntry})

(s/defschema UserPort (s/both s/Int (s/pred #(<= 1024 % 65535))))

(s/defschema TokenRequest {:grant_type String
                           :client_id String
                           :client_secret String
                           :code String
                           :redirect_url String
                           :scope String
                           })

(s/defschema Config
  {:vhosts [s/Str]
   :port UserPort
   :entries Starbucks})


