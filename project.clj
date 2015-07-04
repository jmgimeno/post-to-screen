(defproject post-to-screen "0.6.0-SNAPSHOT"
  :description "Webapp to post code to show on the screen"
  :url "https://github.com/jmgimeno/post-to-screen"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj" "src/cljs"]

  :dependencies [[org.clojure/clojure "1.7.0-alpha5"]
                 [org.clojure/clojurescript "0.0-2850" :scope "provided"]
                 [leiningen "2.5.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [com.taoensso/sente "1.2.0"]

                 ;; Server
                 [ring "1.3.1"]
                 [compojure "1.2.0"]
                 [hiccup "1.0.5"]
                 [http-kit "2.1.16"]
                 [environ "1.0.0"]

                 ;; Client
                 [reagent "0.5.0-alpha3"]

                 ;; Devel

                 [enlive "1.1.5"]
                 [figwheel "0.2.1-SNAPSHOT"]
                 [com.cemerick/piggieback "0.1.4"]
                 [weasel "0.6.0"]]

  :plugins [[lein-cljsbuild "1.0.3"]
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

                   :plugins [[lein-figwheel "0.2.1-SNAPSHOT"]]

                   :figwheel {:http-server-root "public"
                              :port 3449
                              :css-dirs ["resources/public/css"]}

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
