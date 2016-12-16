(ns leiningen.continuum-test
  (:require [clojure.test :as t]
            [clojure.tools.namespace.find :as nsf]
            [robert.hooke]
            [leiningen.test])
  (:import java.io.ByteArrayOutputStream))

(def curr-var (atom nil))

(def ^:dynamic rp t/report)

(def fail-count (atom 0))

(def error-count (atom 0))

(defn continuum-report
 "Generates test output that includes full file paths
 that can be used in VS Code task problem matchers."
 [data]
 (println "IN BINDING")
 (when (= (:type data) :begin-test-var)
   (let [test-ns (eval (:var data))]
     (println (str "testing" (.toString test-ns)))) 
   (swap! curr-var (constantly (:var data))))
 (when (:fail data)
   (swap! fail-count inc)
   (let [metadata (meta @curr-var)
         file (:file metadata)
         line (:line metadata)]
     (println (format "FAIL in %s:%d" file line)))
  (when (:error data)
    (swap! error-count inc)
    (let [metadata (meta @curr-var)
          file (:file metadata)
          line (:line metadata)]
      (println (format "ERROR in %s:%d" file line)))))
 (rp data))

(defmacro with-continuum-report
  [& body]
  `(binding [t/report continuum-report]))


(def ^:dynamic xyz 1)

(def buf (clojure.java.io/writer (ByteArrayOutputStream.)))


(defn test-hook [task & args]
  `(binding [~'clojure.test/*test.out*
             ~'leiningen.continuum-test/buf]
     ~(apply task args)))
  ; (binding [clojure.test/report continuum-report])
  ; (binding [clojure.test/*test-out* buf]
    ; (println "TESTING " xyz)
    ; (apply task args))
  ; `(binding [~'clojure.test/report
  ;            (fn [data#]
  ;              (when (= (:type data) :begin-test-var)
  ;                (let [test-ns (eval (:var data))]
  ;                  (println (str "testing" (.toString test-ns)))) 
  ;                (swap! curr-var (constantly (:var data))))
  ;              (when (:fail data)
  ;                (swap! fail-count inc)
  ;                (let [metadata (meta @curr-var)
  ;                      file (:file metadata)
  ;                      line (:line metadata)]
  ;                  (println (format "FAIL in %s:%d" file line)))
  ;                (when (:error data)
  ;                  (swap! error-count inc)
  ;                  (let [metadata (meta @curr-var)
  ;                        file (:file metadata)
  ;                        line (:line metadata)]
  ;                    (println (format "ERROR in %s:%d" file line)))))
  ;              (rp data))]
  ;    ~(apply task args)))

;; Place the body of the activate function at the top-level for
;; compatibility with Leiningen 1.x
(defn activate []
  (robert.hooke/add-hook #'leiningen.test/form-for-testing-namespaces
                         #'test-hook))

; (defn add-test-var-println [f & args]
;   `(binding [~'clojure.test/assert-expr
;              (fn [msg# form#]
;                (println "Asserting" form#)
;                ((.getRawRoot #'clojure.test/assert-expr) msg# form#))]
;      ~(apply f args)))

; (defn activate []
;   (robert.hooke/add-hook #'leiningen.test/form-for-testing-namespaces
;                          #'add-test-var-println))

(defn continuum-test
  "Run all tests and produce output that can be used by VS Code."
  [project & args]
  (println "OK"))
  ; (binding [t/report continuum-report]
  ;   (apply leiningen.test/test project args)))
  ; (apply leiningen.test/test project args))
