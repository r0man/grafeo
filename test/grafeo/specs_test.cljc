(ns grafeo.specs-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest is testing]]
            [grafeo.core :as gql]
            [grafeo.examples :as examples]))

(deftest test-backquote
  (is (= (gql/conform-document!
          '((query
             (search
              [(text "an")]
              __typename
              ((on Human)
               name)
              ((on Droid)
               name)
              ((on Starship)
               name)))))
         (gql/conform-document!
          `((query
             (search
              [(text "an")]
              __typename
              ((on Human)
               name)
              ((on Droid)
               name)
              ((on Starship)
               name))))))))

(deftest test-backquote-class-symbol
  (is (= (gql/conform-document!
          '((query
             continents
             [($after String)]
             (continents
              [(after $after)]
              (edges (node id))))))
         (gql/conform-document!
          `((query
             continents
             [($after String)]
             (continents
              [(after $after)]
              (edges (node id)))))))))

(deftest test-list-type-nullable
  (is (= '[:list-type [[:named-type Int]]]
         (s/conform :grafeo/type '[Int]))))

(deftest test-list-type-non-nullable
  (is (= '[:non-null-type [:list-type-non-null ([:named-type Int])]]
         (s/conform :grafeo/type '(Int)))))

(deftest test-argument-definitions
  (is (= 100 (count (s/exercise :grafeo/argument-definitions 100)))))

(deftest test-arguments
  (is (= 100 (count (s/exercise :grafeo/arguments 100)))))

(deftest test-directives
  (is (= 100 (count (s/exercise :grafeo/directives 100)))))

(deftest test-field-alias
  (is (= 100 (count (s/exercise :grafeo/field-alias 100)))))

(deftest test-field-name
  (is (= 100 (count (s/exercise :grafeo/field-name 100)))))

;; (deftest test-selection
;;   (is (= 100 (count (s/exercise :grafeo/selection 100)))))

(deftest test-symbol
  (is (= 100 (count (s/exercise :grafeo/symbol 100)))))

(deftest test-type
  (is (= 100 (count (s/exercise :grafeo/type 100)))))

(deftest test-valid-examples
  (doseq [{:keys [name] :as example} (examples/all)]
    (testing (str "Document " name)
      (is (gql/valid-document? (:form example))))))
