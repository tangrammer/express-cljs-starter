(ns rebujito.store.mocks
  (:require [cheshire.core :as json]))

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

(def get-payment-method (json/parse-string "{
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
