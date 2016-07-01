(ns ringi.routes)

(def routes
  ["/" [[""         :index]
        ["login"    :login]
        ["logout"   :logout]
        ["register" :register]        
        ["api"     {:post :api}]
        ["topics" {#"/?" :topics-index
                   ["/"  :topic-id]  :topics-show
                   "/new" :topics-create}]        
        #?@(:clj [[true :resource]])
        [true :not-found]]])
