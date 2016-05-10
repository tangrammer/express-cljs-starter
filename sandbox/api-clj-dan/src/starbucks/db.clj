(ns starbucks.db
  (:require
   [clojure.tools.logging :refer :all]
   [schema.core :as s]
   [starbucks.schema :refer [Starbucks StarbucksEntry]]
   [clojure.test :refer :all]))


(def
  ^{:doc "only for tests"}
  test_full_seed {1 {:username "Malcolm Sparks"
                     :userid "mspar"
                     :password "1234"}
                  2 {:username "Jon Pither"
                     :userid "jpit"
                     :password "5678"}})



(s/defn create-db [entries :- Starbucks]
  (assert entries)
  {:starbucks (ref entries)
   :next-entry (ref (if (not-empty entries)
                      (inc (apply max (keys entries)))
                      1))})

(deftest create-db-test
  (testing "empty db"
    (let [db_test1 (create-db {})]
      (is (= {} @(:starbucks db_test1)))
      (is (= 1 @(:next-entry db_test1)))))
  (testing "2 entries db"
    (let [db_test2 (create-db test_full_seed)]
      (is (= test_full_seed @(:starbucks db_test2)))
      (is (= 3 @(:next-entry db_test2))))))


(s/defn get-entries :- Starbucks
  [db]
  @(:starbucks db))


(s/defn matches? [q :- String
                  entry :- StarbucksEntry]
  (some (partial re-seq (re-pattern (str "(?i:\\Q" q "\\E)")))
        (map str (vals (second entry)))))

(s/defn search-entries :- Starbucks
  [db q]
  (let [entries (get-entries db)
        f (filter (partial matches? q) entries)]
    (into {} f)))

(s/defn get-entry :- (s/maybe StarbucksEntry)
  [db id]
  (get @(:starbucks db) id))

(s/defn count-entries :- s/Int
  [db]
  (count @(:starbucks db)))


(defn add-entry
  "Add a new entry to the database. Returns the id of the newly added
  entry."
  [db entry]
  (dosync
   ;; Why use 2 refs when one atom would do? It comes down to being able
   ;; to return nextval from this function. While this is possible to do
   ;; with an atom, its feels less elegant.
   (let [nextval @(:next-entry db)]
     (alter (:starbucks db) conj [nextval entry])
     (alter (:next-entry db) inc)
     nextval)))

(deftest add-entry-test
  (let [db (create-db {})
        test_entry  {:username "Jon Pither" :userid "jpit" :password "1235"}]
    (add-entry db test_entry)
    (is (= 1 (count (get-entries db))))
    (is (= test_entry (get-entry db 1)))))


(defn update-entry
  "Update a new entry to the database. Returns the id of the newly added
  entry."
  [db id entry]
  (dosync
   (alter (:starbucks db) assoc id entry)))

(deftest update-entry-test
  (let [db (create-db test_full_seed)]
    (update-entry db 2 {:username "Jon Pither" :userid "jpit" :password "8888"})
    (is (= (count-entries db) 2))
    (is "8888" (get-in (get-entry db 2) [:password]))))

(defn delete-entry
  "Delete a entry from the database."
  [db id]
  (dosync
   (alter (:starbucks db) dissoc id)))

(deftest delete-entry-test
  (let [db (create-db test_full_seed)]
    (delete-entry db 1)
    (is (= (count-entries db) 1))))
