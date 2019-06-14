(ns grafeo.util
  (:require [#?(:clj  cheshire.core :cljs goog.json) :as json]
            [clojure.pprint :as pprint]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.walk :as walk]))

(defn encode-json
  "Encode `obj` as JSON."
  [obj]
  #?(:clj (json/encode obj)
     :cljs (json/serialize (clj->js obj))))

(defn decode-json
  "Decode `s` as JSON."
  [s]
  #?(:clj (json/decode s keyword)
     :cljs (js->clj (JSON.parse s) :keywordize-keys true)))

(defn simple-class-name
  "Given an symbol of a class, returns it's simple name. Leaves the
  fragment spread '... as is."
  [sym]
  (str/replace (str sym) #"([^.].*\.)" ""))

(defn conform!
  "Conform `x` against `spec`, or raise an exception."
  [spec x]
  (let [ast (s/conform spec x)]
    (if (s/invalid? ast)
      (throw (ex-info (str "Can't conform " x " against " spec ".")
                      (s/explain-data spec x)))
      ast)))

(defn transform-keys [m transform-fn]
  (let [f (fn [[k v]]
            [(transform-fn k) v])]
    (walk/postwalk
     (fn [x]
       (if (map? x)
         (into {} (map f x))
         x))
     m)))

(defn strip-keys [m ks]
  (let [ks (set ks)
        f (fn [[k v]]
            (when-not (contains? ks k)
              [k v]))]
    (walk/postwalk
     (fn [x]
       (if (map? x)
         (into {} (map f x))
         x))
     m)))

(defn reset-metadata [m]
  (let [f (fn [[k v]]
            (if (= k :alumbra/metadata)
              [k (-> (merge v {:column 0 :row 0})
                     (dissoc :index))]
              [k v]))]
    (walk/postwalk
     (fn [x]
       (if (map? x)
         (into {} (map f x))
         x))
     m)))

(defn transform-loc [m]
  (let [f (fn [[k v]]
            (if (= k :loc)
              [k {:startToken
                  {:column (.. v -startToken -column)
                   :start (.. v -startToken -start)
                   :line (.. v -startToken -line)}}]
              [k v]))]
    (walk/postwalk
     (fn [x]
       (if (map? x)
         (into {} (map f x))
         x))
     m)))

(defn strip-metadata [m]
  (strip-keys m #{:alumbra/metadata}))

(defn strip-non-null [s]
  (str/replace (name s) #"!$" ""))

(defn strip-loc [m]
  (strip-keys m #{:loc}))

(defn strip-variable [s]
  (some-> s name (.substring 1)))

(defn pprint [x]
  (binding [*print-namespace-maps* false]
    (pprint/pprint x)))

(defn hyphenate [s]
  (some-> s name (str/replace "_" "-")))

(defn hyphenate-keys [m]
  (transform-keys m (comp keyword hyphenate)))

(defn underscore [s]
  (some-> s name (str/replace "-" "_")))

(defn underscore-keys [m]
  (transform-keys m (comp keyword underscore)))
