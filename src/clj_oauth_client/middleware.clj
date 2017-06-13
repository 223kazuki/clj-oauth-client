(ns clj-oauth-client.middleware
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]))

(defn get-access-token [code]
    (let [uri (format "%s?grant_type=%s&client_id=%s&code=%s&redirect_uri=%s"
                      "http://localhost:3000/token"
                      "authorization_code"
                      "6P1kUE5eEY"
                      code
                      "http%3A%2F%2Flocalhost%3A3001%2Fcb")
          res (client/post uri {:throw-exceptions false})]
      (when (== 200 (:status res))
        (let [{:keys [access_token token_type expires_in refresh_token] :as body}
              (-> res :body (json/read-str :key-fn keyword))]
          access_token))))

(defn- check-access-token [access-token]
  (let [res (client/get (format "http://localhost:3000/introspect?token=%s&token_hint=%s"
                                access-token "hint")
                        {:headers          {"Content-type" "application/x-www-form-urlencoded"}
                         :throw-exceptions false})]
    (when (== 200 (:status res))
      (let [{:keys [active client_id username scope sub aud iss exp iat] :as res}
            (json/read-str (:body res) :key-fn keyword)]
        active))))

(defn wrap-authorization
  [handler]
  (fn [request]
    (let [{:keys [code]} (:params request)
          {:keys [access_token]} (:session request)]
      (if code
        (if-let [access_token (get-access-token code)]
          (-> request
              (assoc :session {:access_token access_token})
              handler
              (assoc :session {:access_token access_token}))
          ;; TODO: Create authorization error page.
          {:status 401 :body "Invalid authorization code."})
        (if access_token
          (if (check-access-token access_token)
            (handler request)
            ;; TODO: Refresh token
            {:status 401
             :body "Invalid access token."})
          (handler request))))))
