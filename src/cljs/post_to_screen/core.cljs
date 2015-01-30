(ns post-to-screen.core
  (:require-macros [cljs.core.async.macros :as asyncm :refer (go go-loop)])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :as async :refer (<! >! put! chan)]
            [taoensso.sente  :as sente :refer (cb-success?)]))

(enable-console-print!)

(defonce app-state (atom {:selected-tab  "Post"
                          :selected-post 0
                          :posts         []}))

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

(defmulti handle-event (fn [[tag _] _ _] tag))

(defmethod handle-event :post-to-screen/code [[_ code] app _]
  (om/transact! app [:posts] #(conj % code)))

(defmethod handle-event :default [event _ _]
  #_(print "Received:" event))

(defn event-loop [cursor owner]
  (go-loop []
           (let [{:keys [event]} (<! ch-chsk)
                 [ev-id ev-data] event]
             (when (vector? ev-data)
               (case ev-id
                 :chsk/recv (handle-event ev-data cursor owner)
                 nil))
             (recur))))

; UI

(defn code-view [{:keys [selected-post posts] :as cursor} owner]
  (reify
    om/IRender
    (render [_]
      (html
        [:div
         [:div.col-xs-2
          [:ul.list-unstyled
           (map (fn [i]
                  [:li
                   {:on-click (fn [_] (om/update! cursor [:selected-post] i))}
                   ((if (= i selected-post) (fn [text] [:strong text]) identity) (str "Code " (inc i)))])
                (reverse (range (count posts))))]]
         (when (seq posts)
           [:div.col-xs-10
            [:pre#codeview
             [:code (get posts selected-post)]]])]))
    om/IDidUpdate
    (did-update [_ _ _]
      (let [codeview (-> js/document
                         (.getElementById "codeview"))]
        (.highlightBlock js/hljs codeview)))))

(defn submit-code [cursor e]
  (let [code (-> js/document
                 (.getElementById "code"))]
    #_(print "Sent: " [:post-to-screen/code code])
    (chsk-send! [:post-to-screen/code (.-value code)])
    (set! (.-value code) "")
    (om/update! cursor [:selected-tab] "Show")
    (.preventDefault e)))

(defn post-form [cursor owner]
  (reify
    om/IRender
    (render [_]
      (html
        [:form.form-horizontal {:role      "form"
                                :on-submit (partial submit-code cursor)}
         [:div.form-group
          [:label.control-label.col-xs-1 {:for "code"} "Code:"]
          [:div.col-xs-10
           [:textarea#code.form-control {:rows "15"}]]]
         [:div.form-group
          [:div.col-xs-offset-1.col-xs-10
           [:button.btn {:type "submit"} "Post code"]]]]))
    om/IDidMount
    (did-mount [_]
      (-> js/document
          (.getElementById "code")
          .focus))))

(defn tab-view [{:keys [selected-tab] :as cursor} owner]
  (reify
    om/IRender
    (render [_]
      (let [tabs ["Post" "Show"]]
        (html
          [:div
           [:div.btn-group {:role "toolbar"}
            (map-indexed (fn [i tab]
                           [(str "button.btn.btn-default" (if (= tab selected-tab) ".active" ""))
                            {:type     "button"
                             :on-click (fn [_] (om/update! cursor [:selected-tab] tab))}
                            tab])
                         tabs)]
           [:hr]
           (case selected-tab
             "Post" (om/build post-form cursor)
             "Show" (om/build code-view cursor))])))))

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
