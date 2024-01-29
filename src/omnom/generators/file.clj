(ns omnom.generators.file
  "Generate content from disk augmenting with Clj datastructure."
  (:require [clojure.java.io :refer [file reader]]))

(defn pedestal-log->events
  "Generate log file lines given a path.
   Filter using lines-filter e.g. for a ns.
   Split on a given token to create events out of log lines."
  [path lines-filter token]
  (->> (line-seq (reader (file path)))
       (filter #(includes? % lines-filter))
       (map #(.split % token))))
