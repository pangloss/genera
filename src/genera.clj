(ns genera
  (:require [genera.macros :as m]
            potemkin))


(potemkin/import-vars
 (genera.macros defgeneric
                defgeneric*
                defgeneric=
                defgen
                defgen*
                defgen!
                defgen=
                defmethod*
                defmethod!
                defmethod=))
