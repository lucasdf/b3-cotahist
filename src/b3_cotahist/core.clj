(ns b3-cotahist.core
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string]
            [clojure.edn :as clojure.edn]
            [clojure.data.json :as json]
            [clojure.java.io :as java.io])
  (:import [java.time LocalDate])
  (:gen-class))

(defn hash-set-options
  [prev k v]
  (assoc prev k
         (if-let [old (get prev k)]
           (merge old v)
           (hash-set v))))

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

(defn ticket [line]
  (let [register-type (subs line 0 2)
        date (str (subs line 2 6) "-" (subs line 6 8) "-" (subs line 8 10))
        bdi-code (subs line 10 12)
        ticket (string/trim (subs line 12 24))
        company-name (string/trim (subs line 27 39))
        price-close (-> (subs line 108 121)
                        string/trim
                        (#(str (subs % 0 11) "." (subs % 11 13)))
                        bigdec)]
    {:ticket ticket
     :price-close price-close
     :date date}))

(defn parse-file
  [file]
  (with-open [f (clojure.java.io/reader file)]
    (let [lines (line-seq f)
          header (first lines)
          content (->> lines (drop 1) drop-last)]
      (doall (pmap ticket content)))))

(defn filter-data
  [data {:keys [tickets dates]}]
  (-> data
      ((fn [d] (or (and tickets (filter #(get tickets (-> % :ticket string/lower-case keyword)) d)) d)))
      ((fn [d] (or (and dates (filter #(get dates (-> % :date)) d)) d)))))

(def project-version (->> (slurp "project.clj") clojure.edn/read-string (drop 1) (take 2) (string/join " ")))

(defn usage [options-summary]
  (->> [project-version
        ""
        "Usage: b3-cotahist [options] <action"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "  download    Download latest daily historical file"
        ""
        ]
       (string/join \newline)))

(defmulti output
  (fn [_ fmt] (or fmt :json)))
(defmethod output :json [info _]
  (json/write-str info :key-fn #(-> % name string/lower-case (string/replace #"-" "_"))))

(def yearly-file "http://bvmf.bmfbovespa.com.br/InstDados/SerHist/COTAHIST_A{YYYY}.ZIP")
(def monthly-file "http://bvmf.bmfbovespa.com.br/InstDados/SerHist/COTAHIST_M{MM}{YYYY}.ZIP")
(def daily-file "http://bvmf.bmfbovespa.com.br/InstDados/SerHist/COTAHIST_D{DD}{MM}{YYYY}.ZIP")
(defn download-url [url args]
  (reduce
   (fn [url [k v]]
     (string/replace url (re-pattern (str "\\{" (name k) "\\}")) (str v)))
   url
   (seq args)))
(defn download-yesterday-file []
  (let [date (.minusDays (LocalDate/now) 1)
        url  (download-url daily-file
                           {:YYYY (.getYear date)
                            :MM (.getMonthValue date)
                            :DD (.getDayOfMonth date)})
        filename (string/replace (last (string/split url #"/")) #".ZIP" ".TXT")]
    (with-open [in (java.util.zip.ZipInputStream. (java.io/input-stream url))
                out (java.io/output-stream filename)]
      (.getNextEntry in)
      (clojure.java.io/copy in out))))

(defn -main [& args]
  (let [{{:keys [file tickets dates output-format errors version help]} :options 
         arguments :arguments
         :as opts} (parse-opts args cli-options)]
    (when (and errors (empty? arguments))
      (println errors)
      (System/exit 1))
    (when version
      (println project-version)
      (System/exit 0))
    (when help
      (println (usage (:summary opts)))
      (System/exit 0))
    (when (seq arguments)
      (cond
        (get (set arguments) "download") (download-yesterday-file))
      (System/exit 0))
    (when file
      (-> (parse-file file)
          (filter-data {:tickets tickets :dates dates})
          (output output-format)
          println)
      (System/exit 0))
    (System/exit 0)))

