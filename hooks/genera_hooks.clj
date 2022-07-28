(ns genera-hooks)

(defmacro defgenera
  "Do not use. This is for clj-kondo linting / lsp support only"
  [name arity & fn-tail]
  `(defn ~name ~@fn-tail))


(defmacro defgenera*
  "Do not use. This is for clj-kondo linting / lsp support only"
  [name arity & etc]
  `(defn ~name [~@(repeat arity gensym)]))


(defmacro defgenera=
  "Do not use. This is for clj-kondo linting / lsp support only"
  [name arity & etc]
  `(defn ~name [~@(repeat arity gensym)]))


(defmacro defgen
  "Do not use. This is for clj-kondo linting / lsp support only"
  [name arity & etc]
  `(defn ~name [~@(repeat arity gensym)]))

(defmacro defgen
  "Do not use. This is for clj-kondo linting / lsp support only"
  [name _ & fn-tail]
  `(defn ~name ~@fn-tail))
