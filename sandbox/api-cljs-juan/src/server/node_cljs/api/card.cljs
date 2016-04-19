(ns ^:figwheel-always node-cljs.api.card)

(def imageUrls [{:imageType "ImageIcon"
                  :uri
                  "http://globalassets.starbucks.com/images/cardimages/card_Coffee_Aroma_Card_FY11_88.png"}
                 {:imageType "ImageSmall",
                  :uri
                  "http://globalassets.starbucks.com/images/cardimages/card_Coffee_Aroma_Card_FY11_115.png"}
                 {:imageType "ImageMedium",
                  :uri
                  "http://globalassets.starbucks.com/images/cardimages/card_Coffee_Aroma_Card_FY11_165.png"}
                 {:imageType "ImageLarge",
                  :uri
                  "http://globalassets.starbucks.com/images/cardimages/card_Coffee_Aroma_Card_FY11_261.png"}
                 {:imageType "iosThumb",
                  :uri
                  "http://starbucks.com/images/mobilev3/card_Coffee_Aroma_Card_FY11_thumb_82.png"}
                 {:imageType "iosThumbHighRes",
                  :uri
                  "http://starbucks.com/images/mobilev3/card_Coffee_Aroma_Card_FY11_thumb_82@2x.png"}
                 {:imageType "iosLarge",
                  :uri
                  "http://starbucks.com/images/mobilev3/card_Coffee_Aroma_Card_FY11_270.png"}
                 {:imageType "iosLargeHighRes",
                  :uri
                  "http://starbucks.com/images/mobilev3/card_Coffee_Aroma_Card_FY11_270@2x.png"}])

(defn data []
  {:cardNumber "7777064199346273"
   :primary false
   :cardCurrency "USD"
   :partner false
   :nickname "My Card (6273)"
   :type "Standard"
   :actions ["AutoReload" "Transfer" "Reload"]
   :imageUrls imageUrls
   :balance 0.0
   :submarketCode "US"
   :cardId "85607AF09DD610"
   :class "242"
   :balanceDate "2014-03-07T19:52:42.0500000Z"
   :digital true
   :owner true
   :autoReloadProfile nil
   :balanceCurrencyCode "USD"})
