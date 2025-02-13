#!/usr/bin/env bb
; vim: ft=clojure
(load-file "common.bb")

(defn logo [{:keys [site] :as opts}]
  [:div (if-some [image (:logo-image site)]
          [:a {:href (:logo-url site)}
           [:img {:src image
                  :alt "Logo"
                  :style {:max-height "30px"}}]]
          (:title site))])

(def hamburger-icon
  [:div.sm:hidden.cursor-pointer
   {:_ "on click toggle .hidden on #nav-menu"}
   (for [_ (range 3)]
     [:div.bg-white
      {:class "h-[4px] w-[30px] my-[6px]"}])])

(defn parse-nav-links [{:keys [site]}]
  (->> (:nav-links site)
       str/split-lines
       (map #(str/split % #"\s+" 2))))

(defn navbar [{:keys [site navbar/max-w] :as opts}]
  (list
    [:div.bg-primary.py-2
     [:div.flex.mx-auto.items-center.text-white.gap-4.text-lg.flex-wrap.px-3
      {:class (or max-w "max-w-screen-md")}
      (logo opts)
      [:div.flex-grow]
      (for [[href label] (parse-nav-links opts)]
        [:a.hover:underline.hidden.sm:block
         {:href href}
         label])
      hamburger-icon]]
    [:div#nav-menu.bg-primary.px-5.py-2.text-white.text-lg.hidden.transition-all.ease-in-out.sm:hidden
     (for [[href label] (parse-nav-links opts)]
       [:div.my-2 [:a.hover:underline.text-lg {:href href} label]])]))

(defn byline [{:keys [post site byline/card] :as opts}]
  [:div
   {:style {:display "flex"
            :align-items "center"}}
   [:img (if card
           {:src (:author-image site)
            :width "115px"
            :height "115px"
            :style {:border-radius "50%"}}
           {:src (cached-img-url {:url (:author-image site)
                                  :w 200 :h 200})
            :width "50px"
            :height "50px"
            :style {:border-radius "50%"}})]
   [:div {:style {:width "0.75rem"}}]
   [:div
    [:div {:style {:line-height "1.25"}}
     [:a.hover:underline
      {:class (if card
                "text-[2.5rem]"
                "text-blue-600")
       :href (:author-url site)
       :target "_blank"}
      (:author-name site)]]
    [:div {:class (if card "text-[2.2rem]" "text-[90%]")
           :style {:line-height "1"
                   :color "#4b5563"}}
     (format-date "d MMM yyyy" (:published-at post))]]])

(def errors
  {"invalid-email" "It looks like that email is invalid. Try a different one."
   "recaptcha-failed" "reCAPTCHA check failed. Try again."
   "unknown" "There was an unexpected error. Try again."})

(defn subscribe-form [{:keys [site account] lst :list}]
  [:div.flex.flex-col.items-center.text-center.px-3.bg-primary.text-white
   [:div.h-20]
   [:div.font-bold.text-3xl.leading-none
    (:title lst)]
   [:div.h-4]
   [:div.text-lg (:description site)]
   [:div.h-6]
   [:script (raw-string "function onSubscribe(token) { document.getElementById('recaptcha-form').submit(); }")]
   [:form#recaptcha-form.w-full.max-w-md
    {:action "/.netlify/functions/subscribe"
     :method "POST"}
    [:input {:type "hidden"
             :name "href"
             :_ "on load set my value to window.location.href"}]
    [:input {:type "hidden"
             :name "referrer"
             :_ "on load set my value to document.referrer"}]
    [:div.flex.flex-col.sm:flex-row.gap-1.5
     [:input {:class '[rounded
                       shadow
                       border-gray-300
                       focus:ring-0
                       focus:ring-transparent
                       focus:border-gray-300
                       flex-grow
                       text-black]
              :type "email"
              :name "email"
              :placeholder "Enter your email"
              :_ (str "on load "
                      "make a URLSearchParams from window.location.search called p "
                      "then set my value to p.get('email')")}]
     [:button {:class '[bg-accent
                        hover:bg-accent-dark
                        text-white
                        py-2
                        px-4
                        rounded
                        shadow
                        g-recaptcha]
               :data-sitekey (:recaptcha/site account)
               :data-callback "onSubscribe"
               :data-action "subscribe"
               :type "submit"}
      "Subscribe"]]
    (for [[code explanation] errors]
      [:div.text-red-600.hidden.text-left
       {:_ (str "on load if window.location.search.includes('error="
                code
                "') remove .hidden from me")}
       explanation])]
   [:div.h-20]])

(defn post-page [{:keys [site post account base/path] :as opts}]
  (let [width (if ((:tags post) "video")
                "max-w-screen-lg"
                "max-w-screen-sm")]
    (base-html
      opts
      (navbar (assoc opts :navbar/max-w width))
      [:div.mx-auto.p-3.text-lg.flex-grow.w-full
       {:class width}
       [:div.h-2]
       [:h1.font-bold.leading-tight.text-gray-900
        {:style {:font-size "2.75rem"}}
        (:title post)]
       [:div.h-3]
       (byline opts)
       [:div.h-5]
       [:div.post-content (raw-string (:html post))]
       [:div.h-5]
       (when-some [forum-url (not-empty (:discourse-url site))]
         (list
           [:div.text-xl.font-bold.mb-3 "Comments"]
           (embed-discourse {:forum-url forum-url
                             :page-url (str (:url site) path)})
           [:div.h-5]))]
      (subscribe-form opts)
      [:div.bg-primary
       [:div.sm:text-center.text-sm.leading-snug.w-full.px-3.pb-3.text-white.opacity-75
        (recaptcha-disclosure {:link-class "underline"})]])))

(defn render-page [{:keys [site page account] :as opts}]
  (base-html
    opts
    (navbar opts)
    [:div.mx-auto.p-3.text-lg.flex-grow.w-full.max-w-screen-md
     [:div.post-content (raw-string (:html page))]]))

(defn archive-page [{:keys [posts site] lst :list :as opts}]
  (base-html
    (assoc opts :base/title "Archive")
    (navbar opts)
    [:div.bg-tertiary.h-full.flex-grow.flex.flex-col
     [:div.h-5]
     [:div.max-w-screen-md.mx-auto.px-3.w-full
      (for [post posts
            :when (not ((:tags post) "unlisted"))]
        [:a.block.mb-5.bg-white.rounded.p-3.cursor-pointer.w-full
         {:href (str "/p/" (:slug post) "/")
          :class "hover:bg-white/50"}
         [:div.text-sm.text-gray-800 (format-date "d MMM yyyy" (:published-at post))]
         [:div.text-xl.font-bold (:title post)]
         [:div (:description post)]])]
     [:div.flex-grow]]
    (subscribe-form opts)
    [:div.bg-primary
     [:div.sm:text-center.text-sm.leading-snug.w-full.px-3.pb-3.text-white.opacity-75
      (recaptcha-disclosure {:link-class "underline"})]]))

(defn landing-page [{:keys [posts site] lst :list :as opts}]
  (base-html
    (assoc opts :base/title (:title lst))
    (navbar opts)
    (subscribe-form opts)
    [:div.bg-tertiary.h-full.flex-grow.flex.flex-col
     [:div.h-5]
     [:div.max-w-screen-md.mx-auto.px-3.w-full
      (for [post (->> posts
                      (sort-by #(not ((:tags %) "featured")))
                      (remove #((:tags %) "unlisted"))
                      (take 5))]
        [:a.block.mb-5.bg-white.rounded.p-3.cursor-pointer.w-full
         {:href (str "/p/" (:slug post) "/")
          :class "hover:bg-white/50"}
         [:div.text-sm.text-gray-800 (format-date "d MMM yyyy" (:published-at post))]
         [:div.text-xl.font-bold (:title post)]
         [:div (:description post)]])]
     [:div.flex-grow]
     [:div.sm:text-center.text-sm.leading-snug.opacity-75.w-full.px-3
      (recaptcha-disclosure {:link-class "underline"})]
     [:div.h-3]]))

(def pages
  {"/" landing-page
   "/archive/" archive-page})

(defn render-card [{:keys [site post] :as opts}]
  (base-html
    opts
    [:div.mx-auto.border.border-black
     {:style "width:1202px;height:620px"}
     [:div.flex.flex-col.justify-center.h-full.p-12
      [:div [:img {:src "/images/card-logo.png"
             :alt "Logo"
             :style {:max-height "60px"}}]]
      [:div {:class "h-[1.5rem]"}]
      [:h1.font-bold.leading-none
       {:class "text-[6rem]"}
       (str/replace (:title post) #"^\[draft\] " "")]
      [:div {:class "h-[2.5rem]"}]
      (byline (assoc opts :byline/card true))]]))

(defn cards! [{:keys [posts] :as opts}]
  ;; In Firefox, you can inspect element -> screenshot node, then use as the
  ;; post image (for social media previews).
  (doseq [post posts
          :let [path (str "/p/" (:slug post) "/card/")]]
    (render! path
             "<!DOCTYPE html>"
             (render-card (assoc opts :base/path path :post post)))))

(defn main [opts]
  (shell/sh "mkdir" "-p" "public")
  (redirects! opts)
  (netlify-fn-config! opts)
  (pages! opts render-page pages)
  (posts! opts post-page)
  (cards! opts)
  (atom-feed! opts)
  (assets!)
  (tailwind! opts)
  (sitemap! {:exclude [#"/subscribed/" #".*/card/"]}))

(time (main (derive-opts (edn/read-string (slurp "input.edn")))))
nil
