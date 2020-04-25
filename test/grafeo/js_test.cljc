(ns grafeo.js-test
  (:require [clojure.test.check]
            [clojure.test.check.clojure-test #?(:clj :refer :cljs :refer-macros) [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop #?@(:cljs [:include-macros true])]
            [com.gfredericks.test.chuck :as chuck]
            [grafeo.js :as js-util]))

(defspec test-convert-js-value (chuck/times 20)
  (prop/for-all
    [value (gen/recursive-gen
            gen/container-type
            (gen/one-of
             [gen/small-integer
              gen/large-integer
              (gen/double* {:infinite? false :NaN? false})
              gen/char-ascii
              gen/string-ascii
              gen/ratio
              gen/boolean
              gen/keyword
              gen/keyword-ns
              gen/symbol
              gen/symbol-ns
              gen/uuid]))]
    (= value (js-util/js->clj (js-util/clj->js value)))))
