(ns app-client.user
  (:require [app-client.core :as client]))

(defn after-load
  []
  (client/render!))
