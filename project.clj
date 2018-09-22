(defproject org.clojars.ejschoen/clj-tape "0.4.0"
  :description "Clojure wrapper for Tape"
  :url "https://github.com/ejschoen/clj-tape.git"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories {"sonatype-snapshots" {:url "https://oss.sonatype.org/content/repositories/snapshots"
                                       :snapshots true
                                       :releases {:checksum :fail :update :always}}}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [cheshire "5.5.0"]
                 [org.clojure/core.async "0.4.474"]
                 [com.squareup.tape2/tape "2.0.0-beta1"]])
