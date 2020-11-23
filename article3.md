# WebSockets with Spring, part 3: STOMP over WebSocket

## Introduction

The WebSocket protocol is designed to overcome the architecture limitations of HTTP-based solutions in simultaneous bi-directional communication. Most importantly, WebSocket has another communication model (simultaneous bi-directional messaging) than HTTP (request-response).

WebSocket works over TCP that allows transmitting of two-way streams of _bytes_. WebSocket provides thin functionality on top of TCP that allows transmitting binary and text _messages_ providing necessary security constraints of the Web. But WebSocket does not specify the format of such messages.

WebSocket is intentionally designed to be as simple as possible. To avoid additional _protocol_ complexity, clients and servers are intended to use _subprotocols_ on top of WebSocket. STOPM is one such application subprotocol that can work over WebSocket to exchange messages between clients via intermediate servers (message brokers).

## STOMP

### Design

STOMP (Simple/Streaming Text Oriented Message Protocol) is an interoperable text-based protocol for messaging between clients via message brokers.

STOMP is _a simple protocol_ because it implements only a small number of the most commonly used messaging operations of message brokers.

STOMP is _a streaming protocol_ because it can work over any reliable bi-directional streaming network protocol (TCP, WebSocket, Telnet, etc.).

STOMP is _a text protocol_ because clients and message brokers exchange text frames that contain a mandatory command, optional headers, and an optional body (the body is separated from headers by a blank line).

```
COMMAND
header1:value1
Header2:value2

body
```

STOMP is _a messaging protocol_ because clients can produce messages (send messages to a broker destination) and consume them (subscribe to and receive messages from a broker destination).

STOMP is _an interoperable protocol_ because it can work with multiple message brokers (ActiveMQ, RabbitMQ, HornetQ, OpenMQ, etc.) and clients written in many languages and platforms.

### Сonnecting clients to a broker

![STOMP connecting](/images/STOMP_connecting.png)

#### Connecting

To connect to a broker, a client sends a CONNECT frame with two mandatory headers:

*   _accept-version_ - the versions of the STOMP protocol the client supports
*   _host_ - the name of a virtual host that the client wishes to connect to

To accent the connection, the broker sends to the client a CONNECTED frame with the mandatory header: 

*   _version_ - the version of the STOMP protocol the session will be using

#### Disconnecting

A client can disconnect from a broker at any time by closing the socket, but there is no guarantee that the previously sent frames have been received by the broker. To disconnect properly, where the client is assured that all previous frames have been received by the broker, the client must:

1. send a DISCONNECT frame with a _receipt_ header
2. receive a RECEIPT frame
3. close the socket

### Sending messages from clients to a broker

![STOMP sending](/images/STOMP_sending.png)

To send a message to a destination, a client sends a SEND frame with the mandatory header: 

*   _destination_ - the destination to which the client wants to send

If the SEND frame has a body, it must include the _content-length_ and _content-type_ headers.

### Subscribing clients to messages from a broker

![STOMP subscribing](/images/STOMP_subscribing.png)

#### Subscribing

To subscribe to a destination a client sends a SUBSCRIBE frame with two mandatory headers: 

*   _destination_ - the destination to which the client wants to subscribe
*   _id_ - the unique identifier of the subscription

#### Messaging

To transmit messages from subscriptions to the client, the server sends a MESSAGE frame with three mandatory headers: 

*   _destination_ - the destination the message was sent to
*   _subscription_ - the identifier of the subscription that is receiving the message
*   _message-id_ - the unique identifier for that message

#### Unsubscribing

To remove an existing subscription, the client sends an UNSUBSCRIBE frame with the mandatory header:

*   _id_ - the unique identifier of the subscription

### Acknowledgment

To avoid lost or duplicated frames, if a client and a broker are parts of a distributed system, it is necessary to use frames acknowledgment.

#### Client messages acknowledgment

![STOMP client acknowledgment](/images/STOMP_client_acknowledgment.png)

The SUBSCRIBE frame may contain the optional _ack_ header that controls the message acknowledgment mode: _auto_ (by default), _client_, _client-individual_.

When the acknowledgment mode is _auto_, then the client does not need to confirm the messages it receives. The broker will assume the client has received the message as soon as it sends it to the client.

When the acknowledgment mode is _client_, then the client must send the server confirmation for all previous messages: they acknowledge not only the specified message but also all messages sent to the subscription before this one.

When the acknowledgment mode is _client-individual_, then the client must send the server confirmation for the specified message only.

The client uses an ACK frame to confirm the consumption of a message from a subscription using the _client_ or _client-individual_ acknowledgment modes. The client uses a NACK frame to negate the consumption of a message from a subscription. The ACK and NAK frames must include the _id_ header matching the _ack_ header of the MESSAGE frame being acknowledged.

#### Broker commands acknowledgment

![STOMP broker acknowledgment](/images/STOMP_broker_acknowledgment.png)

A broker sends a RECEIPT frame to a client once the broker has successfully processed a client frame that requests a receipt. The RECEIPT frame includes the _receipt-id_ header matching the _receipt_ header of the command being acknowledged.

## Examples

### Introduction

The Spring Framework provides support for STOMP over WebSocket clients and servers in the _spring-websocket_ and _spring-messaging_ modules.

Messages from and to STOMP clients can be handled by a message broker:

*   a simple STOMP broker (which only supports a subset of STOMP commands) embedded into a Spring application
*   an external STOMP broker connected to a Spring application via TCP

Messages from and to STOMP clients also can be handled by a Spring application:

*   messages can be received and sent by _annotated controllers_
*   messages can be sent by _message templates_

The following example implements STOMP over WebSocket messaging with SockJS fallback between a server and clients. The server and the clients work according to the following algorithm:

*   the server sends a one-time message to the client
*   the server sends periodic messages to the client
*   the server receives messages from a client, logs them, and sends them back to the client
*   the client sends aperiodic messages to the server
*   the client receives messages from a server and logs them

The server is implemented as a Spring web application with Spring Web MVC framework to handle static web resources. One client is implemented as a JavaScript browser client and another client is implemented as a Java Spring console application.

### Java Spring server

#### Configuration

The following Spring configuration enables STOMP support in the Java Spring server.

```
@Configuration
@EnableWebSocketMessageBroker
public class StompWebSocketConfig implements WebSocketMessageBrokerConfigurer {

   @Override
   public void registerStompEndpoints(StompEndpointRegistry registry) {
       registry.addEndpoint("/websocket-sockjs-stomp").withSockJS();
   }

   @Override
   public void configureMessageBroker(MessageBrokerRegistry registry) {
       registry.enableSimpleBroker("/queue", "/topic");
       registry.setApplicationDestinationPrefixes("/app");
   }
}
```

Firstly, this configuration registers a STOMP over WebSocket endpoint with SockJS fallback.

Secondly, this configuration configures a STOMP message broker:

*   the destinations with the _/queue_ and _/topic_ prefixes are handled by the embedded simple STOMP broker
*   the destinations with the _/app_ prefix are handled by the annotated controllers in the Spring application

>For the embedded simple broker, destinations with the _/topic_ and _/queue_ prefixes do not have any special meaning. For external brokers, destinations with the _/topic_ prefix often mean _publish-subscribe_ messaging (one producer and many consumers), and destinations with the  _/queue_ prefix mean _point-to-point_ messaging (one producer and one consumer).

#### Receiving and sending messages in annotated controllers

Messages from and to STOMP clients can be handled according to the Spring programming model: by _annotated controllers_ and _message templates_.

##### @SubscribeMapping

The _@SubscribeMapping_ annotation is used for one-time messaging from application to clients, for example,  to load initial data during a client startup.

In the following example, a client sends a SUBSCRIBE frame to the _/app/subscribe_ destination. The server sends a MESSAGE frame to the same _/app/subscribe_ destination directly to the client without involving a broker.

```
@Controller
public class SubscribeMappingController {

   @SubscribeMapping("/subscribe")
   public String sendOneTimeMessage() {
       return "server one-time message via the application";
   }
}
```

##### @MessageMapping

The _@MessageMapping_ annotation is used for repetitive messaging from application to clients.

In the following example, the method annotated with the _@MessageMapping_ annotation with the _void_ return type receives a SEND frame from a client to the _/app/request_ destination, performs some action but does not send any response.

```
@Controller
public class MessageMappingController {

   @MessageMapping("/request")
   public void handleMessageWithoutResponse(String message) {
       logger.info("Message without response: {}", message);
   }
}
```

In the following example, the method annotated with the _@MessageMapping_ and _@SendTo_ annotations with the _String_ return type receives a SEND frame from a client to the _/app/request_ destination, performs some action, and sends a MESSAGE frame to the explicit _/queue/responses_ destination.

```
@Controller
public class MessageMappingController {

   @MessageMapping("/request")
   @SendTo("/queue/responses")
   public String handleMessageWithExplicitResponse(String message) {
       logger.info("Message with response: {}", message);
       return "response to " + HtmlUtils.htmlEscape(message);
   }
}
```

In the following example, the method annotated with the _@MessageMapping_ annotation with the _String_ return type receives a SEND frame from a client to the _/app/request_ destination, performs some action, and sends a MESSAGE frame to the implicit _/app/request_ destination (with the _/topic_ prefix and the _/request_ suffix of the inbound destination).

```
@Controller
public class MessageMappingController {

   @MessageMapping("/request")
   public String handleMessageWithImplicitResponse(String message) {
       logger.info("Message with response: {}", message);
       return "response to " + HtmlUtils.htmlEscape(message);
   }
}
```

##### @MessageExceptionHandler

The _@MessageExceptionHandler_ annotation is used to handle exceptions in the _@SubscribeMapping_ and _@MessageMapping_ annotated controllers. 

In the following example, the method annotated with the _@MessageMapping_ annotations receives a SEND frame from a client to the _/app/request_ destination. In case of success, the method sends a MESSAGE frame to the _/queue/responses_ destination. In case of an error, the exception handling method sends a MESSAGE frame to the _/queue/errors_ destination.

```
@Controller
public class MessageMappingController {

   @MessageMapping("/request")
   @SendTo("/queue/responses")
   public String handleMessageWithResponse(String message) {
       logger.info("Message with response: {}" + message);
       if (message.equals("zero")) {
           throw new RuntimeException(String.format("'%s' is rejected", message));
       }
       return "response to " + HtmlUtils.htmlEscape(message);
   }

   @MessageExceptionHandler
   @SendTo("/queue/errors")
   public String handleException(Throwable exception) {
       return "server exception: " + exception.getMessage();
   }
}
```

>It is possible to handle exceptions for a single _@Controller_ class or across many controllers with a _@ControllerAdvice_ class.

#### Sending messages by message templates

It is possible to send MESSAGE frames to destinations by message templates using the methods of the _MessageSendingOperations_ interface. Also, it is possible to use an implementation of this interface, the _SimpMessagingTemplate_ class, that has additional methods to send messages to specific users.

In the following example, a client sends a SUBSCRIBE frame to the _/topic/periodic_ destination. The server broadcasts MESSAGE frames to each subscriber of the _/topic/periodic_ destination.

```
@Component
public class ScheduledController {

   private final MessageSendingOperations<String> messageSendingOperations;

   public ScheduledController(MessageSendingOperations<String> messageSendingOperations) {
       this.messageSendingOperations = messageSendingOperations;
   }

   @Scheduled(fixedDelay = 10000)
   public void sendPeriodicMessages() {
       String broadcast = String.format("server periodic message %s via the broker", LocalTime.now());
       this.messageSendingOperations.convertAndSend("/topic/periodic", broadcast);
   }
}
```

### JavaScript browser client

The JavaScript browser client uses the _webstomp_ object from the [webstomp-client](https://github.com/JSteunou/webstomp-client) library. As the underlying communicating object the client uses a _SockJS_ object from the [SockJS](https://github.com/sockjs/sockjs-client) library.

When a user clicks the 'Connect' button, the client uses the _webstomp_._over_ method (with a _SockJS_ object argument) to create a _webstomp_ object. After that, the client uses the _webstomp.connect_ method (with empty headers and a callback handler) to initiate a connection to the server. When the connection is established, the callback handler is called. 

After the connection, the client uses the _webstomp.subscribe_ methods to subscribe to destinations. This method accepts a destination and a callback handler that is called when a message is received and returns a subscription. The client uses the _unsubscribe_ method to cancel the existing subscription.

When the user clicks the 'Disconnect' button, the client uses the _webstomp.disconnect_ method (with a  callback handler) to initiate the close of the connection. When the connection is closed, the callback handler is called. 

```
let stomp = null;

// 'Connect' button click handler
function connect() {
   stomp = webstomp.over(new SockJS('/websocket-sockjs-stomp'));

   stomp.connect({}, function (frame) {
       stomp.subscribe('/app/subscribe', function (response) {
           log(response);
       });

       const subscription = stomp.subscribe('/queue/responses', function (response) {
           log(response);
       });

       stomp.subscribe('/queue/errors', function (response) {
           log(response);

           console.log('Client unsubscribes: ' + subscription);
           subscription.unsubscribe({});
       });

       stomp.subscribe('/topic/periodic', function (response) {
           log(response);
       });
   });
}

// 'Disconnect' button click handler
function disconnect() {
   if (stomp !== null) {
       stomp.disconnect(function() {
           console.log("Client disconnected");
       });
       stomp = null;
   }
}
```

When the user clicks the 'Send' button, the client uses the _webstomp.send_ method to send a message to the destination (with empty headers).

```
// 'Send' button click handler
function send() {
   const output = $("#output").val();
   console.log("Client sends: " + output);
   stomp.send("/app/request", output, {});
}
```

![WebSocket](/images/browser-stomp.png)

### Java Spring client

Java Spring client consists of two parts: Spring STOMP events handler and Spring STOMP over WebSocket configuration.

To handle STOMP session events, the client implements the _StompSessionHandler_ interface. The handler uses the _subscribe_ method to subscribe to server destinations, the _handleFrame_ callback method to receive messages from a server, and the _sendMessage_ method to send messages to the server.

```
public class ClientStompSessionHandler extends StompSessionHandlerAdapter {

   @Override
   public void afterConnected(StompSession session, StompHeaders headers) {
       logger.info("Client connected: headers {}", headers);

       session.subscribe("/app/subscribe", this);
       session.subscribe("/queue/responses", this);
       session.subscribe("/queue/errors", this);
       session.subscribe("/topic/periodic", this);

       String message = "one-time message from client";
       logger.info("Client sends: {}", message);
       session.send("/app/request", message);
   }

   @Override
   public void handleFrame(StompHeaders headers, Object payload) {
       logger.info("Client received: payload {}, headers {}", payload, headers);
   }

   @Override
   public void handleException(StompSession session, StompCommand command,
                               StompHeaders headers, byte[] payload, Throwable exception) {
       logger.error("Client error: exception {}, command {}, payload {}, headers {}",
               exception.getMessage(), command, payload, headers);
   }

   @Override
   public void handleTransportError(StompSession session, Throwable exception) {
       logger.error("Client transport error: error {}", exception.getMessage());
   }
}
```

The following Spring configuration enables STOMP over WebSocket support in the Spring client. The configuration defines three Spring beans:

*   the implemented _ClientStompSessionHandler_ class as an implementation of _StompSessionHandler_ interface - for handling STOMP session events
*   the _SockJsClient_ class with selected transports as an implementation of _WebSocketClient_ interface - to provide transports to connect to the WebSocket/SockJS server
*   the _WebSocketStompClient_ class - to connect to a STOMP server using the given URL with the provided transports and to handle STOMP session events in the provided event handler.

The _SockJsClient_ object uses two transports: 

*   the _WebSocketTransport_ object, which supports SockJS _WebSocket_ transport
*   the _RestTemplateXhrTransport_ object, which supports SockJS _XhrStreaming_ and _XhrPolling_ transports

```
@Configuration
public class ClientWebSocketSockJsStompConfig {

   @Bean
   public WebSocketStompClient webSocketStompClient(WebSocketClient webSocketClient,
                                                    StompSessionHandler stompSessionHandler) {
       WebSocketStompClient webSocketStompClient = new WebSocketStompClient(webSocketClient);
       webSocketStompClient.setMessageConverter(new StringMessageConverter());
       webSocketStompClient.connect("http://localhost:8080/websocket-sockjs-stomp", stompSessionHandler);
       return webSocketStompClient;
   }

   @Bean
   public WebSocketClient webSocketClient() {
       List<Transport> transports = new ArrayList<>();
       transports.add(new WebSocketTransport(new StandardWebSocketClient()));
       transports.add(new RestTemplateXhrTransport());
       return new SockJsClient(transports);
   }

   @Bean
   public StompSessionHandler stompSessionHandler() {
       return new ClientStompSessionHandler();
   }
}
```

The client is a console Spring Boot application without Spring Web MVC.

```
@SpringBootApplication
public class ClientWebSocketSockJsStompApplication {

   public static void main(String[] args) {
       new SpringApplicationBuilder(ClientWebSocketSockJsStompApplication.class)
               .web(WebApplicationType.NONE)
               .run(args);
   }
}
```

## Conclusion

Because WebSocket provides full-duplex communication for the Web, it is a good choice to implement various messaging protocols on top of it. Among STOPM, there are [officially registered](https://www.iana.org/assignments/websocket/websocket.xhtml#subprotocol-name) several messaging subprotocols that work over WebSocket, among them:

*   AMQP (Advanced Message Queuing Protocol) - another protocol to communicate between clients and message brokers
*   MSRP (Message Session Relay Protocol) - a protocol for transmitting a series of related instant messages during a session
*   WAMP (Web Application Messaging Protocol) - a general-purpose messaging protocol for publishing-subscribe communication and remote procedure calls
*   XMPP (Extensible Messaging and Presence Protocol) - a protocol for near real-time instant messaging, presence information, and contact list maintenance

Before implementing your own subprotocol on top of WebSocket, try to reuse an existing protocol and its client and server libraries - you can save a lot of time and avoid many design and implementation errors.

Complete code examples are available in [the GitHub repository](https://github.com/aliakh/demo-spring-websocket/tree/master/websocket-sockjs-stomp-server).
