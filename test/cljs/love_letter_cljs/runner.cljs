(ns love-letter-cljs.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [love-letter-cljs.game-test]
              [love-letter-cljs.utils-test]
              ))

(doo-tests 'love-letter-cljs.game-test
           'love-letter-cljs.utils-test)
