(ns post-to-screen.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(enable-console-print!)

(defonce app-state (atom {:posts []}))

(defn submit-code [posts e]
  (let [code (-> js/document
                 (.getElementById "code")
                 .-value)]
    (om/transact! posts #(conj % code))
    (.preventDefault e)))

(defn post-form [posts]
  (html
    [:form.form-horizontal {:role "form"
                            :on-submit (partial submit-code posts)}
     [:div.form-group
      [:label.control-label.col-xs-1 {:for "code"} "Code:"]
      [:div.col-xs-10
       [:textarea#code.form-control {:rows "15"}]]]
     [:div.form-group
      [:div.col-xs-offset-1.col-xs-10
       [:button.btn {:type "submit"} "Post code"]]]]))

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
                         [(str "button.btn.btn-default" (if (= i selected) ".active" ""))
                          {:type "button"
                           :on-click (fn [_] (om/set-state! owner [:selected] i))}
                          tab])
                       tabs)]
         [:hr]
         (case (get tabs selected)
           "Home" [:h2 "Home"]
           "Post" (post-form (:posts cursor)))]))))


(defn application [cursor owner]
  (reify
    om/IRender
    (render [_]
      (html
        [:div.container
         [:h1 "Post to screen"]
         (om/build tab-view cursor)]))))

(defn main []
  (om/root
    application
    app-state
    {:target (. js/document (getElementById "app"))}))
