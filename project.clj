(defproject mccraigmccraig/onyx-manifold "0.1.0"
  :description "Onyx plugin for Manifold"
  :url "https://github.com/mccraigmccraig/onyx-manifold"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[manifold "0.1.1-alpha3"]
                 [com.mdrogalis/onyx "0.5.3"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.7.0"]
                                  [midje "1.7.0"]]
                   :plugins [[lein-midje "3.1.3"]]}})
