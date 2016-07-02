(ns dev
  (:require [figwheel.client :as figwheel :include-macros true]
            [devtools.core   :as devtools]))

(enable-console-print!)

(devtools/set-pref! :install-sanity-hints true) ; this is optional
(devtools/install!)

(figwheel/watch-and-reload :websocket-url "ws://localhost:3449/figwheel-ws")
