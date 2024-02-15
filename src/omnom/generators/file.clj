(ns omnom.generators.file
  "Generate content from disk augmenting with Clj datastructure."
  (:require [clojure.edn :refer [read-string]]
            [clojure.java.io :refer [file reader]]
            [clojure.string :refer [includes?]]))

(defn with-io-exists-xs-handling
  "Handles cases where FileNotFoundException arises by returning empty collection"
  [f] (println "handling exceptions in wrapper") (fn [& args] (try (apply f args) (catch Exception e []))))

(defn pedestal-log->es!
  "Generate log file lines given a path.
   Filter using ns-filter e.g. to include onlines that relate to a namespace.
   Split on a given token to create events out of log lines."
  [path ns-filter token]
  (->> (line-seq (reader (file path)))
       (filter #(includes? % ns-filter))
       (map #(.split % token))
       (map #(hash-map :ns ns-filter :instant (first (.split (first %) " ")) :log (read-string (second %))))))

(def pedestal-log->es
  "Wrap pedestal-log->es to return empty collection if file does not exist."
  (with-io-exists-xs-handling pedestal-log->es!))
