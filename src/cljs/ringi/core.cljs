(ns ringi.core
    (:require-macros [cljs.core.async.macros :refer [go alt!]])
    (:require [goog.events :as events]
              [cljs.core.async :refer [put! <! >! chan timeout]]
              [om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]
              [cljs-http.client :as http]
              [ringi.utils :refer [guid]]))

(enable-console-print!)

(def ESCAPE_KEY 27)
(def ENTER_KEY 13)

(def app-data
  (atom {
         :vote {:id (guid)
          :title "are you worried heroku outages will take VoteIt down?"
          :created "tbd"
          :user "alexhemard"
          :choices [{:id (guid)
                     :title "yes. I am very worried."
                     :votes [{:id (guid) :user "vaxinate"    :value 2}
                             {:id (guid) :user "mculp"       :value 1}
                             {:id (guid) :user "jwheeler"    :value 1}
                             {:id (guid) :user "joeellis"    :value 0}
                             {:id (guid) :user "feedjoelpie" :value 2}
                             {:id (guid) :user "coryf"       :value 2}
                             {:id (guid) :user "wedgex"      :value 2}
                             {:id (guid) :user-id 99999 :user "alexhemard"  :value 0}]
                     :comments [{:id (guid)
                                 :user "alexhemard"
                                 :text "I can't believe vote it is still holding on. god bless."}]},
                    {:id    (guid)
                     :title "i'm not very concerned."
                     :votes [{:id (guid) :user-id 99999 :user "alexhemard"  :value 1}
                             {:id (guid) :user "vaxinate"    :value 2}
                             {:id (guid) :user "mculp"       :value 1}
                             {:id (guid) :user "jwheeler"    :value 1}
                             {:id (guid) :user "feedjoelpie" :value 0}]
                     :comments [{:id (guid)
                                 :user "feedjoelpie"
                                 :text "How were we so wrong on this issue."}]}]
          :comments [{:id (guid)
                      :user "jwheeler"
                      :text "thanks obama"}]}}))

(def shared-data
  {:modal-chan (chan)})

(defn- with-id
  [m]
  (assoc m :id (guid)))

(defn- value-from-node
  [component field]
  (let [n (om/get-node component field)
        v (-> n .-value clojure.string/trim)]
    (when-not (empty? v)
      [v n])))

(defn- clear-nodes!
  [& nodes]
  (doall (map #(set! (.-value %) "") nodes)))

(defn require-login! [owner opts]
  (let [current-user (:current-user (om/get-state owner))
        modal (:modal-chan (om/get-shared owner))]
    (when (not current-user)
      (put! modal [:login opts]))))

(defn comment-form [app owner opts]
  (om/component
   (dom/form
    #js {:className "comment-form"}
    (dom/input #js {:type "text" :placeholder "Your Name here..." :ref "user"})
    (dom/input #js {:type "text" :placeholder "Your message here." :ref "text"})
    (dom/input #js {:type "submit" :value "Post"}))))

(defn parse-comment [value user]
  {:id   (guid)
   :user user
   :text value})

(defn add-comment [app owner]
  (require-login! owner)
  (let [new-comment (-> (om/get-node owner "new-comment")
                        .-value
                        (parse-comment (-> (om/get-state owner) :current-user :name)))]
    (clear-nodes! (om/get-node owner "new-comment"))
    (when new-comment
      (om/transact! app :comments conj new-comment))))

(defn comment-view [comment owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [delete]}]
      (dom/li
       #js {:className "comment"}
       (dom/div #js {:className "comment-user"} (:user comment))
       (dom/div #js {:className "comment-text"} (:text comment)
       (dom/button #js {:onClick (fn [e] (put! delete @comment))} "x"))))))

(defn comments-view [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:delete (chan)})
    om/IWillMount
    (will-mount [_]
      (let [delete (om/get-state owner :delete)]
        (go (loop []
          (let [comment (<! delete)]
            (om/transact! app :comments
              (fn [xs] (into [] (remove #(= comment %) xs))))
            (recur))))))
    om/IRenderState
    (render-state [_ {:keys [delete current-user] }]
      (dom/div
       #js {:className "comments"}
       (dom/h4 nil "comments")
       (apply dom/ul nil
              (om/build-all comment-view (:comments app) {:init-state {:delete delete}
                                                          :state      {:current-user current-user}
                                                          :key        :id}))
       (dom/div
        nil
        (dom/input #js {:type "text" :ref "new-comment"})
        (dom/button #js {:onClick #(add-comment app owner)} "Add"))))))

(defn radio-with-label [vote owner {:keys [value text] :as opts}]
  (reify
    om/IRenderState
    (render-state [_ {:keys [change]}]
      (let [name (str "choice-" (:id vote))
            html-id (str name "-" value)]
        (dom/div
         nil
         (dom/input #js {:id html-id
                         :name name
                         :value value
                         :type "radio"
                         :checked (= value (:value vote))} )
         (dom/label #js {:htmlFor html-id} text))))))

(defn choice-form [app owner opts]
  (reify
    om/IRenderState
    (render-state [_ {:keys [change]}]
      (let [user-id (-> (om/get-shared owner) :user :id)
            my-vote (first (filter #(= (-> % :user-id) user-id) (:votes app)))]
        (dom/form
         #js {:className "choice-form"}
         (om/build radio-with-label app {:opts {:value 0 :text "Yes"}})
         (om/build radio-with-label app {:opts {:value 1 :text "Ok"}})
         (om/build radio-with-label app {:opts {:value 2 :text "No"}}))))))

(defn choice-tally [vote owner opts]
  (reify
    om/IRender
    (render [this]
      (dom/div
       #js {:className (clojure.string/join " " ["choice-tally" (str "v" (:value vote))])}
       (dom/div #js {:className "choice-tally-info"} (:user vote))))))

(defn choice-data [choice owner opts]
  (reify
    om/IRender
    (render [_]
      (dom/div
       #js {:className "choice-data"}
       (om/build-all choice-tally (sort-by :value (:votes choice)) {})))))

(defn choice-view [choice owner opts]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/li
       #js {:className "choice"}
       (om/build choice-form choice {})
       (dom/div #js {:className "choice-title"} (:title choice))
       (om/build choice-data choice {})
       (om/build comments-view choice {:state state})))))

(defn choices-view [app owner opts]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/div
       #js {:className "choices-box"}
       (apply
        dom/ul {:className "choices"}
        (om/build-all choice-view app {:key :id :state state}))))))

(defn login [app owner]
  (let [user  (-> (om/get-node owner "name") .-value)]
    (om/update! app assoc-in [:user] {:id (guid)
                                      :name user})
    (put! (:modal-chan (om/get-shared owner)) :none)))

(defn login-modal [app owner opts]
  (reify
    om/IRender
    (render [this]
      (dom/div
       #js {:className "login-modal"}
       (dom/h2 nil "Login")
       (dom/form
        #js {:onSubmit (fn [e]
                         (.preventDefault e)
                         (login app owner))}
        (dom/div
         #js {:className "login-field"}
         (dom/label #js {:htmlFor "login-username"} "username")
         (dom/input #js {:type "text" :id "login-username" :name "username" :ref "name"}))
        (dom/div
         #js {:className "login-field"}
         (dom/label #js {:htmlFor "login-password"} "password")
         (dom/input #js {:type "password" :id "login-password" :disable true :name "password"}))
        (dom/input #js {:type "submit" 
                        :value "Login" 
                        :ref "submit"}))))))

(defn vote [vote owner opts]
  (reify
    om/IRenderState
    (render-state [this state]
      (dom/div
       #js {:className "vote"}
       (dom/h2 #js {:className "vote-title"} (:title vote))
       (dom/div #js {:className "vote-user"} (str "by " (:user vote)))
       (om/build choices-view (:choices vote) {:key :id
                                               :state state})
       (om/build comments-view vote {:state state})))))

(defn login-button [app owner] 
  (om/component
   (let [user (:current-user (om/get-state owner))]
     (if user
       (dom/button #js {:onClick #(om/update! app assoc-in [:user] nil)} "logout")
       (dom/button #js {:onClick #(require-login! owner)} "login")))))

(defn ringi-app [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [modal (:modal-chan (om/get-shared owner))]
        (go
         (loop []
           (let [current-modal (<! modal)]
             (om/set-state! owner :modal current-modal)
             (recur))))))
    om/IRenderState
    (render-state [this {:keys [modal current-user] :as state}]
      (dom/div
       #js {:className "ringi"}
       (condp = modal
         :login (om/build login-modal app)
         :none nil)
       (when (not (= modal :none)) (dom/div #js {:className "modal-background"}))
       (dom/h1 nil "ringi")
       (om/build login-button app {:state state})
       (when current-user
         (dom/div 
          #js {:className "current-user"}
          (str "currently logged in as: " (:name current-user))))
       (om/build vote (:vote app) {:state {:current-user current-user}})))))

(om/root 
 app-data
 shared-data 
 (fn [app owner]
   (om/component 
    (om/build ringi-app app {:state      {:current-user (:user @app)}
                             :init-state {:modal :none}})))
 (. js/document (getElementById "content")))