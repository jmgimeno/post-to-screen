(ns post-to-screen.server
  (:require [clojure.java.io :as io]
            [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [resources not-found]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.logger :refer [wrap-with-logger]]
            [environ.core :refer [env]]
            [org.httpkit.server :refer [run-server]]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer (sente-web-server-adapter)]
            [clojure.core.async :as async :refer [<! go-loop thread]])
  (:import [java.util UUID])
  (:gen-class))

(let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn
              connected-uids]}
      (sente/make-channel-socket! sente-web-server-adapter {})]
  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids) ; Watchable, read-only atom
  )

; Event handling

(defmulti handle-event (fn [[ev-id _]] ev-id))

(defmethod handle-event :post-to-screen/code [event]
  #_(println "Received " event)
  (doseq [uid (:any @connected-uids)]
    #_(println "Sent " event " to " uid)
    (chsk-send! uid event)))

(defmethod handle-event :default [[ev-id ev-data :as event]]
  #_(println "Received:" event))

(defn event-loop []
  (go-loop []
    (let [{:keys [event]} (<! ch-chsk)]
      (thread (handle-event event)))
    (recur)))

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

(defn index
  "Handle index page request. Injects session uid if needed."
  [req]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :session (if (session-uid req)
              (:session req)
              (assoc (:session req) :uid (unique-id)))
   :body (io/input-stream (io/resource "public/index.html"))})

; Application routes

(defroutes routes
  (resources "/")
  (resources "/react" {:root "react"})

  (GET  "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post                req))

  (GET "/" req (#'index req))

  (not-found "These are not the androids that you're looking for."))

(def http-handler
  (-> routes
      (wrap-defaults api-defaults)
      wrap-with-logger
      wrap-gzip))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 10555))]
    (event-loop)
    (run-server http-handler {:port port :join? false})))
