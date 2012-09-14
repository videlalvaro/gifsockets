(ns gifsockets.server
  "Functions for creating a threaded TCP server."
  (:require [clojure.java.io :as io])
  (:import [java.net InetAddress ServerSocket Socket SocketException]))

(defn- server-socket [server]
  (ServerSocket.
   (:port server)
   (:backlog server)
   (InetAddress/getByName (:host server))))

(defn tcp-server
  "Create a new TCP server. Takes the following keyword arguments:
    :host    - the host to bind to (defaults to 127.0.0.1)
    :port    - the port to bind to
    :handler - a function to handle incoming connections, expects a socket as
               an argument
    :backlog - the maximum backlog of connections to keep (defaults to 50)"
  [& {:as options}]
  {:pre [(:port options)
         (:handler options)]}
  (merge
   {:host "127.0.0.1"
    :backlog 50
    :socket (atom nil)
    :connections (atom #{})}
   options))

(defn close-socket [server socket]
  (swap! (:connections server) disj socket)
  (when-not (.isClosed socket)
    (.close socket)))

(defn- open-server-socket [server]
  (reset! (:socket server)
          (server-socket server)))

(defn- accept-connection
  [{:keys [handler connections socket] :as server}]
  (let [conn (.accept @socket)]
    (swap! connections conj conn)
    (future
      (try (handler conn)
           (finally (close-socket server conn))))))

(defn- accept-connection2
  [{:keys [handler connections socket] :as server}]
  (let [conn (.accept @socket)]
    (swap! connections conj conn)
    (future
      (try (handler conn)))))

(defn running?
  "True if the server is running."
  [server]
  (if-let [socket @(:socket server)]
    (not (.isClosed socket))))

(defn start
  "Start a TCP server going."
  [server]
  (open-server-socket server)
  (future
    (while (running? server)
      (try
        (accept-connection server)
        (catch SocketException _)))))

(defn start2
  "Start a TCP server going."
  [server]
  (open-server-socket server)
  (future
    (while (running? server)
      (try
        (accept-connection2 server)
        (catch SocketException _)))))

(defn stop
  "Stop the TCP server and close all open connections."
  [server]
  (doseq [socket @(:connections server)]
    (close-socket server socket))
  (.close @(:socket server)))

(defn wrap-streams
  "Wrap a handler so that it expects an InputStream and an OutputStream
  as arguments, rather than a raw Socket."
  [handler]
  (fn [socket]
    (with-open [input  (.getInputStream socket)
                output (.getOutputStream socket)]
      (handler input output))))

(defn wrap-io
  "Wrap a handler so that it expects a Reader and Writer as arguments, rather
  than a raw Socket."
  [handler]
  (wrap-streams
   (fn [input output]
     (with-open [reader (io/reader input)
                 writer (io/writer output)]
              (handler reader writer)))))