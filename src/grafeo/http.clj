(ns grafeo.http
  (:require [clj-http.client :as http]
            [grafeo.core :as lang]
            [grafeo.ring :as ring]
            [grafeo.util :as util]))

(def defaults
  "The default client options."
  {:accept "application/json"
   :as :auto
   :coerce :always
   :date-format "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"})

(defn wrap-defaults [client]
  (fn [request]
    (client (merge defaults request))))

(defn wrap-document [client]
  (fn [{:keys [document] :as request}]
    (client (if document
              (ring/request document request)
              request))))

(defn wrap-response [client]
  (fn [request]
    (let [response (client request)]
      (update response :body
              (fn [body]
                (if (or (map? body)
                        (sequential? body))
                  (util/hyphenate-keys body)
                  body))))))

(def request*
  (-> http/request
      wrap-defaults
      wrap-document
      wrap-response))

(defn request [client doc & [opts]]
  (let [doc' (lang/parse-document doc)]
    (request* (merge client {:document doc'} opts))))

(comment

  (->> (request
        defaults
        '((query
           HeroNameAndFriends
           [($episode Episode)]
           (hero
            [(episode $episode)]
            name
            (friends name))))
        {:variables {:episode "JEDI"}})
       :body clojure.pprint/pprint)

  )
