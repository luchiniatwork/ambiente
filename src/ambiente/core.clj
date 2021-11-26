(ns ambiente.core
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as s]))

(def ^:dynamic *log-fn* println)

(defn ^:private unquote-doublequoted-string [string]
  (-> string
      (s/replace #"^\"|\"$" "")
      (s/replace #"\\\"" "\"")))

(defn ^:private unquote-singlequoted-string [string]
  (-> string
      (s/replace #"^'|'$" "")
      (s/replace #"\\'" "'")))

(defn ^:private unquote-string [string]
  (cond (s/starts-with? string "\"") (unquote-doublequoted-string string)
        (s/starts-with? string "'")  (unquote-singlequoted-string string)
        :else string))

(defn ^:private to-pairs [rawstring]
  "converts a string containing the contents of a .env file into a list of pairs"
  (->> rawstring
       s/split-lines
       (map s/trim)
       (remove #(-> % empty? (s/starts-with? "#")))
       (map #(s/split % #"=" 2))
       (map #(vec (->> % (map s/trim) (map unquote-string))))))

(defn ^:private keywordize [s]
  (-> (s/lower-case s)
      (s/replace "_" "-")
      (s/replace "." "-")
      (keyword)))

(defn ^:private sanitize-key [k]
  (let [s (keywordize (name k))]
    (if-not (= k s) (*log-fn* "Warning: environ key" k "has been corrected to" s))
    s))

(defn ^:private sanitize-val [k v]
  (if (string? v)
    v
    (do (*log-fn* "Warning: environ value" (pr-str v) "for key" k "has been cast to string")
        (str v))))

(defn ^:private read-system-env []
  (->> (System/getenv)
       (map (fn [[k v]] [(keywordize k) v "environment variable"]))))

(defn ^:private read-system-props []
  (->> (System/getProperties)
       (map (fn [[k v]] [(keywordize k) v "system properties"]))))

(defn ^:private slurp-file [f]
  (when-let [f (io/file f)]
    (when (.exists f)
      (slurp f))))

(defn ^:private read-env-edn-file [f]
  (when-let [content (slurp-file f)]
    (for [[k v] (edn/read-string content)]
      [(sanitize-key k) (sanitize-val k v) ".env.edn file"])))

(defn ^:private read-dotenv-file [f]
  (when-let [content (slurp-file f)]
    (for [[k v] (to-pairs content)]
      [(sanitize-key k) (sanitize-val k v) ".env file"])))

(defn ^:private warn-on-overwrite [ms]
  (let [all (apply concat ms)
        repeated (->> all
                      (group-by first)
                      (filter (fn [[_ vs]] (> (count (distinct (map second vs))) 1))))]
    (doseq [[k vs] repeated]
      (*log-fn* "Warning: environ value" (second (first vs)) "for key" k
                "has been overwritten with" (second (last vs)) "by" (last (last vs))))))

(defn ^:private merge-env [& ms]
  (warn-on-overwrite ms)
  (->> (apply concat ms)
       (map (fn [i] [(first i) (second i)]))
       (into {})))

(defn read-env []
  (merge-env
   (read-env-edn-file ".env.edn")
   (read-dotenv-file ".env")
   (read-system-env)
   (read-system-props)))

(defonce ^{:doc "A map of environment variables."}
  env (read-env))
