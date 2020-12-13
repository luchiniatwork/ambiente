(ns user)

(defn refresh-ns []
  (remove-ns 'ambiente.core)
  (dosync (alter @#'clojure.core/*loaded-libs* disj 'ambiente.core))
  (require 'ambiente.core))

(defn refresh-env []
  (refresh-ns)
  (var-get (find-var 'ambiente.core/env)))

(do
  (let [env (refresh-env)]
    (println (env :path))))
