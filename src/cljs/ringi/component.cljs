(ns ringi.component
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [datascript   :as d]
            [clojure.string :refer [blank?]]
            [ringi.db     :as db :refer [unbind bind conn]]
            [ringi.service :as s]
            [ringi.query  :as q]))

(defn index [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/a #js {:href "/pizza"} "pizza"))))

(comment
  (defn login-form []
    [:form {:class "login" :method :POST :action "/login" }
     [:span {:class "login-title"} "Log in"]
     [:label {:for "username"} "Username"]
     [:input {:type :text :name :username :id :username :placeholder "username"} ]
     [:label {:for "password"} "Password"]
     [:input {:type :password :name :password :id :password :placeholder "password123"}]
     [:input {:class "login-button" :type :submit :value "Submit"}]]))


(comment
  (defn registration-form []
    [:form {:class "register" :method :POST :action "/register" }
     [:span {:class "register-title"} "Sign Up"]
     [:label {:for "username"} "Username"]
     [:input {:type :text :name :username :id :username :placeholder "username"} ]
     [:label {:for "email"} "Email"]
     [:input {:type :email :name :email :id :email :placeholder "example@coolguys.pro"} ]   
     [:label {:for "password"} "Password"]
     [:input {:type :password :name :password :id :password :placeholder "password123"}]
     [:input {:class "register-button" :type :submit :value "Submit"}]]))

(comment
  (defn logged-in-menu-list []
    [:ul {:class "menu-list"}
     [:li {:class "menu-list-item"} [:a {:href "#"} (str "@" (:name (current-user)))]]
     [:li {:class "menu-list-item"} [:a {:href "/logout"} "logout"]]
     [:li {:class "menu-list-item"} [:a {:href "/t/new"} "new"]]]))

(comment
  (defn menu-list []
    [:ul {:class "menu-list"}
     [:li {:class "menu-list-item"} [:a {:href "/register"} "sign up"]]
     [:li {:class "menu-list-item"} [:a {:href "/login"} "login"]]
     [:li {:class "menu-list-item"} [:a {:href "/t/new"} "new"]]]))

(comment
  (defn menu-bar []
    [:div {:class "menu"}
     [:h1 {:class "menu-title"} [:a {:href "/"} "Ringi"]]
     (if (current-user)
       [logged-in-menu-list]
       [menu-list])]))

(comment
  (defn page []
    (let [this (r/current-component)]
      [:div {:class "container"}
       [menu-bar]
       (into
        [:div {:class "content"}]
        (r/children this))])))

(comment
  (defn register []
    [registration-form])

  (defn login []
    [login-form])

  (defn home []
    [:h2 "welcome"])

  (defn vote []
    [:li]))

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
                                      :on-change (fn [e]
                                                   (s/call-server
                                                    :vote {:choice-id choice-id
                                                           :vote value}))
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
          (bind conn topic q/topic-by-id id)
          (s/call-server :fetch-topic {:topic-id id}))
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
        (fn [] (when (current-user)
                 (s/call-server :fetch-topics))
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
