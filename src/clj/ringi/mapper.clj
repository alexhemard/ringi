(ns ringi.mapper)

(defn one
  [name & {:keys [from fn] :as options}]
  [name (into {:arity :one} options)])

(defn many
  [name & {:keys [from fn] :as options}]
  [name (into {:arity :many} options)])

(defmacro defmap
  [name & fields]
  `(defn ~name [in#] (build-mapping* in# [~@fields])))

(defn build-mapping* [m fields]
  (loop [result (array-map)
         fields (reverse fields)]
    (if-not (empty? fields)
      (let [[name options] (first fields)
            {:keys [fn arity from allow-nil] :or {fn identity from name}} options
            value (let [val (from m)
                        fn' #(if % (fn %))]
                    (if (= arity :many)
                      (mapv fn' val)
                      (fn' val)))]
        (recur
         (if-not (or (nil? value)
                     allow-nil)
           (assoc result name value)
           result)
         (rest fields)))
      result)))
