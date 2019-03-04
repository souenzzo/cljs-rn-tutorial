(ns app-native.rn
  (:require ["react-native" :as rn]
            ["react" :as r]
            [app-native.rn-support :as rn-support]
            [fulcro.client :as fc]
            [goog.dom :as gdom]
            [fulcro.client.primitives :as fp]
            [fulcro.client.routing :as fr]
            [fulcro.client.mutations :as fm]
            [fulcro.client.network :as net]
            [goog.object :as gobj]
            [fulcro.client.data-fetch :as df]))

(defn Text
  [props & childs]
  (apply r/createElement rn/Text props childs))

(defn Button
  [props & childs]
  (apply r/createElement rn/Button props childs))

(defn View
  [props & childs]
  (apply r/createElement rn/View props childs))

(fp/defsc CounterComp [this {:app.counter/keys [value id]}]
  {:query [:app.counter/id
           :app.counter/value]
   :ident [:app.counter/id :app.counter/id]}
  (View
    #js {}
    (Text #js {} (pr-str value))
    (Button #js {:disabled (not (number? value))
                 :title    "Inc"
                 :onClick  #(fp/transact! this `[(app/inc ~{:app.counter/id id})])})))

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

  (View
    #js {}
    (Button #js {:title   "Refresh"
                 :onClick #(df/load this :>/counter CounterComp
                                    {:target [::home ::home :ui/counter]})})
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

(defonce app
         (atom (fc/make-fulcro-client
                 {:reconciler-options {:root-render  rn-support/root-render
                                       :root-unmount rn-support/root-unmount}})))

(defonce RootNode (rn-support/root-node! 1))
(defonce ui-root-node (fp/factory RootNode))

(defn reload []
  (swap! app fc/mount Root 1))

(defn main
  []
  (reload)
  (ui-root-node {}))


