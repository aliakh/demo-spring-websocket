let sockJS = null;

function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);
    $("#send").prop("disabled", !connected);

    if (connected) {
        $("#conversation").show();
    } else {
        $("#conversation").hide();
    }

    $("#responses").html("");
}

function connect() {
    const option = $("#transports").find('option:selected').val();
    console.log('Option: ' + option);

    const transports = (option === 'all') ? [] : [option];
    console.log('Transports: ' + transports);

    sockJS = new SockJS('http://localhost:8080/websocket-sockjs',
        'subprotocol.demo.websocket', {debug: true, transports: transports});

    sockJS.onopen = function () {
        setConnected(true);
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
        setConnected(false);
        log('Client connection closed: ' + event.code);
    };
}

function disconnect() {
    if (sockJS != null) {
        sockJS.close();
        sockJS = null;
    }
    setConnected(false);
}

function send() {
    const message = $("#request").val();
    log('Client sends: ' + message);
    sockJS.send(message);
}

function log(message) {
    $("#responses").append("<tr><td>" + message + "</td></tr>");
    console.log(message);
}

$(function () {
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
    $("#connect").click(function () {
        connect();
    });
    $("#disconnect").click(function () {
        disconnect();
    });
    $("#send").click(function () {
        send();
    });
});
