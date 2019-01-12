(ns grafeo.examples-test
  (:require [alumbra.js.ast :as ast]
            [alumbra.parser :as parser]
            [alumbra.printer :as printer]
            [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [com.gfredericks.test.chuck :as chuck]
            [grafeo.examples :as examples]
            [grafeo.test :as test]
            [grafeo.util :as util]))

(defn- parse [doc parse-fn]
  (let [ast (parse-fn doc)]
    (when-not (contains? ast :alumbra/parser-errors)
      ast)))

(defn- parse-str [doc & [opts]]
  (or (parse doc parser/parse-document)
      (parse doc parser/parse-schema)))

(defn debug [string]
  (let [ast (parse-str string)
        ast' (ast/js->alumbra (ast/alumbra->js ast))]
    (println "\n---------- Alumbra AST ----------\n")
    (util/pprint ast)
    (spit "ALUMBRA.edn" (with-out-str (util/pprint ast)))
    (spit "ALUMBRA.graphql" string)
    (println "\n---------- JS AST ----------")
    (util/pprint (ast/alumbra->js ast))
    (println "\n---------- Grafeo AST ---------- \n")
    (util/pprint ast')
    (spit "GRAFEO.edn" (with-out-str (util/pprint ast')))
    (spit "GRAFEO.graphql" (with-out-str (printer/pprint ast')))
    (println "\n")))

(defn- roundtrip? [doc]
  (let [ast (parse-str doc)
        ast' (ast/js->alumbra (ast/alumbra->js ast))]
    (is (= ast ast'))))

(defspec t-roundtrip-document (chuck/times 20)
  (prop/for-all [document (test/gen-document)]
    (roundtrip? document)))

(defspec t-roundtrip-raw-document (chuck/times 20)
  (prop/for-all
    [document (test/gen-raw-document)]
    (let [ast (parse-str document)]
      (= ast (ast/js->alumbra (ast/alumbra->js ast))))))

(defspec t-roundtrip-raw-schema (chuck/times 20)
  (prop/for-all
    [document (test/gen-raw-schema)]
    (let [ast (parse-str document)
          ast' (ast/js->alumbra (ast/alumbra->js ast))]
      (= ast ast'))))

(deftest test-debug
  (doseq [{:keys [name] :as example} (examples/all)
          :when (test/supported-by-alumbra? name)]
    (is (string? (with-out-str (debug (:string example)))))))

(deftest test-examples
  (doseq [{:keys [name] :as example} (examples/all)
          :when (test/supported-by-alumbra? name)]
    (testing (str "Document " name)
      (is (roundtrip? (:string example))))))
