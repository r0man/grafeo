(ns grafeo.test.runner
  (:require [clojure.spec.test.alpha :as stest]
            [doo.runner :refer-macros [doo-tests]]
            [grafeo.core-test]
            [grafeo.examples-test]
            [grafeo.js-test]
            [grafeo.multipart-test]
            [grafeo.ring-test]
            [grafeo.specs-test]
            [grafeo.transform-test]))

(stest/instrument)

(doo-tests
 'grafeo.core-test
 'grafeo.examples-test
 'grafeo.js-test
 'grafeo.multipart-test
 'grafeo.ring-test
 'grafeo.specs-test
 'grafeo.transform-test)
