(ns ^:figwheel-hooks learn-cljs.weather ;; project folder name with hyphen and filename as namespace
  (:require
   [goog.dom :as gdom]  ;; for dom manipulation using underlying Google's clouSure compiler
   [reagent.dom :as rdom] ;; Reagent is clojure wrapper over React
   [reagent.core :as r]
   [ajax.core :as ajax])) ;; To make AJAX request to Openweather

;; Entire App state\values is kept in an Atom - a thread safe Clojure construct - also helps in keeping state across browser transitions
(defonce app-state (r/atom {:title "WhichWeather"
                            :postal-code ""
                            :temperatures {:today {:label "Today"
                                                   :value nil}
                                           :tomorrow {:label "Tomorrow"
                                                      :value nil}
                                           :weather {:label "Weather" :value nil}}})) ;; Added entry for weather in the app-state atom

(def api-key "<Your API key>") ;; Replace your API key

(defn handle-response [resp]
  (let [today (get-in resp ["list" 0 "main" "temp"])  ;; get-in searches the resp-onse based on nesting provided ["list" 0.. etc]
        tomorrow (get-in resp ["list" 8 "main" "temp"])
        weather (get-in resp ["list" 8 "weather" 0 "description"])] ;; added for description - the numbers are json array indexes
    (swap! app-state
           update-in [:temperatures :today :value] (constantly today))
    (swap! app-state
           update-in [:temperatures :tomorrow :value] (constantly tomorrow))
    (swap! app-state
           update-in [:temperatures :weather :value] (constantly weather)))) ;; Swap Atom's  with weather description added

;; Make the API call with search term and api-key and map our above handler function
(defn get-forecast! []
  (let [postal-code (:postal-code @app-state)]
    (ajax/GET "http://api.openweathermap.org/data/2.5/forecast"
      {:params {"q" postal-code
                "units" "metric" ;; alternatively, use "imperial"
                "appid" api-key}
       :handler handle-response})))

(defn title []
  [:h1 (:title @app-state)]) ;; generate HTMl elements using hiccup library syntax , get the required title value from App state atom
;; fucntion which helps in displaying the min max temp. and weather description as common template
(defn temperature [temp]
  [:div {:class "temperature"}
   [:div {:class "value"}
    (:value temp)]
   [:h2 (:label temp)]])

(defn postal-code []
  [:div {:class "postal-code"}
   [:h2 "Enter your postal code"]
   [:input {:type "text"
            :placeholder "Postal Code"
            :value (:postal-code @app-state)
            :on-change #(swap! app-state assoc :postal-code (-> % .-target .-value))}] ;; Trigger on change in postal-code field and update new postal code into app-state atom
   [:button {:on-click get-forecast!} "Go"]]) ;; Call API with postal-code [search term] above

;; Main app element generation
(defn app []
  [:div {:class "app"} ;; main app div
   [title] ;; add title
   [:div {:class "temperatures"}
    (for [temp (vals (:temperatures @app-state))] ;; Create the 3 display - min ,max and description by looping over app-state atom
      [temperature temp])]
   [postal-code]]) ;; Create the postal code field and button

(defn mount-app-element []
  (rdom/render [app] (gdom/getElement "app"))) ;; mount app in actual "app" DOM element

(mount-app-element)

(defn ^:after-load on-reload []
  (mount-app-element))