(ns post-to-screen.core
  (:require-macros [cljs.core.async.macros :as asyncm :refer (go go-loop)])
  (:require [reagent.core :as reagent :refer [atom]]
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

(defmulti handle-event (fn [[tag _] _] tag))

(defmethod handle-event :post-to-screen/code [[_ code] state]
  (swap! state update-in [:posts] #(conj % {:key (count %) :code code})))

(defmethod handle-event :default [event _ _]
  #_(print "Received:" event))

(defn event-loop [data]
  (go-loop []
           (let [{:keys [event]} (<! ch-chsk)
                 [ev-id ev-data] event]
             (when (vector? ev-data)
               (case ev-id
                 :chsk/recv (handle-event ev-data data)
                 nil))
             (recur))))

; UI

(defn code-view [data]
  (let [{:keys [selected-post posts]} @data]
    [:div
     [:div.col-xs-2
      [:ul.list-unstyled
       (map (fn [{key :key}]
              ^{:key key}
              [:li
               {:on-click (fn [_] (swap! data assoc :selected-post key))}
               ((if (= key selected-post) (fn [text] [:strong text]) identity) (str "Code " (inc key)))])
            (reverse posts))]]
     (when (seq posts)
       [:div.col-xs-10
        [:pre#codeview
         [:code (get-in posts [selected-post :code])]]])]))

(def code-view
  (with-meta code-view
             {:component-did-update
              (fn [_ _]
                (let [codeview (-> js/document
                                   (.getElementById "codeview"))]
                  (.highlightBlock js/hljs codeview)))}))

(defn submit-code [data e]
  (let [code (-> js/document
                 (.getElementById "code"))]
    #_(print "Sent: " [:post-to-screen/code code])
    (chsk-send! [:post-to-screen/code (.-value code)])
    (set! (.-value code) "")
    (swap! data assoc :selected-tab "Show")
    (.preventDefault e)))

(defn post-form [data]
  [:form.form-horizontal {:role      "form"
                          :on-submit (partial submit-code data)}
   [:div.form-group
    [:label.control-label.col-xs-1 {:for "code"} "Code:"]
    [:div.col-xs-10
     [:textarea#code.form-control {:rows "15"}]]]
   [:div.form-group
    [:div.col-xs-offset-1.col-xs-10
     [:button.btn {:type "submit"} "Post code"]]]])

(def post-form
  (with-meta post-form
             {:component-did-mount
              (fn [_]
                (-> js/document
                    (.getElementById "code")
                    .focus))}))

(defn tab-view [data]
  (let [tabs ["Post" "Show"]
        selected-tab (:selected-tab @data)]
    [:div
     [:div.btn-group {:role "toolbar"}
      (map-indexed (fn [i tab]
                     ^{:key i}
                     [(str "button.btn.btn-default" (if (= tab selected-tab) ".active" ""))
                      {:type     "button"
                       :on-click (fn [_] (swap! data assoc :selected-tab tab))}
                      tab])
                   tabs)]
     [:hr]
     (case selected-tab
       "Post" [post-form data]
       "Show" [code-view data])]))

(defn application [data]
  [:div.container
   [:h1 "Post to screen"]
   [tab-view data]])

(defn main []
  (let [data (atom {:selected-tab  "Post"
                    :selected-post 0
                    :posts         []})]
    (event-loop data)
    (reagent/render-component
      [application data]
      (. js/document (getElementById "app")))))
