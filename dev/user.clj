(ns user
  (:require [b3-cotahist.core :as core]))

(def line
  "012020090102TIET11      010AES TIETE E UNT     N2   R$  000000000150800000000015110000000001494000000000150200000000015050000000001503000000000150505885000000000001447500000000002174694800000000000000009999123100000010000000000000BRTIETCDAM15125")

(comment
  (core/ticket line)
  (subs (subs line 108 121) 11 13)
  (core/parse-file "./resources/DemoCotacoesHistoricas12022003.txt"))

(comment
  (parse-opts ["trololo" "-t" "BRFS3" "-t" "petr3" "-d" "2020-01-01" "trelele"] cli-options)
  (parse-opts ["-t" "brfs3" "-t" "petr3" "-f" "bovespa.csv"] cli-options))
