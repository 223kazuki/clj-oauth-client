(ns clj-oauth-client.endpoint.site
  (:require [compojure.core :refer :all]
            [clojure.java.io :as io]
            [ring.util.response :refer [redirect]]
            [hiccup.page :refer [html5 include-css include-js]]
            [clj-http.client :as client]
            [clojure.data.json :as json]))

(defn index-page [config]
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:title "Index"]]
   [:body
    [:a {:href "http://localhost:3000/authorize?response_type=code&client_id=6P1kUE5eEY&state=xyz&redirect_uri=http%3A%2F%2Flocalhost%3A3001%2Fcb"} "OAuth 2.0 Login"]]))

(defn accounts-page [config]
  (fn [request]
    (if-let [access_token (get-in request [:session :access_token])]
      (let [response (client/get "http://localhost:3000/api/accounts"
                                 {:headers {"Authorization" (str "Bearer " access_token)}
                                  :throw-exceptions false})]
        (if (== 200 (:status response))
          (let [accounts (-> response :body (json/read-str :key-fn keyword))]
            (html5
             [:head
              [:meta {:charset "utf-8"}]
              [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
              (include-css "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css")
              (include-css "/css/main.css")
              [:title "Accounts"]]
             [:body
              [:div.container
               [:div.row.main {:style "padding-top: 100px;"}
                [:table.table
                 [:thead
                  [:tr
                   [:th "#"]
                   [:th "Name"]]]
                 [:tbody
                  (for [account accounts]
                    [:tr
                     [:th {:scope "row"} (:id account)]
                     [:td (:name account)]])]]
                [:br]
                [:a {:href "/logout"} "Logout"]]]
              (include-js "https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js")
              (include-js "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js")]))
          (redirect "/")))
      (redirect "/"))))

(defn logout []
  {:status 302
   :session nil
   :headers {"Location" "/"}})

(defn site-endpoint [config]
  (routes
   (GET "/" [] (index-page config))
   (GET "/cb" [] (redirect "/accounts"))
   (GET "/accounts" [] (accounts-page config))
   (GET "/logout" [] (logout))))
