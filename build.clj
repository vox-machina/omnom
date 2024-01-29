(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'voxmachina/omnom)
(def major-v 0)
(def minor-v 1)
(def version (format "%d.%d.%s" major-v minor-v (b/git-count-revs nil)))
(def version-file "version.edn")
(def class-dir "target/classes")

(defn build
  "builds omnom - taking care of setup, version files etc"
  [_]
  (println "building...")
  (println "version: " version)
  (b/write-file {:path version-file :content {:omnom version}}))

(defn- pom-template [version]
  [[:description "Omnom content consumer app and library."]
   [:url "https://github.com/vox-machina/omnom"]
   [:licenses
    [:license
     [:name "MIT License"]
     [:url "https://opensource.org/license/mit/"]]]
   [:developers
    [:developer
     [:name "rossajmcd"]]]
   [:scm
    [:url "https://github.com/vox-machina/omnom"]
    [:connection "scm:git:https://github.com/vox-machina/omnom.git"]
    [:developerConnection "scm:git:ssh:git@github.com:vox-machina/omnom.git"]
    [:tag (str "v" version)]]])

(defn- jar-opts [opts]
  (assoc opts
         :lib lib   :version version
         :jar-file  (format "target/%s-%s.jar" lib version)
         :basis     (b/create-basis {})
         :class-dir class-dir
         :target    "target"
         :src-dirs  ["src"]
         :pom-data  (pom-template version)))

(defn ci "Run the CI pipeline of tests (and build the JAR)." [opts]
  (println "Running continuous integration...")
  (println "version: " version)
  (b/delete {:path "target"})
  (let [opts (jar-opts opts)]
    (println "\nWriting pom.xml...")
    (b/write-pom opts)
    (println "\nCopying source...")
    (b/copy-dir {:src-dirs ["resources" "src"] :target-dir class-dir})
    (println "\nBuilding JAR..." (:jar-file opts))
    (b/jar opts))
  opts)

(defn install "Install the JAR locally." [opts]
  (let [opts (jar-opts opts)]
    (b/install opts))
  opts)
