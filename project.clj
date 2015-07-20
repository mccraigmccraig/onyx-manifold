(defproject mccraigmccraig/onyx-manifold "0.2.0"
  :description "Onyx plugin for Manifold"
  :url "https://github.com/mccraigmccraig/onyx-manifold"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[manifold "0.1.1-alpha3"]
                 [org.onyxplatform/onyx "0.6.0"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.7.0"]
                                  [yeller-timbre-appender "0.4.1"]
                                  [midje "1.7.0"]]
                   :plugins [[lein-midje "3.1.3"]]}})
