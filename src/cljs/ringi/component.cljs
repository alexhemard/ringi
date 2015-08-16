(ns ringi.component
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [datascript   :as d]
            [clojure.string :refer [blank?]]
            [ringi.db     :as db :refer [unbind bind conn]]
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
        (dom/input #js {:className "login-button" :type "submit" :value "Submit"})))))

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
        (dom/input #js {:className "login-button" :type "submit" :value "Submit"})))))

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
          (dom/a #js {:href "/t/new"} "+"))))))

(defn menu [app owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className "menu"}
        (dom/h1 #js {:className "menu-title"}
          (dom/a #js {:href "/"} "Ringi"))
        (om/build menu-list app)))))

(defn topic-item [topic owner]
  (reify
    om/IRender
    (render [this]
      (let [{:keys [topic/title
                    topic/id
                    topic/author]} topic]
        (dom/li #js {:className "topics-list-item"}
          (dom/a #js {:className "topics-title" :href (str "/t/" id)} title)
          (dom/div #js {:className "topics-author"} (:user/name author)))))))

(defn dashboard [{:keys [topics
                         conn
                         current-user] :as app} owner]
  (reify
    om/IWillMount
    (will-mount [this]
      (let [current-user (:id current-user)]
        (bind conn app :topics q/topics-by-author [:user/id current-user])))
    om/IWillUnmount
    (will-unmount [this]
      (unbind conn app :topics))
    om/IRender
    (render [this]
      (dom/div nil
        (dom/h2 nil "My Topics")
        (apply dom/ul #js {:className "topics-choices"}
          (om/build-all topic-item topics {:key :db/id}))))))

(defn show-topic [{:keys [topic
                          conn
                          params] :as app} owner]
  (reify
    om/IWillMount
    (will-mount [this]
      (let [{:keys [id]} (:params app)]
        (bind conn app :topic q/topic-by-id id)))
    om/IWillUnmount
    (will-unmount [this]
      (unbind conn app :topic))
    om/IRender
    (render [this]
      (let [{:keys [topic/title
                    topic/author
                    topic/choices]} (first topic)]
        (dom/a #js {:href "/pizza"} title)))))

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
      (dom/a #js {:href "/pizza"} "new-topic"))))

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
                  :index      (if (:current-user app) dashboard index)
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

(comment
  (defn vote-data [votes]
    (let [votes (group-by :vote/value votes)]
      [:ul {:class "votes"}
       (for [option ["yes" "ok" "no"]
             vote   (get votes option)
             :let [voter (get-in vote [:vote/author :user/name])]]
         ^{:key voter} [:li {:class (str "vote " option)}])])))

(comment
  (defn vote-form [{choice-eid :db/id
                    choice-id  :choice/id}]
    (let [my-vote (atom nil)]
      (r/create-class
       {:component-will-mount
        (fn []
          (bind conn my-vote q/vote-by-author [:choice/id choice-id] [:user/id (:id (current-user))]))
        :component-will-unmount
        (fn [] (unbind conn my-vote))
        :render
        (fn [choice]
          (let [my-vote (:vote/value (first @my-vote))]
            [:form {:class "vote-form"}
             (for [value ["yes" "no" "ok"]
                   :let [name (str choice-id "-" value)]]
               ^{:key name} [:div {:class (str "vote-form-" value)}
                             [:input {:value    value
                                      :name     name
                                      :id       name
                                      :type     :radio
                                      :on-change (fn [e])
                                      :checked  (= my-vote value)}]
                             [:label {:for name} value]])]))}))))

(comment
  (defn choice [{:keys [:db/id
                        :choice/id
                        :choice/title
                        :choice/author
                        :votes] :as choice}]
    [:li {:class "choice"}
     [vote-form choice]
     [:div {:class "choice-container"}
      [:h3 {:class "choice-title"} title]
      [vote-data votes]]]))

(comment
  (defn topic-author [{:keys [:user/name]}]
    [:div {:class "topic-author"} name]))

(comment
  (defn topic-view [{:keys [:topic/id
                            :topic/title
                            :topic/timestamp
                            :topic/description
                            :topic/author
                            :topic/choices
                            :topic/comments] :as topic}]
    [:div {:class "topic"}
     [:h2 {:class "topic-title"} title]
     [topic-author author]
     [:div {:class "topic-description"} description]
     [:ul {:class "topic-choices"}
      (for [c choices]
        ^{:key (:db/id c)} [choice c])]]))

(comment
  (defn topic-show [id]
    (let [topic (atom nil)]
      (r/create-class
       {:component-will-mount
        (fn []
          (bind conn topic q/topic-by-id id))
        :component-will-unmount
        (fn []
          (unbind conn topic))
        :render
        (fn [id]
          (let [t (first @topic)]
            [topic-view t]))}))))

(comment
  (defn topics-list-item
    [{:keys [topic/id
             topic/title
             topic/author]}]
    [:li {:class "topics-list-item"}
     [:a {:href (str "/t/" id) :class "topics-title"} title]
     [:div {:class "topics-author"}  (:user/name author)]]))

(comment
  (defn topics []
    (let [topics (atom nil)]
      (r/create-class
       {:component-will-mount
        (fn [] (when (current-user))
          (bind conn topics q/topics-by-author [:user/id (:id (current-user))]))
        :component-will-unmount
        (fn [] (unbind conn topics))
        :render
        (fn []
          [:div
           [:h2 "My Topics"]
           [:ul {:class "topics-list"}
            (for [t @topics]
              ^{:key (:db/id t)} [topics-list-item t])]])}))))

(comment
  (defn create-topic [topic]
    (let [t @topic
          t (update-in t [:choices] (fn [c] (remove #(nil? (:title %)) (vals c))))]
      (s/call-server :create-topic t)
      (reset! topic nil))))

(comment
  (defn topics-form []
    (let [topic   (atom nil)
          choices (atom 3)]
      (fn []
        [:form {:class "topics-form"
                :on-submit (fn [e]
                             (.preventDefault e)
                             (create-topic topic))}
         [:input {:type :text
                  :name :title
                  :placeholder "Title"
                  :on-change #(swap! topic assoc :title (-> % .-target .-value))} ]
         [:textarea {:name :description
                     :placeholder "Description..."
                     :on-change #(swap! topic assoc :description (-> % .-target .-value))}]
         [:h3 "Choices"]
         (for [c (range @choices)
               :let [id (str "choice-" c)]]
           ^{:key c} [:input {:type :text
                              :name id
                              :placeholder "Add choice..."
                              :on-change #(swap! topic assoc-in [:choices c :title] (-> % .-target .-value))} ])
         (if (> 5 @choices)
           [:div {:on-click #(swap! choices inc)} "add choice"])
         [:input {:type :submit}]]))))

(comment
  (defn topics-new []
    [:div
     [:h2 "New Topic"]
     [topics-form]]))
