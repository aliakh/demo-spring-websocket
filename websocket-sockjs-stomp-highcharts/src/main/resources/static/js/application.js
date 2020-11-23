$(function () {
    const stomp = Stomp.over(new SockJS('/performance'));

    stomp.connect({}, function (frame) {
        console.log('Client connected: ' + frame);

        stomp.subscribe("/app/names", function (message) {
            const names = JSON.parse(message.body);
            console.log('Names: ' + frame);

            createChart('performanceChart', names);
            stomp.subscribe("/topic/performance", function (message) {
                const performance = JSON.parse(message.body);
                updateChart(names, performance);
            });

            stomp.subscribe("/queue/performance", function (message) {
                const performance = JSON.parse(message.body);
                updateChart(names, performance);
            });

            $("#performanceChart").click(function () {
                stomp.send("/app/request", {}, {})
            });
        });
    });
});
