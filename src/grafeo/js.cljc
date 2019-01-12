(ns grafeo.js
  (:refer-clojure :exclude [clj->js js->clj])
  (:require #?(:cljs [cljs.tagged-literals :refer [JSValue]])
            [clojure.walk :as walk])
  #?(:clj (:import cljs.tagged_literals.JSValue)))

(defn clj->js
  "Convert all Clojure maps and seqs in `x` to `JSValue`s."
  [x]
  (let [f (fn [[k v]]
            [(clj->js k)
             (clj->js v)])]
    (cond
      (map? x)
      (walk/postwalk
       (fn [x]
         (cond
           (map? x)
           (JSValue. (into {} (map f x)))
           :else x))
       x)
      (sequential? x)
      (JSValue. (mapv clj->js x))
      :else x)))

(defn js->clj
  "Convert all `JSValue`s in `x` to Clojure maps."
  [x]
  (if (instance? JSValue x)
    (let [val (.-val x)]
      (cond
        (map? val)
        (walk/postwalk
         #(cond
            (map? %)
            (into {} (map
                      (fn [[k v]]
                        [(js->clj k)
                         (js->clj v)]) %))
            :else %)
         val)
        (sequential? val)
        (mapv js->clj val)
        :else val))
    x))

#?(:clj (defmethod print-method JSValue
          [^JSValue v, ^java.io.Writer w]
          (.write w "#js ")
          (.write w (pr-str (.-val v)))))
