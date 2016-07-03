(ns rebujito.api.resources.content)

(def terms-json {:name "Terms of Use"
              	 :item {:id nil
                    		:templateId nil
                    		:templateName "Mobile Content"
                    		:displayname "Terms of Use"
                    		:headline ""
                    		:image ""
                    		:imagehires ""
                    		:key ""
                    		:sharetext ""
                    		:text (slurp (clojure.java.io/resource "mocks/lorem_ipsum.html"))
                    		:updated nil}
              	 :children []})

(def settings-json {:appSettings
                    {:markets
                     {:ZA
                      {:currencyCode "ZAR"

                       :features {:signIn true
                                  :dashboard true
                                  :rewards true
                                  :pay true
                                  :menu false}

                       :pay {:reload {:enabled true
                                      :buttonValue1 "100"
                                      :buttonValue2 "500"
                                      :buttonValue3 "1000"}

                             :autoreload {:enabled true
                                          :amounts "100, 500, 1000, 1500"}}
                       :locales {:en-ZA {:website "http://www.starbucks.co.za"}}}}}})
