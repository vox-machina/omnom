(ns app.core
  (:require [clojure.data.json :refer [read-str write-str]]
            [clojure.pprint :refer [code-dispatch write]]
            [clojure.string :refer [replace split]]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.zip :refer [end? next node zipper]]
            [aero.core :refer (read-config)]
            [io.pedestal.http :as http]
            [io.pedestal.http.body-params :refer [body-params]]
            [io.pedestal.log :refer [debug info error]]
            [ring.util.response :refer [response]]
            [org.httpkit.client :refer [post]]
            [org.httpkit.sni-client :as sni-client]
            [hiccup.page :refer [html5]]
            [java-time.api :refer [as duration format instant local-date]])
  (:import (java.io File)
           (java.util UUID)))

(alter-var-root #'org.httpkit.client/*default-client* (fn [_] sni-client/default-client))
(def cfg (read-config "config.edn" {}))
(def data-path "resources/public/data")
(def start-inst (instant))
(def htm-tors [(body-params) http/html-body])

(defn- uptime-by-unit [unit] (as (duration start-inst (instant)) unit))

(defn- pretty-spit
  [f-name xs]
  (spit (File. f-name) (with-out-str (write xs :dispatch code-dispatch))))

(defn- lookup-key [k coll]
  (let [coll-zip (zipper coll? #(if (map? %) (vals %) %) nil coll)]
    (loop [x coll-zip]
      (when-not (end? x)
        (if-let [v (-> x node k)] v (recur (next x)))))))

(defn- ep-scaffolding []
  (let [now (local-date)
        published (format "yyyy-MM-dd" now)
        date-pathfrag (replace published "-" "")
        uid (str (UUID/randomUUID))
        e-id (str (UUID/randomUUID))]
    {:inst (instant) :published published :uid uid :u-frag (first (split uid #"-")) :e-id e-id :e-frag (first (split e-id #"-")) :date-pathfrag date-pathfrag}))

(defn- github [req]
  (let [{:keys [inst published date-pathfrag uid u-frag e-id e-frag]} (ep-scaffolding)
        params (-> (:params req) keywordize-keys)
        payload (-> (:payload params) read-str keywordize-keys)
        repo (get-in payload [:repository :name])
        org (or (get-in payload [:organization :login]) "N/A")
        git-evt (cond
          (:issue payload) "issue"
          (:hook payload) "webhook"
          :else "unknown event")
        action (or (get-in payload [:action]) "unknown action")
        provider (or (get-in payload [:sender :html_url]) "unknown provider")]
    (pretty-spit (str data-path "/github/" date-pathfrag "-" u-frag ".edn")
        {:id uid :payload {:action action :org org :repo repo :provider provider :git-evt git-evt}})
    (pretty-spit (str data-path "/events/" date-pathfrag "-" e-frag ".edn")
        {:published (str inst) :eventId e-id :providerId {:id uid} :object (str org "/" repo) :predicate "transmits repository event data via webhook" :category "github"})
    {:status 200 :body "ok"}))

(defn- gps [req]
  (let [{:keys [inst published date-pathfrag e-id e-frag]} (ep-scaffolding)
        uid (str (UUID/randomUUID))
        u-frag (first (split uid #"-"))
        locations (get-in req [:json-params])
        id (lookup-key :device_id req)]
    (when (= id (:gps-device-id cfg))
      (pretty-spit (str data-path "/gps/" date-pathfrag "-" u-frag ".edn") {:id uid :payload locations})
      (pretty-spit (str data-path "/events/" date-pathfrag "-" e-frag ".edn")
        {:published (str inst) :eventId e-id :providerId {:id uid} :object (:gps-device-id cfg) :predicate "transmits GPS data" :category "gps"})
      {:status 200 :body (write-str {:result "ok"}) :headers {"Content-Type" "application/json"}})))

(defn- steps [req]
  (let [{:keys [inst published date-pathfrag e-id e-frag]} (ep-scaffolding)
        uid (str (UUID/randomUUID))
        u-frag (first (split uid #"-"))
        steps (get-in req [:query-params :count])]
    (when (= 1 1); TODO: secure with param in request
      (pretty-spit (str data-path "/steps/" date-pathfrag "-" u-frag ".edn") {:id uid :payload steps})
      (pretty-spit (str data-path "/events/" date-pathfrag "-" e-frag ".edn")
        {:published (str inst) :eventId e-id :providerId {:id uid} :object (:steps-device-id cfg) :predicate "transmits steps data" :category "steps"})
      (let [uri "https://alerty.dev/api/notify"
            options {:headers {"Authorization" (str "Bearer " (:alerty-api-key cfg))}
                     :body (write-str {:title "Riker Daily Summary" :message (str "See summary at " (:site-root cfg) "daily-summaries/" date-pathfrag "-" e-frag)})}]
              @(post uri options))
      {:status 200 :body (write-str {:result "ok"}) :headers {"Content-Type" "application/json"}})))

(defn head []
  [:html [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
   [:link {:rel "microsub" :href (:microsub-uri cfg)}]
   [:link {:rel "stylesheet" :type "text/css" :href "/css/bootstrap.min.css"}]
   [:link {:rel "stylesheet" :type "text/css" :href "https://cdn.jsdelivr.net/npm/bootstrap-icons@1.5.0/font/bootstrap-icons.css"}]
   [:link {:rel "stylesheet" :type "text/css" :href "/css/style.css"}]
   [:title "Omnom"]]])

(defn navbar [user]
  [:nav {:class "navbar navbar-expand-md navbar-light fixed-top bg-light"}
   [:div {:class "container-fluid"}
    [:a {:class "navbar-brand" :href "/"} "Omnom Dashboard"]
    [:button {:class "navbar-toggler" :type "button" :data-toggle "collapse" :data-target "#navbarCollapse" :aria-controls "navbarCollapse" :aria-expanded "false" :aria-label "Toggle navigation"}
     [:span {:class "navbar-toggler-icon"}]]
    [:div {:class "collapse navbar-collapse" :id "navbarCollapse"}]]])

(defn body
  [user & content]
  [:body
    (navbar user)
    [:div {:class "container-fluid"}
    [:div {:class "row"}
      content]]
    [:div {:class "container-fluid"}
    [:footer
      [:p
      [:small "Omnom v" (get-in cfg [:version :omnom]) ", uptime " (uptime-by-unit :days) " days"]]]]
    [:script {:src "//code.jquery.com/jquery.js"}]
    [:script {:src "/js/bootstrap.min.js"}]])

(defn home [req]
(response
 (html5
  (head)
  (body nil
   [:div {:class "col-lg-9" :role "main"}
    [:div {:class "card"}
     [:div {:class "card-header"} [:h2 "Omnom"]]
     [:img {:src "https://media.giphy.com/media/jgUG5cnss7T9K/giphy.gif"}]
     [:div {:class "card-body"}
      [:h5 {:class "card-title"} "About Omnom"]
      [:p "Omnom collects content for you - any content you like. E.G:"]
      [:ul
       [:li "GPS data from Apps"]
       [:li "Biometrics data from devices"]]]]]))))

(def routes
  #{["/"       :get  (conj htm-tors `home)]
    ["/github" :post [(body-params) `github]]
    ["/gps"    :post [(body-params) `gps]]
    ["/steps"  :get  [(body-params) `steps]]})

(def service-map {
    ::http/secure-headers    {:content-security-policy-settings {:object-src "none"}}
    ::http/routes            routes
    ::http/type              :jetty
    ::http/resource-path     "public"
    ::http/host              "0.0.0.0"
    ::http/port              (Integer. (or (:port cfg) 5001))
    ::http/container-options {:h2c? true :h2?  false :ssl? false}})

(defn create-server []
  (-> service-map
      (http/default-interceptors)
      http/create-server))

(defn -main [_]
  (info :omnom/main (str "starting omnom v" (get-in cfg [:version :omnom])))
  (http/start (create-server)))