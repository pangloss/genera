(ns genera.macros
  (:require [genera.core :as c]
            [clojure.core.memoize :as memo]))

(defmacro defgenera
  "Create a new generic dispatch multi-method with the given name and arity.

  This version also defines a function body that will be called in case no
  [[defgen]]-type method is able to be dispatched. The fn-tail arity must match
  the specified arity, but otherwise is the same as the body of a clojure
  (fn ...) expression."
  {:see-also '[defgenera* defgenera= defgen]}
  [name arity & fn-tail]
  (let [[doc & fn-tail] (if (string? (first fn-tail))
                          fn-tail
                          (cons nil fn-tail))]
    `(do (def ~name (c/generic-procedure-constructor '~name ~arity (fn ~name ~@fn-tail)))
         (alter-meta! (var ~name) merge {:doc ~doc :arglists (list '~(first fn-tail))})
         (var ~name))))

(defmacro defgenera*
  "Create a new generic dispatch multi-method with the given name and arity.

  This version is given a default-handler function that will be called in case
  no [[defgen]]-type method is able to be dispatched. The default-handler must
  handle the specified arity."
  {:see-also '[defgenera defgenera= defgen]}
  ([name arity default-handler]
   `(defgenera* ~name ~arity nil ~default-handler))
  ([name arity doc default-handler]
   `(let [handler# ~default-handler]
      (def ~name (c/generic-procedure-constructor '~name ~arity handler#))
      (alter-meta! (var ~name) merge {:doc ~doc
                                      ;; a bunch of work to try to produce a nice arglist
                                      :arglists
                                      (filter #(= ~arity (count %))
                                              (or ~(when (symbol? default-handler)
                                                     `(:arglists (meta (var ~default-handler))))
                                                  (:arglists (meta ~default-handler))
                                                  ['~(vec (repeat arity '_))]))})

      (var ~name))))

(defmacro defgenera=
  "Create a new generic dispatch multi-method with the given name and arity.

  This version is given a default value that will be returned in case
  no [[defgen]]-type method is able to be dispatched."
  {:see-also '[defgenera defgenera* defgen]}
  ([name arity default-value]
   `(defgenera= ~name ~arity nil ~default-value))
  ([name arity doc default-value]
   `(defgenera* ~name ~arity ~doc (constantly ~default-value))))

(defmacro defgen
  "Add or update a specialization of a generic dispatch multi-method.

  The generic argument must refer to a method defined by a [[defgenera]]-class macro.

  The handlers argument is a list of handler functions. There must be one
  handler in each argument position. If all handlers return a truthy value, the
  function will be dispatched.

  The rest of the arguments are the same as the body of a clojure (fn ...)
  expression."
  {:see-also '[defgenera defgen* defgen=]}
  [generic handlers & fn-tail]
  `(c/assign-handler! ~generic (fn ~(symbol (name generic)) ~@fn-tail) ~@handlers))

(defmacro defgen* [generic handlers fn]
  `(c/assign-handler! ~generic ~fn ~@handlers))

(defmacro defgen! [generic handlers fn]
  `(c/assign-handler! ~generic (memo/lru ~fn :lru/threshold 1024) ~@handlers))

(defmacro defgen= [generic handlers const]
  `(c/assign-handler! ~generic (constantly ~const) ~@handlers))

(defmacro defmethod*
  "Add the given function as a handler"
  [multifn dispatch-val fn]
  `(. ~multifn clojure.core/addMethod ~dispatch-val ~fn))

(defmacro defmethod!
  "Add the given function as a handler. Memoized with lru 1024 deep."
  [multifn dispatch-val fn]
  `(. ~multifn clojure.core/addMethod ~dispatch-val (memo/lru ~fn :lru/threshold 1024)))

(defmacro defmethod=
  "Return the given constant when this handler is reached."
  [multifn dispatch-val value]
  `(. ~multifn clojure.core/addMethod ~dispatch-val (constantly ~value)))
