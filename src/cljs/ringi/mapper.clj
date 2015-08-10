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


