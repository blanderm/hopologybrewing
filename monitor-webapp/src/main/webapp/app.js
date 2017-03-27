angular.module('hopologybrewing-bcs', [])
    .controller('outputController', function ($scope, $http) {
        $http.get('/output').
            then(function (response) {
                $scope.outputs = response.data;
            });
    })

    .controller('processController', function ($scope, $http) {
        $http.get('/process').
            then(function (response) {
                // find active process and get current state
                if (response.data != null) {
                    $scope.processes = response.data;
                }
            })
    })

    .controller('currentStateController', function ($scope, $http) {
        $http.get('/process/status').
            then(function (response) {
                // find active process and get current state
                if (response.data != null) {
                    var enabledProcesses = [];
                    for (var i = 0; i < response.data.length; i++) {
                        if (response.data[i]) {
                            enabledProcesses.push(i);
                        }
                    }

                    for (var j = 0; j < enabledProcesses.length; j++) {
                        $http.get('/process/'.concat(enabledProcesses[j])).
                            then(function (processResponse) {
                                if (processResponse.data != null) {

                                    $scope.activeProcess = processResponse.data;
                                }
                            });

                        $http.get('/process/'.concat(enabledProcesses[j]).concat('/current_state')).
                            then(function (stateResponse) {
                                $scope.activeState = stateResponse.data;
                                var exitConditions = stateResponse.data.exitConditions;

                                if (exitConditions != null) {
                                    for (var k = 0; k < exitConditions.length; k++) {
                                        $scope.nextState = $scope.activeProcess.states[exitConditions[k].next_state];
                                    }
                                }

                                $scope.convertTimerValue = function (value) {
                                    // days
                                    var calculatedTimer = value / 10 / 60 / 60 / 24;
                                    var strTimer = "";
                                    var floor;

                                    floor = Math.floor(calculatedTimer);
                                    if (floor >= 1) {
                                        strTimer = floor + (floor > 1 ? " days " : " day ");
                                    }

                                    // hours
                                    calculatedTimer = calculatedTimer % 1 * 24;
                                    floor = Math.floor(calculatedTimer);
                                    strTimer = strTimer + (floor < 10 ? '0' + floor : floor)  + ":";

                                    // mins
                                    calculatedTimer = calculatedTimer % 1 * 60;
                                    floor = Math.floor(calculatedTimer);
                                    strTimer = strTimer + (floor < 10 ? '0' + floor : floor)  + ":";

                                    // seconds
                                    calculatedTimer = calculatedTimer % 1 * 60;
                                    floor = Math.floor(calculatedTimer);

                                    return strTimer + (floor < 10 ? '0' + floor : floor) ;
                                };

                                //$scope.activeStateTimers = stateResponse.data.timers;
                            });
                    }
                }
            });
    })

    .controller('gaugeController', function ($scope, $http) {
        $http.get('/temp').
            then(function (response) {
                // find active process and get current state
                if (response.data != null) {
                    $scope.tempProbes = response.data;
                    console.log($scope.tempProbes);
                }
            });


        $scope.getGauge = function (probeId, divId) {
            $http.get('/temp/' + probeId).
                then(function (response) {
                    Highcharts.chart(divId + probeId, {
                            chart: {
                                type: 'gauge',
                                plotBackgroundColor: null,
                                plotBackgroundImage: null,
                                plotBorderWidth: 0,
                                plotShadow: false
                            },

                            exporting: {
                                enabled: false
                            },

                            title: {
                                text: response.data[0].name + "<br>(" + response.data[0].setpoint + " SP)"
                            },

                            pane: {
                                startAngle: -150,
                                endAngle: 150,
                                background: [{
                                    backgroundColor: {
                                        linearGradient: {x1: 0, y1: 0, x2: 0, y2: 1},
                                        stops: [
                                            [0, '#FFF'],
                                            [1, '#333']
                                        ]
                                    },
                                    borderWidth: 0,
                                    outerRadius: '109%'
                                }, {
                                    backgroundColor: {
                                        linearGradient: {x1: 0, y1: 0, x2: 0, y2: 1},
                                        stops: [
                                            [0, '#333'],
                                            [1, '#FFF']
                                        ]
                                    },
                                    borderWidth: 1,
                                    outerRadius: '107%'
                                }, {
                                    // default background
                                }, {
                                    backgroundColor: '#DDD',
                                    borderWidth: 0,
                                    outerRadius: '105%',
                                    innerRadius: '103%'
                                }]
                            },

                            // the value axis
                            yAxis: {
                                min: 35,
                                max: 80,

                                minorTickInterval: 'auto',
                                minorTickWidth: 1,
                                minorTickLength: 10,
                                minorTickPosition: 'inside',
                                minorTickColor: '#666',

                                tickPixelInterval: 30,
                                tickWidth: 2,
                                tickPosition: 'inside',
                                tickLength: 10,
                                tickColor: '#666',
                                labels: {
                                    step: 2,
                                    rotation: 'auto'
                                },
                                plotBands: [{
                                    from: 65,
                                    to: 70,
                                    color: '#606060'
                                },
                                    {
                                        from: ((response.data[0].setpoint > 0) ? (response.data[0].setpoint - 0.25) : 68),
                                        to: ((response.data[0].setpoint > 0) ? (response.data[0].setpoint + 0.25) : 68),
                                        color: ((response.data[0].setpoint > 0) ? '#55BF3B' : '#606060')
                                    }]
                            },
                            series: response.data
                        },
                        function (chart) {
                            if (!chart.renderer.forExport) {
                                setInterval(function () {
                                    var point = chart.series[0].points[0];
                                    $.getJSON('/temp/' + probeId, function (data) {
                                        point.update(data[0].data[0]);
                                    });
                                }, 300000);
                            }
                        }
                    )
                });
        };
    })

    .controller('chartController', function ($scope, $http) {
        $http.get('/temp/history').
            then(function (response) {
                Highcharts.chart('temp-history', {
                        chart: {
                            zoomType: 'x'
                        },

                        exporting: {
                            enabled: false
                        },

                        title: {
                            text: 'Temperature History'
                        },

                        tooltip: {
                            shared: true,
                            crosshairs: true
                        },

                        xAxis: {
                            type: 'datetime',
                            //                        tickInterval: 7 * 24 * 3600 * 1000, // 24 hours
                            //                        tickWidth: 0,
                            //                        gridLineWidth: 1,
                            labels: {
                                //format: '{value:%mm/%dd/%yyyy HH:mm:ss}',
                                align: 'right',
                                rotation: -30
                            }
                        },

                        yAxis: {
                            softMin: 60,
                            softMax: 78,
                            startOnTick: false,
                            plotBands: [{
                                from: 65,
                                to: 70,
                                color: 'rgba(68, 170, 213, 0.1)',
                                label: {
                                    text: 'Ale Range',
                                    style: {
                                        color: '#606060'
                                    },
                                    textAlign: 'left'
                                }
                            }]
                        },

                        plotOptions: {
                            spline: {
                                lineWidth: 4,
                                states: {
                                    hover: {
                                        lineWidth: 5
                                    }
                                },
                                marker: {
                                    enabled: false
                                },
                                pointInterval: 5000, // one hour
                                pointStart: Date.UTC(2016, 1, 12, 0, 0, 0)
                            }
                        },

                        series: response.data
                    }
                    //,
                    //function (chart) {
                    //    if (!chart.renderer.forExport) {
                    //        setInterval(function () {
                    //            // set up the updating of the chart each second
                    //            var series1 = chart.series[0];
                    //            var x = (new Date()).getTime();
                    //
                    //            $.getJSON('/temp/1', function (data) {
                    //                series1.addPoint([x, data[0].data[0]], true, true);
                    //            });
                    //
                    //            var series2 = chart.series[1];
                    //            $.getJSON('/temp/0', function (data) {
                    //                series2.addPoint([x, data[0].data[0]], true, true);
                    //            });
                    //        }, 300000);
                    //    }
                    //}
                )
            });

        $http.get('/output/history').
            then(function (response) {
                Highcharts.chart('output-history', {
                        chart: {
                            zoomType: 'x'
                        },

                        exporting: {
                            enabled: false
                        },

                        title: {
                            text: 'Output History'
                        },

                        tooltip: {
                            shared: true,
                            crosshairs: true
                        },

                        xAxis: {
                            type: 'datetime',
                            //                        tickInterval: 7 * 24 * 3600 * 1000, // 24 hours
                            //                        tickWidth: 0,
                            //                        gridLineWidth: 1,
                            labels: {
                                //format: '{value:%mm/%dd/%yyyy HH:mm:ss}',
                                align: 'right',
                                rotation: -30
                            }
                        },

                        yAxis: {
                            softMin: 0,
                            softMax: 1,
                            startOnTick: false,
                        },

                        plotOptions: {
                            spline: {
                                lineWidth: 4,
                                states: {
                                    hover: {
                                        lineWidth: 5
                                    }
                                },
                                marker: {
                                    enabled: false
                                },
                                pointInterval: 5000, // one hour
                                pointStart: Date.UTC(2016, 1, 12, 0, 0, 0)
                            }
                        },

                        series: response.data
                    }
                )
            });

        $http.get('/temp/0').
            then(function (response) {
                Highcharts.chart('temp-gauge0', {
                        chart: {
                            type: 'gauge',
                            plotBackgroundColor: null,
                            plotBackgroundImage: null,
                            plotBorderWidth: 0,
                            plotShadow: false
                        },

                        exporting: {
                            enabled: false
                        },

                        title: {
                            text: response.data[0].name + "<br>(" + response.data[0].setpoint + " SP)"
                        },

                        pane: {
                            startAngle: -150,
                            endAngle: 150,
                            background: [{
                                backgroundColor: {
                                    linearGradient: {x1: 0, y1: 0, x2: 0, y2: 1},
                                    stops: [
                                        [0, '#FFF'],
                                        [1, '#333']
                                    ]
                                },
                                borderWidth: 0,
                                outerRadius: '109%'
                            }, {
                                backgroundColor: {
                                    linearGradient: {x1: 0, y1: 0, x2: 0, y2: 1},
                                    stops: [
                                        [0, '#333'],
                                        [1, '#FFF']
                                    ]
                                },
                                borderWidth: 1,
                                outerRadius: '107%'
                            }, {
                                // default background
                            }, {
                                backgroundColor: '#DDD',
                                borderWidth: 0,
                                outerRadius: '105%',
                                innerRadius: '103%'
                            }]
                        },

                        // the value axis
                        yAxis: {
                            min: 35,
                            max: 80,

                            minorTickInterval: 'auto',
                            minorTickWidth: 1,
                            minorTickLength: 10,
                            minorTickPosition: 'inside',
                            minorTickColor: '#666',

                            tickPixelInterval: 30,
                            tickWidth: 2,
                            tickPosition: 'inside',
                            tickLength: 10,
                            tickColor: '#666',
                            labels: {
                                step: 2,
                                rotation: 'auto'
                            },
                            plotBands: [{
                                from: 65,
                                to: 70,
                                color: '#606060'
                            },
                                {
                                    from: ((response.data[0].setpoint > 0) ? (response.data[0].setpoint - 0.25) : 68),
                                    to: ((response.data[0].setpoint > 0) ? (response.data[0].setpoint + 0.25) : 68),
                                    color: ((response.data[0].setpoint > 0) ? '#55BF3B' : '#606060')
                                }]
                        },
                        series: response.data
                    }
                    //,
                    //function (chart) {
                    //    if (!chart.renderer.forExport) {
                    //        setInterval(function () {
                    //            var point = chart.series[0].points[0];
                    //            $.getJSON('/temp/0', function (data) {
                    //                point.update(data[0].data[0]);
                    //            });
                    //        }, 300000);
                    //    }
                    //}
                )
            });

        $http.get('/temp/1').
            then(function (response) {
                Highcharts.chart('temp-gauge1', {
                        chart: {
                            type: 'gauge',
                            plotBackgroundColor: null,
                            plotBackgroundImage: null,
                            plotBorderWidth: 0,
                            plotShadow: false
                        },

                        exporting: {
                            enabled: false
                        },

                        title: {
                            text: response.data[0].name + "<br>(" + response.data[0].setpoint + " SP)"
                        },

                        pane: {
                            startAngle: -150,
                            endAngle: 150,
                            background: [{
                                backgroundColor: {
                                    linearGradient: {x1: 0, y1: 0, x2: 0, y2: 1},
                                    stops: [
                                        [0, '#FFF'],
                                        [1, '#333']
                                    ]
                                },
                                borderWidth: 0,
                                outerRadius: '109%'
                            }, {
                                backgroundColor: {
                                    linearGradient: {x1: 0, y1: 0, x2: 0, y2: 1},
                                    stops: [
                                        [0, '#333'],
                                        [1, '#FFF']
                                    ]
                                },
                                borderWidth: 1,
                                outerRadius: '107%'
                            }, {
                                // default background
                            }, {
                                backgroundColor: '#DDD',
                                borderWidth: 0,
                                outerRadius: '105%',
                                innerRadius: '103%'
                            }]
                        },

                        // the value axis
                        yAxis: {
                            min: 35,
                            max: 80,

                            minorTickInterval: 'auto',
                            minorTickWidth: 1,
                            minorTickLength: 10,
                            minorTickPosition: 'inside',
                            minorTickColor: '#666',

                            tickPixelInterval: 30,
                            tickWidth: 2,
                            tickPosition: 'inside',
                            tickLength: 10,
                            tickColor: '#666',
                            labels: {
                                step: 2,
                                rotation: 'auto'
                            },
                            plotBands: [{
                                from: 65,
                                to: 70,
                                color: '#606060'
                            },
                                {
                                    from: ((response.data[0].setpoint > 0) ? (response.data[0].setpoint - 0.25) : 68),
                                    to: ((response.data[0].setpoint > 0) ? (response.data[0].setpoint + 0.25) : 68),
                                    color: ((response.data[0].setpoint > 0) ? '#55BF3B' : '#606060')
                                }]
                        },
                        series: response.data
                    }
                    //,
                    //function (chart) {
                    //    if (!chart.renderer.forExport) {
                    //        setInterval(function () {
                    //            var point = chart.series[0].points[0];
                    //            $.getJSON('/temp/1', function (data) {
                    //                point.update(data[0].data[0]);
                    //            });
                    //        }, 300000);
                    //    }
                    //}
                )
            })
    });