(ns genera
  (:refer-clojure :exclude [trampoline])
  (:require genera.macros
            genera.trampoline
            potemkin))


(potemkin/import-vars
 (genera.macros defgenera
                defgenera*
                defgenera=
                defgen
                defgen*
                defgen!
                defgen=
                defmethod*
                defmethod!
                defmethod=)
 (genera.trampoline trampoline bounce
                    trampolining bouncing))
