(ns rebujito.schemas
  (:require [schema.core :as s]))
;;              :_id


(def PaymentMethodMongo
  {
   :accountNumberLastFour String
   (s/optional-key :billingAddressId) String
   (s/optional-key :default) String
   :expirationMonth Long
   :expirationYear Long
   (s/optional-key :fullName) String
   (s/optional-key :nickName) s/Any
   :paymentType String
   :paymentMethodId String
   :routingNumber String
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
   }
  )

(def MongoUser
  (merge
   UserProfileData
   {
    (s/optional-key :_id) org.bson.types.ObjectId
    :addressLine1 String
    :addressLine2 String
    :birthDay (s/conditional number? Integer :else String)
    :birthMonth (s/conditional number? Integer :else String)
    :city String
    :country String
    :countrySubdivision String
    :password String
    :postalCode String
    :receiveStarbucksEmailCommunications Boolean
    :registrationSource String
    }))


(def MimiUser
  {
   :firstName String
   :lastName  String
   :email     String
   :postalcode String
   :city String
   :region String
   :birth {:dayOfMonth String
           :month String}})
