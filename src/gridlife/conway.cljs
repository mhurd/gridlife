(ns library.books
  (:require [clojure.browser.repl]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [ajax.core :refer [GET POST]]
            [cemerick.url :refer [url-encode]]
            [jayq.core :refer [$ fade-in fade-out]]))

(enable-console-print!)

;; Browser History
;; https://github.com/fmw/vix/blob/master/src/cljs/src/util.cljs
;; https://github.com/fmw/vix/blob/master/src/cljs/src/core.cljs

(def app-state (atom {:sorted-books [],
                      :indexed-books {},
                      :display {}}))

(defn display-book [book]
  (swap! app-state assoc :display book)
  )

(defn display-index []
  (swap! app-state assoc :display {})
  )

(defn set-books [books]
  (let [sorted (sort-by :title books)
        indexed (zipmap (map #(:asin %) sorted) sorted)]
    (swap! app-state assoc :sorted-books sorted)
    (swap! app-state assoc :indexed-books indexed))
  )

(defn handle-error [error]
  (println (str error)))

(defn get-books []
  (println "Getting books...")
  (GET "/api/books"
       {:response-format :transit,
        :handler         set-books,
        :error-handler   handle-error})
  )

(defn get-price [book price-key]
  (let [price (get book price-key)]
    (if (nil? price)
      "no-data"
      (str "£" (.toFixed (/ (js/parseFloat price) 100) 2))
      )
    ))

(defn get-attribute [book key]
  (let [val (get book key)]
    (if (nil? val)
      "no-data"
      val
      )
    )
  )

(defn has-increased-in-value [book]
  (let [listPrice (get book :listPrice)
        lowestPrice (get book :lowestPrice)]
    (if (or (nil? lowestPrice) (nil? listPrice))
      false
      (> lowestPrice listPrice)
    )
    ))

(defn get-price-change [book]
  (let [listPrice (get book :listPrice)
        lowestPrice (get book :lowestPrice)
        difference (- lowestPrice listPrice)]
    (if (or (nil? lowestPrice) (nil? listPrice))
      "?"
      (str "£" (.toFixed (/ (js/parseFloat difference) 100) 2))
    )))

(defn format-timestamp [timestamp]
  (let [date (js/Date. (js/parseInt timestamp))
        formatted (.toUTCString date)]
    formatted
    )
  )

(defn get-image [book key]
  (let [url (get book key)]
    (if (nil? url) "/img/no-image.jpg" url)
  ))

(defn light-book-view [book owner]
  (reify
    om/IRender
    (render [_]
      (html
        [:div {:class "book-div"}
         [:legend (str (get-attribute book :title))]
         [:table {:class "table"}
          [:tr {:class (if (has-increased-in-value book) "increased-value" "decreased-value")}
           [:td {:class "book-img-td" :align "right"}
            [:img {:class "book-img" :src (get-image book :smallImage) :on-click #(display-book book)}]]
           [:td {:class "book-details-td" :align: "left"}
            [:dl {:class "dl-horizontal details"}
             [:dt "Author(s):"] [:dd (get-attribute book :authors)]
             [:dt "ASIN:"] [:dd (get-attribute book :asin)]
             [:dt "Publisher:"] [:dd (get-attribute book :publisher)]
             [:dt "Publication Date:"] [:dd (get-attribute book :publicationDate)]
             [:dt "Price Change:"] [:dd (get-price-change book)]
             ]]]]]))))

(defn full-book-view [app owner]
  (reify
    om/IRender
    (render [_]
      (let [book (:display app)]
        (html
          (if (empty? (:display app))
            [:div {:class "book-div"}]
            [:div {:class "book-div"}
             [:legend (get-attribute (:display app) :title)]
             [:table {:class "table"}
              [:tr {:class (if (has-increased-in-value book) "increased-value" "decreased-value")}
               [:td {:class "large-book-img-td" :align "right"}
                [:img {:class "large-book-img" :src (get-image book :largeImage) :on-click #(display-index)}]]
               [:td {:class "book-details-td" :align: "left"}
                [:dl {:class "dl-horizontal details"}
                 [:dt "Author(s):"] [:dd (get-attribute book :authors)]
                 [:dt "ASIN:"] [:dd (get-attribute book :asin)]
                 [:dt "ISBN:"] [:dd (get-attribute book :isbn)]
                 [:dt "EAN:"] [:dd (get-attribute book :ean)]
                 [:dt "Publisher:"] [:dd (get-attribute book :publisher)]
                 [:dt "Publication Date:"] [:dd (get-attribute book :publicationDate)]
                 [:dt "Binding:"] [:dd (get-attribute book :binding)]
                 [:dt "Edition:"] [:dd (get-attribute book :edition)]
                 [:dt "Format:"] [:dd (get-attribute book :format)]
                 [:dt "No. of Pages:"] [:dd (get-attribute book :numberOfPages)]
                 [:dt "List price:"] [:dd (get-price book :listPrice)]
                 [:dt "Lowest Price:"] [:dd (get-price book :lowestPrice)]
                 [:dt "Price Change:"] [:dd (get-price-change book)]
                 [:dt "Total Available:"] [:dd (get-attribute book :totalAvailable)]
                 [:dt "Price's Updated:"] [:dd (format-timestamp (get-attribute book :lastPriceUpdateTimestamp))]
                 [:dt] [:dd
                        [:a {:href (get book :amazonPageUrl) :target "_blank"}
                         [:img {:src "/img/buy-from-amazon-button.gif" :caption "Buy from Amazon" :alt "Buy from Amazon"}]]]]]
               ]]]))
        )
      )))

(defn index-view [app owner]
  (reify
    om/IInitState
    (init-state [_]
      (get-books)
      {})
    om/IRenderState
    (render-state [this state]
      (html
        (if (empty? (:display app))
          [:div {:class "content"}
            (om/build-all light-book-view (:books app))]
          [:div {:class "content"}])))))

(om/root index-view app-state
         {:target (. js/document (getElementById "book-list"))})

(om/root full-book-view app-state
         {:target (. js/document (getElementById "book"))})