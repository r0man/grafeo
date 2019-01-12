(ns grafeo.client
  (:require [alumbra.printer :as printer]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [grafeo.core :as lang]
            [grafeo.util :as util]))

(def defaults
  "The default client options."
  {:accept "application/json"
   :as :auto
   :coerce :always
   :content-type "application/json"
   :date-format "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
   :request-method :post
   :scheme :http
   :server-name "localhost"
   :server-port 4000
   :uri "/graphql"})

(defn wrap-defaults [client]
  (fn [request]
    (client (merge defaults request))))

(defn- make-query [doc & [opts]]
  (printer/pr-str (lang/parse-document doc) opts))

(defn- make-body
  "Make the GraphQL request body."
  [{:keys [document variables]}]
  (->> {:query (make-query document {:indentation 0})
        :variables (util/underscore-keys variables)}
       (json/generate-string)))

(defn wrap-document [client]
  (fn [{:keys [document] :as request}]
    (client (if document
              (assoc request :body (make-body request))
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

(def request
  (-> http/request
      wrap-defaults
      wrap-document
      wrap-response))

(defn query [doc & [opts]]
  (request (assoc opts :document doc)))

(comment

  (time (->> '((allStarships (edges (node id name))))
             query :body clojure.pprint/pprint))

  (time (->> '((countries
                (edges
                 (node
                  name
                  iso-3166-1-alpha-2)))
               (spots
                (edges
                 (node
                  name
                  (country name)
                  (region name )))))
             query :body clojure.pprint/pprint))

  )
