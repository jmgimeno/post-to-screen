(ns post-to-screen.test-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [post-to-screen.core-test]))

(enable-console-print!)

(doo-tests 'post-to-screen.core-test)
