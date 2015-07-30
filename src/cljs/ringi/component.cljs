(ns ringi.component
  (:require [reagent.core :as r :refer [atom]]
            [datascript   :as d]
            [ringi.db     :as db :refer [unbind bind conn]]
            [ringi.query  :as q]))


(defn login-form []
  [:form {:class "login" :method :POST :action "/login" }
   [:span {:class "login-title"} "Log in"]
   [:label {:for "username"} "Username"]
   [:input {:type :text :size 20 :name :username :id :username :placeholder "username"} ]
   [:label {:for "password"} "Password"]
   [:input {:type :password :size 20 :name :password :id :password :placeholder "password123"}]
   [:input {:class "login-button" :type :submit :value "Submit"}]])

(defn registration-form []
  [:form {:class "register" :method :POST :action "/register" }
   [:span {:class "register-title"} "Sign Up"]
   [:label {:for "username"} "Username"]
   [:input {:type :text :size 20 :name :username :id :username :placeholder "username"} ]
   [:label {:for "email"} "Email"]
   [:input {:type :email :size 20 :name :email :id :email :placeholder "example@coolguys.pro"} ]   
   [:label {:for "password"} "Password"]
   [:input {:type :password :size 20 :name :password :id :password :placeholder "password123"}]
   [:input {:class "register-button" :type :submit :value "Submit"}]])

(defn menu-bar []
  [:div {:class "menu"}
   [:h1 {:class "menu-title"} [:a {:href "/"} "Ringi"]]
   [:ul {:class "menu-list"}
    [:li {:class "menu-list-item"} [:a {:href "/register"} "sign up"]]
    [:li {:class "menu-list-item"} [:a {:href "/login"} "login"]]
    [:li {:class "menu-list-item"} [:a {:href "/t/new"} "new"]]]])

(defn page []
  (let [this (r/current-component)]
    [:div {:class "container"}
     [menu-bar]
     (into
      [:div {:class "content"}]
      (r/children this))]))

(defn register []
  [registration-form])

(defn login []
  [login-form])

(defn home []
  [:h2 "welcome"])

(defn vote []
  [:li]

)

(defn vote-data [votes]
  (let [votes (group-by :vote/value votes)]
    [:ul {:class "votes"}
     (for [option [:yes :ok :no]
           vote   (get votes option)
           :let [voter (get-in vote [:vote/author :user/name])]]
       ^{:key voter} [:li {:class (str "vote " (name option))}])]))

(defn vote-form [{db-id :db/id
                  id :choice/id}]
  [:form {:class "vote-form"}
   (for [value ["yes" "no" "ok"]
         :let [name (str db-id "-" value)]]
     ^{:key name} [:div {:class (str "vote-form-" value)}
                   [:input {:value value :name name :id name :type :radio}]
                   [:label {:for name} value]])])

(defn choice [{:keys [:db/id
                      :choice/id
                      :choice/title
                      :choice/author
                      :choice/votes] :as choice}]
  [:li {:class "choice"}
   [vote-form choice]
   [:div {:class "choice-container"}
    [:h3 {:class "choice-title"} title]
    [vote-data votes]]])

(defn topic-author [{:keys [:user/name]}]
  [:div {:class "topic-author"} name])

(defn topic-view [{:keys [:topic/id
                          :topic/title
                          :topic/timestamp
                          :topic/description
                          :topic/author
                          :topic/choices
                          :topic/comments]}]
  [:div {:class "topic"}
   [:h2 {:class "topic-title"} title]
   [topic-author author]
   [:div {:class "topic-description"} description]
   [:ul {:class "topic-choices"}
    (for [c choices]
      ^{:key (:db/id c)} [choice c])]])

(defn topic-show [id]
  (let [topic (bind conn q/topic-by-id id)]
    (fn []
      (let [t (d/pull @conn q/topic-p (ffirst @topic))]
        [topic-view t]))))

(defn topics-list-item
  [{:keys [topic/id
           topic/title
           topic/author]}]
  [:li {:class "topics-list-item"}
   [:a {:href (str "/t/" id) :class "topics-title"} title]
   [:div {:class "topics-author"}  (:user/name author)]])

(defn topics []
  (let [topics (bind conn q/topics)]
    (fn []
      [:h2 "Topics"]
      [:ul {:class "topics-list"}
       (for [[e t] @topics]
         ^{:key e} [topics-list-item t])])))

(defn topics-new []
  [:div "New Topic"])
