(ns post-to-screen.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [om-bootstrap.button :as b]))

(enable-console-print!)

(defonce app-state (atom {:text "Hello Chestnut!"}))

(defn tab-view [cursor owner]
  (reify
    om/IInitState
    (init-state [_]
      {:tabs ["Home" "Post"]
       :selected 0})
    om/IRenderState
    (render-state [_ {:keys [tabs selected]}]
      (html
        [:div
         (b/toolbar {}
                    (map-indexed (fn [i tab]
                                   (b/button {:bs-style
                                              (if (= i selected) "primary" "default")
                                              :on-click
                                              (fn [_]
                                                (om/set-state! owner [:selected] i))}
                                             tab))
                                 tabs))
         (case (get tabs selected)
           "Home" [:h2 "Home"]
           "Post" [:h2 "Post"])]))))


(defn application [cursor owner]
  (reify
    om/IRender
    (render [_]
      (html
        [:div.container
         [:h1 (:text cursor)]
         (om/build tab-view cursor)]))))

(defn main []
  (om/root
    application
    app-state
    {:target (. js/document (getElementById "app"))}))
