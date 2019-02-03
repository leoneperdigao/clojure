(ns tweedler.core
  (:require [net.cgrand.enlive-html :as enlive]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :refer [resources]]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.params :refer [wrap-params]]
            [markdown.core :refer [md-to-html-string]]
            [clojure.string :refer [escape]]))

(defrecord Tweed [title content])

(defprotocol TweedStore
    (get-tweeds [store])
    (put-tweed! [store tweed]))

(defrecord AtomStore [data])

(extend-protocol TweedStore
    AtomStore
    (get-tweeds [store]
        (get @(:data store) :tweeds))
    (put-tweed! [store tweed]
        (swap! (:data store)
               update-in [:tweeds] conj tweed)))

(def store (->AtomStore (atom {:tweeds '()})))
(get-tweeds store)
(put-tweed! store (->Tweed "Test Title 2" "Test Content"))

(enlive/defsnippet tweed-tpl "tweedler/index.html" [[:article.tweed enlive/first-of-type]]
  [tweed]
  [:.title] (enlive/html-content (:title tweed))
  [:.content] (enlive/html-content  (md-to-html-string (:content tweed))))

(enlive/deftemplate index-tpl "tweedler/index.html"
  [tweeds]
  [:section.tweeds] (enlive/content (map tweed-tpl tweeds))
  [:form] (enlive/set-attr :method "post" :action "/"))

(defn escape-html [s]
  (escape s {\> "&gt;" \< "&lt;"}))

(defn handle-create [{{title "title" content "content"}:params}]
  (put-tweed! store (->Tweed (escape-html title) (escape-html content)))
  {:body "" :status 302 :headers {"Location" "/"}})

(defroutes app-routes
  (GET "/" [] (index-tpl (get-tweeds store)))
  (POST "/" req (handle-create req))
  (resources "/css" {:root "tweedler/css"})
  (resources "/img" {:root "tweedler/img"}))

(def app (-> app-routes
             (wrap-params)))

(def server (jetty/run-jetty (var app) {:port 3000 :join? false}))



