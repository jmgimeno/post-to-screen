(ns post-to-screen.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(enable-console-print!)

(defonce app-state (atom {:text "Hello Chestnut!"}))

(defn post-form []
  (html
    [:form.form-horizontal {:role "form"}
     [:div.form-group
      [:label.control-label.col-xs-1 {:for "code"} "Code:"]
      [:div.col-xs-10
       [:textarea.form-control {:rows "15"}]]]
     [:div.form-group
      [:div.col-xs-offset-1.col-xs-10
       [:button.btn.btn-primary {:type "submit"} "Post code"]]]]))

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
         [:div.btn-group {:role "toolbar"}
          (map-indexed (fn [i tab]
                         [(str "button.btn.btn-" (if (= i selected) "primary" "default"))
                          {:type "button"
                           :on-click (fn [_] (om/set-state! owner [:selected] i))}
                          tab])
                       tabs)]
         [:hr]
         (case (get tabs selected)
           "Home" [:h2 "Home"]
           "Post" (post-form))]))))


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
