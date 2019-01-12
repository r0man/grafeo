(ns grafeo.test.runner
  (:require [clojure.spec.test.alpha :as stest]
            [doo.runner :refer-macros [doo-tests]]
            [grafeo.js-test]
            [grafeo.examples-test]
            [grafeo.core-test]
            [grafeo.specs-test]
            [grafeo.transform-test]))

(stest/instrument)

(doo-tests
 'grafeo.js-test
 'grafeo.core-test
 'grafeo.specs-test
 'grafeo.transform-test
 'grafeo.examples-test)
