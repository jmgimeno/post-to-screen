(ns post-to-screen.server
  (:require [clojure.java.io :as io]
            [post-to-screen.dev :refer [is-dev? inject-devmode-html browser-repl start-figwheel]]
            [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [resources not-found]]
            [compojure.handler :refer [site]]
            [net.cgrand.enlive-html :refer [deftemplate]]
            [ring.middleware.reload :as reload]
            [environ.core :refer [env]]
            [hiccup.page :as page]
            [hiccup.form :as form]
            [hiccup.util :as util]
            [ring.util.response :refer [redirect]]
            [org.httpkit.server :refer [run-server]]))

; State

(defonce posts (ref {}))
(defonce nextId (ref 1))

(defn clear-state []
  (dosync
    (ref-set posts {})
    (ref-set nextId 1)))

; Post form

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

(defn post-form []
  (page-layout
    "Post to Screen"
    [:h1 "Post to Screen"]
    (form/form-to {:role "form"}
                  [:post "/posts"]
                  [:div.form-group
                   (form/text-area {:class "form-control" :rows 20} "code")]
                  (form/submit-button {:class "btn btn-primary"} "Post code"))))

; Show code

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

(defn show-code [id]
  (let [code (get @posts (read-string id))]
    (if code
      (code-layout
        (str "Fragment " id)
        [:h1 (str "Fragment " id)]
        [:pre
         [:code (util/escape-html code)]])
      (not-found
        (page-layout
          "Unexistent fragment"
          [:div {:class "alert alert-danger" :role "alert"} "Unexistent fragment"])))))

; Post code

(defn post-code [code]
  (dosync
    (alter posts assoc @nextId code)
    (alter nextId inc))
  (redirect "/"))

; Show list of posts

(defn show-posts []
  (page-layout
    "List of fragments"
    [:h1 "List of fragments"]
    [:ul
     (for [k (reverse (sort (keys @posts)))]
       [:li [:a {:href (str "/posts/" k)} (str "Fragment " k)]])]))

; 404 page

(defn show-not-found []
  (page-layout
    "Unexistent page"
    [:div {:class "alert alert-danger" :role "alert"} "Unexistent page"]))

(deftemplate page
  (io/resource "index.html") [] [:body] (if is-dev? inject-devmode-html identity))

(defroutes routes
  (resources "/")
  (resources "/react" {:root "react"})

  (GET "/" [_] (post-form))

  (GET  "/posts" [_] (show-posts))
  (POST "/posts" [code] (post-code code))

  (GET "/posts/:id" [id] (show-code id))

  #_(GET "/*" req (page))

  (not-found (show-not-found)))

(def http-handler
  (if is-dev?
    (reload/wrap-reload (site #'routes))
    (site routes)))

(defn run [& [port]]
  (defonce ^:private server
    (do
      (if is-dev? (start-figwheel))
      (let [port (Integer. (or port (env :port) 10555))]
        (print "Starting web server on port" port ".\n")
        (run-server http-handler {:port port}))))
  server)

(defn -main [& [port]]
  (run port))
