(ns co.za.swarmloyalty.rebujito.mocks
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
