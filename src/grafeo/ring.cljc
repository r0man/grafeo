(ns grafeo.ring
  (:require [alumbra.printer :as printer]
            [grafeo.multipart :as multipart]
            [grafeo.util :as util]))

(defn query
  "Returns the :query parameter in the Ring request :body for `ast`."
  [ast & [opts]]
  (printer/pr-str ast opts))

(defn body
  "Returns the Ring request :body for `ast`."
  [ast & [{:keys [indentation variables]}]]
  (util/encode-json
   {:query (query ast {:indentation (or indentation 0)})
    :variables (util/underscore-keys variables)}))

(defn request
  "Returns a Ring request map for the GraphQL document."
  [ast & [{:keys [indentation variables] :as opts}]]
  (or (multipart/request ast opts)
      (merge {:content-type "application/json"
              :body (body ast opts)
              :request-method :post
              :scheme :http
              :server-name "localhost"
              :server-port 4000
              :uri "/graphql"}
             (dissoc opts :indentation :variables))))
