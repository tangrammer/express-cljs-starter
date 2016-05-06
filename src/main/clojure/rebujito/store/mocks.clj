(ns rebujito.store.mocks
  (:require [cheshire.core :as json]
            [clojure.string :refer (split)]))

(def card (json/parse-string "{
   \"cardCurrency\":\"USD\",
   \"cardId\":\"85607AF09DD610\",
   \"cardNumber\":\"7777064199346273\",
   \"nickname\":\"My Card (6273)\",
   \"class\":\"242\",
   \"type\":\"Standard\",
   \"balanceCurrencyCode\":\"USD\",
   \"submarketCode\":\"US\",
   \"balance\":0.00,
   \"balanceDate\":\"2014-03-07T19:52:42.0500000Z\",
   \"imageUrls\":[{
         \"imageType\":\"ImageIcon\",
         \"uri\":\"http://globalassets.starbucks.com/images/cardimages/card_Coffee_Aroma_Card_FY11_88.png\"},
      {
         \"imageType\":\"ImageSmall\",
         \"uri\":\"http://globalassets.starbucks.com/images/cardimages/card_Coffee_Aroma_Card_FY11_115.png\"
               },
      {
         \"imageType\":\"ImageMedium\",
         \"uri\":\"http://globalassets.starbucks.com/images/cardimages/card_Coffee_Aroma_Card_FY11_165.png\"
               },
      {
         \"imageType\":\"ImageLarge\",
         \"uri\":\"http://globalassets.starbucks.com/images/cardimages/card_Coffee_Aroma_Card_FY11_261.png\"
               },
      {
         \"imageType\":\"iosThumb\",
         \"uri\":\"http://starbucks.com/images/mobilev3/card_Coffee_Aroma_Card_FY11_thumb_82.png\"
               },
      {
         \"imageType\":\"iosThumbHighRes\",
         \"uri\":\"http://starbucks.com/images/mobilev3/card_Coffee_Aroma_Card_FY11_thumb_82@2x.png\"
               },
      {
         \"imageType\":\"iosLarge\",
         \"uri\":\"http://starbucks.com/images/mobilev3/card_Coffee_Aroma_Card_FY11_270.png\"
               },
      {
         \"imageType\":\"iosLargeHighRes\",
         \"uri\":\"http://starbucks.com/images/mobilev3/card_Coffee_Aroma_Card_FY11_270@2x.png\"
               }
   ],
   \"primary\":false,
   \"partner\":false,
   \"autoReloadProfile\":null,
   \"digital\":true,
   \"owner\":true,
   \"actions\":[
      \"AutoReload\",
      \"Transfer\",
      \"Reload\"
   ]
  }"
                             true))

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


(def oauth (let [data (split "grant_type=password&client_id=kcpttbyxc7rt4kzvyvmgxqvg&client_secret=APIPassword&username=XTest&password=aaaaaa&scope=test_scope" #"&")]
             (apply array-map
                    (mapcat #(let [[k v] (split % #"=")]
                               [(keyword k) v]) data))))


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

                                    (json/parse-string true)

                                    ))


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
