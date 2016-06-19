(ns rebujito.store.mocks
  (:require [cheshire.core :as json]
            [clojure.string :refer (split)]))


(def mimi-card {:id "7777064199346273"
                :currency "USD"
                :balance 125.0
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
           :balance 86.0
           :submarketCode "US"
           :cardId "85607AF09DD610"
           :class "242"
           :balanceDate "2014-03-07T19:52:42.0500000Z"
           :digital true
           :owner true
           :autoReloadProfile nil
           :balanceCurrencyCode "USD"})

(def transaction-history
  {:paging {:total 6
            :offset 0
            :limit 50
            :returned 6}
   :historyItems
   [{
      :historyId 123124
      :historyType "SvcTransactionWithPoints"
      :cardId "a5ede6d4-a4ae-4ec3-bded-758c863a9874"
      :isoDate "2016-06-16T22:28:59.0000000Z"
      :modifiedDate nil
      :currency "ZAR"
      :localCurrency "ZAR"
      :totalAmount 101.1
      :svcTransaction {
         :checkId nil
         :transactionType "Redemption"
         :isVoid false
         :localizedStoreName "Brokhurstspruit"
         :storeId "00303"
         :storeType "Physical"
         :localDate nil
         :currency "ZAR"
         :localCurrency "ZAR"
         :transactionAmount 0
         :localTransactionAmount 0
         :tax nil
         :newBalance 0
         :description nil
         :tipInfo {
            :tippable false
            :tippableEndDate nil
            :tipTransactionId nil
            :amount nil
            :status "None"
         }
      }
      :localTotalAmount 0
      :points [
         {
            :pointType "Purchases"
            :pointsEarned 2
            :promotionName "Purchase"
            :amount 0
            :currency 0
         }
      ]
      :coupon nil
   }
   {
      :historyId 100119
      :historyType "SvcTransactionWithPoints"
      :cardId "a5ede6d4-a4ae-4ec3-bded-758c863a9874"
      :isoDate "2016-06-16T20:42:37.0000000Z"
      :modifiedDate nil
      :currency "ZAR"
      :localCurrency "ZAR"
      :totalAmount 62.3
      :svcTransaction {
         :checkId nil
         :transactionType "Redemption"
         :isVoid false
         :localizedStoreName "Small Town"
         :storeId "00303"
         :storeType "Physical"
         :localDate nil
         :currency "ZAR"
         :localCurrency "ZAR"
         :transactionAmount 0
         :localTransactionAmount 0
         :tax nil
         :newBalance 0
         :description nil
         :tipInfo {
            :tippable false
            :tippableEndDate nil
            :tipTransactionId nil
            :amount nil
            :status "None"
         }
      }
      :localTotalAmount 6.24
      :points [
         {
            :pointType "Purchases"
            :pointsEarned 1
            :promotionName "Purchase"
            :amount 6.24
            :currency 0
         }
      ]
      :coupon nil
   }
   {
      :historyId 100174
      :historyType "Coupon"
      :cardId nil
      :isoDate "2016-06-16T00:00:00.0000000Z"
      :modifiedDate nil
      :currency nil
      :localCurrency nil
      :totalAmount nil
      :svcTransaction nil
      :localTotalAmount nil
      :points nil
      :coupon {
         :couponCode "BFB"
         :name "BIRTHDAY FREE BEVERAGE US"
         :issueDate nil
         :expirationDate "2016-07-16T10:21:44.0000000Z"
         :allowedRedemptionCount 1
         :voucherType "MSREarnCoupon"
         :status "Active"
         :startDate "2016-06-16T00:00:00.0000000Z"
         :lastRedemptionDate "1904-01-01T00:00:00.0000000Z"
         :redemptionCount 0
         :posCouponCode "593"
         :deliveryMethod "Email"
         :source "Unknown"
      }
   }
   {
      :historyId 100188
      :historyType "Coupon"
      :cardId nil
      :isoDate "2016-06-15T00:00:00.0000000Z"
      :modifiedDate nil
      :currency nil
      :localCurrency nil
      :totalAmount nil
      :svcTransaction nil
      :localTotalAmount nil
      :points nil
      :coupon {
         :couponCode "EFD"
         :name "EARNED FREE DRINK"
         :issueDate "2016-06-14T00:00:00.0000000Z"
         :expirationDate "2016-07-30T09:49:08.0000000Z"
         :allowedRedemptionCount 1
         :voucherType "MSREarnCoupon"
         :status "Available"
         :startDate "2016-06-13T00:00:00.0000000Z"
         :lastRedemptionDate "1904-01-01T00:00:00.0000000Z"
         :redemptionCount 0
         :posCouponCode "594"
         :deliveryMethod "Email"
         :source "Unknown"
      }
   }
   {
      :historyId 100207
      :historyType "SvcTransaction"
      :cardId "85607AFB93D21H"
      :isoDate "2016-06-10T20:28:59.0000000Z"
      :modifiedDate nil
      :currency "ZAR"
      :localCurrency "ZAR"
      :totalAmount 4.6
      :svcTransaction {
         :checkId "a5ede6d4-a4ae-4ec3-bded-758c863a9874"
         :transactionType "Redemption"
         :isVoid false
         :localizedStoreName "Bellevue"
         :storeId "00303"
         :storeType "Physical"
         :localDate "2016-06-09T12:28:59.0000000-07:00"
         :currency "ZAR"
         :localCurrency "ZAR"
         :transactionAmount 4.60
         :localTransactionAmount 4.60
         :tax nil
         :newBalance 249.31
         :description nil
         :tipInfo {
            :tippable false
            :tippableEndDate nil
            :tipTransactionId nil
            :amount nil
            :status "None"
         }
      }
      :localTotalAmount 4.6
      :points [
         {
            :pointType "Purchases"
            :pointsEarned 1
            :promotionName "Purchase"
            :amount 4.6
            :currency 0
         }
      ]
      :coupon nil
   }
   {
      :historyId 100207
      :historyType "Point"
      :cardId "a5ede6d4-a4ae-4ec3-bded-758c863a9874"
      :isoDate "2016-06-11T20:28:59.0000000Z"
      :modifiedDate "2016-06-12T18:05:08.4900000Z"
      :currency "ZAR"
      :localCurrency "ZAR"
      :totalAmount 4.6
      :svcTransaction nil
      :localTotalAmount 4.6
      :points [
         {
            :pointType "Purchases"
            :pointsEarned 1
            :promotionName "Purchase"
            :amount 4.6
            :currency 0
         }
      ]
      :coupon nil
   }]})

(def get-payment-method-detail (json/parse-string "{
   \"isTemporary\":false,
   \"routingNumber\":\"7a17ba06-deab-4e53-b7d9-ca4cfc3d94ec\",
   \"bankName\":null,
   \"paymentMethodId\":\"866F71F993D6\",
   \"billingAddressId\":null,
   \"paymentType\":\"Paypal\",
   \"accountNumberLastFour\":null,
   \"accountNumber\":\"12345678\",
   \"expirationMonth\":0,
   \"expirationYear\":0,
   \"cvn\":\"123\",
   \"fullName\":\"API Test User1\",
   \"default\":false,
   \"nickname\":\"SODOOO\"
}"
                                true))

(def put-payment-method-detail (json/parse-string "{
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
   \"receiveStarbucksEmailCommunications\": false,
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



(def me-profile (json/parse-string

  "{
   \"tippingPreferences\": {
      \"defaultAmount\": 13,
      \"enabled\": true,
      \"receivePushNotifications\": true
   },
   \"devices\": [
      {
         \"id\": \"80354465-386A-43FB-AE2C-93A5C576B72L\",
         \"applicationId\": \"2\",
         \"hardwareDeviceId\": \"80354465-386A-43FB-AE2C-93A5C576B72L\",
         \"osVersion\": \"6.0\",
         \"notificationToken\": null,
         \"startScreenPreference\": null,
         \"tabOrderPreference\": null,
         \"springBoardSequencePreference\": null,
         \"countryCode\": null,
         \"languageCode\": null,
         \"uaInboxUserName\": null,
         \"uaDeviceId\": null,
         \"pickOfTheWeekNotifications\": false,
         \"pickOfTheWeekRichNotifications\": false,
         \"specialOffersNotifications\": false,
         \"specialOffersRichNotifications\": false
      },
      {
         \"id\": \"80354465-386A-43FB-AE2C-93A5C576B72P\",
         \"applicationId\": \"2\",
         \"hardwareDeviceId\": \"80354465-386A-43FB-AE2C-93A5C576B72P\",
         \"osVersion\": \"6.0\",
         \"notificationToken\": null,
         \"startScreenPreference\": \"Last Screen\",
         \"tabOrderPreference\": \"0,9,10,11,1\",
         \"springBoardSequencePreference\": \"17,12,3,4,2,14,15\",
         \"countryCode\": \"US\",
         \"languageCode\": \"en\",
         \"uaInboxUserName\": \"qjr08tULSqyT78gmviW_Nw\",
         \"uaDeviceId\": null,
         \"pickOfTheWeekNotifications\": false,
         \"pickOfTheWeekRichNotifications\": false,
         \"specialOffersNotifications\": false,
         \"specialOffersRichNotifications\": false
      }
   ],
   \"favoriteStores\": [
      {
         \"storeId\": 22665,
         \"nickname\": \"SODOOO\"
      },
      {
         \"storeId\": 32245,
         \"nickname\": \"Sunshine\"
      },
      {
         \"storeId\": 33228,
         \"nickname\": \"Rainy Cafe\"
      }
   ],
   \"user\": {
      \"subMarket\": \"FR\",
      \"exId\": \"93A985A0-12E2-42B6-A995-AA701358BD47\",
      \"userName\": \"sbuxapitestuser@gmail.com\",
      \"firstName\": \"API Test\",
      \"lastName\": \"User1\",
      \"email\": \"sbuxapitestuser@gmail.com\",
      \"partner\": false
   },
   \"socialProfile\": {
      \"account\": {
         \"accountImageUrl\": null
      },
      \"facebook\": {
         \"facebookToken\": null
      },
      \"twitter\": {
         \"twitterUserId\": null,
         \"accessToken\": null,
         \"accessTokenSecret\": null,
         \"consumerKey\": null
      }
   },
   \"addresses\": [
      {
         \"addressId\": \"65c651fb-9fde-4c58-b757-c5cec1aa6c96\",
         \"type\": \"Shipping\",
         \"firstName\": \"API Test\",
         \"lastName\": \"User1\",
         \"phoneNumber\": \"918-444-2222\",
         \"addressLine1\": \"4545\",
         \"addressLine2\": null,
         \"city\": \"Seattle\",
         \"postalCode\": \"98122\",
         \"countrySubdivision\": \"Wa\",
         \"country\": \"US\",
         \"name\": null
      },
      {
         \"addressId\": \"b6cdb7f9-1660-40bc-a488-85401fae4520\",
         \"type\": \"Shipping\",
         \"firstName\": \"API Test\",
         \"lastName\": \"User1\",
         \"phoneNumber\": \"918-444-2222\",
         \"addressLine1\": \"4545\",
         \"addressLine2\": null,
         \"city\": \"Seattle\",
         \"postalCode\": \"98122\",
         \"countrySubdivision\": \"Wa\",
         \"country\": \"US\",
         \"name\": null
      },
      {
         \"addressId\": \"b45d049b-78e0-44f4-bcb0-32d9aecd69b8\",
         \"type\": \"Billing\",
         \"firstName\": \"API Test\",
         \"lastName\": \"User1\",
         \"phoneNumber\": \"918-444-2222\",
         \"addressLine1\": \"4454\",
         \"addressLine2\": null,
         \"city\": \"Seattle\",
         \"postalCode\": \"98122\",
         \"countrySubdivision\": \"Wa\",
         \"country\": \"US\",
         \"name\": null
      },
      {
         \"addressId\": \"706F596A-A147-4EBA-9CEA-2CFC0A2A31D8\",
         \"type\": \"Registration\",
         \"firstName\": \"API Test\",
         \"lastName\": \"User1\",
         \"phoneNumber\": null,
         \"addressLine1\": \"Your Street Address\",
         \"addressLine2\": null,
         \"city\": \"Seattle\",
         \"postalCode\": \"98118\",
         \"countrySubdivision\": \"WA\",
         \"country\": \"US\",
         \"name\": null
      }
   ],
   \"starbucksCards\": [
      {
         \"cardId\": \"856F75FA\",
         \"cardNumber\": \"7777005331886756\",
         \"nickname\": \"My Card (6756)\",
         \"type\": \"Standard\",
         \"balanceCurrencyCode\": \"USD\",
         \"submarketCode\": \"US\",
         \"balance\": 323.76,
         \"balanceDate\": \"2013-12-16T12:42:37.0000000Z\",
         \"imageUrls\": [
            {
               \"imageType\": \"ImageIcon\",
               \"uri\": \"http://globalassets.starbucks.com/images/cardimages/card_Coffee_Aroma_Card_FY11_88.png\"
            },
            {
               \"imageType\": \"ImageSmall\",
               \"uri\": \"http://globalassets.starbucks.com/images/cardimages/card_Coffee_Aroma_Card_FY11_115.png\"
            },
            {
               \"imageType\": \"ImageMedium\",
               \"uri\": \"http://globalassets.starbucks.com/images/cardimages/card_Coffee_Aroma_Card_FY11_165.png\"
            },
            {
               \"imageType\": \"ImageLarge\",
               \"uri\": \"http://globalassets.starbucks.com/images/cardimages/card_Coffee_Aroma_Card_FY11_261.png\"
            },
            {
               \"imageType\": \"iosThumb\",
               \"uri\": \"https://starbucks.com/images/mobilev3/card_Coffee_Aroma_Card_FY11_thumb_82.png\"
            },
            {
               \"imageType\": \"iosThumbHighRes\",
               \"uri\": \"https://starbucks.com/images/mobilev3/card_Coffee_Aroma_Card_FY11_thumb_82@2x.png\"
            },
            {
               \"imageType\": \"iosLarge\",
               \"uri\": \"https://starbucks.com/images/mobilev3/card_Coffee_Aroma_Card_FY11_270.png\"
            },
            {
               \"imageType\": \"iosLargeHighRes\",
               \"uri\": \"https://starbucks.com/images/mobilev3/card_Coffee_Aroma_Card_FY11_270@2x.png\"
            }
         ],
         \"primary\": false,
         \"partner\": false,
         \"autoReloadProfile\": null,
         \"digital\": false,
         \"owner\": true,
         \"actions\": [
            \"AutoReload\",
            \"Transfer\",
            \"Reload\"
         ]
      },
      {
         \"cardId\": \"85607AFB93D21F\",
         \"cardNumber\": \"7777064158669559\",
         \"nickname\": \"My Card (9559)\",
         \"type\": \"Standard\",
         \"balanceCurrencyCode\": \"USD\",
         \"submarketCode\": \"US\",
         \"balance\": 178.83,
         \"balanceDate\": \"2014-01-16T22:07:28.0000000Z\",
         \"imageUrls\": [
            {
               \"imageType\": \"ImageIcon\",
               \"uri\": \"http://globalassets.starbucks.com/images/cardimages/card_Coffee_Aroma_Card_FY11_88.png\"
            },
            {
               \"imageType\": \"ImageSmall\",
               \"uri\": \"http://globalassets.starbucks.com/images/cardimages/card_Coffee_Aroma_Card_FY11_115.png\"
            },
            {
               \"imageType\": \"ImageMedium\",
               \"uri\": \"http://globalassets.starbucks.com/images/cardimages/card_Coffee_Aroma_Card_FY11_165.png\"
            },
            {
               \"imageType\": \"ImageLarge\",
               \"uri\": \"http://globalassets.starbucks.com/images/cardimages/card_Coffee_Aroma_Card_FY11_261.png\"
            },
            {
               \"imageType\": \"iosThumb\",
               \"uri\": \"https://starbucks.com/images/mobilev3/card_Coffee_Aroma_Card_FY11_thumb_82.png\"
            },
            {
               \"imageType\": \"iosThumbHighRes\",
               \"uri\": \"https://starbucks.com/images/mobilev3/card_Coffee_Aroma_Card_FY11_thumb_82@2x.png\"
            },
            {
               \"imageType\": \"iosLarge\",
               \"uri\": \"https://starbucks.com/images/mobilev3/card_Coffee_Aroma_Card_FY11_270.png\"
            },
            {
               \"imageType\": \"iosLargeHighRes\",
               \"uri\": \"https://starbucks.com/images/mobilev3/card_Coffee_Aroma_Card_FY11_270@2x.png\"
            }
         ],
         \"primary\": true,
         \"partner\": false,
         \"autoReloadProfile\": null,
         \"digital\": false,
         \"owner\": true,
         \"actions\": [
            \"AutoReload\",
            \"Transfer\",
            \"Reload\"
         ]
      },
      {
         \"cardId\": \"85607AFB93D21E\",
         \"cardNumber\": \"7777064158671182\",
         \"nickname\": \"My Card (1182)\",
         \"type\": \"Standard\",
         \"balanceCurrencyCode\": \"USD\",
         \"submarketCode\": \"US\",
         \"balance\": 11,
         \"balanceDate\": \"2014-01-21T05:06:40.0000000Z\",
         \"imageUrls\": [
            {
               \"imageType\": \"ImageIcon\",
               \"uri\": \"http://globalassets.starbucks.com/images/cardimages/card_Coffee_Aroma_Card_FY11_88.png\"
            },
            {
               \"imageType\": \"ImageSmall\",
               \"uri\": \"http://globalassets.starbucks.com/images/cardimages/card_Coffee_Aroma_Card_FY11_115.png\"
            },
            {
               \"imageType\": \"ImageMedium\",
               \"uri\": \"http://globalassets.starbucks.com/images/cardimages/card_Coffee_Aroma_Card_FY11_165.png\"
            },
            {
               \"imageType\": \"ImageLarge\",
               \"uri\": \"http://globalassets.starbucks.com/images/cardimages/card_Coffee_Aroma_Card_FY11_261.png\"
            },
            {
               \"imageType\": \"iosThumb\",
               \"uri\": \"https://starbucks.com/images/mobilev3/card_Coffee_Aroma_Card_FY11_thumb_82.png\"
            },
            {
               \"imageType\": \"iosThumbHighRes\",
               \"uri\": \"https://starbucks.com/images/mobilev3/card_Coffee_Aroma_Card_FY11_thumb_82@2x.png\"
            },
            {
               \"imageType\": \"iosLarge\",
               \"uri\": \"https://starbucks.com/images/mobilev3/card_Coffee_Aroma_Card_FY11_270.png\"
            },
            {
               \"imageType\": \"iosLargeHighRes\",
               \"uri\": \"https://starbucks.com/images/mobilev3/card_Coffee_Aroma_Card_FY11_270@2x.png\"
            }
         ],
         \"primary\": false,
         \"partner\": false,
         \"autoReloadProfile\": null,
         \"digital\": false,
         \"owner\": true,
         \"actions\": [
            \"AutoReload\",
            \"Transfer\",
            \"Reload\"
         ]
      },
      {
         \"cardId\": \"85607AFB93D211\",
         \"cardNumber\": \"7777064158689581\",
         \"nickname\": \"tryMe\",
         \"type\": \"Standard\",
         \"balanceCurrencyCode\": \"USD\",
         \"submarketCode\": \"US\",
         \"balance\": 270.81,
         \"balanceDate\": \"2014-01-16T22:03:35.0000000Z\",
         \"imageUrls\": [
            {
               \"imageType\": \"ImageIcon\",
               \"uri\": \"http://globalassets.starbucks.com/images/cardimages/card_Coffee_Aroma_Card_FY11_88.png\"
            },
            {
               \"imageType\": \"ImageSmall\",
               \"uri\": \"http://globalassets.starbucks.com/images/cardimages/card_Coffee_Aroma_Card_FY11_115.png\"
            },
            {
               \"imageType\": \"ImageMedium\",
               \"uri\": \"http://globalassets.starbucks.com/images/cardimages/card_Coffee_Aroma_Card_FY11_165.png\"
            },
            {
               \"imageType\": \"ImageLarge\",
               \"uri\": \"http://globalassets.starbucks.com/images/cardimages/card_Coffee_Aroma_Card_FY11_261.png\"
            },
            {
               \"imageType\": \"iosThumb\",
               \"uri\": \"https://starbucks.com/images/mobilev3/card_Coffee_Aroma_Card_FY11_thumb_82.png\"
            },
            {
               \"imageType\": \"iosThumbHighRes\",
               \"uri\": \"https://starbucks.com/images/mobilev3/card_Coffee_Aroma_Card_FY11_thumb_82@2x.png\"
            },
            {
               \"imageType\": \"iosLarge\",
               \"uri\": \"https://starbucks.com/images/mobilev3/card_Coffee_Aroma_Card_FY11_270.png\"
            },
            {
               \"imageType\": \"iosLargeHighRes\",
               \"uri\": \"https://starbucks.com/images/mobilev3/card_Coffee_Aroma_Card_FY11_270@2x.png\"
            }
         ],
         \"primary\": false,
         \"partner\": false,
         \"autoReloadProfile\": null,
         \"digital\": false,
         \"owner\": true,
         \"actions\": [
            \"AutoReload\",
            \"Transfer\",
            \"Reload\"
         ]
      }
   ],
   \"paymentMethods\": [
      {
         \"paymentMethodId\": \"866174F89ED5\",
         \"billingAddressId\": \"b45d049b-78e0-44f4-bcb0-32d9aecd69b8\",
         \"paymentType\": \"VISA\",
         \"accountNumberLastFour\": \"1111\",
         \"expirationMonth\": 5,
         \"expirationYear\": 2017,
         \"fullName\": \"API Test User1\",
         \"isDefault\": true,
         \"nickname\": \"SODOOO1\"
      }
   ],
   \"rewardsSummary\": {
      \"reevaluationDate\": \"2014-11-09T23:59:59.0000000Z\",
      \"myTiers\": [
         {
            \"tierNumber\": 1,
            \"tierLevelName\": \"Welcome\",
            \"tierAnniversaryDate\": null
         },
         {
            \"tierNumber\": 2,
            \"tierLevelName\": \"Green\",
            \"tierAnniversaryDate\": \"2013-11-08T00:00:00.0000000Z\"
         },
         {
            \"tierNumber\": 3,
            \"tierLevelName\": \"Gold\",
            \"tierAnniversaryDate\": \"2013-11-11T00:00:00.0000000Z\"
         }
      ],
      \"currentLevel\": \"Gold\",
      \"nextLevel\": null,
      \"pointsTotal\": 101,
      \"pointsNeededForNextLevel\": -1,
      \"pointsNeededForNextFreeReward\": 7,
      \"pointsNeededForReevaluation\": 0,
      \"coupons\": [
         {
            \"status\": \"Available\",
            \"posCouponCode\": \"354\",
            \"deliveryMethod\": \"Email\",
            \"source\": null,
            \"allowedRedemptionCount\": 1,
            \"lastRedemptionDate\": \"1904-01-01T00:00:00.0000000Z\",
            \"redemptionCount\": 0,
            \"voucherType\": \"MSRPromotionalCoupon\",
            \"couponCode\": \"354\",
            \"expirationDate\": \"2023-11-09T08:54:55.0000000Z\",
            \"issueDate\": \"2013-11-11T00:00:00.0000000Z\",
            \"name\": \"Code 354-50% OFF ESPRESSO\",
            \"startDate\": \"2013-11-11T00:00:00.0000000Z\"
         },
         {
            \"status\": \"Available\",
            \"posCouponCode\": \"365\",
            \"deliveryMethod\": \"Email\",
            \"source\": null,
            \"allowedRedemptionCount\": 1,
            \"lastRedemptionDate\": \"1904-01-01T00:00:00.0000000Z\",
            \"redemptionCount\": 0,
            \"voucherType\": \"MSRPromotionalCoupon\",
            \"couponCode\": \"365\",
            \"expirationDate\": \"2023-11-10T14:43:49.0000000Z\",
            \"issueDate\": \"2013-11-12T00:00:00.0000000Z\",
            \"name\": \"Code 365-BOGO TEA\",
            \"startDate\": \"2013-11-12T00:00:00.0000000Z\"
         },
         {
            \"status\": \"Available\",
            \"posCouponCode\": \"367\",
            \"deliveryMethod\": \"Email\",
            \"source\": null,
            \"allowedRedemptionCount\": 1,
            \"lastRedemptionDate\": \"1904-01-01T00:00:00.0000000Z\",
            \"redemptionCount\": 0,
            \"voucherType\": \"MSRPromotionalCoupon\",
            \"couponCode\": \"367\",
            \"expirationDate\": \"2023-11-10T14:44:03.0000000Z\",
            \"issueDate\": \"2013-11-12T00:00:00.0000000Z\",
            \"name\": \"Code 367-FREE TEA\",
            \"startDate\": \"2013-11-12T00:00:00.0000000Z\"
         },
         {
            \"status\": \"Available\",
            \"posCouponCode\": \"398\",
            \"deliveryMethod\": \"Email\",
            \"source\": null,
            \"allowedRedemptionCount\": 1,
            \"lastRedemptionDate\": \"1904-01-01T00:00:00.0000000Z\",
            \"redemptionCount\": 0,
            \"voucherType\": \"MSRPromotionalCoupon\",
            \"couponCode\": \"398\",
            \"expirationDate\": \"2023-11-10T09:24:49.0000000Z\",
            \"issueDate\": \"2013-11-12T00:00:00.0000000Z\",
            \"name\": \"Code 398-FREE OATMEAL\",
            \"startDate\": \"2013-11-12T00:00:00.0000000Z\"
         },
         {
            \"status\": \"Available\",
            \"posCouponCode\": \"560\",
            \"deliveryMethod\": \"Email\",
            \"source\": null,
            \"allowedRedemptionCount\": 1,
            \"lastRedemptionDate\": \"1904-01-01T00:00:00.0000000Z\",
            \"redemptionCount\": 0,
            \"voucherType\": \"MSRPromotionalCoupon\",
            \"couponCode\": \"560\",
            \"expirationDate\": \"2023-11-09T09:50:42.0000000Z\",
            \"issueDate\": \"2013-11-11T00:00:00.0000000Z\",
            \"name\": \"Code 560-FREE TALL DRINK\",
            \"startDate\": \"2013-11-11T00:00:00.0000000Z\"
         },
         {
            \"status\": \"Available\",
            \"posCouponCode\": \"560\",
            \"deliveryMethod\": \"Email\",
            \"source\": null,
            \"allowedRedemptionCount\": 1,
            \"lastRedemptionDate\": \"1904-01-01T00:00:00.0000000Z\",
            \"redemptionCount\": 0,
            \"voucherType\": \"MSRPromotionalCoupon\",
            \"couponCode\": \"560\",
            \"expirationDate\": \"2023-11-10T14:56:36.0000000Z\",
            \"issueDate\": \"2013-11-12T00:00:00.0000000Z\",
            \"name\": \"Code 560-FREE TALL DRINK\",
            \"startDate\": \"2013-11-12T00:00:00.0000000Z\"
         },
         {
            \"status\": \"Available\",
            \"posCouponCode\": \"569\",
            \"deliveryMethod\": \"Email\",
            \"source\": null,
            \"allowedRedemptionCount\": 1,
            \"lastRedemptionDate\": \"1904-01-01T00:00:00.0000000Z\",
            \"redemptionCount\": 0,
            \"voucherType\": \"MSRPromotionalCoupon\",
            \"couponCode\": \"569\",
            \"expirationDate\": \"2023-11-10T09:23:45.0000000Z\",
            \"issueDate\": \"2013-11-12T00:00:00.0000000Z\",
            \"name\": \"Code 569-$1 OFF ESPRESSO/TEA\",
            \"startDate\": \"2013-11-12T00:00:00.0000000Z\"
         },
         {
            \"status\": \"Available\",
            \"posCouponCode\": \"647\",
            \"deliveryMethod\": \"Email\",
            \"source\": null,
            \"allowedRedemptionCount\": 1,
            \"lastRedemptionDate\": \"1904-01-01T00:00:00.0000000Z\",
            \"redemptionCount\": 0,
            \"voucherType\": \"MSRPromotionalCoupon\",
            \"couponCode\": \"647\",
            \"expirationDate\": \"2023-11-09T09:51:27.0000000Z\",
            \"issueDate\": \"2013-11-11T00:00:00.0000000Z\",
            \"name\": \"Code 647-$10 OFF $30 PURCHASE\",
            \"startDate\": \"2013-11-11T00:00:00.0000000Z\"
         },
         {
            \"status\": \"Available\",
            \"posCouponCode\": \"660\",
            \"deliveryMethod\": \"Email\",
            \"source\": null,
            \"allowedRedemptionCount\": 1,
            \"lastRedemptionDate\": \"1904-01-01T00:00:00.0000000Z\",
            \"redemptionCount\": 0,
            \"voucherType\": \"MSRPromotionalCoupon\",
            \"couponCode\": \"660\",
            \"expirationDate\": \"2023-11-09T09:50:19.0000000Z\",
            \"issueDate\": \"2013-11-11T00:00:00.0000000Z\",
            \"name\": \"Code 660-$5 OFF PRODUCT\",
            \"startDate\": \"2013-11-11T00:00:00.0000000Z\"
         },
         {
            \"status\": \"Available\",
            \"posCouponCode\": \"715\",
            \"deliveryMethod\": \"Email\",
            \"source\": null,
            \"allowedRedemptionCount\": 1,
            \"lastRedemptionDate\": \"1904-01-01T00:00:00.0000000Z\",
            \"redemptionCount\": 0,
            \"voucherType\": \"MSRPromotionalCoupon\",
            \"couponCode\": \"715\",
            \"expirationDate\": \"2023-11-09T08:55:10.0000000Z\",
            \"issueDate\": \"2013-11-11T00:00:00.0000000Z\",
            \"name\": \"Code 715-FREE FOOD ITEM\",
            \"startDate\": \"2013-11-11T00:00:00.0000000Z\"
         },
         {
            \"status\": \"Available\",
            \"posCouponCode\": \"715\",
            \"deliveryMethod\": \"Email\",
            \"source\": null,
            \"allowedRedemptionCount\": 1,
            \"lastRedemptionDate\": \"1904-01-01T00:00:00.0000000Z\",
            \"redemptionCount\": 0,
            \"voucherType\": \"MSRPromotionalCoupon\",
            \"couponCode\": \"715\",
            \"expirationDate\": \"2023-11-10T14:56:42.0000000Z\",
            \"issueDate\": \"2013-11-12T00:00:00.0000000Z\",
            \"name\": \"Code 715-FREE FOOD ITEM\",
            \"startDate\": \"2013-11-12T00:00:00.0000000Z\"
         },
         {
            \"status\": \"Available\",
            \"posCouponCode\": \"727\",
            \"deliveryMethod\": \"Email\",
            \"source\": null,
            \"allowedRedemptionCount\": 1,
            \"lastRedemptionDate\": \"1904-01-01T00:00:00.0000000Z\",
            \"redemptionCount\": 0,
            \"voucherType\": \"MSRPromotionalCoupon\",
            \"couponCode\": \"727\",
            \"expirationDate\": \"2023-11-10T15:16:26.0000000Z\",
            \"issueDate\": \"2013-11-12T00:00:00.0000000Z\",
            \"name\": \"Code 727-MSR FREE DRINK\",
            \"startDate\": \"2013-11-12T00:00:00.0000000Z\"
         }
      ],
      \"cardHolderSinceDate\": \"2013-11-06T00:00:00.0000000Z\",
      \"pointsEarnedTowardNextFreeReward\": 5,
      \"dateRetrieved\": \"2014-02-05T23:35:30.5102177Z\",
      \"rewardsProgram\": {
         \"programName\": \"My Starbucks Rewards\",
         \"numberOfTiers\": 3,
         \"countryCodes\": [
            \"US\",
            \"CA\"
         ],
         \"tierInfos\": [
            {
               \"tierNumber\": 1,
               \"tierLevelName\": \"Welcome\",
               \"tierPointsEntryThreshold\": 0,
               \"tierPointsExitThreshold\": 5,
               \"tierPointsReevaluationThreshold\": null,
               \"tierPointsFreeItemThreshold\": null
            },
            {
               \"tierNumber\": 2,
               \"tierLevelName\": \"Green\",
               \"tierPointsEntryThreshold\": 5,
               \"tierPointsExitThreshold\": 30,
               \"tierPointsReevaluationThreshold\": 5,
               \"tierPointsFreeItemThreshold\": null
            },
            {
               \"tierNumber\": 3,
               \"tierLevelName\": \"Gold\",
               \"tierPointsEntryThreshold\": 30,
               \"tierPointsExitThreshold\": null,
               \"tierPointierPointsReevaluationThreshold\": 30,
               \"tierPointsFreeItemThreshold\": 12
            }
         ]
      }
   }
}"

  true))

(def me-rewards (json/parse-string
  "{
   \"currentLevel\":\"Mocked\",
   \"nextLevel\":null,
   \"reevaluationDate\":\"2014-11-09T23:59:59.0000000Z\",
   \"pointsTotal\":101,
   \"pointsNeededForNextLevel\":-1,
   \"pointsNeededForNextFreeReward\":7,
   \"pointsEarnedTowardNextFreeReward\":5,
   \"pointsNeededForReevaluation\":0,
   \"cardHolderSinceDate\":\"2013-11-06T00:00:00.0000000Z\",
   \"dateRetrieved\":\"2014-01-07T00:46:28.1991817Z\",
   \"myTiers\":[
      {
         \"tierNumber\":1,
         \"tierLevelName\":\"Welcome\",
         \"tierAnniversaryDate\":null
      },
      {
         \"tierNumber\":2,
         \"tierLevelName\":\"Green\",
         \"tierAnniversaryDate\":\"2013-11-08T00:00:00.0000000Z\"
      },
      {
         \"tierNumber\":3,
         \"tierLevelName\":\"Gold\",
         \"tierAnniversaryDate\":\"2013-11-11T00:00:00.0000000Z\"
      }
   ],
   \"rewardsProgram\":{
      \"programName\":\"My Starbucks Rewards\",
      \"numberOfTiers\":3,
      \"countryCodes\":[
         \"US\",
         \"CA\"
      ],
      \"tierInfos\":[
         {
            \"tierNumber\":1,
            \"tierLevelName\":\"Welcome\",
            \"tierPointsEntryThreshold\":0,
            \"tierPointsExitThreshold\":5,
            \"tierPointsReevaluationThreshold\":null,
            \"tierPointsFreeItemThreshold\":null
         },
         {
            \"tierNumber\":2,
            \"tierLevelName\":\"Green\",
            \"tierPointsEntryThreshold\":5,
            \"tierPointsExitThreshold\":30,
            \"tierPointsReevaluationThreshold\":5,
            \"tierPointsFreeItemThreshold\":null
         },
         {
            \"tierNumber\":3,
            \"tierLevelName\":\"Gold\",
            \"tierPointsEntryThreshold\":30,
            \"tierPointsExitThreshold\":null,
            \"tierPointsReevaluationThreshold\":30,
            \"tierPointsFreeItemThreshold\":12
         }
      ]
   },
   \"coupons\":[
      {
         \"couponCode\":\"354\",
         \"name\":\"Code 354-50% OFF ESPRESSO\",
         \"issueDate\":\"2013-11-11T00:00:00.0000000Z\",
         \"expirationDate\":\"2023-11-09T08:54:55.0000000Z\",
         \"allowedRedemptionCount\":1,
         \"voucherType\":\"MSRPromotionalCoupon\",
         \"status\":\"Available\",
         \"startDate\":\"2013-11-11T00:00:00.0000000Z\",
         \"lastRedemptionDate\":\"1904-01-01T00:00:00.0000000Z\",
         \"redemptionCount\":0,
         \"posCouponCode\":\"354\",
         \"deliveryMethod\":\"Email\",
         \"source\":\"Unknown\"
      },
      {
         \"couponCode\":\"365\",
         \"name\":\"Code 365-BOGO TEA\",
         \"issueDate\":\"2013-11-12T00:00:00.0000000Z\",
         \"expirationDate\":\"2023-11-10T14:43:49.0000000Z\",
         \"allowedRedemptionCount\":1,
         \"voucherType\":\"MSRPromotionalCoupon\",
         \"status\":\"Available\",
         \"startDate\":\"2013-11-12T00:00:00.0000000Z\",
         \"lastRedemptionDate\":\"1904-01-01T00:00:00.0000000Z\",
         \"redemptionCount\":0,
         \"posCouponCode\":\"365\",
         \"deliveryMethod\":\"Email\",
         \"source\":\"Unknown\"
      },
      {
         \"couponCode\":\"367\",
         \"name\":\"Code 367-FREE TEA\",
         \"issueDate\":\"2013-11-12T00:00:00.0000000Z\",
         \"expirationDate\":\"2023-11-10T14:44:03.0000000Z\",
         \"allowedRedemptionCount\":1,
         \"voucherType\":\"MSRPromotionalCoupon\",
         \"status\":\"Available\",
         \"startDate\":\"2013-11-12T00:00:00.0000000Z\",
         \"lastRedemptionDate\":\"1904-01-01T00:00:00.0000000Z\",
         \"redemptionCount\":0,
         \"posCouponCode\":\"367\",
         \"deliveryMethod\":\"Email\",
         \"source\":\"Unknown\"
      },
      {
         \"couponCode\":\"398\",
         \"name\":\"Code 398-FREE OATMEAL\",
         \"issueDate\":\"2013-11-12T00:00:00.0000000Z\",
         \"expirationDate\":\"2023-11-10T09:24:49.0000000Z\",
         \"allowedRedemptionCount\":1,
         \"voucherType\":\"MSRPromotionalCoupon\",
         \"status\":\"Available\",
         \"startDate\":\"2013-11-12T00:00:00.0000000Z\",
         \"lastRedemptionDate\":\"1904-01-01T00:00:00.0000000Z\",
         \"redemptionCount\":0,
         \"posCouponCode\":\"398\",
         \"deliveryMethod\":\"Email\",
         \"source\":\"Unknown\"
      },
      {
         \"couponCode\":\"560\",
         \"name\":\"Code 560-FREE TALL DRINK\",
         \"issueDate\":\"2013-11-11T00:00:00.0000000Z\",
         \"expirationDate\":\"2023-11-09T09:50:42.0000000Z\",
         \"allowedRedemptionCount\":1,
         \"voucherType\":\"MSRPromotionalCoupon\",
         \"status\":\"Available\",
         \"startDate\":\"2013-11-11T00:00:00.0000000Z\",
         \"lastRedemptionDate\":\"1904-01-01T00:00:00.0000000Z\",
         \"redemptionCount\":0,
         \"posCouponCode\":\"560\",
         \"deliveryMethod\":\"Email\",
         \"source\":\"Unknown\"
      },
      {
         \"couponCode\":\"560\",
         \"name\":\"Code 560-FREE TALL DRINK\",
         \"issueDate\":\"2013-11-12T00:00:00.0000000Z\",
         \"expirationDate\":\"2023-11-10T14:56:36.0000000Z\",
         \"allowedRedemptionCount\":1,
         \"voucherType\":\"MSRPromotionalCoupon\",
         \"status\":\"Available\",
         \"startDate\":\"2013-11-12T00:00:00.0000000Z\",
         \"lastRedemptionDate\":\"1904-01-01T00:00:00.0000000Z\",
         \"redemptionCount\":0,
         \"posCouponCode\":\"560\",
         \"deliveryMethod\":\"Email\",
         \"source\":\"Unknown\"
      },
      {
         \"couponCode\":\"569\",
         \"name\":\"Code 569-$1 OFF ESPRESSO/TEA\",
         \"issueDate\":\"2013-11-12T00:00:00.0000000Z\",
         \"expirationDate\":\"2023-11-10T09:23:45.0000000Z\",
         \"allowedRedemptionCount\":1,
         \"voucherType\":\"MSRPromotionalCoupon\",
         \"status\":\"Available\",
         \"startDate\":\"2013-11-12T00:00:00.0000000Z\",
         \"lastRedemptionDate\":\"1904-01-01T00:00:00.0000000Z\",
         \"redemptionCount\":0,
         \"posCouponCode\":\"569\",
         \"deliveryMethod\":\"Email\",
         \"source\":\"Unknown\"
      },
      {
         \"couponCode\":\"647\",
         \"name\":\"Code 647-$10 OFF $30 PURCHASE\",
         \"issueDate\":\"2013-11-11T00:00:00.0000000Z\",
         \"expirationDate\":\"2023-11-09T09:51:27.0000000Z\",
         \"allowedRedemptionCount\":1,
         \"voucherType\":\"MSRPromotionalCoupon\",
         \"status\":\"Available\",
         \"startDate\":\"2013-11-11T00:00:00.0000000Z\",
         \"lastRedemptionDate\":\"1904-01-01T00:00:00.0000000Z\",
         \"redemptionCount\":0,
         \"posCouponCode\":\"647\",
         \"deliveryMethod\":\"Email\",
         \"source\":\"Unknown\"
      },
      {
         \"couponCode\":\"660\",
         \"name\":\"Code 660-$5 OFF PRODUCT\",
         \"issueDate\":\"2013-11-11T00:00:00.0000000Z\",
         \"expirationDate\":\"2023-11-09T09:50:19.0000000Z\",
         \"allowedRedemptionCount\":1,
         \"voucherType\":\"MSRPromotionalCoupon\",
         \"status\":\"Available\",
         \"startDate\":\"2013-11-11T00:00:00.0000000Z\",
         \"lastRedemptionDate\":\"1904-01-01T00:00:00.0000000Z\",
         \"redemptionCount\":0,
         \"posCouponCode\":\"660\",
         \"deliveryMethod\":\"Email\",
         \"source\":\"Unknown\"
      },
      {
         \"couponCode\":\"715\",
         \"name\":\"Code 715-FREE FOOD ITEM\",
         \"issueDate\":\"2013-11-11T00:00:00.0000000Z\",
         \"expirationDate\":\"2023-11-09T08:55:10.0000000Z\",
         \"allowedRedemptionCount\":1,
         \"voucherType\":\"MSRPromotionalCoupon\",
         \"status\":\"Available\",
         \"startDate\":\"2013-11-11T00:00:00.0000000Z\",
         \"lastRedemptionDate\":\"1904-01-01T00:00:00.0000000Z\",
         \"redemptionCount\":0,
         \"posCouponCode\":\"715\",
         \"deliveryMethod\":\"Email\",
         \"source\":\"Unknown\"
      },
      {
         \"couponCode\":\"715\",
         \"name\":\"Code 715-FREE FOOD ITEM\",
         \"issueDate\":\"2013-11-12T00:00:00.0000000Z\",
         \"expirationDate\":\"2023-11-10T14:56:42.0000000Z\",
         \"allowedRedemptionCount\":1,
         \"voucherType\":\"MSRPromotionalCoupon\",
         \"status\":\"Available\",
         \"startDate\":\"2013-11-12T00:00:00.0000000Z\",
         \"lastRedemptionDate\":\"1904-01-01T00:00:00.0000000Z\",
         \"redemptionCount\":0,
         \"posCouponCode\":\"715\",
         \"deliveryMethod\":\"Email\",
         \"source\":\"Unknown\"
      },
      {
         \"couponCode\":\"727\",
         \"name\":\"Code 727-MSR FREE DRINK\",
         \"issueDate\":\"2013-11-12T00:00:00.0000000Z\",
         \"expirationDate\":\"2023-11-10T15:16:26.0000000Z\",
         \"allowedRedemptionCount\":1,
         \"voucherType\":\"MSRPromotionalCoupon\",
         \"status\":\"Available\",
         \"startDate\":\"2013-11-12T00:00:00.0000000Z\",
         \"lastRedemptionDate\":\"1904-01-01T00:00:00.0000000Z\",
         \"redemptionCount\":0,
         \"posCouponCode\":\"727\",
         \"deliveryMethod\":\"Email\",
         \"source\":\"Unknown\"
      }
   ]
}" true
))


(def address-put-payload{
  :addressLine1 "1236"
  :addressLine2 ""
  :city "Seattle"
  :country "US"
  :countrySubdivision "Wa"
  :firstName "George"
  :lastName "Gd"
  :name ""
  :phoneNumber "123-456-7810"
  :postalCode "98122"
  :type "Billing"
  })
