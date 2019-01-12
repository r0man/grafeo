(ns grafeo.examples-test
  (:require [alumbra.js.ast :as ast]
            [alumbra.spec]
            [clojure.pprint :refer [pprint]]
            [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest is testing]]
            [grafeo.core :as core]
            [grafeo.examples :as examples]
            [grafeo.test :as test]
            [grafeo.transform :as transform]
            [grafeo.util :as util]
            [graphql :as graphql]))

(defn- parse-form [doc]
  (core/parse-document-js doc))

(defn- parse-str [doc]
  (-> (.parse graphql doc)
      (js->clj :keywordize-keys true)))

(defn debug [{:keys [form string]}]
  (when form
    (println "\n;;---------- Alumbra AST ----------\n")
    (util/pprint (transform/parse-document form))
    (println "\n;;---------- Grafeo Spec AST ----------\n")
    (util/pprint (core/conform-document! form))
    (println "\n;;---------- Grafeo AST ---------- \n")
    (util/pprint (parse-form form)))
  (when-not form
    (println "\n;;---------- Grafeo JS AST ---------- \n")
    (util/pprint (ast/js->alumbra (parse-str string))))
  (println "\n;;---------- JS AST ----------")
  (util/pprint (parse-str string))
  (println "\n"))

(defn same-ast? [example]
  (let [result (= (util/strip-loc (parse-form (:form example)))
                  (util/strip-loc (parse-str (:string example))))]
    (when-not result (debug example))
    result))

(defn- roundtrip?
  [{:keys [form string] :as example}]
  (let [ast (parse-str string)
        ast' (ast/alumbra->js (ast/js->alumbra ast))]
    (when-not (= (util/strip-loc ast)
                 (util/strip-loc ast'))
      (debug example))
    (= (util/strip-loc ast)
       (util/strip-loc ast'))))

(deftest test-examples-same-ast
  (doseq [{:keys [name] :as example} (examples/all)]
    (testing (str "Document " name)
      (is (same-ast? example)))))

(deftest test-examples-roundtrip
  (doseq [{:keys [name] :as example} (examples/all)]
    (testing (str "Document " name)
      (is (roundtrip? example)))))
