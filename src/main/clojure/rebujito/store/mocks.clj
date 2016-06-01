(ns rebujito.store.mocks
  (:require [cheshire.core :as json]
            [clojure.string :refer (split)]))


(def mimi-card {:id "7777064199346273"
                :currency "USD"
                :balance 0.0
                :balanceDate "2014-03-07T19:52:42.0500000Z"})

(def card {:cardNumber "7777064199346273"
           :primary false
           :cardCurrency "USD"
           :partner false
           :nickname "My Card (6273)"
           :type "Standard"
           :actions ["AutoReload" "Transfer" "Reload"]
           :imageUrls
           [
            {:imageType "ImageIcon"
             :uri
             "http://globalassets.starbucks.com/images/cardimages/card_Coffee_Aroma_Card_FY11_88.png"}
            {:imageType "ImageSmall"
             :uri
             "http://globalassets.starbucks.com/images/cardimages/card_Coffee_Aroma_Card_FY11_115.png"}
            {:imageType "ImageMedium"
             :uri
             "http://globalassets.starbucks.com/images/cardimages/card_Coffee_Aroma_Card_FY11_165.png"}
            {:imageType "ImageLarge"
             :uri
             "http://globalassets.starbucks.com/images/cardimages/card_Coffee_Aroma_Card_FY11_261.png"}
            {:imageType "iosThumb"
             :uri
             "http://starbucks.com/images/mobilev3/card_Coffee_Aroma_Card_FY11_thumb_82.png"}
            {:imageType "iosThumbHighRes"
             :uri
             "http://starbucks.com/images/mobilev3/card_Coffee_Aroma_Card_FY11_thumb_82@2x.png"}
            {:imageType "iosLarge"
             :uri
             "http://starbucks.com/images/mobilev3/card_Coffee_Aroma_Card_FY11_270.png"}
            {:imageType "iosLargeHighRes"
             :uri
             "http://starbucks.com/images/mobilev3/card_Coffee_Aroma_Card_FY11_270@2x.png"}]
           :balance 0.0
           :submarketCode "US"
           :cardId "85607AF09DD610"
           :class "242"
           :balanceDate "2014-03-07T19:52:42.0500000Z"
           :digital true
           :owner true
           :autoReloadProfile nil
           :balanceCurrencyCode "USD"})


(def get-payment-method-detail (json/parse-string "{
   \"isTemporary\":false,
   \"routingNumber\":null,
   \"bankName\":null,
   \"paymentMethodId\":\"866F71F993D6\",
   \"billingAddressId\":null,
   \"paymentType\":\"Paypal\",
   \"accountNumberLastFour\":null,
   \"accountNumber\":\"F67A77FC92D518AC9BF69B1028BCF7A711B1\",
   \"expirationMonth\":0,
   \"expirationYear\":0,
   \"cvn\":null,
   \"fullName\":\"API Test User1\",
   \"default\":false,
   \"nickname\":\"SODOOO\"
}"
                                true))

(def post-payment-method (json/parse-string "{
   \"isTemporary\":false,
   \"routingNumber\":\"F67A77FC92D518AC9BF69B1028BCF7A711B1\",
   \"bankName\":null,
   \"paymentMethodId\":\"866F71F993D6\",
   \"billingAddressId\":\"1234567890\",
   \"paymentType\":\"Paypal\",
   \"accountNumberLastFour\":\"1234\",
   \"accountNumber\":\"1234567890123456\",
   \"expirationMonth\":0,
   \"expirationYear\":0,
   \"cvn\":\"123\",
   \"fullName\":\"API Test User1\",
   \"default\":false,
   \"nickname\":\"SODOOO\"
}"
                             true))


(def get-payment-method (vec (json/parse-string  "[
   {
      \"paymentMethodId\": \"86617AFB92D8\",
      \"billingAddressId\": \"b45d049b-78e0-44f4-bcb0-32d9aecd69b8\",
      \"type\": \"amex\",
      \"accountNumberLastFour\": \"0005\",
      \"accountNumber\": \"\",
      \"expirationMonth\": 2,
      \"expirationYear\": 2016,
      \"fullName\": \"test amex\",
      \"default\": true,
      \"nickname\": \"my amex\"
   },
   {
      \"paymentMethodId\": \"86617AFB92D8\",
      \"billingAddressId\": \"b45d049b-78e0-44f4-bcb0-32d9aecd69b8\",
      \"type\": \"VISA\",
      \"accountNumberLastFour\": \"1111\",
      \"accountNumber\": \"\",
      \"expirationMonth\": 3,
      \"expirationYear\": 2013,
      \"fullName\": \"First Last\",
      \"default\": false,
      \"nickname\": \"my Visa\"
   }
]"
                                                 true)))


(defn extract-body-form
  ([s] (extract-body-form s keyword))
  ([s fkw] (let [data (split s #"&")]
                (apply array-map
                       (mapcat #(let [[k v] (split % #"=")]
                                  [(fkw k) v]) data)))))

(def oauth-token-body (extract-body-form "grant_type=password&client_id=kcpttbyxc7rt4kzvyvmgxqvg&client_secret=APIPassword&username=XTest&password=aaaaaa&scope=test_scope"))

(def oauth-refresh-token-body (extract-body-form "grant_type=refresh_token&refresh_token=chmeua4zhprntu8hyvp68yk9&client_id=kcpttbyxc7rt4kzvyvmgxqvg&client_secret=jonsPassword"))


(json/generate-string oauth-refresh-token-body)


(def account (-> "{
   \"addressLine1\": \"757 Richards Street\",
   \"birthDay\": \"31\",
   \"birthMonth\": \"9\",
   \"city\": \"Vancouver\",
   \"countrySubdivision\": \"BC\",
   \"country\": \"CA\",
   \"emailAddress\": \"json2@coffee.com\",
   \"firstName\": \"Jessica\",
   \"lastName\": \"Sons\",
   \"password\": \"jsonPa$$w0rd2\",
   \"postalCode\": \"V6B 3A6\",
   \"receiveStarbucksEmailCommunications\": \"\",
   \"registrationSource\": \"myMobileApp\"
}"
     (json/parse-string true)))


(def post-token-resource-owner  (-> "{
   \"return_type\": \"json\",
   \"access_token\": \"chrrndqxyj7ctqqwbszfed4x\",
   \"token_type\": \"bearer\",
   \"expires_in\": 3600,
   \"refresh_token\": \"chmeua4zhprntu8hyvp68yk9\",
   \"scope\": \"test_scope\",
   \"state\": null,
   \"uri\": null,
   \"extended\": null
}"

                                    (json/parse-string true)))



(def post-refresh-token (->
                         "{
   \"return_type\": \"json\",
   \"access_token\": \"fuam55n4eudrre6uuqwjk7ca\",
   \"token_type\": \"bearer\",
   \"expires_in\": 3600,
   \"refresh_token\": \"cqj2tg24ekjtapncahtkpav4\",
   \"scope\": \"test_scope\",
   \"state\": null,
   \"uri\": null,
   \"extended\": null
}"
                         (json/parse-string true)))


(json/parse-string "{
   \"paymentMethodId\": \"806E77\",
   \"nickname\": \"My Updated Nickname\",
   \"type\": \"amex\",
   \"fullName\": \"John Smith\",
   \"default\": true,
   \"accountNumberLastFour\": \"0005\",
   \"accountNumber\": \"1234567891230005\",
   \"cvn\": \"987\",
   \"expirationMonth\": 2,
   \"expirationYear\": 2016,
   \"billingAddressId\": \"{E912B1F2-39CB-4286-AEA4-7650AB63FB8B}\"
}" true)
