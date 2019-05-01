(defproject grafeo "0.1.5-SNAPSHOT"
  :description "A GraphQL document and schema language based on S-expressions."
  :url "https://github.com/r0man/grafeo"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :deploy-repositories [["releases" :clojars]]
  :dependencies [[cheshire "5.8.1"]
                 [clj-http "3.10.0"]
                 [org.clojure/clojure "1.10.0"]
                 [r0man/alumbra.js "0.1.0"]
                 [r0man/alumbra.printer "0.1.1"]]
  :plugins [[jonase/eastwood "0.3.5"]
            [lein-cljsbuild "1.1.7"]
            [lein-doo "0.1.11"]]
  :aliases
  {"ci" ["do"
         ["clean"]
         ["test"]
         ["doo" "node" "node" "once"]
         ["lint"]]
   "lint" ["do"  ["eastwood"]]}
  :cljsbuild
  {:builds
   [{:id "node"
     :compiler
     {:main grafeo.test.runner
      :npm-deps {:graphql "14.0.2"}
      :install-deps true
      :optimizations :none
      :output-dir "target/node"
      :output-to "target/node.js"
      :parallel-build true
      :pretty-print true
      :target :nodejs
      :verbose false}
     :source-paths ["src" "test"]}]}
  :profiles
  {:dev
   {:dependencies [[alumbra/analyzer "0.1.17"]
                   [alumbra/generators "0.2.2"]
                   [alumbra/parser "0.1.7"]
                   [com.gfredericks/test.chuck "0.2.9"]
                   [criterium "0.4.5"]
                   [expound "0.7.2"]
                   [org.clojure/test.check "0.9.0"]
                   [r0man/alumbra.spec "0.1.11"]]}
   :provided
   {:dependencies [[org.clojure/clojurescript "1.10.520"]]}
   :repl
   {:dependencies [[cider/piggieback "0.4.0"]]
    :nrepl-middleware [cider.piggieback/wrap-cljs-repl]}})
