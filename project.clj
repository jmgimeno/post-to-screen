(defproject post-to-screen "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/clojurescript "1.11.60"]
                 [ring "1.9.6"]
                 [ring/ring-defaults "0.3.3"]
                 [bk/ring-gzip "0.3.0"]
                 [ring-logger "1.1.1"]
                 [compojure "1.7.0"]
                 [environ "1.2.0"]
                 [http-kit "2.6.0"]
                 [org.clojure/core.async "1.5.648"]
                 [com.taoensso/sente "1.8.1"]
                 [hiccup "1.0.5"]
                 [reagent "0.5.1"]
                 [cljsjs/bootstrap "3.4.1-0"]
                 [cljsjs/highlight "11.5.1-0"]]

  :plugins [[lein-cljsbuild "1.1.8"]
            [lein-environ "1.2.0"]]

  :source-paths ["src/clj" "src/cljs"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled"] 

  :hooks [leiningen.cljsbuild]

  :cljsbuild {:builds
              {:prod
               {:source-paths ["src/cljs"]
                :compiler {:main post-to-screen.core
                           :output-to "resources/public/js/compiled/post_to_screen.js"
                           :optimizations :advanced}}}}

  :profiles {:uberjar {:main post-to-screen.server
                       :omit-source true
                       :aot :all}})
