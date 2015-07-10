(defproject post-to-screen "0.6.0-SNAPSHOT"
  :description "Webapp to post code to show on the screen"
  :url "https://github.com/jmgimeno/post-to-screen"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj" "src/cljs"]

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "0.0-3308" :scope "provided"]
                 [leiningen "2.5.1"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [com.taoensso/sente "1.5.0"]

                 ;; Server
                 [ring "1.4.0"]
                 [compojure "1.3.4"]
                 [hiccup "1.0.5"]
                 [http-kit "2.1.19"]
                 [environ "1.0.0"]

                 ;; Client
                 [reagent "0.5.0"]

                 ;; Devel

                 [enlive "1.1.5"]
                 [figwheel "0.3.7"]
                 [com.cemerick/piggieback "0.2.1"]
                 [weasel "0.7.0"]]

  :plugins [[lein-cljsbuild "1.0.6"]
            [lein-environ "1.0.0"]]

  :min-lein-version "2.5.0"

  :uberjar-name "post-to-screen.jar"

  :cljsbuild {:builds {:app {:source-paths ["src/cljs"]
                             :compiler {:output-to     "resources/public/js/app.js"
                                        :output-dir    "resources/public/js/out"
                                        :source-map    "resources/public/js/out.js.map"
                                        :externs       ["externs/highlight-externs.js"]
                                        :optimizations :none
                                        :pretty-print  true}}}}

  :profiles {:dev {:repl-options {:timeout 60000
                                  :init-ns post-to-screen.server
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

                   :dependencies [[org.clojure/tools.nrepl "0.2.10"]]

                   :plugins [[lein-figwheel "0.3.7"]]

                   :figwheel {:http-server-root "public"
                              :port 3449
                              :css-dirs ["resources/public/css"]
                              :repl false}

                   :env {:is-dev true}

                   :cljsbuild {:builds {:app {:source-paths ["env/dev/cljs"]}}}}

             :uberjar {:hooks [leiningen.cljsbuild]
                       :env {:production true}
                       :omit-source true
                       :aot :all
                       :cljsbuild {:builds {:app
                                            {:source-paths ["env/prod/cljs"]
                                             :compiler
                                             {:optimizations :advanced
                                              :pretty-print false}}}}}})
