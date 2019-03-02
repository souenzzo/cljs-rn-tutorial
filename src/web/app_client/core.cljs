(ns app-client.core
  (:require [fulcro.client :as fc]
            [goog.dom :as gdom]
            [fulcro.client.primitives :as fp]
            [fulcro.client.routing :as fr]
            [fulcro.client.dom :as dom]
            [fulcro.client.mutations :as fm]
            [fulcro.client.network :as net]
            [goog.object :as gobj]
            [fulcro.client.data-fetch :as df]))

(fp/defsc CounterComp [this {:app.counter/keys [value id]}]
  {:query [:app.counter/id
           :app.counter/value]
   :ident [:app.counter/id :app.counter/id]}
  (dom/div
    (pr-str value)
    (dom/button {:disabled (not (number? value))
                 :onClick  #(fp/transact! this `[(app/inc ~{:app.counter/id id})])} "Inc")))

(def ui-counter (fp/factory CounterComp {:keyfn :app.counter/id}))

(fm/defmutation app/inc
  [{:app.counter/keys [id]}]
  (action [{:keys [state]}]
          (swap! state update-in [:app.counter/id id :app.counter/value] inc))
  (remote [{:keys [ast state]}]
          (-> ast
              (fm/returning state CounterComp))))

(fp/defsc Home [this {::keys   [page id]
                      :ui/keys [counter]}]
  {:query         [{:ui/counter (fp/get-query CounterComp)}
                   ::id
                   ::page]
   :ident         (fn [] [page id])
   :initial-state (fn [_] {::page ::home
                           ::id   ::home})}

  (dom/div
    (dom/button {:onClick #(df/load this :>/counter CounterComp
                                    {:target [::home ::home :ui/counter]})} "Refresh")
    (ui-counter counter)))

(fr/defsc-router Router [this {::keys [page id]}]
  {:router-targets {::home Home}
   :ident          (fn [] [page id])
   :router-id      ::router
   :default-route  Home}
  "404")

(def ui-router (fp/factory Router))

(fp/defsc Root [this {::keys [router]}]
  {:query         [{::router (fp/get-query Router)}]
   :initial-state (fn [_]
                    {::router (fp/get-initial-state Router _)})}
  (ui-router router))

(defonce PWA
         (atom nil))

(defn render!
  []
  (swap! PWA fc/mount Root (gdom/getElement "app")))

(defn ^:export main
  []
  (let [csrf-token (-> js/document
                       (gobj/get "body")
                       (gobj/get "dataset")
                       (gobj/get "antiForgeryToken"))
        client (fc/make-fulcro-client
                 {:networking (net/fulcro-http-remote {:url                "/api"
                                                       :request-middleware (comp (net/wrap-csrf-token csrf-token)
                                                                                 (net/wrap-fulcro-request))})})]
    (reset! PWA client)
    (render!)))
