(ns ringi.core
    (:require-macros [cljs.core.async.macros :refer [go alt!]])
    (:require [goog.events :as events]
              [cljs.core.async :refer [put! <! >! chan timeout]]
              [om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]
              [cljs-http.client :as http]
              [ringi.utils :refer [guid]]))

;; Lets you do (prn "stuff") to the console
(enable-console-print!)

(def app-state
  (atom {:vote
         {:id (guid)
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

(defn shared-state []
  {:current-user {:id 99999
                   :user "alexhemard"}})

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
  (let [new-comment (-> (om/get-node owner "new-comment")
                        .-value
                        (parse-comment (-> (om/get-shared owner) :current-user :user) ))]
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
      {:delete (chan)
       :create (chan)})
    om/IWillMount
    (will-mount [_]
      (let [delete (om/get-state owner :delete)]
        (go (loop []
          (let [comment (<! delete)]
            (om/transact! app :comments
              (fn [xs] (into [] (remove #(= comment %) xs))))
            (recur))))))
    om/IRenderState
    (render-state [this {:keys [delete]}]
      (dom/div
       #js {:className "comments"}
       (dom/h4 nil "comments")
       (apply dom/ul nil
              (om/build-all comment-view
                            (:comments app)
                            {:init-state {:delete delete}
                             :key        :id}))
       (dom/div
        nil
        (dom/input #js {:type "text" :ref "new-comment"})
        (dom/button #js {:onClick #(add-comment app owner)} "Add"))))))

(defn radio-with-label [vote owner {:keys [value text] :as opts}]
  (reify
    om/IRenderState
    (render-state [this {:keys [change]}]
      (let [name (str "choice-" (:id vote))
            html-id (str name "-" value)]
        (dom/div
         nil
         (dom/input #js {:id html-id
                         :name name
                         :value value
                         :type "radio"
                         :checked (= value (:value vote))
                         :onChange (fn [e] (om/update! vote assoc-in [:value] value))} )
         (dom/label #js {:htmlFor html-id} text))))))

(defn choice-form [choice owner opts]
  (reify
    om/IRenderState
    (render-state [this {:keys [change]}]
      (let [user-id (-> (om/get-shared owner) :current-user :id)
            my-vote (first (filter #(= (-> % :user-id) user-id) (:votes choice)))]
        (dom/form
         #js {:className "choice-form"}
         (om/build radio-with-label my-vote {:opts {:value 0 :text "Yes"}})
         (om/build radio-with-label my-vote {:opts {:value 1 :text "Ok"}})
         (om/build radio-with-label my-vote {:opts {:value 2 :text "No"}}))))))

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
    (render [this]
      (dom/div
       #js {:className "choice-data"}
       (om/build-all choice-tally (sort-by :value (:votes choice)) {})))))

(defn choice-view [choice owner opts]
  (reify
    om/IRender
    (render [this]
      (dom/li
       #js {:className "choice"}
       (om/build choice-form choice {})
       (dom/div #js {:className "choice-title"} (:title choice))
       (om/build choice-data choice {})
       (om/build comments-view choice {})))))

(defn choices-view [app owner opts]
  (om/component
   (dom/div
    #js {:className "choices-box"}
    (apply
     dom/ul {:className "choices"}
     (om/build-all choice-view app {:key :id})))))


(defn poop [vote owner opts]
  (reify
    om/IRender
    (render [this]
      (dom/div
       #js {:className "vote"} "poop"))))

(defn vote [vote owner opts]
  (reify
    om/IRender
    (render [this]
      (dom/div
       #js {:className "vote"}
       (dom/h2 #js {:className "vote-title"} (:title vote))
       (dom/div #js {:className "vote-user"} (str "by " (:user vote)))
       (om/build choices-view (:choices vote) {:key :id})
       (om/build comments-view vote)
       ))))


(defn ringi-app [app owner]
  (reify
    om/IRender
    (render [this]
      (dom/div
       #js {:className "ringi"}
       (dom/h1 nil "ringi")
       (om/build vote (:vote app) {})))))

(om/root app-state (shared-state) ringi-app (. js/document (getElementById "content")))
