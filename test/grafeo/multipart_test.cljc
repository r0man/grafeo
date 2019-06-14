(ns grafeo.multipart-test
  (:require [clojure.test :refer [are deftest is testing]]
            [grafeo.core :as lang]
            [grafeo.multipart :as multipart]
            [grafeo.util :as util]))

(def file-a
  (multipart/file "a.txt"))

(def file-b
  (multipart/file "b.txt"))

(def file-c
  (multipart/file "c.txt"))

(def single-file
  (lang/parse-document
   '((mutation
      SingleFile
      [($file File)]
      (uploadFile
       [(file $file)]
       id)))))

(def multiple-files
  (lang/parse-document
   '((mutation
      MultipleFiles
      [($files [File])]
      (uploadFiles
       [(files [$file])]
       id)))))

(deftest test-paths
  (are [m expected]
      (= expected (multipart/paths m))
    nil []
    {} []
    ;; File
    {:variables {:file file-a}}
    [[:variables :file file-a]]
    ;; Array of files
    {:variables {:files [file-b file-c]}}
    [[:variables :files [file-b file-c]]]))

(deftest test-file-paths
  (are [m expected]
      (= expected (multipart/file-paths m))
    nil []
    {} []
    {:variables {:hello "world"}} []
    ;; File
    {:variables {:file file-a}}
    [[:variables :file file-a]]
    ;; Array of files
    {:variables {:files [file-b file-c]}}
    [[:variables :files [file-b file-c]]]))

(deftest test-expand-file-path
  (are [file-path expected]
      (= expected (multipart/expand-file-path file-path))
    nil nil
    [] nil
    ;; File
    [:variables :file file-a]
    [{:name "variables.file"
      :file file-a
      :path [:variables :file]}]
    ;; Array of files
    [:variables :files [file-b file-c]]
    [{:array-index 0
      :name "variables.files.0"
      :file file-b
      :path [:variables :files 0]}
     {:array-index 1
      :name "variables.files.1"
      :file file-c
      :path [:variables :files 1]}]))

(deftest test-expand-file-paths
  (are [file-paths expected]
      (= expected (multipart/expand-file-paths file-paths))
    nil nil
    [] nil
    ;; Single file
    [[:variables :file file-a]]
    [{:name "variables.file"
      :file file-a
      :path [:variables :file]}]
    ;; Array of files
    [[:variables :files [file-b file-c]]]
    [{:array-index 0
      :name "variables.files.0"
      :file file-b
      :path [:variables :files 0]}
     {:array-index 1
      :name "variables.files.1"
      :file file-c
      :path [:variables :files 1]}]
    ;; Single and array of file
    [[:variables :file file-a]
     [:variables :files [file-b file-c]]]
    [{:name "variables.file"
      :file file-a
      :path [:variables :file]}
     {:array-index 0
      :name "variables.files.0"
      :file file-b
      :path [:variables :files 0]}
     {:array-index 1
      :name "variables.files.1"
      :file file-c
      :path [:variables :files 1]}]))

(deftest test-single-file
  (let [{:keys [multipart] :as request}
        (multipart/request single-file {:variables {:file file-a}})]
    (is (= 3 (count multipart)))
    (testing "operations part"
      (let [part (nth multipart 0)]
        (is (= "operations" (:name part)))
        (is (= {:query "mutation SingleFile ($file: File) { uploadFile(file: $file) { id  }  }  "
                :variables {:file nil}}
               (util/decode-json (:content part))))))
    (testing "map part"
      (let [part (nth multipart 1)]
        (is (= "map" (:name part)))
        (is (= {:0 ["variables.file"]}
               (util/decode-json (:content part))))))
    (testing "file part #1"
      (let [part (nth multipart 2)]
        (is (= "0" (:name part)))
        (is (= (multipart/file-ref (:path file-a)) (:content part)))))))

(deftest test-multiple-files
  (let [{:keys [multipart] :as request}
        (multipart/request multiple-files {:variables {:files [file-b file-c]}})]
    (is (= 4 (count multipart)))
    (testing "operations part"
      (let [part (nth multipart 0)]
        (is (= "operations" (:name part)))
        (is (= {:query "mutation MultipleFiles ($files: [File]) { uploadFiles(files: [$file]) { id  }  }  "
                :variables {:files [nil nil]}}
               (util/decode-json (:content part))))))
    (testing "map part"
      (let [part (nth multipart 1)]
        (is (= "map" (:name part)))
        (is (= {:0 ["variables.files.0"]
                :1 ["variables.files.1"]}
               (util/decode-json (:content part))))))
    (testing "file part #1"
      (let [part (nth multipart 2)]
        (is (= "0" (:name part)))
        (is (= (multipart/file-ref (:path file-b)) (:content part)))))
    (testing "file part #2"
      (let [part (nth multipart 3)]
        (is (= "1" (:name part)))
        (is (= (multipart/file-ref (:path file-c)) (:content part)))))))
