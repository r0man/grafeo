(ns grafeo.transform-test
  (:require #?(:clj [alumbra.parser :as parser])
            [alumbra.spec]
            [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest is testing]]
            [grafeo.core :as gql]
            [grafeo.examples :as examples]
            [grafeo.test :as test]
            [grafeo.transform :as transform]
            [grafeo.util :as util]))

(defn- parse-form [doc & [opts]]
  (transform/parse-document doc opts))

(defn- parse [doc parse-fn]
  (let [ast (parse-fn doc)]
    (when-not (contains? ast :alumbra/parser-errors)
      ast)))

#?(:clj (defn- parse-str [doc & [opts]]
          (-> (or (parse doc parser/parse-document)
                  (parse doc parser/parse-schema))
              (util/reset-metadata))))

#?(:clj (defn debug [{:keys [form string]} & [opts]]
          (println "\n---------- Grafeo Spec AST ----------\n")
          (util/pprint (gql/conform-document! form))
          (println "\n---------- Grafeo AST ---------- \n")
          (util/pprint (parse-form form opts))
          (println "\n---------- Alumbra AST ----------")
          (util/pprint (parse-str string))
          (println "\n")))

#?(:clj (defn same-ast? [example]
          (let [result (is (= (parse-form (:form example))
                              (parse-str (:string example))))]
            (when-not result (debug example))
            result)))

(deftest test-enum-extension
  (is (= (parse-form (:form examples/enum-extension))
         {:alumbra/enum-extensions
          [{:alumbra/metadata {:column 0 :row 0}
            :alumbra/type-name "Direction"
            :alumbra/enum-fields
            [{:alumbra/enum "NORTH_EAST"
              :alumbra/metadata {:column 0 :row 0}}]}]
          :alumbra/metadata {:column 0 :row 0}})))

(deftest test-input-object-type-extension
  (is (= (parse-form (:form examples/input-object-type-extension))
         {:alumbra/metadata {:column 0 :row 0}
          :alumbra/input-extensions
          [{:alumbra/metadata {:column 0 :row 0}
            :alumbra/type-name "NamedEntity"
            :alumbra/input-field-definitions
            [{:alumbra/field-name "nickname"
              :alumbra/metadata {:column 0 :row 0}
              :alumbra/type
              {:alumbra/metadata {:column 0 :row 0}
               :alumbra/non-null? false
               :alumbra/type-class :named-type
               :alumbra/type-name "String"}}]}]})))

(deftest test-interface-type-extension
  (is (= (parse-form (:form examples/interface-type-extension))
         {:alumbra/metadata {:column 0 :row 0}
          :alumbra/interface-extensions
          [{:alumbra/type-name "NamedEntity"
            :alumbra/metadata {:column 0, :row 0}
            :alumbra/field-definitions
            [{:alumbra/field-name "nickname"
              :alumbra/metadata {:column 0 :row 0}
              :alumbra/type
              {:alumbra/metadata {:column 0 :row 0}
               :alumbra/non-null? false
               :alumbra/type-class :named-type
               :alumbra/type-name "String"}}]}]})))

(deftest test-scalar-type-extension
  (is (= (parse-form (:form examples/scalar-type-extension))
         {:alumbra/metadata {:column 0 :row 0}
          :alumbra/scalar-extensions
          [{:alumbra/metadata {:column 0 :row 0}
            :alumbra/type-name "Url"
            :alumbra/directives
            [{:alumbra/directive-name "example"
              :alumbra/metadata {:column 0 :row 0}}]}]})))

(deftest test-schema-type-extension
  (is (= (parse-form (:form examples/schema-extension))
         {:alumbra/metadata {:column 0 :row 0}
          :alumbra/schema-extensions
          [{:alumbra/metadata {:column 0 :row 0}
            :alumbra/schema-fields
            [{:alumbra/metadata {:column 0 :row 0}
              :alumbra/operation-type "query"
              :alumbra/schema-type
              {:alumbra/metadata {:column 0 :row 0}
               :alumbra/type-name "MyQueryRootType"}}
             {:alumbra/metadata {:column 0 :row 0}
              :alumbra/operation-type "mutation"
              :alumbra/schema-type
              {:alumbra/metadata {:column 0 :row 0}
               :alumbra/type-name "MyMutationRootType"}}]}]})))

(deftest test-union-type-extension
  (is (= (parse-form (:form examples/union-type-extension))
         {:alumbra/metadata {:column 0 :row 0}
          :alumbra/union-extensions
          [{:alumbra/metadata {:column 0 :row 0}
            :alumbra/type-name "SearchResult"
            :alumbra/union-types
            [{:alumbra/metadata {:column 0 :row 0}
              :alumbra/type-name "Photo"}
             {:alumbra/metadata {:column 0 :row 0}
              :alumbra/type-name "Person"}]}]})))

(deftest test-naming-strategies
  (is (= (parse-form
          '((query
             Countries
             [($min-spots Int)]
             (countries
              [(min_spots $min_spots)]
              name
              iso-3166-1-alpha-2))))
         {:alumbra/metadata {:column 0 :row 0}
          :alumbra/operations
          [{:alumbra/metadata {:column 0 :row 0}
            :alumbra/operation-type "query"
            :alumbra/operation-name "Countries"
            :alumbra/selection-set
            [{:alumbra/field-name "countries"
              :alumbra/metadata {:column 0 :row 0}
              :alumbra/arguments
              [{:alumbra/argument-name "min_spots"
                :alumbra/argument-value
                {:alumbra/metadata {:column 0 :row 0}
                 :alumbra/value-type :variable
                 :alumbra/variable-name "min_spots"}
                :alumbra/metadata {:column 0 :row 0}}]
              :alumbra/selection-set
              [{:alumbra/field-name "name"
                :alumbra/metadata {:column 0 :row 0}}
               {:alumbra/field-name "iso_3166_1_alpha_2"
                :alumbra/metadata {:column 0 :row 0}}]}]
            :alumbra/variables
            [{:alumbra/metadata {:column 0 :row 0}
              :alumbra/type
              {:alumbra/metadata {:column 0 :row 0}
               :alumbra/non-null? false
               :alumbra/type-class :named-type
               :alumbra/type-name "Int"}
              :alumbra/variable-name "min_spots"}]}]})))

(deftest test-valid-document
  (doseq [{:keys [form name]} (examples/all)]
    (testing (str "Document " name)
      (is (s/valid? :alumbra/document (parse-form form))))))

#?(:clj (deftest test-transform
          (doseq [{:keys [name] :as example} (examples/all)]
            (when (test/supported-by-alumbra? name)
              (testing (str "Document " name)
                (is (same-ast? example)))))))
