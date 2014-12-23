(ns post-to-screen.server
  (:require [clojure.java.io :as io]
            [post-to-screen.dev :refer [is-dev? inject-devmode-html browser-repl start-figwheel]]
            [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [resources not-found]]
            [compojure.handler :refer [site]]
            [net.cgrand.enlive-html :refer [deftemplate]]
            [ring.middleware.reload :as reload]
            [environ.core :refer [env]]
            [ring.util.response :refer [redirect]]
            [org.httpkit.server :refer [run-server]]
            [taoensso.sente :as sente]
            [clojure.core.async :as async :refer [<! <!! chan go-loop thread]])
  (:import [java.util UUID]))

(let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn
              connected-uids]}
      (sente/make-channel-socket! {})]
  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids) ; Watchable, read-only atom
 )

; UUID and session management

(defn unique-id
  "Return a really unique ID (for an unsecured session ID).
  No, a random number is not unique enough. Use a UUID for real!"
  []
  (.toString (UUID/randomUUID)))

(defn session-uid
  "Convenient to extract the UID that Sente needs from the request."
  [req]
  (get-in req [:session :uid]))

(deftemplate page
  (io/resource "index.html") [] [:body] (if is-dev? inject-devmode-html identity))

(defn index
  "Handle index page request. Injects session uid if needed."
  [req]
  {:status 200
   :session (if (session-uid req)
              (:session req)
              (assoc (:session req) :uid (unique-id)))
   :body (page)})

; Application routes

(defroutes routes
  (resources "/")
  (resources "/react" {:root "react"})

  (POST "/posts" [code] (post-code code))

  (GET  "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post                req))

  (GET "/" req (#'index req))

  (not-found "These are not the androids that you're looking for."))

; Event handling

(defmulti handle-event (fn [[ev-id ev-data]] ev-id))

(defmethod handle-event :post-to-screen/code [event]
  #_(println "Received " event)
  (doseq [uid (:any @connected-uids)]
    #_(println "Sent " event " to " uid)
    (chsk-send! uid event)))

(defmethod handle-event :default [[ev-id ev-data :as event]]
  #_(println "Received:" event))

(def http-handler
  (if is-dev?
    (reload/wrap-reload (site #'routes))
    (site routes)))

(defn event-loop []
  (go-loop []
           (let [{:keys [event]} (<! ch-chsk)]
             (thread (handle-event event)))
           (recur)))

; Server

(defn run [& [port]]
  (event-loop)
  (defonce ^:private server
    (do
      (if is-dev? (start-figwheel))
      (let [port (Integer. (or port (env :port) 10555))]
        (print "Starting web server on port" port ".\n")
        (run-server http-handler {:port port}))))
  server)

(defn -main [& [port]]
  (run port))
