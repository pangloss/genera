(ns genera.core
  (:require [genera.trie :as trie]))

;; To make the predicate dispatch system cacheable, I would need to split up
;; generating a value from the argument and then evaluating whether that
;; resulting value matches. This would be useful for hierarchical matching like
;; what the isa? predicate in clojure does, but in many cases it would not be
;; useful. So the optimal set up is probably simple predicates or a system
;; that marked cacheable where it's broken up into a function that generates the
;; cacheable value and there's another function that evaluates whether it matches.
;; Overall, clojure has this type of dispatch nicely built already, so I don't
;; think it has a huge value here.


;; IDEA: you could be way smarter about dispatch. You could do analysis of the
;; predicates and do the dispatch in the most efficient order using the same
;; type of static pre-compilation that core.match uses. When functions are
;; added, the analysis should be redone to maximize efficiency.  Predicates
;; could also be marked as cacheable if they behave like types, enabling the
;; whole precomputation and caching mechanism, which could be autogenerated as
;; well, I think.

;; metadata

(defn make-generic-metadata [name arity store]
  {:name name
   :arity arity
   :store store
   :getter (store :get-handler)
   :default-getter (store :get-default-handler)})

(defn predicates-match? [preds args]
  (loop [[pred & preds] preds
         [arg & args] args]
    (if pred
      (when (pred arg)
        (recur preds args))
      true)))

(defn error-generic-procedure-handler [name]
  (fn [& args]
    (throw (ex-info "Inapplicable generic procedure" {:name name :args args}))))

;; Dispatcher implementations

(defn make-simple-dispatch-store [{:keys [default-handler]}]
  ;; This seems like it should be a defrecord or deftype with a protocol instead
  ;; of this handler setup.  But I'm going to do it this way then once it works
  ;; see if I can refactor it that way.
  ;; Each rule is a {:preds [...] :handler fn}. Probably shoud be a record.
  (let [rules (atom [])
        default-handler (atom default-handler)]
    (letfn [(get-handler [args]
              ;; scan through a list of rules and match if all predicates in a given rule match the args
              (some (fn [rule]
                      (when (predicates-match? (:preds rule) args)
                        (:handler rule)))
                    @rules))
            (add-handler! [applicability handler]
              ;; If the rule is already present, just change the handler. Otherwise add the rule.
              ;; Some appl. specs generate multiple applicabilities. For instance `any-arg`.
              ;; How is the rule applicability test set organized? I think they're just in the order they were added.
              (reset! rules
                      (reduce (fn [rules preds]
                                (loop [[rule & rules] rules
                                       done []]
                                  (cond (nil? rule)
                                        (conj done {:preds preds :handler handler})
                                        (= (:preds rule) preds)
                                        (into (conj done {:preds preds :handler handler}) rules)
                                        :else
                                        (recur rules (conj done rule)))))
                              @rules
                              applicability)))
            (get-default-handler []
              @default-handler)
            (set-default-handler! [handler]
              (reset! default-handler handler))]
      (fn [message]
        (case message
          :get-handler get-handler
          :add-handler! add-handler!
          :get-default-handler get-default-handler
          :set-default-handler! set-default-handler!
          :get-rules (fn [] @rules)
          (throw (ex-info "unknown message" {:message message})))))))

(defn make-trie-dispatch-store [{:keys [default-handler] :as opts}]
  (let [delegate (make-simple-dispatch-store opts)
        trie (atom (trie/make-trie))]
    (letfn [(get-handler [args]
              (trie/get-a-value @trie args))
            (add-handler! [applicability handler]
              ((delegate :add-handler!) applicability handler)
              (swap! trie
                     #(reduce (fn [trie path]
                                ;; should just be assoc-in...?
                                (trie/set-path-value trie path handler))
                              %
                              applicability)))]
      (fn [message]
        (case message
          :get-handler get-handler
          :add-handler! add-handler!
          (delegate message))))))

(defn get-generic-procedure-handler [metadata args]
  (or ((:getter metadata) args)
      ((:default-getter metadata))))

(defn generic-procedure-dispatch [metadata args]
  (let [handler (get-generic-procedure-handler metadata args)]
    (apply handler args)))

;; Standard predicate matcher patterns

(defn match-args [& preds]
  [preds])

(defn all-args [arity predicate]
  [(vec (repeat arity predicate))])

(defn any-arg
  ([arity predicate]
   (any-arg arity predicate (constantly true) (constantly true)))
  ([arity predicate base-predicate]
   (any-arg arity predicate base-predicate (constantly true)))
  ([arity predicate base-predicate replace?]
   (let [template (->> (if (sequential? base-predicate)
                         (cycle base-predicate)
                         (repeat arity base-predicate))
                       (take arity)
                       vec)]
     (distinct
      (if (zero? arity)
        []
        (loop [before []
               after (rest template)
               result []]
          (if (seq after)
            (let [to-replace (nth template (count before))]
              (recur (conj before to-replace)
                     (rest after)
                     (conj result (into (conj before (if (replace? to-replace (count before))
                                                       predicate
                                                       to-replace))
                                        after))))
            (conj result (conj before predicate)))))))))

;; Generic procedures

(defn generic-procedure-constructor [name arity default-handler]
  (assert (pos-int? arity))
  (let [dispatch-store-maker (if (= 1 arity)
                               make-simple-dispatch-store
                               make-trie-dispatch-store)]
    (let [default-handler (or default-handler
                              (error-generic-procedure-handler name))
          metadata (make-generic-metadata name arity (dispatch-store-maker
                                                      {:default-handler default-handler}))]
      (with-meta (fn the-generic-procedure [& args]
                   (generic-procedure-dispatch metadata args))
        metadata))))

(defn define-generic-procedure-handler [generic-procedure applicability handler]
  ((((meta generic-procedure) :store) :add-handler!) applicability handler))

(defn assign-handler! [procedure handler & preds]
  (define-generic-procedure-handler procedure (apply match-args preds) handler))

(defn generic-procedure-name [proc]
  ((meta proc) :name))

(defn generic-procedure-arity [proc]
  ((meta proc) :arity))

(defn generic-procedure-rules [proc]
  (((meta proc) :store) :get-rules))

(defn generic-procedure-handlers [proc]
  (map :handler (generic-procedure-rules proc)))
