(ns genera.trampoline
  (:refer-clojure :exclude [trampoline]))

(defprotocol ITrampoliner
  (get-fn [_]))

(deftype Trampoliner [f]
  ITrampoliner
  (get-fn [_] f))

(defn bounce
  "Mark the given function with `::bounce` so that `trampoline` will call it."
  {:see-also ["trampoline" "bounce" "trampolining" "bouncing"]}
  [f]
  (Trampoliner. f))

(defn bounce?
  "Returns the function if argument is Trampoliner containing a function to bounce."
  [x]
  (when (instance? Trampoliner x) (get-fn x)))

(defn trampoline*
  "Trampoline can be used to convert algorithms requiring mutual recursion, or
  that recur from within a callback so that they don't consume the stack.

  Trampoline calls f with the optional supplied args, inpsects the return and if
  it is a function, tests that it is marked as a trampoline function by checking
  its metadata for `::bounce`. Functions can be marked by calling `(bounce f)`.

  The addition of the bounce check is the only difference between this
  implementation and the one in clojure.core. The purpose of the check is to
  prevent accidental trampolining when a function being trampolined could itself
  potentially return a function."
  {:see-also ["trampoline" "bounce" "trampolining" "bouncing" "clojure.core/trampoline"]}
  [ret]
  (if (instance? Trampoliner ret)
    (recur ((get-fn ret)))
    ret))

(defn trampoline
  "Trampoline can be used to convert algorithms requiring mutual recursion, or
  that recur from within a callback so that they don't consume the stack.

  Trampoline calls f with the optional supplied args, inpsects the return and if
  it is a function, tests that it is marked as a trampoline function by checking
  its metadata for `::bounce`. Functions can be marked by calling `(bounce f)`.

  The addition of the bounce check is the only difference between this
  implementation and the one in clojure.core. The purpose of the check is to
  prevent accidental trampolining when a function being trampolined could itself
  potentially return a function."
  {:see-also ["trampoline" "bounce" "trampolining" "bouncing" "clojure.core/trampoline"]}
  ([f] (trampoline* (f)))
  ([f & args]
   (trampoline* (bounce #(apply f args)))))

(defmacro trampolining
  "Wrap the given body in `trampoline`."
  {:see-also ["trampoline" "bounce" "trampolining" "bouncing"]}
  [& forms]
  `(trampoline* (bounce (fn [] ~@forms))))

(defmacro bouncing
  "Turn the given body into a function to be returned to the enclosing `trampoline` function"
  {:see-also ["trampoline" "bounce" "trampolining" "bouncing+"]}
  [& forms]
  `(Trampoliner. (fn [] ~@forms)))

(defmacro bouncing+
  "Turn the given body into a function to be returned to the enclosing `trampoline` function."
  {:see-also ["trampoline" "bounce" "trampolining" "bouncing"]}
  [name & forms]
  `(Trampoliner. (fn ~name [] ~@forms)))
