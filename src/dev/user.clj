(ns user
  (:require [app-server.core :as server]
            [shadow.cljs.devtools.server :as shadow.server]
            [shadow.cljs.devtools.api :as shadow]))

(defonce shadow-server (delay
                         (shadow.server/start!)))

(defn -main
  [& _]
  (server/run-dev)
  (prn [@shadow-server])
  (shadow/watch :web))
