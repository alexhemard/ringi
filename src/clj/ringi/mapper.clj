(ns ringi.mapper)

(defmacro ^{:private true} assert-args
  [& pairs]
  `(do (when-not ~(first pairs)
         (throw (IllegalArgumentException.
                  (str (first ~'&form) " requires " ~(second pairs) " in " ~'*ns* ":" (:line (meta ~'&form))))))
     ~(let [more (nnext pairs)]
        (when more
          (list* `assert-args more)))))

(defmacro defmap
  [name args mappings]
  (assert-args
   (vector? mappings) "a vector for its mapping"
   (even? (count mappings))) "an even number of forms in mapping vector"
  `(defn ~name ~args (build-mapping* ~@args ~mappings)))

(defn build-mapping* [m mappings]
  (loop [result (array-map)
         mappings (partition 2 mappings)]
    (if-not (empty? mappings)
      (let [[name options] (first mappings)
            {:keys [fn cardinality from allow-nil] :or {fn identity from name}} options
            value (let [val (from m)
                        fn' #(if % (fn %))]
                    (if (= cardinality :many)
                      (mapv fn' val)
                      (fn' val)))]
        (recur
         (if-not (or (nil? value)
                     allow-nil)
           (assoc result name value)
           result)
         (rest mappings)))
      result)))
