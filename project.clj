(defproject mccraigmccraig/onyx-manifold "0.2.1"
  :description "Onyx plugin for Manifold"
  :url "https://github.com/mccraigmccraig/onyx-manifold"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[manifold "0.1.1-alpha3"]
                 [org.onyxplatform/onyx "0.6.0"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.7.0"]

                                  [org.clojure/tools.logging "0.3.1"]
                                  [ch.qos.logback/logback-classic "1.1.3"]
                                  [org.slf4j/slf4j-api "1.7.12"]
                                  [org.slf4j/jcl-over-slf4j "1.7.12"]
                                  [org.slf4j/log4j-over-slf4j "1.7.12"]
                                  [org.slf4j/jul-to-slf4j "1.7.12"]

                                  [yeller-timbre-appender "0.4.1"]
                                  [midje "1.7.0"]]
                   :plugins [[lein-midje "3.1.3"]]}})
