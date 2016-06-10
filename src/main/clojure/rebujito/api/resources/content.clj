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
