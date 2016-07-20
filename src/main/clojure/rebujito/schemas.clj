(ns rebujito.schemas
  (:require [schema.core :as s]))
;;:_id


(def PaymentMethodMongo
  {
   (s/optional-key :createdDate) (s/maybe String)
   :accountNumberLastFour String
   (s/optional-key :billingAddressId) String
   (s/optional-key :default) String
   :expirationMonth Long
   :expirationYear Long
   (s/optional-key :fullName) String
   (s/optional-key :nickName) (s/maybe String)
   :paymentType String
   :paymentMethodId String
   :routingNumber String
   })


(def AutoReloadMongo
  {
   (s/optional-key :createdDate) (s/maybe String)
   :autoReloadId String
   :cardId String
   :status (s/enum "active" "disabled" "enabled")
   :autoReloadType (s/enum "Date" "Amount")
   (s/optional-key :day) s/Num
   :triggerAmount s/Num
   :amount s/Num
   :paymentMethodId String
   :active Boolean
   })

(def PaymentMethodRes
  (-> PaymentMethodMongo
      (assoc :accountNumber nil
             :bankName nil
             :paymentMethodId String
             :isDefault Boolean)
      (dissoc :default
              :_id)))

(def UserProfileData
  {:emailAddress String
   :firstName String
   :lastName String
   :receiveStarbucksEmailCommunications Boolean
   (s/optional-key  :verifiedEmail)  Boolean})

(def UpdateMongoUser
  {(s/optional-key :emailAddress) (s/maybe String)
   (s/optional-key :firstName) (s/maybe String)
   (s/optional-key :lastName) (s/maybe String)
   (s/optional-key :birthDay) (s/conditional number? Integer :else String)
   (s/optional-key :birthMonth) (s/conditional number? Integer :else String)})

(def MongoCreateAccountAddress
  {:firstName String
   :lastName String
   :addressLine1 String
   (s/optional-key :addressLine2) String
   :city String
   :postalCode String
   :country String})

(def MongoUser
  (merge
   UserProfileData
   {
    (s/optional-key :createdDate) (s/maybe String)
    (s/optional-key :_id) org.bson.types.ObjectId
    :birthDay (s/conditional number? Integer :else String)
    :birthMonth (s/conditional number? Integer :else String)
    :market String
    :countrySubdivision String
    :password String
    :registrationSource String
    (s/optional-key :verifiedEmail) Boolean}))

(def MimiUser
  {:firstName String
   :lastName  String
   :email     String
   :postalcode String
   :city String
   :region String
   :birth {:dayOfMonth String
           :month String}})
