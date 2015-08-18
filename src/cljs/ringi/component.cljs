(ns ringi.component
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [datascript   :as d]
            [clojure.string :refer [blank?]]
            [ringi.api    :as api]
            [ringi.db     :as db :refer [unbind bind conn persist!]]
            [ringi.query  :as q]))

(defn index [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/a #js {:href "/"} "please register/login"))))

(defn login-form [app owner]
  (reify
    om/IRender
    (render [this]
      (dom/form #js {:className "login" :method "POST" :action "/login"}
        (dom/span  #js {:className "login-title"} "Log in")
        (dom/label #js {:htmlFor "username"} "Username")
        (dom/input #js {:type "text" :name "username" :placeholder "username"})
        (dom/label #js {:htmlFor "password"} "Password")
        (dom/input #js {:type "password" :name "password" :placeholder "password123"})
        (dom/input #js {:className "button login-button" :type "submit" :value "Log in"})))))

(defn registration-form [app owner]
  (reify
    om/IRender
    (render [this]
      (dom/form #js {:className "register" :method "POST" :action "/register"}
        (dom/span  #js {:className "register-title"} "Sign Up")
        (dom/label #js {:htmlFor "username"} "Username")
        (dom/input #js {:type "text" :name "username" :placeholder "username"})
        (dom/label #js {:htmlFor "email"} "Email")
        (dom/input #js {:type "text" :name "email" :placeholder "example@coolguys.pro"})
        (dom/label #js {:htmlFor "password"} "Password")
        (dom/input #js {:type "password" :name "password" :placeholder "password123"})
        (dom/input #js {:className "button register-button" :type "submit" :value "Register"})))))

(defn menu-list [app owner]
  (reify
    om/IRender
    (render [this]
      (dom/ul #js {:className "menu-list"}
        (dom/li #js {:className "menu-list-item"}
          (dom/a #js {:href "/register"} "sign up"))
        (dom/li #js {:className "menu-list-item"}
          (dom/a #js {:href "/login"} "log in"))
        (dom/li #js {:className "menu-list-item"}
          (dom/a #js {:href "/t/new"} "new"))))))

(defn logged-in-menu-list [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:show-menu false})
    om/IRenderState
    (render-state [this state]
      (dom/ul #js {:className "menu-list"}
        (if (:show-menu state)
          (dom/div #js {:className "submenu"}
            (dom/ul #js {:className "submenu-list"}
              (dom/li #js {:className "submenu-list-item"}
                (dom/a #js {:href "/logout"} "log out")))))
        (dom/li #js {:className "menu-list-item"
                     :onClick (fn [e] (om/update-state! owner :show-menu not))}
          (str "@" (get-in app [:current-user :name])))
        (dom/li #js {:className "menu-list-item"}
          (dom/a #js {:href "/t/new"} "new"))))))


(defn menu [app owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className "menu"}
        (dom/h1 #js {:className "menu-title"}
          (dom/a #js {:href "/"} "Ringi"))
        (if (:current-user app)
          (om/build logged-in-menu-list app)
          (om/build menu-list app))))))

(defn topic-item [topic owner]
  (reify
    om/IRender
    (render [this]
      (let [{:keys [topic/id
                    topic/title
                    topic/author]} topic]
        (dom/li #js {:className "topics-list-item"}
          (dom/a #js {:className "topics-title" :href (str "/t/" id)} title))))))

(defn dashboard [{:keys [topics
                         conn
                         current-user] :as app} owner]
  (reify
    om/IWillMount
    (will-mount [this]
      (let [current-user (:id current-user)]
        (bind conn app :topics q/topics-by-author [current-user q/topic-p])))
    om/IWillUnmount
    (will-unmount [this]
      (unbind conn app :topics))
    om/IRender
    (render [this]
      (dom/div nil
        (dom/h2 nil "My Topics")
        (apply dom/ul #js {:className "topics"}
          (om/build-all topic-item topics {:key :db/id}))))))

(defn handle-change [e owner korks]
  (om/set-state! owner korks (.. e -target -value)))

(defn add-choice [owner]
  (om/update-state! owner :choices (fn [s] (conj s {:title ""}))))

(defn create-topic [owner state]
  (let [api-ch (get-in state [:comms :api])
        topic (om/get-state owner)
        topic (update-in topic [:description] #(if-not (clojure.string/blank? %) % nil))
        topic (update-in topic [:choices] (partial remove #(clojure.string/blank? (:title %))))]

    (api/call! api-ch :create-topic {:data topic})))

(defn topic-form [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:title       ""
       :description ""
       :choices (vec (repeatedly 3 (fn [] {:title ""})))})
      om/IRenderState
      (render-state [this {:keys [title description choices] :as state}]
        (dom/form #js {:className "topics-form"
                       :onSubmit  (fn [e]
                                    (create-topic owner app)
                                    (.preventDefault e))}
          (dom/input #js {:type "text"
                          :name "title"
                          :value title
                          :onChange #(handle-change % owner :title)
                          :placeholder "Title"})
          (dom/textarea #js {:description "title"
                             :placeholder "Description..."
                             :value description
                             :onChange #(handle-change % owner :description)})
          (dom/h3 nil "Choices")
          (apply dom/ul #js {:className "topic-form-choices"}
            (for [[idx choice] (map-indexed vector choices)]
              (dom/li #js {:className "choice-form"}
                (dom/input #js {:type "text"
                                :placeholder "Add Choice"
                                :value (:title choice)
                                :onChange #(handle-change % owner [:choices idx :title])}))))
          (if (< (count choices) 5)
            (dom/div #js {:onClick #(add-choice owner)} "add choice"))
          (dom/input #js {:className "button" :type "submit" :value "Submit"})))))

(defn submit-vote [e owner user my-vote choice]
  (let [shared (om/get-shared owner)
        {api-ch :api persist-ch :persist} (:comms shared)

        value (.. e -target -value)]
    (if my-vote
      (persist! persist-ch [{:db/id (:db/id my-vote) :vote/value value}])
      (persist! persist-ch [{:_votes      (:db/id choice)
                             :vote/value  value
                             :vote/author {:user/id user}}]))
    (api/call! api-ch :vote {:choice choice
                            :value value})))

(defn vote-form [app owner]
  (reify
    om/IRender
    (render [this]
      (let [user    (:id (:user app))
            choice  (:choice app)
            votes   (:votes choice)
            my-vote (first (filter #(= (get-in % [:vote/author :user/id]) user) votes))]
        (apply dom/form #js {:className "vote-form"}
          (for [value ["yes" "ok" "no"]
                :let [name (str (:choice/id choice) "-" value)]]
            (dom/div #js {:className (str "vote-form-" value)}
              (dom/input #js {:type "radio"
                              :value value
                              :name name
                              :id   name
                              :onChange (fn [e] (if user
                                                  (submit-vote e owner user my-vote choice)

                                                  (set! (.-location js/window) "/login")))
                              :checked (= (:vote/value my-vote) value)})
              (dom/label #js {:htmlFor name} value))))))))

(defn vote-data [app owner]
  (reify
    om/IRender
    (render [this]
      (let [choice (:choice app)
            votes (group-by :vote/value (:votes choice))]
        (apply dom/ul #js {:className "votes"}
          (for [value ["yes" "ok" "no"]
                vote  (get votes value)]
            (dom/li #js {:className (str "vote " value)})))))))

(defn choice-view [app owner]
  (reify
    om/IRender
    (render [this]
      (dom/li #js {:className "choice"}
        (om/build vote-form app)
        (dom/div #js {:className "choice-container"}
          (dom/h3 {:className "choice-title"} (:choice/title (:choice app)))
          (om/build vote-data app))))))

(defn topic-view [{:keys [topic
                          conn
                          params] :as app} owner]
  (reify
    om/IWillMount
    (will-mount [this]
      (let [{:keys [id]} (:params app)]
        (bind conn app :topic q/topic-by-id [id q/topic-p])))
    om/IWillUnmount
    (will-unmount [this]
      (unbind conn app :topic))
    om/IRender
    (render [this]
      (let [user   (:current-user app)
            {:keys [topic/title
                    topic/description
                    topic/description
                    topic/author
                    topic/choices]} topic]
        (dom/div nil
          (dom/h2 #js {:className "topic-title"} title)
          (dom/div #js {:className "topic-author"} (:user/name author))
          (dom/div #js {:className "topic-description"} description)
          (apply dom/ul nil
            (for [choice choices]
              (om/build choice-view {:topic  topic
                                     :choice choice
                                     :user   user}
                {:react-key (:db/id choice)}))))))))

(defn show-topic [app owner]
  (reify
    om/IRender
    (render [this]
      (om/build topic-view app))))

(defn register [app owner]
  (reify
    om/IRender
    (render [this]
      (om/build registration-form app))))

(defn login [app owner]
  (reify
    om/IRender
    (render [this]
      (om/build login-form app))))

  (defn new-topic [app owner]
    (reify
      om/IRender
      (render [this]
        (dom/div #js {:className "topics-new"}
          (dom/h2 #js {:className "topics-title"} "New Topic")
          (om/build topic-form app)))))

(defn not-found [app owner]
  (reify
    om/IRender
    (render [this]
      (dom/a #js {:href "/"} "nothing here..."))))

(defn page [app owner]
  (reify
    om/IRender
    (render [this]
      (if-let
          [comp (case (:current-page app)
                  :index       index
                  :dashboard   dashboard
                  :register    register
                  :login       login
                  :new-topic   new-topic
                  :show-topic  show-topic
                  :not-found   not-found
                  nil)]
        (om/build comp app)
        (dom/div nil)))))

(defn app [app owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className "container"}
        (om/build menu app)
        (dom/div #js {:className "content"}
          (om/build page app))))))
