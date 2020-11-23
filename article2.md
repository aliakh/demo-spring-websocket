# WebSockets with Spring, part 2: WebSocket with SockJS fallback

## Introduction

According to some sources, the WebSocket API is currently (2020) implemented in the [most common browsers](https://caniuse.com/websockets):

![https://caniuse.com/#feat=websockets](/images/caniuse.com-websockets.png)

But in addition to outdated browsers (mainly on mobile platforms), there are network intermediaries (proxies, firewalls, routers, etc.) that can prevent WebSocket communication. These intermediaries may not pass HTTP to WebSocket protocol upgrade or may close long-lived connections. 

One possible solution to this problem is WebSocket emulation - first trying to use WebSocket and then falling back to HTTP-based techniques that expose the same API.

## SockJS

### Design

SockJS is one of the existing WebSocket browser fallbacks. SockJS includes a protocol, a JavaScript browser client, and Node.js, Erlang, Ruby servers. There are also third-party SockJS clients and servers for different programming languages and platforms. SockJS was designed to emulate the WebSocket API as close as possible to provide the ability to use WebSocket subprotocols on top of it. 

SockJS has the following design considerations:

*   simple browser API as close to WebSocket API as possible
*   only JavaScript, no Flash/Java plugins
*   support of scaling and load balancing techniques
*   support of cross-domain communication and cookies
*   fast connection establishment
*   _graceful degradation_ in case of outdated browsers and restrictive proxies

>_Gradceful degradation_ in web development is a design principle that focuses on trying to use the best features that work in newer browsers but falls back on other features that, while not as good, still provides essential functionality in older browsers.

SockJS supports a wide range of browser versions and has at least one long polling and streaming transport for each of them. It is important, that streaming transports support _cross-domain communications_ and _cookies_. _Cross-domain communication_ is required when SockJS is hosted on a different server than the main web application. _Cookies_ are essential for authentication in web applications and cookie-based sticky sessions in load balancers. 

#### SockJS and the same-origin policy

The _same-origin policy_ is a critical concept in web application security. An _origin_ is a combination of scheme, host name, and port number. This policy describes how a document or script loaded from one origin can interact with a resource from another origin. Some resources such as images, sounds, videos, stylesheets, fonts, iframes can be accessed across origins. Scripts can be loaded across origins as well, but some of their actions (such as cross-origin Ajax calls) are disabled by default. 

Some of the techniques related to cross-domain communication used in SockJS are briefly described below ( their source codes are available in the provided GitHub repository).

##### JSON with Padding (JSONP)

This outdated technique is based on the ability of scripts to be loaded from other origins. According to it, a script element is inserted into a document at run-time and the script body is loaded dynamically from a server. The server returns a JSON that is wrapped in a function that is already defined in the JavaScript environment. When the script is loaded, the function is executed with the JSON argument. This method is vulnerable because the server (if compromised) can execute any JavaScript on the client.

##### The iframe element

A page from one origin can be loaded into an iframe on a page from another origin. However, some servers may prevent their pages from being included in iframes. The _X-Frame-Options_ response header can be sent by the server to indicate if the browser is allowed to load the page in an iframe. This header can have the following values:

*   _DENY_ - the page cannot be loaded in a frame, regardless of the site attempting to do so
*   _SAMEORIGIN_ - the page can only be loaded in a frame on the same origin as the page itself
*   _ALLOW-FROM origin_ - the page can be loaded in a frame only on the specified _origin_

It is important, that a script inside an iframe is not allowed to access or modify a document of its parent window and vice-versa unless both have the same origin. 

##### Cross-Origin Resource Sharing (CORS)

CORS is a standard that uses special _Access-Control-Allow-*_ headers to specify policies on how pages running at one origin can provide access to their resources to pages from other origins. 

For the cross-origin requests that can only read data (the GET and HEAD methods or the POST method with certain content types; all the methods without custom headers), a browser uses a "simple" request. The browser sends a request with the _Origin_ request header and a server responds with: 

*   the _Access-Control-Allow-Origin_ header with this origin (if requests from this origin are allowed) 
*   the _Access-Control-Allow-Origin_ header with a wildcard * (if requests from all origins are allowed)
*   an error (if requests from this origin are forbidden)

For the cross-origin requests that can modify data, the browser first sends the "preflight" request by the OPTIONS method to determine if the actual request is allowed to send. If the server responds with the appropriate _Access-Control-Allow-Origin_ response header, the browser sends the actual request. During the "preflight" browser can also identify by using special _Access-Control-Allow-*_ headers, if it is allowed to use specific HTTP methods, custom HTTP headers, user credentials (cookies or HTTP authentication).

##### Cross-document messaging

Cross-document messaging is a standard that allows different browser windows (iframes, pop-ups, tabs) to communicate with each other, even if they are from different origins. The _Window_ browser object has the _postMessage_ method to send messages to another window and the _onmessage_ event handler to receive messages from it.

This standard allows implementing a cross-origin communication technique that is known as _iframe via postMessage_. A page from a "local" origin loads in its iframe a page from a "remote" origin. The page in the iframe has a script loaded from the same "remote" origin that can therefore make calls to the server. So the script from the "local" origin communicates across origins with the script in the iframe with the _postMessage_ and _onmessage_ methods, and the script from the iframe makes the same-origin Ajax calls to the server.

#### SockJS and load balancers

SockJS supports sticky sessions in load balancers. With sticky sessions, a load balancer identifies a user and routes all of the requests from this user to a specific server. Among other methods, load balancers can use application cookies for this (for example, _JSESSIONID_ cookie that uses Java Servlet containers for an HTTP session).

Some load balancers do not support WebSocket. In this case, it is necessary to exclude WebSocket from the list of available SockJS transports.

### SockJS transports

SockJS transports fall into three categories: native WebSocket, HTTP streaming, HTTP long polling. 

#### WebSocket transport

_WebSocket_ is the transport with the best latency and throughput, and it has built-in support for cross-domain communication. 

>SockJS exists precisely because some browsers or network intermediaries still do not support WebSocket.

![SockJS over WebSocket](/images/browser-sockjs-websocket.png)

#### Streaming transports

SockJS streaming transports are based on HTTP 1.1 _chunked transfer encoding_ (the _Transfer-Encoding: chunked_ response header) that allows the browser to receive a single response from the server in many parts. 

Every browser supports a different set of streaming transports and they usually do not support cross-domain communication. SockJS overcomes that limitation by using an iframe and communicating with it using cross-document messaging (the technique is known as _iframe via postMessage_).

SockJS has the following streaming transports:

*   _xhr-streaming_ - the transport using _XMLHttpRequest_ object via streaming capability
*   _xdr-streaming_ - the transport using _XDomainRequest_ object via streaming capability
*   _eventsource_ - the transport using _EventSource_ object (Server-Sent Events)
*   _iframe-eventsource_ - the transport using _EventSource_ object (Server-Sent Events) from an _iframe via postMessage_
*   _htmlfile_ - the transport using ActiveXObject _HtmlFile_ object 
*   _iframe-htmlfile_ - the transport using ActiveXObject _HtmlFile_ object from an _iframe via postMessage_

>_XMLHttpRequest_ (XHR) is a browser interface to make Ajax calls.

>_XDomainRequest_ (XDR) is a deprecated browser interface in Internet Explorer to make Ajax calls.

##### XhrStreaming transport

_XhrStreaming_ transport is an example of streaming transports. This transport uses (as all Comet techniques) two simultaneous half-duplex HTTP connections to emulate a full-duplex WebSocket connection. 

The first HTTP connection sends each message from the browser to the server in a separate request. The second HTTP connection sends all messages from the server to the browser using the same request (response has the _Transfer-Encoding: chunked_ header). In a browser, the _XMLHttpRequest_ object process the partial responses using its streaming capability (the technique is known as _readyState=3_).

Equivalen example of using the _XMLHttpRequest_ object for streaming:

```
const xhr = new XMLHttpRequest();
xhr.open('POST', '/xhr_streaming');
xhr.seenBytes = 0;

xhr.onreadystatechange = function() {
    if (xhr.readyState == 3) {
        const data = xhr.response.substr(xhr.seenBytes);
        // new data is received
        xhr.seenBytes = xhr.responseText.length;
    }
};

xhr.send();
```

![SockJS over xhr-streaming](/images/browser-sockjs-xhr-streaming.png)

#### Polling transports

SockJS supports a few polling transports for outdated browsers. SockJS uses slow and outdated JSONP transport when nothing else works.

SockJS has the following polling transports:

*   _xhr-polling_ - transport using _XMLHttpRequest_ object
*   _xdr-polling_ - transport using _XDomainRequest_ object
*   _iframe-xhr-polling_ - transport using _XMLHttpRequest_ object from an _iframe via postMessage_
*   _jsonp-polling_ - transport using _JSONP_ technique

##### XhrPolling transport

_XhrPolling_ transport is an example of long polling transports. This transport also uses two simultaneous half-duplex HTTP connections to emulate a full-duplex WebSocket connection. 

The first HTTP connection sends each message from the browser to the server in a separate request. The second HTTP connection sends each message from the server to the browser using a separate request as well.

Equivalen example of using the _XMLHttpRequest_ object for long polling:

```
const xhr = new XMLHttpRequest();
xhr.open('POST','/xhr);
xhr.timeout = 60000;

xhr.onreadystatechange = function() {
    if (xhr.readyState == 4) {
        // response is received
        const data = xhr.response;
    }
};

xhr.ontimeout = function () { 
    // timeout has happened   
};

xhr.send(); 
```

![SockJS over xhr-polling](/images/browser-sockjs-xhr-polling.png)

### SockJS protocol

The SockJS network protocol consists of two components: 

1. the opening handshake for identifying the parameters of the SockJS session
2. the session for full-duplex communication

Before starting a session, a SockJS client sends a GET request to the _/info_ server URL to obtain basic information from the server. The server responds with the following properties:

*   _websocket_ - the property to specify whether the server needs WebSocket transport
*   _cookie_needed_ - the property to specify whether the server needs cookie support
*   _origins_ - the list of allowed origins
*   _entropy_ - the source of entropy for the random number generator

After the handshake, the client decides which transport to use. If it is possible, the client selects WebSocket. If not, in most browsers there is at least one HTTP streaming transport. Otherwise, the client uses some HTTP long polling transport.

During a SockJS session, the client sends requests to the _/server/session/transport_ server URLs, where:

*   _server_ - the identifier of a server in a cluster
*   _session_ - the identifier of a SockJS session
*   _transport_ - the transport type

A SockJS client accepts the following frames:

*   "o" - open frame is sent by the server every time a new session is established
*   "h" - heartbeat frame is periodically sent (if there is no message flow) by the server to prevent load balancers from closing connection by timeout
*   "a" - messages frame is an array of JSON-encoded messages (for example, _a["Hello!"]_)
*   "c" - close frame is sent by the server to close the session; close frame contains a code and a string explaining a reason for closure (for example, _c[3000, "Go away!"_])

A SockJS server does not define any framing. All incoming data is accepted as incoming messages, either single JSON-encoded messages or an array of JSON-encoded messages (depending on the transport).

## Examples

### Introduction

The Spring Framework provides support for WebSocket/SockJS clients and servers in the _spring-websocket_ module.

The following example implements full-duplex WebSocket text communication with SockJS fallback between a server and clients. The server and the clients work according to the following algorithm:

*   the server sends a one-time message to the client
*   the server sends periodic messages to the client
*   the server receives messages from a client, logs them, and sends them back to the client
*   the client sends aperiodic messages to the server
*   the client receives messages from a server and logs them

The server is implemented as a Spring web application with Spring Web MVC framework to handle static web resources. One client is implemented as a JavaScript browser client and another client is implemented as a Java Spring console application.

### Java Spring server

Java Spring server consists of two parts: Spring WebSocket events handler and Spring WebSocket/SockJS configuration.

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

>We can notice, that the Spring WebSocket events handlers for the servers with plain WebSocket and with WebSocket with SockJS fallback are the same.

The following Spring configuration enables WebSocket support in the Spring server with the _@EnableWebSocket_ annotation. This configuration also registers the implemented WebSocket handler for the WebSocket endpoint with the SockJS fallback.

```
@Configuration
@EnableWebSocket
public class ServerWebSocketSockJsConfig implements WebSocketConfigurer {

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
     registry.addHandler(webSocketHandler(), "/websocket-sockjs")
           .setAllowedOrigins("*")
           .withSockJS()
           .setWebSocketEnabled(true)
           .setHeartbeatTime(25000)
           .setDisconnectDelay(5000)
           .setClientLibraryUrl("/webjars/sockjs-client/1.1.2/sockjs.js")
           .setSessionCookieNeeded(false);
  }

  @Bean
  public WebSocketHandler webSocketHandler() {
     return new ServerWebSocketHandler();
  }
}
```

This configuration demonstrates some of the configuration properties available for a Spring SockJS server:

*   _allowedOrigins_ - this property can be used to specify allowed origins for CORS
*   _webSocketEnabled_ - this property can be used to disable the WebSocket transport if the load balancer does not support WebSocket
*   _heartbeatTime_ - this property can be used to specify the period with which server (if there is no message flow) sends heartbeat frames to the client to keep the connection from closing
*   _disconnectDelay_ - this property can be used to specify a timeout before closing an expired session
*   _clientLibraryUrl_ - this property can be used to specify the URL of the SockJS JavaScript client library for the iframe-based transports
*   _sessionCookieNeeded_ - this property can be used to specify whether the server needs cookies for load balancing or in Java Servlet containers to use HTTP session

The server is a Spring Boot web application with Spring Web MVC framework to handle static web resources for the JavaScript browser client. However, Spring WebSocket support does not depend on Spring MVC and can be used with any Java Servlet framework.

```
@SpringBootApplication
@EnableScheduling
public class ServerWebSocketSockJsApplicaion {

   public static void main(String[] args) {
       SpringApplication.run(ServerWebSocketSockJsApplicaion.class, args);
   }
}
```

### JavaScript browser client

The JavaScript browser client uses the _SockJS_ object from the [SockJS](https://github.com/sockjs/sockjs-client) library. It is important, that the client uses the "http" scheme (not the "ws" scheme) to specify the server URL.

When a user clicks the 'Connect' button, the client uses the _SockJS_ constructor (with the server URL, the subprotocol, and the selected SockJS transports) to initiate a connection to the server. When the connection is established, the _SockJS.onopen_ callback handler is called. 

When the user clicks the 'Disconnect' button, the client uses the _SockJS.close_ method to initiate the close of the connection. When the connection is closed, the _SockJS.onclose_ callback handler is called.

```
let sockJS = null;

// 'Connect' button click handler
function connect() {
   const option = $("#transports").find('option:selected').val();
   const transports = (option === 'all') ? [] : [option];

   sockJS = new SockJS('http://localhost:8080/websocket-sockjs',
       'subprotocol.demo.websocket', {debug: true, transports: transports});

   sockJS.onopen = function () {
       log('Client connection opened');

       console.log('Subprotocol: ' + sockJS.protocol);
       console.log('Extensions: ' + sockJS.extensions);
   };

   sockJS.onmessage = function (event) {
       log('Client received: ' + event.data);
   };

   sockJS.onerror = function (event) {
       log('Client error: ' + event);
   };

   sockJS.onclose = function (event) {
       log('Client connection closed: ' + event.code);
   };
}

// 'Disconnect' button click handler
function disconnect() {
   if (sockJS != null) {
       sockJS.close();
       sockJS = null;
   }
}
```

When the user clicks the 'Send' button, the client uses the _SockJS.send_ method to send a text message to the server.

```
// 'Send' button click handler
function send() {
   var message = $("#request").val();
   log('Client sends: ' + message);
   sockJS.send(message);
}
```

When the client receives a message, the _SockJS.onmessage_ callback handler is called. Incoming messages are received and outgoing messages are transmitted independently of each other.

>We can notice, that the JavaScript browser client for the clients with plain WebSocket and with WebSocket with SockJS fallback are very similar (they differ only in the creation of the communicating object).

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

>We can notice, that the Spring WebSocket events handlers for the clients (as for the servers) with plain WebSocket and with WebSocket with SockJS fallback are the same.

The following Spring configuration enables WebSocket support in the Spring client. The configuration defines a _WebSocketConnectionManager_ object, that uses two Spring beans: 

*   the _SockJsClient_ class (from the _spring-websocket_ dependency) as an implementation of the _WebSocketClient_ interface - to connect to the WebSocket/SockJS server
*   the implemented _WebSocketHandler_ - to handle WebSocket events during communication

The _SockJsClient_ object uses two transports: 

*   the _WebSocketTransport_ object, which supports SockJS _WebSocket_ transport
*   the _RestTemplateXhrTransport_ object, which supports SockJS _XhrStreaming_ and _XhrPolling_ transports

```
@Configuration
public class ClientWebSocketSockJsConfig {

   @Bean
   public WebSocketConnectionManager webSocketConnectionManager() {
       WebSocketConnectionManager manager = new WebSocketConnectionManager(
               webSocketClient(),
               webSocketHandler(),
               "http://localhost:8080/websocket-sockjs"
       );
       manager.setAutoStartup(true);
       return manager;
   }

   @Bean
   public WebSocketClient webSocketClient() {
       List<Transport> transports = new ArrayList<>();
       transports.add(new WebSocketTransport(new StandardWebSocketClient()));
       transports.add(new RestTemplateXhrTransport());
       return new SockJsClient(transports);
   }

   @Bean
   public WebSocketHandler webSocketHandler() {
       return new ClientWebSocketHandler();
   }
}
```

>We can notice, that the Spring WebSocket configuration with plain WebSocket and with WebSocket with SockJS fallback are very similar (they differ only in the used implementation of the _WebSocketClient_ interface).

The client is a console Spring Boot application without Spring Web MVC.

```
@SpringBootApplication
public class ClientWebSocketSockJsApplication {

   public static void main(String[] args) {
       new SpringApplicationBuilder(ClientWebSocketSockJsApplication.class)
               .web(WebApplicationType.NONE)
               .run(args);
   }
}
```

## Conclusion

There are two strategies to deal with the absence of WebSocket support in browsers and network infrastructure: _emulation_ and _extensions_. The first strategy (SockJS follows it) is to emulate the WebSocket API as close as possible. The second strategy (most of the other fallbacks follow it) is to build a top-level API and use WebSocket as one of the transports along with Flash/Java plugins or Comet techniques. 

Emulation strategy does not provide any additional API on top of the WebSocket protocol and might require additional development. However, this strategy may be beneficial in the long term when all browsers will eventually get WebSocket support, and fallbacks are no longer needed.

Many commercial operators provide their solutions that include WebSocket as one of the protocols: Kaazing WebSocket Gateway, PubNub, Pusher, Ably, etc. Although all these solutions are non-compliant and proprietary, using them can be a reasonable decision to get a business solution right now and do not deal with the Web development that requires a lot of time and expertise.

Complete code examples are available in the [GitHub repository](https://github.com/aliakh/demo-spring-websocket/tree/master/websocket-sockjs-server).
