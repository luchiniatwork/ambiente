(ns ambiente.core-test
  (:require [ambiente.core :as ambiente]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing use-fixtures]]))

(defn ^:private delete-file [f]
  (.delete (io/file f)))

(defn ^:private test-fixture [test]
  (test)
  (delete-file ".env")
  (delete-file ".env.edn"))

(use-fixtures :each test-fixture)

(defn ^:private get-env [x]
  (System/getenv x))

(defn ^:private refresh-ns []
  (remove-ns 'ambiente.core)
  (dosync (alter @#'clojure.core/*loaded-libs* disj 'ambiente.core))
  (require 'ambiente.core))

(defn ^:private refresh-env []
  (refresh-ns)
  (var-get (find-var 'ambiente.core/env)))

(deftest test-env
  (is (map? ambiente/env)))

(deftest test-env-and-properties
  (testing "env variables"
    (let [env (refresh-env)]
      (is (= (:user env) (get-env "USER")))
      (is (= (:java-arch env) (get-env "JAVA_ARCH")))))
  (testing "system properties"
    (let [env (refresh-env)]
      (is (= (:user-name env) (System/getProperty "user.name")))
      (is (= (:user-country env) (System/getProperty "user.country"))))))

(deftest test-env-edn
  (testing ".env.edn file"
    (spit ".env.edn" (prn-str {:foo "bar"}))
    (let [env (refresh-env)]
      (is (= (:foo env) "bar"))))
  (testing ".env.edn file with irregular keys"
    (spit ".env.edn" (prn-str {:foo.bar "baz"}))
    (let [env (refresh-env)]
      (is (= (:foo-bar env) "baz"))))
  (testing ".env.edn file with irregular keys"
    (spit ".env.edn" "{:foo #=(str \"bar\" \"baz\")}")
    (is (thrown? RuntimeException (refresh-env))))
  (testing ".env.edn file with non-string values"
    (spit ".env.edn" (prn-str {:foo 1 :bar :baz}))
    (let [env (refresh-env)]
      (is (= (:foo env) "1"))
      (is (= (:bar env) ":baz")))))

(deftest test-dotenv
  (testing ".env file"
    (spit ".env" "FOO=bar")
    (let [env (refresh-env)]
      (is (= (:foo env) "bar"))))
  (testing ".env file with irregular keys"
    (spit ".env" "FOO.BAR=baz\nfOo_BaZ-33=foobaz")
    (let [env (refresh-env)]
      (is (= (:foo-bar env) "baz"))
      (is (= (:foo-baz-33 env) "foobaz"))))
  (testing ".env is trimmed properly"
    (spit ".env" "FOO = bar\nBAR= foo\nBAZ =baz")
    (let [env (refresh-env)]
      (is (= (:foo env) "bar"))
      (is (= (:bar env) "foo"))
      (is (= (:baz env) "baz"))))
  (testing ".env file with non-string values"
    (spit ".env" "FOO=1\nBAR=true")
    (let [env (refresh-env)]
      (is (= (:foo env) "1"))
      (is (= (:bar env) "true"))))
  (testing ".env file with single quote"
    (spit ".env" "FOO='bar'")
    (let [env (refresh-env)]
      (is (= (:foo env) "bar"))))
  (testing ".env file with double quote"
    (spit ".env" "FOO=\"bar\"")
    (let [env (refresh-env)]
      (is (= (:foo env) "bar"))))
  (testing ".env file with spaces"
    (spit ".env" "FOO=bar baz")
    (let [env (refresh-env)]
      (is (= (:foo env) "bar baz"))))
  (testing ".env file ignore comments"
    (spit ".env" "#FOO=bar\nFOO=baz")
    (let [env (refresh-env)]
      (is (= (:foo env) "baz")))))

(deftest test-priority-order
  (testing ".env trumps .env.edn"
    (spit ".env.edn" (prn-str {:foo "bar"}))
    (spit ".env" "FOO=baz")
    (let [env (refresh-env)]
      (is (= (:foo env) "baz"))))
  (testing "env var trumps .env"
    (spit ".env" "PATH=baz")
    (let [env (refresh-env)]
      (is (= (:path env) (get-env "PATH"))))))
