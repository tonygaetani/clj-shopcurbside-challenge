(ns sandbox.core
  (:require [clj-http.client :as client]
            [clojure.data.json :as json])
  (:use clojure.pprint))

(defonce URL_BASE "http://challenge.shopcurbside.com")

(defn get-sessionid []
  (->>
   (client/get (str URL_BASE "/get-session"))
   :body))

(defn smooth-keys [e]
  {(->> (key e) .toLowerCase keyword) (val e)})

(defn get-start-ids [sessionid]
  (->>
   (client/get (str URL_BASE "/start") {:follow_redirects true
                                        :headers {"Session" sessionid}})
   :body
   json/read-str
   (map smooth-keys)
   (into {})
   :next))

(defn get-response-for-id [id sessionid]
  ;; (pprint (str URL_BASE "/" id {:headers {"Session" sessionid}}))
  (->>
   (client/get (str URL_BASE "/" id) {:follow_redirects true
                                      :headers {"Session" sessionid}})
   :body
   json/read-str
   (map smooth-keys)
   (into {})))

(defn -make-request [req-fn sessionid]
  (println "making request...")
  (try
    [(req-fn sessionid) sessionid]
    (catch Exception e
      ;; (pprint e)
      (let [new-sessionid (get-sessionid)]
        [(make-request req-fn new-sessionid) new-sessionid]))))

(defn -find-secrets [id sessionid]
  (let [[response sessionid] (make-request (partial get-response-for-id id) sessionid)]
    ;; (pprint response)
    (if (contains? response :secret)
      (:secret response)
      (map #(-find-secrets % sessionid) (:next response)))))

(defn find-secrets [id sessionid]
  (try
    (let [response (get-response-for-id id sessionid)]
      ;; (pprint response)
      (if (contains? response :secret)
        (:secret response)
        (map #(find-secrets % sessionid) (into [] (flatten [(:next response)])))))
    (catch Exception e
      (find-secrets id (get-sessionid)))))

(defn main []
  (let [sessionid (get-sessionid)
        start-ids (get-start-ids sessionid)]
    ;; (pprint start-ids)
    (map #(find-secrets % sessionid) start-ids)))
