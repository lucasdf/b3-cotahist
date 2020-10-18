(ns b3-cotahist.core-test
  (:require [clojure.test :refer :all]
            [b3-cotahist.core :as core]
            [clojure.edn :as edn]
            [clojure.data.json :as json]))

(deftest core-test
  (testing "Can parse file"
    (with-redefs [core/exit identity]
      (is (= [{"ticket" "VALE5T" "price_close" 101.94 "date" "2003-02-12"}]
             (json/read-str
              (with-out-str
                (core/-main "-f" "resources/DemoCotacoesHistoricas12022003.txt" "-t" "VALE5T"))))))))

