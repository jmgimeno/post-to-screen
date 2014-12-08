(ns post-to-screen.server
  (:require [clojure.java.io :as io]
            [post-to-screen.dev :refer [is-dev? inject-devmode-html browser-repl start-figwheel]]
            [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [resources]]
            [compojure.handler :refer [api]]
            [net.cgrand.enlive-html :refer [deftemplate]]
            [ring.middleware.reload :as reload]
            [environ.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]
            [hiccup.page :as page]
            [hiccup.form :as form]
            [hiccup.util :as util]))

(defn page-layout [title & body]
  (page/html5
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     [:title title]
     (page/include-css "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css")]
    [:body
     [:div.container
      body]
     (page/include-js "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/js/bootstrap.min.js")]))


(defn code-layout [title & body]
  (page/html5
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     [:title title]
     (page/include-css "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css")
     (page/include-css "http://cdnjs.cloudflare.com/ajax/libs/highlight.js/8.4/styles/zenburn.min.css")
     (page/include-js "http://cdnjs.cloudflare.com/ajax/libs/highlight.js/8.4/highlight.min.js")]
    [:body
     [:script "hljs.initHighlightingOnLoad();"]
     [:div.container
      body]
     (page/include-js "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/js/bootstrap.min.js")]))

(defn post-form []
  (page-layout
    "Post to Screen"
    [:h1 "Post to Screen"]
    (form/form-to {:role "form"}
      [:post "/"]
      [:div.form-group
       (form/text-area {:class "form-control" :rows 20} "code")]
      (form/submit-button {:class "btn btn-primary"} "Post code"))))

(defn show-code [code]
  (code-layout
    "Posted code"
    [:pre
     [:code (util/escape-html code)]]))

(deftemplate page
  (io/resource "index.html") [] [:body] (if is-dev? inject-devmode-html identity))

(defroutes routes
  (resources "/")
  (resources "/react" {:root "react"})
  (GET "/" [_] (post-form))
  (POST "/" [code] (show-code code))
  (GET "/*" req (page)))

(def http-handler
  (if is-dev?
    (reload/wrap-reload (api #'routes))
    (api routes)))

(defn run [& [port]]
  (defonce ^:private server
    (do
      (if is-dev? (start-figwheel))
      (let [port (Integer. (or port (env :port) 10555))]
        (print "Starting web server on port" port ".\n")
        (run-jetty http-handler {:port port
                          :join? false}))))
  server)

(defn -main [& [port]]
  (run port))
