# genera

Maximally flexible multimethods for Clojure.

This library is a full implementation of the methods described in the book Software Design for Flexibility.

## How it differs from Clojure's default multimethods

Built-in multimethods provide a highly flexible multiple dispatch mechanism. 
The dispatch function is defined together with the multimethod. 
It sometimes happens that a method a user wants to later add can not be added.
This problem may be because an argument crashes the dispatch function, or the dispatch function can not distinguish the case from some other function.

The genera-style dispatch mechanism provides a higher degree of flexibility.
Each method defines a dispatch function for each of its arguments.

### Example Usage

This example is pulled from my pattern library.
The call to `defgenera` defines a `matcher-type` function, arity `1` with an optional docstring.
The remaining arguments, `[var] :value` are just like a normal function definition. This defines a default handler function that returns `:value` without looking at its `var` argument.

    (require '[genera :refer [defgenera defgen defgen* defgen=]])

    (defgenera matcher-type 1
      "Return the type indicator symbol for the variable if it is a matcher."
      [var] :value)

The same function could also be defined with the following variants:

    (defgenera= matcher-type 1 :value)
    (defgenera* matcher-type 1 (constantly :value))

Several matchers are then added, which will be tested in top-down order.
All three variants of `defgen` are used..

- `defgen` takes an argument vector and a body which are used to create a handler function.
- `defgen*` uses a pre-defined function as its handler.
- `defgen=` returns a constant value.

The example:

    (defgen* matcher-type [matcher-form?] matcher-form?)

    (defgen matcher-type [simple-named-var?] [x]
      (symbol (apply str (take-while #{\?} (name x)))))

    (defgen= matcher-type [sequential?] :list)
    (defgen= matcher-type [simple-ref?] '?:ref)
    (defgen= matcher-type [compiled-matcher?] :compiled-matcher)
    (defgen= matcher-type [compiled*-matcher?] :compiled*-matcher)
    (defgen= matcher-type [fn?] :plain-function)

Just like `defgen` has three variants, `defgenera` also has the equivalent three variants which are used to define the default handler. 


### Extracting a specific handler

If the need arises to repeatedly call a generic function where a known handler will be used, you can use `specialize` to refer directly to that handler.

    (specialize matcher-type '??my-simple-named-var) 
    ;; => #function[...]
    
The function returned will be the one defined in this matcher:

    (defgen matcher-type [simple-named-var?] [x]
      (symbol (apply str (take-while #{\?} (name x)))))

### Dispatch options

Dispatch is done through a pluggable system.
If the arity is 1, the `make-simple-dispatch-store` is used by default.
With a higher arity, `make-trie-dispatch-store` is used instead.

The simple dispatch store trie walk, minimizing the number of dispatch functions that need to be called.


## Bonus feature! Extended trampoline

The built-in `trampoline` function in Clojure will call any function that is returned to it.
This is problematic if you want to return a function!

The genera trampoline function solves this problem by only calling the function if it is annotated with `:genera.trampoline/bounce`.

This library provides a simple `bounce` helper function to attach that metadata.

I find that this method both makes it clearer why a function is being returned and makes it safer to use trampoline.

### Example trampoline usage

Trampolines tend to appear in fairly complex codebases where the stack depth becomes a limitation.
In clojuredocs, there is a nice simple state machine example.
Here, I've extended it and translated it for genera.trampoline.

In the `a->` transition function, `bounce` is used to wrap a simple function.
In the `b->` and `c->` functions, `bouncing` is used to create and wrap a function.
The two methods are equivalent.

The result is either the `success` or `failure` function, which is then called normally.

    (require '[genera :refer [bounce trampoline]])
  
    (defn success [x] (str "Success! It's " x))
    (defn failure [x] (str "Oh no! " x))
    
    (defn state-machine [cmds]
      (letfn [(a-> [[transition & rs]]
                (bounce
                  #(case transition
                     :a-b (b-> rs)
                     :a-c (c-> rs)
                     failure)))
              (b-> [[transition & rs]]
                (bouncing
                  (case transition
                    :b-a (a-> rs)
                    :b-c (c-> rs)
                    failure)))
              (c-> [[transition & rs]]
                (bouncing
                  (case transition
                    :c-a (a-> rs)
                    :c-b (c-> rs)
                    :final success
                    failure)))]
        (trampoline a-> cmds)))
    
    ((state-machine [:a-b :b-c :c-a :a-c :final]) :result)

    ;; => "Success! It's :result"

## License

Copyright © 2022 Darrick Wiebe

_EPLv1.0 is just the default for projects generated by `clj-new`: you are not_
_required to open source this project, nor are you required to use EPLv1.0!_
_Feel free to remove or change the `LICENSE` file and remove or update this_
_section of the `README.md` file!_

Distributed under the Eclipse Public License version 1.0.
