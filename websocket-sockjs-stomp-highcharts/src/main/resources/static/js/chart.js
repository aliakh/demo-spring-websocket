let chart;

function createChart(id, names) {
    const series = [];

    let i;
    for (i = 0; i < names.length; i++) {
        series.push({
            name: names[i],
            marker: {symbol: 'circle'},
            data: []
        });
    }

    chart = Highcharts.chart(id, {
        chart: {
            type: 'line',
        },
        title: {
            text: false
        },
        xAxis: {
            type: 'datetime',
            minRange: 60 * 1000
        },
        yAxis: {
            title: {
                text: false
            }
        },
        legend: {
            layout: 'vertical',
            align: 'right',
            verticalAlign: 'middle'
        },
        series: series
    });
}

function updateChart(names, performance) {
    const time = performance.time;
    const shift = chart.series[0].data.length > 60;

    let i;
    for (i = 0; i < names.length; i++) {
        const name = names[i];
        const value = performance[name];
        chart.series[i].addPoint([time, value], true, shift);
    }
}
