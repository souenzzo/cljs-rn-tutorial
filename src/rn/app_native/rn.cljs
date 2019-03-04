(ns app-native.rn
  (:require ["react-native" :as rn]
            ["react" :as r]))

(defn main
  []
  (r/createElement rn/Text #js {} "Hello from cljs"))