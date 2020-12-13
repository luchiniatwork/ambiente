(ns ambiente.core
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as s]))

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
       (remove #(-> % empty?
                    (s/starts-with? "#")))
       (map #(s/split % #"="))
       (map #(vec (->> % (map s/trim)
                       (map unquote-string))))))

(defn ^:private keywordize [s]
  (-> (s/lower-case s)
      (s/replace "_" "-")
      (s/replace "." "-")
      (keyword)))

(defn ^:private sanitize-key [k]
  (let [s (keywordize (name k))]
    (if-not (= k s) (println "Warning: environ key" k "has been corrected to" s))
    s))

(defn ^:private sanitize-val [k v]
  (if (string? v)
    v
    (do (println "Warning: environ value" (pr-str v) "for key" k "has been cast to string")
        (str v))))

(defn ^:private read-system-env []
  (->> (System/getenv)
       (map (fn [[k v]] [(keywordize k) v]))
       (into {})))

(defn ^:private read-system-props []
  (->> (System/getProperties)
       (map (fn [[k v]] [(keywordize k) v]))
       (into {})))

(defn ^:private slurp-file [f]
  (when-let [f (io/file f)]
    (when (.exists f)
      (slurp f))))

(defn ^:private read-env-edn-file [f]
  (when-let [content (slurp-file f)]
    (into {} (for [[k v] (edn/read-string content)]
               [(sanitize-key k) (sanitize-val k v)]))))

(defn ^:private read-dotenv-file [f]
  (when-let [content (slurp-file f)]
    (into {} (for [[k v] (to-pairs content)]
               [(sanitize-key k) (sanitize-val k v)]))))

(defn ^:private warn-on-overwrite [ms]
  (doseq [[k kvs] (group-by key (apply concat ms))
          :let  [vs (map val kvs)]
          :when (and (next kvs) (not= (first vs) (last vs)))]
    (println "Warning: environ value" (first vs) "for key" k
             "has been overwritten with" (last vs))))

(defn ^:private merge-env [& ms]
  (warn-on-overwrite ms)
  (apply merge ms))

(defn ^:private read-env []
  (merge-env
   (read-env-edn-file ".env.edn")
   (read-dotenv-file ".env")
   (read-system-env)
   (read-system-props)))

(def ^{:doc "A map of environment variables."}
  env (read-env))
