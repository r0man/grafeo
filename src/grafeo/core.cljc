(ns grafeo.core
  (:refer-clojure :exclude [print])
  (:require [alumbra.js.ast :as ast]
            [alumbra.printer :as printer]
            [clojure.spec.alpha :as s]
            [grafeo.specs :as specs]
            [grafeo.transform :as transform]
            [grafeo.util :as util]))

(defn conform-document
  "Conform the GraphQL document `doc` in s-expression format
  to :grafeo/document."
  [doc]
  (s/conform :grafeo/document doc))

(defn conform-document!
  "Conform the GraphQL document `doc` in s-expression format
  to :grafeo/document, or throw an exception."
  [doc]
  (util/conform! :grafeo/document doc))

(defn parse-document
  "Parse the GraphqQL `doc` document and return an Alumbra AST."
  [doc & [opts]]
  (transform/parse-document doc opts))

(defn parse-document-js
  "Parse the GraphqQL `doc` document and return a JavaScript AST."
  [doc & [opts]]
  (ast/alumbra->js (parse-document doc opts)))

(defn valid-document?
  "Returns true if `x` is a valid GraphQL document in s-expression
  format, otherwise false."
  [x]
  (s/valid? :grafeo/document x))

(defn print
  "Parse the GraphQL `document` in s-expression form and print it."
  [document & [opts]]
  (-> document parse-document (printer/print opts)))

(defn pprint
  "Parse the GraphQL `document` in s-expression form and pretty print it."
  [document & [opts]]
  (-> document parse-document (printer/pprint opts)))
