(ns starbucks.html
  (:require
   [clojure.java.io :as io]
   [hiccup.core :refer [html]]
   [yada.yada :as yada]))

(defn index-html [ctx entries q]
  (html
   [:body
    [:form#search {:method :get}
     [:input {:type :text :name :q :value q}]
     [:input {:type :submit :value "Search"}]]

    (if (not-empty entries)
      [:table
       [:thead
        [:tr
         [:th "Entry"]
         [:th "Name"]
         [:th "Id"]
         [:th "Password"]]]

       [:tbody
        (for [[id {:keys [username userid password]}] entries
              :let [href (:href (yada/uri-for ctx :starbucks.api/entry {:route-params {:entry id}}))]]
          [:tr
           [:td [:a {:href href} href]]
           [:td username]
           [:td userid]
           [:td password]])]]
      [:h2 "No entries"])

    [:h4 "Add entry"]

    [:form {:method :post}
     [:style "label { margin: 6pt }"]
     [:p
      [:label "Name"]
      [:input {:name "username" :type :text}]]
     [:p
      [:label "Identification"]
      [:input {:name "userid" :type :text}]]
     [:p
      [:label "Password"]
      [:input {:name "password" :type :text}]]
     [:p
      [:input {:type :submit :value "Add entry"}]]]]))

(defn entry-html [{:keys [userid username password]}
                  {:keys [entry index]}]
  (html
   [:body
    [:script (format "index=\"%s\";" index)]
    [:script (format "entry=\"%s\";" entry)]
    [:h2 (format "%s %s" userid username)]
    [:p "Password: " password]

    [:h4 "Update entry"]
    [:form#entry
     [:style "label { margin: 6pt }"]
     [:p
      [:label "Identification"]
      [:input {:type :text :name "userid" :value userid}]]
     [:p
      [:label "Name"]
      [:input {:type :text :name "username" :value username}]]
     [:p
      [:label "Password"]
      [:input {:type :text :name "password" :value password}]]]

    [:button {:onclick (format "starbucks.update('%s')" entry)} "Update"]
    [:button {:onclick (format "starbucks.delete('%s')" entry)} "Delete"]
    [:p [:a {:href index} "Index"]]
    (when-let [js (io/resource "js/starbucks.js")]
      [:script (slurp js)])]))
