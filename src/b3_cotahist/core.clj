(ns b3-cotahist.core
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string]
            [clojure.edn :as clojure.edn]
            [clojure.data.json :as json]
            [clojure.java.io :as java.io]
            [clj-http.client :as client])
  (:import [java.time LocalDate])
  (:gen-class))

;; helpers
(defn exit [code]
  (System/exit code))

;; query
(defn hash-set-options
  [prev k v]
  (assoc prev k
         (if-let [old (get prev k)]
           (merge old v)
           (hash-set v))))

(defn ticket [^String line]
  (let [register-type (subs line 0 2)
        date (str (subs line 2 6) "-" (subs line 6 8) "-" (subs line 8 10))
        bdi-code (subs line 10 12)
        ticket (string/trim (subs line 12 24))
        company-name (string/trim (subs line 27 39))
        price-close (Float/parseFloat (str (subs line 108 119) "." (subs line 119 121)))]
    {:ticket ticket
     :price-close price-close
     :date date}))

(defn parse-file
  [^String file]
  (with-open [f (clojure.java.io/reader file)]
    (let [lines (line-seq f)
          header (first lines)
          content (->> lines (drop 1) drop-last)]
      (doall (map ticket content)))))

(defn filter-data
  [data {:keys [tickets dates]}]
  (if (or tickets dates)
    (filter (fn [d]
              (and (or (empty? tickets) (get tickets (-> d :ticket string/lower-case keyword)))
                   (or (empty? dates) (get dates (-> d :date))))) data)
    data))

(defmulti output
  (fn [_ fmt] (or fmt :json)))
(defmethod output :json [info _]
  (json/write-str info :key-fn #(-> % name string/lower-case (string/replace #"-" "_"))))

;; historical quotes file download
(def yearly-file "https://bvmf.bmfbovespa.com.br/InstDados/SerHist/COTAHIST_A{YYYY}.ZIP")
(def monthly-file "https://bvmf.bmfbovespa.com.br/InstDados/SerHist/COTAHIST_M{MM}{YYYY}.ZIP")
(def daily-file "https://bvmf.bmfbovespa.com.br/InstDados/SerHist/COTAHIST_D{DD}{MM}{YYYY}.ZIP")
(defn replace-download-url [url args]
  (reduce
   (fn [url [k v]]
     (string/replace url (re-pattern (str "\\{" (name k) "\\}")) (str v)))
   url
   (seq args)))

(defn download-file [url]
  (let [filename (string/replace (last (string/split url #"/")) #".ZIP" ".TXT")
        zip (client/get url {:insecure? true :as :stream})]
    (with-open [in (java.util.zip.ZipInputStream. (:body zip))
                out (java.io/output-stream filename)]
      (.getNextEntry in)
      (clojure.java.io/copy in out))))

(defn download-url [date]
  (case date
    "today" (let [date (LocalDate/now)]
              (replace-download-url daily-file {:DD (.getDayOfMonth date)
                                                :MM (.getMonthValue date)
                                                :YYYY (.getYear date)}))
    "yesterday" (let [date (.minusDays (LocalDate/now) 1)]
                  (replace-download-url daily-file {:DD (.getDayOfMonth date)
                                                    :MM (.getMonthValue date)
                                                    :YYYY (.getYear date)}))
    (condp re-find date
      #"^(\d{4})$" :>> (fn [[_ year]]
                         (replace-download-url yearly-file {:YYYY year}))
      #"^(\d{4})-?(\d{2})$" :>> (fn [[_ year month]]
                                  (replace-download-url monthly-file {:YYYY year :MM month}))
      #"^(\d{4})-?(\d{2})-?(\d{2})$" :>> (fn [[_ year month day]]
                                           (replace-download-url daily-file {:YYYY year :MM month :DD day}))
      (do
        (println (str "Cannot parse date: " date "\nUse YYYY-MM-DD"))
        (exit 1)))))

;; cli controller
(def project-version (->> (slurp "project.clj") clojure.edn/read-string (drop 1) (take 2) (string/join " ")))

(defn usage [options-summary]
  (->> [project-version
        ""
        "Usage: b3-cotahist [options] <action"
        ""
        "Options:"
        options-summary
        ""
        "Commands:"
        "  download DATE  Download historical file for DATE: today, yesterday or date formatted as YYYY-MM-DD for daily files; YYYY-MM for monthly file; YYYY for yearly file"
        ""]
       (string/join \newline)))

(def cli-options
  [["-t" "--ticket TICKET" "Filter tickets (eg. '-t PETR3 -t brfs3')"
    :assoc-fn hash-set-options
    :id :tickets
    :parse-fn (comp keyword string/lower-case)
    :missing :missing-ticket]
   ["-d" "--date DATE" "Filter dates (eg. '-d 2020-01-01 -d 2020-01-02')"
    :id :dates
    :assoc-fn hash-set-options
    :parse-fn str]
   [nil "--output-format" "Output format"
    :parse-fn keyword]
   ["-f" "--file FILE" "Bovespa file"
    :missing :missing-file]
   ["-v" "--version" "Prints version"
    :assoc-fn (fn [m k _] (assoc m k true))]
   ["-h" "--help"]])

(defn -main [& args]
  (let [{{:keys [file tickets dates output-format errors version help]} :options
         arguments :arguments
         :as opts} (parse-opts args cli-options)]
    (when (and errors (empty? arguments))
      (println errors)
      (exit 1))
    (when version
      (println project-version)
      (exit 0))
    (when help
      (println (usage (:summary opts)))
      (exit 0))
    (when (seq arguments)
      (case (first arguments)
        "download" (download-file (download-url (second arguments))))
      (exit 0))
    (when file
      (-> (parse-file file)
          (filter-data {:tickets tickets :dates dates})
          (output output-format)
          println)
      (exit 0))
    (exit 0)))

