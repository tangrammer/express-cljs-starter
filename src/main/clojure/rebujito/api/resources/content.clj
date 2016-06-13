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
                                      :buttonValue1 "50"
                                      :buttonValue2 "100"
                                      :buttonValue3 "150"}

                             :autoreload {:enabled true
                                          :amounts "50, 100, 150, 200"}}
                       :locales {:en-ZA {:website "http://www.starbucks.co.za"}}}}}})
