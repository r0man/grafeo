(ns grafeo.multipart
  (:require #?(:clj [clojure.java.io :as io])
            [alumbra.printer :as printer]
            [clojure.string :as str]
            [grafeo.util :as util]))

(defrecord File [path])

(defn file [path]
  (File. path))

(defn file-ref [path]
  #?(:clj (io/file path)
     ;; TODO: JS
     :cljs path))

(defn file? [path]
  (instance? File path))

(defn files? [paths]
  (and (sequential? paths)
       (every? #(instance? File %) paths)))

(defn paths [m]
  (letfn [(paths* [ps ks m]
            (reduce-kv
             (fn [ps k v]
               (if (and (map? v) (not (record? v)))
                 (paths* ps (conj ks k) v)
                 (conj ps (concat ks [k v]))))
             ps
             m))]
    (paths* () [] m)))

(defn file-paths [m]
  (filter #(or (file? (last %)) (files? (last %))) (paths m)))

(defn- expand-file [path]
  [{:name (str/join "." (map name (butlast path)))
    :file (last path)
    :path (vec (butlast path))}])

(defn- expand-files [path]
  (for [[array-index file] (map-indexed vector (last path))]
    {:array-index array-index
     :name (str (str/join "." (map name (butlast path))) "." array-index)
     :file file
     :path (vec (concat (butlast path) [array-index]))}))

(defn expand-file-path [path]
  (cond (files? (last path))
        (expand-files path)
        (file? (last path))
        (expand-file path)))

(defn expand-file-paths [file-paths]
  (seq (mapcat expand-file-path file-paths)))

(defn files [{:keys [variables] :as opts}]
  (expand-file-paths (file-paths {:variables variables})))

(defn replace-files [variables files]
  (reduce #(assoc-in %1 (rest (:path %2)) nil) variables files))

(defn map-part
  "Returns the map part for `files`. "
  [files]
  {:name "map"
   :content (util/encode-json
             (reduce (fn [m [index [file group]]]
                       (assoc m (str index) (mapv :name group)))
                     {} (map-indexed vector (group-by :file files))))})

(defn file-parts
  "Returns the file parts for `files`. "
  [files]
  (map (fn [[index [file group]]]
         {:name (str index) :content (file-ref (:path file))})
       (map-indexed vector (group-by :file files))))

(defn operations-part
  "Returns the operations parts for `ast`, `opts` and `files`. "
  [ast {:keys [variables] :as opts} files]
  {:name "operations"
   :content (util/encode-json
             {:query (printer/pr-str ast opts)
              :variables (replace-files variables files)})})

(defn multipart
  "Returns a seq of multipart for `ast` and `opts`, or nil if there are
  no files in the :variables of `opts`."
  [ast & [{:keys [variables] :as opts}]]
  (when-let [files (files opts)]
    (concat [(operations-part ast opts files)]
            [(map-part files)]
            (file-parts files))))

(defn request
  "Returns the multipart Ring request `ast` and `opts`."
  [ast & [{:keys [variables] :as opts}]]
  (when-let [multipart (multipart ast opts)]
    (merge {:request-method :post
            :scheme :http
            :server-name "localhost"
            :server-port 4000
            :uri "/graphql"
            :multipart multipart}
           (dissoc opts :indentation :variables))))
