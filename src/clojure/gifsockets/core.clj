(ns gifsockets.core
  (:import java.awt.image.BufferedImage
           java.awt.Graphics2D
           [java.io File IOException ByteArrayOutputStream]
           javax.imageio.ImageIO
           AnimatedGifEncoder
           java.net.ServerSocket))

(defn save-to-disc [img format path]
    (ImageIO/write img format (File. path)))

(defn write-text [text w h x y]
  (let [bufferedImage (BufferedImage. w h BufferedImage/TYPE_INT_ARGB)
        gd2 (.createGraphics bufferedImage)]
    (.drawString gd2 text x y)
    bufferedImage
    ))

(defn text-to-file [text w h x y]
  (save-to-disc (write-text text w h x y) "jpg" "/tmp/text.jpg"))

(defn create-gif [output]
  (let [e (AnimatedGifEncoder.)]
    (.start e output)
    (.setDelay e 1000)
    e))

(defn add-frame [gif text w h x y]
  (.addFrame gif (write-text text w h x y)))

(defn start-server [port]
  (let [os (.getOutputStream (.accept (ServerSocket. port)))
        e (create-gif os)]
    ;; see how to destroy the writer without closing the buffer
    e))

(defn get-encoder []
  (create-gif (ByteArrayOutputStream.)))

(defn add-message [encoder message]
  "adds a message positioned at (20,20) in a 300x50 gif image"
  (do
    (add-frame encoder message 300 50 20 20)
    (add-frame encoder message 300 50 20 20)))

(defn flush-encoder [encoder]
  (.outFlush encoder))

(defn get-last-frame [encoder]
  (.getFrameByteArray encoder))

(defn gif-handler [conn]
    (def client conn))