(ns shopcurbside.core
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]))

(defonce URL_BASE "http://challenge.shopcurbside.com")

(defn get-sessionid []
  (->>
   (client/get (str URL_BASE "/get-session"))
   :body))

(defn smooth-keys [e]
  {(->> (key e) .toLowerCase keyword) (val e)})

(defn get-start-ids [sessionid]
  (->>
   (client/get (str URL_BASE "/start") {:headers {"Session" sessionid}})
   :body
   json/read-str
   (map smooth-keys)
   (into {})
   :next))

(defn get-response-for-id [id sessionid]
  (->>
   (client/get (str URL_BASE "/" id) {:headers {"Session" sessionid}})
   :body
   json/read-str
   (map smooth-keys)
   (into {})))

(defn find-secrets [id sessionid]
  (try
    (let [response (get-response-for-id id sessionid)]
      (if (contains? response :secret)
        (:secret response)
        (map #(find-secrets % sessionid) (into [] (flatten [(:next response)])))))
    (catch Exception e
      ;; assume the exception was caused by an expired session id
      (find-secrets id (get-sessionid)))))

(defn main []
  (let [sessionid (get-sessionid)
        start-ids (get-start-ids sessionid)]
    (->>
     start-ids
     (map #(find-secrets % sessionid))
     flatten
     (filter not-empty))))
