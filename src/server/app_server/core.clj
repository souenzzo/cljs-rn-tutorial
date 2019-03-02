(ns app-server.core
  (:require [clojure.core.async :as async]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [cognitect.transit :as transit]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.core :as p]
            [fulcro.client.dom-server :as dom]
            [fulcro.client.primitives :as fp]
            [io.pedestal.http :as http]
            [io.pedestal.http.csrf :as csrf]
            [io.pedestal.log :as log]
            [ring.util.mime-type :as mime])
  (:import (org.eclipse.jetty.servlet ServletContextHandler)
           (org.eclipse.jetty.server.handler.gzip GzipHandler)))

(pc/defmutation inc-mutation [{::keys [counter]} _]
  {::pc/sym    `app/inc
   ::pc/output [:app.counter/value
                :app.counter/id]}
  (let [value (swap! counter inc)]
    {:app.counter/value value
     :app.counter/id    0}))

(pc/defresolver current-value [{::keys [counter]} _]
  {::pc/output [:app.counter/value
                :app.counter/id]}
  {:app.counter/value @counter
   :app.counter/id    0})

(def my-registers
  [inc-mutation current-value])

(defonce counter (atom 0))

(def parser
  (p/parallel-parser
    {::p/env     {::p/reader               [p/map-reader
                                            pc/all-parallel-readers
                                            p/env-placeholder-reader]
                  ::counter                counter
                  ::p/placeholder-prefixes #{">"}}
     ::p/mutate  pc/mutate-async
     ::p/plugins [(pc/connect-plugin {::pc/register my-registers})
                  p/error-handler-plugin]}))

(defn pr-transit
  [type body]
  (fn pr-transit [out]
    (try
      (let [writer (transit/writer out type)]
        (transit/write writer body))
      (catch Throwable e
        (log/error :type type :body body :pr-transit e)))))

(defn http-parser
  [{:keys [transit-params]
    :as   req}]
  {:body   (pr-transit :json-verbose (async/<!! (parser req transit-params)))
   :status 200})

(fp/defsc Index [this {::csrf/keys [anti-forgery-token]}]
  {:query [::csrf/anti-forgery-token]}
  (dom/html
    (dom/head
      (dom/link {:rel  "icon"
                 :href "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg'%3E%3C/svg%3E"})
      (dom/title "Hello"))
    (dom/body
      {:data-anti-forgery-token anti-forgery-token
       :data-target-id          "app"}
      (dom/div {:id "app"})
      (dom/script {:src "/_static/web/main.js"})
      (dom/script {:dangerouslySetInnerHTML {:__html "app_client.core.main()"}}))))


(def ui-index (fp/factory Index))

(defn http-index
  [req]
  {:headers {"Cache-Control" "no-cache"
             "Content-Type"  "text/html; charset=utf-8"}
   :body    (->> ["<!DOCTYPE html>"
                  (dom/render-to-str (ui-index req))]
                 (string/join "\n"))
   :status  200})

(defn http-static
  [{{:keys [path]} :path-params}]
  (let [f (io/file (str "target/public/" path))]
    (if (.isFile f)
      {:body   f
       :status 200}
      {:status 404})))

(def routes
  `#{["/api" :post [http-parser]]
     ["/" :get [http-index]]
     ["/_static/*path" :get [http-static]]})

(defn context-configurator
  "Habilitando gzip nas respostas"
  [^ServletContextHandler context]
  (let [gzip-handler (GzipHandler.)]
    (.addIncludedMethods gzip-handler (into-array ["GET" "POST"]))
    (.setExcludedAgentPatterns gzip-handler (make-array String 0))
    (.setGzipHandler context gzip-handler))
  context)


(def content-security-policy-settings
  (string/join " " ["script-src"
                    "'self'"
                    "'unsafe-inline'"
                    "'unsafe-eval'"]))

(def service
  {:env                     :prod
   ::http/routes            routes
   ::http/port              8080
   ::http/enable-csrf       {}
   ::http/mime-types        mime/default-mime-types
   ::http/join?             false
   ::http/container-options {:context-configurator context-configurator}
   ::http/secure-headers    {:content-security-policy-settings content-security-policy-settings}
   ::http/type              :jetty})

(defonce server (atom nil))

(defn run-dev
  []
  (swap! server (fn [st]
                  (when st
                    (http/stop st))
                  (-> service
                      (assoc :env :dev)
                      http/default-interceptors
                      http/dev-interceptors
                      http/create-server
                      (http/start)))))

(defn -main
  []
  (swap! server (fn [st]
                  (when st
                    (http/stop st))
                  (-> service
                      http/default-interceptors
                      http/create-server
                      (http/start)))))
