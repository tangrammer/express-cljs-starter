(ns rebujito.schemas
  (:require [schema.core :as s]))
;;:_id


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


(def AutoReloadMongo
  {
   :autoReloadId String
   :cardId String
   :status (s/enum "active" "disabled" "enabled")
   :autoReloadType (s/enum "Date" "Amount")
   :day s/Num
   :triggerAmount s/Num
   :amount s/Num
   :paymentMethodId String
   :active Boolean
;  :disableUntilDate Date value that indicates when the status of the AutoReload profile will be set to active.
; :stoppedDate Date value that indicates when the status of the AutoReload profile was set to disabled.

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
    (s/optional-key :addressLine2) String
    :birthDay Integer
    :birthMonth Integer
    :city String
    :market String
    :country String
    :countrySubdivision String
    :password String
    :postalCode String
    :receiveStarbucksEmailCommunications Boolean
    :registrationSource String
    (s/optional-key :verifiedEmail) Boolean
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
