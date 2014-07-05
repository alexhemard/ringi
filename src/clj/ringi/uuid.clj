(ns ringi.uuid
  (:import [java.util UUID]
           [java.nio ByteBuffer]
           [org.apache.commons.codec.binary Base64]))

(defn uuid->b64 [^UUID uuid]
  (let [ba (-> (ByteBuffer/wrap (make-array Byte/TYPE 16))
               (.putLong (.getMostSignificantBits uuid))
               (.putLong (.getLeastSignificantBits uuid))
               (.array))]
    (String. (Base64/encodeBase64URLSafeString ba))))

(defn b64->uuid [^String b64uuid]
  (let [bb (ByteBuffer/wrap (Base64/decodeBase64 b64uuid))]
    (UUID. (.getLong bb) (.getLong bb))))
