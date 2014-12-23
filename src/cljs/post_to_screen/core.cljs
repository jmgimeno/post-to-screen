(ns post-to-screen.core
  (:require-macros [cljs.core.async.macros :as asyncm :refer (go go-loop)])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :as async :refer (<! >! put! chan)]
            [taoensso.sente  :as sente :refer (cb-success?)]))

(enable-console-print!)

(defonce app-state (atom {:status :unconnected
                          :posts []}))

; WebSockets

(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket! "/chsk" ; Note the same path as before
                                  {:type :auto ; e/o #{:auto :ajax :ws}
                                   })]
  (def chsk       chsk)
  (def ch-chsk    ch-recv) ; ChannelSocket's receive channel
  (def chsk-send! send-fn) ; ChannelSocket's send API fn
  (def chsk-state state)   ; Watchable, read-only atom
  )

(defmulti handle-event (fn [[ev-id ev-data] app owner] ev-id))

(defmethod handle-event :default [[ev-id ev-data :as event] app owner]
  (print "Received:" event))

(defn event-loop [cursor owner]
  (go-loop []
           (let [{:keys [event]} (<! ch-chsk)]
             (handle-event event cursor owner)
             (recur))))

; UI

(defn code-view [posts owner]
  (reify
    om/IInitState
    (init-state [_]
      {:selected nil})
    om/IRenderState
    (render-state [_ {:keys [selected]}]
      (html
        [:div
         [:div.col-xs-1
          [:ul.list-unstyled
           (map-indexed
             (fn [i post]
               (let [pos (- (count posts) i)]
                 [:li
                  {:on-click (fn [_] (om/set-state! owner [:selected] (dec pos)))}
                  (str "Code " pos)]))
             (reverse posts))]]
         (when selected
           [:div.col-xs-11
            [:pre#codeview
             [:code (get posts selected)]]])]))
    om/IDidUpdate
    (did-update [this prev-props prev-state]
      (let [code (-> js/document
                     (.getElementById "codeview"))]
        (.highlightBlock js/hljs code)))))

(defn submit-code [posts e]
  (let [code (-> js/document
                 (.getElementById "code")
                 .-value)]
    #_(om/transact! posts #(conj % code))
    (print "Sent: " [:post-to-screen/code code])
    (chsk-send! [:post-to-screen/code code])
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
           "Home" (om/build code-view (:posts cursor))
           "Post" (post-form (:posts cursor)))]))))


(defn application [cursor owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (event-loop cursor owner))
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
