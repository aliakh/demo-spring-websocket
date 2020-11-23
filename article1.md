# WebSockets with Spring, part 1: HTTP and WebSocket

## Introduction

The HTTP protocol is a _request-response_ protocol. That means that only a client can send HTTP requests to a server. A server can only service HTTP requests by sending back HTTP responses, but a server can not send unrequested HTTP responses to a client.

This is because HTTP was originally designed for request-response resources transfer in distributed hypermedia systems but not for simultaneous bi-directional communication. To overcome these architecture limitations are used several HTTP mechanisms (grouped under the unofficial name _Comet_) that are often complicated and inefficient.

The WebSocket protocol is designed to replace existing workaround HTTP mechanisms and provide an effective protocol for low-latency, simultaneous, bi-directional communication between browsers and servers over a single TCP connection.

>This article describes the relationships between WebSocket and HTTP/1.1.

## HTTP-based mechanisms

Because HTTP was not designed to support server-initiated messages, several mechanisms to achieve this have been developed, each with different benefits and drawbacks. 

### HTTP polling

During the _polling_ mechanism, a client sends periodic requests to a server, and the server responds immediately. If there is new data, the server returns it, otherwise the server returns an empty response. After receiving the response, the client waits for a while before sending another request. 

![HTTP polling](/images/HTTP_polling.png)

Polling can be efficient if we know the update period of the data on the server. Otherwise, the client may poll the server either too rarely (adding additional latency in transferring data from the server to the client) or too often (wasting server processing and network resources).

### HTTP long polling

During the _long polling_ mechanism, a client sends a request to a server and starts waiting for a response. The server does not respond until new data arrives or a timeout occurs. When new data becomes available, the server sends a response to the client. After receiving the response, the client immediately sends another request.

![HTTP long polling](/images/HTTP_long_polling.png)

Long polling reduces the use of server processing and network resources to receive data updates with low latency, especially where new data becomes available at irregular intervals. However, the server must keep track of multiple open requests. Also, long-running requests can time out, and the new requests must be sent periodically, even if the data is not updated.

### HTTP streaming

During the _streaming_ mechanism, a client sends a request to a server and keeps it open indefinitely. The server does not respond until new data arrives. When new data becomes available, the server sends it back to the client as a part of the response. The data sent by the server does not close the request. 

![HTTP streaming](/images/HTTP_streaming.png)

Streaming is based on the capability of the server to send several pieces of data in the same response, without closing the request. This mechanism significantly reduces the network latency because the client and the server do not need to send and receive new requests.

However, the client and server need to agree on how to interpret the response stream so that the client will know where one piece of data ends and another begins. Also, network intermediaries can disrupt streaming - they may buffer the response and cause latency or disconnect connections that are kept open for a long time.

### Server-Sent Events

Server-Sent Events (SSE) is a standardized streaming mechanism that has the network protocol and the [EventSource API](https://html.spec.whatwg.org/multipage/server-sent-events.html) for browsers. SSE defines a uni-directional UTF-8 encoded events stream from a server to a browser. Events have mandatory values and can have optional types and unique identifiers. In case of failure, SSE supports automatic client reconnection from the last received event.

An example of the SSE request:

```
GET /sse HTTP/1.1 
Host: server.com
Accept: text/event-stream
```

An example of the SSE response:

```
HTTP/1.1 200 OK 
Connection: keep-alive
Content-Type: text/event-stream
Transfer-Encoding: chunked

retry: 1000

data: A text message

data: {"message": "a JSON message"} 

event: text
data: A message of type 'text'

id: 1
event: text
data: A message of type 'text' with a unique identifier

:ping
```

>Server-Sent Events can send streaming data only from a server to a browser and supports only text data.

## WebSocket

### Prerequisites

WebSocket is designed to overcome the limitations of HTTP-based mechanisms (polling, long polling, streaming) in _full-duplex_ communication between browsers and servers:

>In _full-duplex_ communication, both parties can send and receive messages in both directions at the same time. 

>In _half-duplex_ communication, both parties can send and receive messages in both directions, but in one direction at a time.

HTTP allows _half-duplex_ communication between a browser and a server: a browser can either send requests to a server or receive responses from a server, but not both at the same time. To overcome these limitations, several Comet mechanisms use two simultaneous HTTP connections for upstream and downstream communication between a browser and a server that leads to additional complexity.

Here are the main design differences between HTTP and WebSocket:

*   HTTP is a text protocol, WebSocket is a binary protocol (binary protocols transfer fewer data over the network than text protocols)
*   HTTP has request and response headers, WebSocket messages can have a format suitable for specific applications (unnecessary metadata are not transmitted over the network)
*   HTTP is a half-duplex protocol, WebSocket is a full-duplex protocol (low-latency messages can be transmitted at the same time in both directions)

### Design

WebSocket is a protocol that allows simultaneous bi-directional transmission of text and binary messages between clients (mostly browsers) and servers over a single TCP connection. WebSocket can communicate over TCP on port 80 ("ws" scheme) or over TLS/TCP on port 443 ("wss" scheme).

![WebSocket](/images/WebSocket.png)

WebSocket is an independent TCP-based protocol distinguished from HTTP. However, it is designed to coexist with HTTP: 

*   WebSocket handshake is interpreted by HTTP servers as HTTP _Upgrade_ request
*   WebSocket shares the same 80 and 443 ports as HTTP and HTTPS
*   WebSocket supports HTTP network intermediaries (proxies, firewalls, routers, etc.)

WebSocket is designed to add support for TCP sockets with as little modifications as possible to browser-server communication, providing necessary security constraints of the Web. WebSocket adds just minimum functionality on top of TCP, nothing more than the following:

*   origin-based security model
*   conversion between IP addresses used in TCP to URLs used on the Web
*   message protocol on top of byte stream protocol
*   closing handshake

The WebSocket protocol is designed to be a simple protocol and to provide a foundation to build application subprotocols on top of it, similar to how the TCP protocol allows building application protocols (HTTP, FTP, SMTP, POP3, Telnet, etc.).

The WebSocket standard contains two parts: the WebSocket protocol standardized as [RFC 6455](https://tools.ietf.org/html/rfc6455) and the [WebSocket API](https://html.spec.whatwg.org/multipage/web-sockets.html).

### The WebSocket protocol

The WebSocket network protocol consists of two components: 

1. the opening handshake for negotiating the parameters of the WebSocket connection
2. the binary message framing for sending text and binary messages

#### Opening handshake

Before starting the exchange of messages, the client and server negotiate the parameters of the establishing connection. WebSocket reuses the existing HTTP _Upgrade_ mechanism with special _Sec-WebSocket-*_ headers to perform the connection negotiation.

>WebSocket _subprotocols_ are top-level protocols that provide additional functionality for applications (for example, the STOMP subprotocol provides the publish-subscribe messaging model). 

>WebSocket _extensions_ are a mechanism to modify message framing without affecting application protocols.  (for example, the _permessage-deflate_ extension compresses payload data by the LZ77 algorithm). 

An example of an HTTP to WebSocket upgrade request:

```
GET /socket HTTP/1.1
Host: server.com
Connection: Upgrade
Upgrade: websocket 
Origin: http://example.com
Sec-WebSocket-Version: 8, 13
Sec-WebSocket-Key: 7c0RT+Z1px24ypyYfnPNbw==
Sec-WebSocket-Protocol: v10.stomp, v11.stomp, v12.stomp
Sec-WebSocket-Extensions: permessage-deflate; client_max_window_bits
```

An example of an HTTP to WebSocket upgrade response:

```
HTTP/1.1 101 Switching Protocols 
Connection: Upgrade
Upgrade: websocket
Access-Control-Allow-Origin: http://example.com
Sec-WebSocket-Accept: O1a/o0MeFzoDgn+kCKR91UkYDO4=
Sec-WebSocket-Protocol: v12.stomp
Sec-WebSocket-Extensions: permessage-deflate;client_max_window_bits=15
```

The opening handshake consists of the following parts: _protocol upgrade, origin policies negotiation, protocol negotiation, subprotocol negotiation, extensions negotiation_.

To pass the _protocol upgrade_:

*   the client sends a request with the _Connection_ and _Upgrade_ headers
*   the server confirms the protocol upgrade with _101 Switching Protocols_ response line and the same _Connection_ and _Upgrade_ headers

To pass the _origin policies negotiation_:

*   the client sends the _Origin_ header (scheme, host name, port number)
*   the server confirms that the client from this origin is allowed to access the resource via the _Access-Control-Allow-Origin_ header

To pass the _protocol negotiation_:

*   the client sends the _Sec-WebSocket-Version_ (a list of protocol versions, 13 for RFC 6455) and _Sec-WebSocket-Key_ (an auto-generated key) headers
*   the server confirms the protocol by returning the _Sec-WebSocket-Accept_ header

>Equivalent Java code for calculating the _Sec-WebSocket-Accept_ header: 

```
Base64
       .getEncoder()
       .encodeToString(
               MessageDigest
                       .getInstance("SHA-1")
                       .digest((secWebSocketKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
                               .getBytes(StandardCharsets.UTF_8)));
```

To pass _subprotocol negotiation_:

*   the client sends a list of subprotocols via the _Sec-WebSocket-Protocol_ header
*   the server select one of the subprotocols via the _Sec-WebSocket-Protocol_ header (if the server does not support any subprotocol, then the connection is canceled)

To pass the _extensions negotiation_:

*   the client sends a list of extensions via the _Sec-WebSocket-Extensions_ header
*   the server confirms _one or more_ extensions via the _Sec-WebSocket-Extensions_ header (if the server does not support some extensions, then the connection proceeds without them)

After a successful handshake, the client and the server switch from text HTTP protocol to binary WebSocket message framing and can perform full-duplex communication.

#### Message framing

WebSocket uses a binary message framing: the sender splits each application _message_ into one or more _frames_, transports them across the network to the destination, reassembles them, and notifies the receiver once the entire message has been received. 

WebSocking framing has the following format:

1. FIN (1 bit) - the flag that indicates whether the frame is the final frame of a message
2. reserve (3 bits) - the reserve flags for extensions
3. operation code (4 bits) - the type of frame: data frames (text or binary) or control frames (connection close, ping/pong for connection liveness checks)
4. mask (1 bit) - the flag that indicates whether the payload data is masked (all frames sent from client to server are masked)
5. payload length (7 bits, or 7+16 bits, or 7+64 bits) - the variable-length payload length (if 0-125, then that is the payload length; if 126, then the following 2 bytes represent the payload length; if 127, then the following 8 bytes represent the payload length)
6. masking key (0 or 4 bytes) - the masking key contains a 32-bit value used to XOR the payload data
7. payload data (n bytes) - the payload data contains extension data (if extensions are used) concatenated with application data

In such binary message framing, the variable-length payload length field allows low framing overhead during exchanging as small as big messages. According to some sources, the WebSocket protocol compared with the HTTP protocol can provide about 500:1 reduction in traffic and 3:1 reduction in latency.

#### Closing handshake

Either party can initiate a closing handshake by sending a closing frame. On receiving such a frame, the other party sends a closing frame in response, if it has not already sent one. After sending the closing frame, a party does not send any further data. After receiving a closing frame, a party discards any further data received. Once a party has both sent and received a closing frame, that endpoint closes the WebSocket connection.

Besides closing the connection by a closing handshake, a WebSocket connection might be closed abruptly when another party goes away or the underlying TCP collection closes. Status codes in closing frames can identify the reason.

### The WebSocket API

The WebSocket API is the interface that a browser must implement to communicate with servers using the WebSocket protocol.

Before using the WebSocket API, it is necessary to make sure that the browser supports it.

```
if (window.WebSocket) {
    // WebSocket is supported
} else {
    // WebSocket is not supported
}
```

To establish a connection to the server, the API provides the _WebSocket_ constructor with a mandatory server URL and optional subprotocols. Once the connection is established, the _onopen_ event listener is called. After the connection, it is possible to read the _protocol_ and _extensions_ properties to determine the connection parameters selected by the server.

The API provides the _readyState_ property to determine the current state of the connection: whether the connection is established, has not yet been established, already closed, or is going through the closing handshake.

The API allows sending and receiving text and binary messages. Text messages are encoded in UTF-8 and use the _DOMString_ objects. Binary messages can use either _Blob_ objects (when messages are supposed to be immutable) or _ArrayBuffer_ objects (when messages may be modified). The _binaryType_ property specifies the type of binary objects being used by the connection.

The API provides the _send_ method to send messages. It is important, that this method is non-blocking: it enqueues the data to be transmitted to the server and returns immediately. The _bufferedAmount_ property returns the number of bytes that have been queued using the _send_ method but not yet transmitted to the network.

The API provides receiving messages in a non-blocking manner. Once a message is received, the _onmessage_ event listener is called. 

The API provides the _close_ method to close the connection. The method has an optional status _code_ and an optional human-readable _reason_. Once the connection is closed, the _onclose_ event listener is called.

Once an error occurs, the _onerror_ event listener is called. After any error, the connection is closed.

An example of WebSocket browser application:

```
const ws = new WebSocket('ws://server.com/socket'); 
ws.binaryType = "blob";

ws.onopen = function () { 
    // send binary messages
    ws.send(new Blob([new Uint8Array([0x48, 0x65, 0x6c, 0x6c, 0x6f, 0x21]).buffer])); 

    // send text messages
    ws.send("Hello!"); 
}

ws.onclose = function () {
    // handle disconnect
} 

ws.onmessage = function(msg) { 
    if (msg.data instanceof Blob) { 
        // receive binary messages
    } else {
        // receive text messages
    }
}

ws.onerror = function (error) {
    // handle errors
} 
```

>The WebSocket API exposes neither framing information nor ping/pong methods to applications.

## Examples

### Introduction

The Spring Framework provides support for WebSocket clients and servers in the _spring-websocket_ module.

The following example implements full-duplex WebSocket text communication between a server and clients. The server and the clients work according to the following algorithm:

*   the server sends a one-time message to the client
*   the server sends periodic messages to the client
*   the server receives messages from a client, logs them, and sends them back to the client
*   the client sends aperiodic messages to the server
*   the client receives messages from a server and logs them

The server is implemented as a Spring web application with Spring Web MVC framework to handle static web resources. One client is implemented as a JavaScript browser client and another client is implemented as a Java Spring console application.

### Java Spring server

Java Spring server consists of two parts: Spring WebSocket events handler and Spring WebSocket configuration.

Because the server uses text (not binary) messages, the events handler extends the existing _TextWebSocketHandler_ class as the required implementation of the _WebSocketHandler_ interface. The handler uses the _handleTextMessage_ callback method to receive messages from a client and the _sendMessage_ method to send messages back to the client.

Existing Spring WebSocket event handlers do not support broadcasting messages to many clients. To implement this manually, the _afterConnectionEstablished_ and _afterConnectionClosed_ methods maintain the thread-safe list of active clients. The _@Scheduled_ method broadcasts periodic messages to active clients with the same _sendMessage_ method.

```
public class ServerWebSocketHandler extends TextWebSocketHandler implements SubProtocolCapable {

   private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

   @Override
   public void afterConnectionEstablished(WebSocketSession session) throws Exception {
       logger.info("Server connection opened");
       sessions.add(session);

       TextMessage message = new TextMessage("one-time message from server");
       logger.info("Server sends: {}", message);
       session.sendMessage(message);
   }

   @Override
   public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
       logger.info("Server connection closed: {}", status);
       sessions.remove(session);
   }

   @Scheduled(fixedRate = 10000)
   void sendPeriodicMessages() throws IOException {
       for (WebSocketSession session : sessions) {
           if (session.isOpen()) {
               String broadcast = "server periodic message " + LocalTime.now();
               logger.info("Server sends: {}", broadcast);
               session.sendMessage(new TextMessage(broadcast));
           }
       }
   }

   @Override
   public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
       String request = message.getPayload();
       logger.info("Server received: {}", request);

       String response = String.format("response from server to '%s'", HtmlUtils.htmlEscape(request));
       logger.info("Server sends: {}", response);
       session.sendMessage(new TextMessage(response));
   }

   @Override
   public void handleTransportError(WebSocketSession session, Throwable exception) {
       logger.info("Server transport error: {}", exception.getMessage());
   }

   @Override
   public List<String> getSubProtocols() {
       return Collections.singletonList("subprotocol.demo.websocket");
   }
}
```

The following Spring configuration enables WebSocket support in the Spring server with the _@EnableWebSocket_ annotation. This configuration also registers the implemented WebSocket handler for the WebSocket endpoint.

```
@Configuration
@EnableWebSocket
public class ServerWebSocketConfig implements WebSocketConfigurer {

   @Override
   public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
       registry.addHandler(webSocketHandler(), "/websocket");
   }

   @Bean
   public WebSocketHandler webSocketHandler() {
       return new ServerWebSocketHandler();
   }
}
```

The server is a Spring Boot web application with Spring Web MVC framework to handle static web resources for the JavaScript browser client. However, Spring WebSocket support does not depend on Spring MVC and can be used with any Java Servlet framework.

```
@SpringBootApplication
@EnableScheduling
public class ServerWebSocketApplicaion {

   public static void main(String[] args) {
       SpringApplication.run(ServerWebSocketApplicaion.class, args);
   }
}
```

### JavaScript browser client

The JavaScript browser client uses the standard _WebSocket_ browser object. It is important, that the client uses the "ws" scheme to specify the server URL.

When a user clicks the 'Connect' button, the client uses the _WebSocket_ constructor (with the server URL and the subprotocol) to initiate a connection to the server. When the connection is established, the _WebSocket.onopen_ callback handler is called. 

When the user clicks the 'Disconnect' button, the client uses the _WebSocket.close_ method to initiate the close of the connection. When the connection is closed, the _WebSocket.onclose_ callback handler is called. 

```
let webSocket = null;

// 'Connect' button click handler
function connect() {
   webSocket = new WebSocket('ws://localhost:8080/websocket',
       'subprotocol.demo.websocket');

   webSocket.onopen = function () {
       log('Client connection opened');

       console.log('Subprotocol: ' + webSocket.protocol);
       console.log('Extensions: ' + webSocket.extensions);
   };

   webSocket.onmessage = function (event) {
       log('Client received: ' + event.data);
   };

   webSocket.onerror = function (event) {
       log('Client error: ' + event);
   };

   webSocket.onclose = function (event) {
       log('Client connection closed: ' + event.code);
   };
}

// 'Disconnect' button click handler
function disconnect() {
   if (webSocket != null) {
       webSocket.close();
       webSocket = null;
   }
}
```

When the user clicks the 'Send' button, the client uses the _WebSocket.send_ method to send a text message to the server.

```
// 'Send' button click handler
function send() {
   const message = $("#request").val();
   log('Client sends: ' + message);
   webSocket.send(message);
}
```

When the client receives a message, the _WebSocket.onmessage_ callback handler is called. Incoming messages are received and outgoing messages are transmitted independently of each other.

![WebSocket](/images/browser-websocket.png)

### Java Spring client

Java Spring client consists of two parts: Spring WebSocket events handler and Spring WebSocket configuration.

The client (as the server) extends the existing _TextWebSocketHandler_ class. The handler uses the _handleTextMessage_ callback method to receive messages from a server and the _sendMessage_ method to send messages to the server.

```
public class ClientWebSocketHandler extends TextWebSocketHandler {

   @Override
   public void afterConnectionEstablished(WebSocketSession session) throws Exception {
       logger.info("Client connection opened");

       TextMessage message = new TextMessage("one-time message from client");
       logger.info("Client sends: {}", message);
       session.sendMessage(message);
   }

   @Override
   public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
       logger.info("Client connection closed: {}", status);
   }

   @Override
   public void handleTextMessage(WebSocketSession session, TextMessage message) {
       logger.info("Client received: {}", message);
   }

   @Override
   public void handleTransportError(WebSocketSession session, Throwable exception) {
       logger.info("Client transport error: {}", exception.getMessage());
   }
}
```

The following Spring configuration enables WebSocket support in the Spring client. The configuration defines a _WebSocketConnectionManager_ object that uses two Spring beans:

*   the _StandardWebSocketClient_ class (from the _tomcat-embed-websocket_ dependency) as an implementation of the _WebSocketClient_ interface - to connect to the WebSocket server
*   the implemented _WebSocketHandler_ class - to handle WebSocket events during communication

```
@Configuration
public class ClientWebSocketConfig {

   @Bean
   public WebSocketConnectionManager webSocketConnectionManager() {
       WebSocketConnectionManager manager = new WebSocketConnectionManager(
               webSocketClient(),
               webSocketHandler(),
               "ws://localhost:8080/websocket"
       );
       manager.setAutoStartup(true);
       return manager;
   }

   @Bean
   public WebSocketClient webSocketClient() {
       return new StandardWebSocketClient();
   }

   @Bean
   public WebSocketHandler webSocketHandler() {
       return new ClientWebSocketHandler();
   }
}
```

The client is a console Spring Boot application without Spring Web MVC.

```
@SpringBootApplication
public class ClientWebSocketApplication {

   public static void main(String[] args) {
       new SpringApplicationBuilder(ClientWebSocketApplication.class)
               .web(WebApplicationType.NONE)
               .run(args);
   }
}
```

## Conclusion

WebSocket is another communication technology for the Web designed to solve a specific range of problems where the capabilities of HTTP-based solutions are limited. But like any other technology, WebSockets is not a "silver bullet" and it has its advantages and drawbacks.

It is better to use WebSocket when:

*   it is necessary to get _updates_ of a resource with the lowest possible latency
*   high-frequency messages with small payloads are used 
*   the messaging communication model is used - when messages are sent by either party independently of each other
*   in enterprise applications when browsers and networks infrastructure is under control

It is better to use HTTP when:

*   it is necessary to get the _current state_ of a resource
*   it is possible to benefit from idempotency, safety, cacheability HTTP requests
*   the request-response communication model is used - when requests are always acknowledged by responses
*   it is expensive to modify the existing hardware and software infrastructure to support WebSocket

Complete code examples are available in the [GitHub repository](https://github.com/aliakh/demo-spring-websocket/tree/master/websocket-server).
