(ns ringi.routes)

(def routes
  ["/" [[""         :index]
        ["login"    :login]
        ["register" :register]
        ["topics" {#"/?" :topics/list
                   ["/"  :topic/id] :topics/show
                   "/new" :topics/create}]                
        #?@(:clj [["logout"  {:delete :logout}]
                  ["api"     {:post   :api}]
                  [true :resource]])]])
