(ns ringi.mapper)

(defn build-mapping* [m mappings]
  (loop [result (array-map)
         mappings (partition 2 mappings)]
    (if-not (empty? mappings)
      (let [[name options] (first mappings)
            options (if (keyword? options) {:from options} options)
            {:keys [fn cardinality from allow-nil] :or {fn identity from name}} options
            value (let [val (get m from)
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

