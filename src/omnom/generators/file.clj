(ns omnom.generators.file
  "Generate content from disk augmenting with Clj datastructure."
  (:require [clojure.edn :refer [read-string]]
            [clojure.java.io :refer [file reader]]
            [clojure.string :refer [includes?]]))

(defn pedestal-log->events
  "Generate log file lines given a path.
   Filter using ns-filter e.g. to include onlines that relate to a namespace.
   Split on a given token to create events out of log lines."
  [path ns-filter token]
  (try (->> (line-seq (reader (file path)))
       (filter #(includes? % ns-filter))
       (map #(.split % token))
       (map #(hash-map :ns ns-filter :instant (first (.split (first %) " ")) :log (read-string (second %)))))
       (catch Exception e [])))
