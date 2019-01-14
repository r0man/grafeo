(ns grafeo.ring-test
  (:require [clojure.test :refer [deftest is]]
            [grafeo.core :as lang]
            [grafeo.examples :as examples]
            [grafeo.ring :as ring]))

(def ast
  (lang/parse-document (:form examples/variables)))

(def opts
  {:variables {:episode "JEDI"}})

(deftest test-body
  (is (= (str "{\"query\":\"query HeroNameAndFriends ($episode: Episode) { "
              "hero(episode: $episode) { name  friends { name  }  }  }  \","
              "\"variables\":{\"episode\":\"JEDI\"}}")
         (ring/body ast opts))))

(deftest test-request
  (is (= {:content-type "application/json"
          :body (ring/body ast)
          :request-method :post
          :scheme :http
          :server-name "localhost"
          :server-port 4000
          :uri "/graphql"}
         (ring/request ast))))

(deftest test-query
  (is (= (str "query HeroNameAndFriends ($episode: Episode) { "
              "hero(episode: $episode) { name  friends { name  }  }  }  ")
         (ring/query ast))))
