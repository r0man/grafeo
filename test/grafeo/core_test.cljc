(ns grafeo.core-test
  (:require [clojure.test :refer [deftest is]]
            [grafeo.core :as lang]
            [grafeo.util :as util]))

(deftest test-parse-document-js
  (is (= {:kind "Document"
          :definitions
          [{:directives []
            :kind "OperationDefinition"
            :name {:kind "Name" :value "continents"}
            :operation "query"
            :selectionSet
            {:kind "SelectionSet"
             :selections
             [{:alias nil
               :arguments []
               :directives []
               :kind "Field"
               :name {:kind "Name" :value "continents"}
               :selectionSet
               {:kind "SelectionSet"
                :selections
                [{:alias nil
                  :arguments []
                  :directives []
                  :kind "Field"
                  :name {:kind "Name" :value "edges"}
                  :selectionSet
                  {:kind "SelectionSet"
                   :selections
                   [{:alias nil
                     :arguments []
                     :directives []
                     :kind "Field"
                     :name {:kind "Name" :value "node"}
                     :selectionSet
                     {:kind "SelectionSet"
                      :selections
                      [{:alias nil
                        :arguments []
                        :directives []
                        :kind "Field"
                        :name {:kind "Name" :value "id"}
                        :selectionSet nil}
                       {:alias nil
                        :arguments []
                        :directives []
                        :kind "Field"
                        :name {:kind "Name" :value "name"}
                        :selectionSet nil}
                       {:alias nil
                        :arguments []
                        :directives []
                        :kind "Field"
                        :name {:kind "Name" :value "slug"}
                        :selectionSet
                        {:kind "SelectionSet"
                         :selections
                         [{:alias nil
                           :arguments []
                           :directives []
                           :kind "Field"
                           :name {:kind "Name" :value "path"}
                           :selectionSet nil}]}}]}}]}}]}}]}
            :variableDefinitions []}]}
         (util/strip-loc
          (lang/parse-document-js
           '((query
              continents
              (continents
               (edges
                (node
                 id
                 name
                 (slug path)))))))))))
