const BREW_INFO_API_URL = "https://XXXXXXXXXX.execute-api.us-west-2.amazonaws.com/api";
const CLOUDWATCH_API_URL = "https://XXXXXXXXXX.execute-api.us-west-2.amazonaws.com/api";

app = angular.module('hopologybrewing-bcs', ['daterangepicker'])
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
                $scope.activeProcesses = [];

                for (var i = 0; i < response.data.length; i++) {
                    if (response.data[i]) {
                        enabledProcesses.push(i);
                    }
                }

                for (var j = 0; j < enabledProcesses.length; j++) {
                    $http.get('/process/'.concat(enabledProcesses[j]).concat('/current_state')).
                    then(function (processStateResponse) {
                        var exitConditions = processStateResponse.data.statesObj[processStateResponse.data.current_state.state].exitConditions;

                        if (exitConditions != null) {
                            for (var k = 0; k < exitConditions.length; k++) {
                                processStateResponse.data.nextState = processStateResponse.data.states[exitConditions[k].next_state];
                            }
                        }

                        $scope.activeProcesses.push(processStateResponse.data);
                    });
                }
            }
        });

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
    })

    .controller('gaugeController', function ($scope, $http) {
        $scope.generateOptions = function() {
            $http.get('/temp').then(function (response) {
                // find active process and get current state
                $scope.gaugeOptions = [];
                for (var i = 0; i < response.data.length; i++) {
                    $http.get('/temp/' + i).then(function (response) {
                        var probeId = response.config.url.split('/')[2];

                        $scope.gaugeOptions[probeId] = {
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
                        };
                    });
                }
            });
        };

        $scope.generateOptions();
        var divIds = [];
        $scope.renderGauges = function(divId, options) {
            divIds.push(divId);
            chart = $('#' + divId).highcharts(options);
        };

        $scope.reloadGauges = function() {
            $scope.generateOptions();

            for (var i=0; i < divIds.length; i++) {
                $scope.renderGauges(divIds[i], $scope.gaugeOptions[i]);
            }
        };
    })

    .controller('brewController', function ($scope, $http) {
        $http.get('/brews').
        then(function (response) {
            $scope.brews = response.data.brews;
            $scope.selectedBrew = response.data.brews[response.data.mostRecent];

            $scope.brewDate = moment($scope.selectedBrew.brewDate).format("MMM Do YYYY");

            if ($scope.selectedBrew.brewCompleteDate) $scope.brewCompleteDate = moment($scope.selectedBrew.brewCompleteDate).format("M/D h:mm a");
            if ($scope.selectedBrew.yeastPitch) $scope.yeastPitch = moment($scope.selectedBrew.yeastPitch).format("M/D h:mm a");
            if ($scope.selectedBrew.crashStart) $scope.crashStart = moment($scope.selectedBrew.crashStart).format("M/D h:mm a");

            var startDate = null;
            var endDate = null;
            if ($scope.selectedBrew && $scope.selectedBrew.brewCompleteDate > 0) {
                // if brew is complete, render all data
                endDate = moment($scope.selectedBrew.brewCompleteDate);
                startDate = moment($scope.selectedBrew.brewDate);
            } else {
                endDate = moment();
                startDate = moment(endDate).subtract(2, 'days');
            }

            // add a separate date picker for output and pumps
            // pumps default to last 7 days
            // output defaults to pre-crash and set max date to crashStart to avoid getting output data during crash
            $scope.datePickerOptions = {
                locale: {
                    applyLabel: "Apply",
                    fromLabel: "From",
                    format: "MM-DD-YYYY",
                    toLabel: "To",
                    cancelLabel: 'Cancel',
                    customRangeLabel: 'Custom range'
                },
                ranges: {
                    'First 7 Days': [moment($scope.selectedBrew.brewDate), moment($scope.selectedBrew.brewDate).add(6, 'days')],
                    'Pre-Crash': [moment($scope.selectedBrew.brewDate), moment($scope.selectedBrew.crashStart)],
                    'Last 3 Days': [moment(endDate).subtract(2, 'days'), moment(endDate)],
                    'Last 7 Days': [moment(endDate).subtract(6, 'days'), moment(endDate)],
                    'Entire Brew': [moment($scope.selectedBrew.brewDate), moment(endDate)]
                },
                eventHandlers: {
                    'apply.daterangepicker': function(ev, picker) {
                        $scope.renderCharts($scope.selectedBrew, $scope.selectedBrew.brewDate, $scope.datePicker.startDate, $scope.datePicker.endDate)
                    }
                }
            };

            $scope.dateMin =  $scope.selectedBrew.brewDate;
            $scope.dateMax =  ($scope.selectedBrew.brewCompleteDate > 0 ? $scope.selectedBrew.brewCompleteDate : null);
            $scope.datePicker = {startDate: startDate, endDate: endDate};
            $scope.renderCharts($scope.selectedBrew, $scope.selectedBrew.brewDate, startDate, endDate);
        });

        $scope.createBrew = function (brew_name, brew_description, month, day, year) {
            if (!brew_name || !month || !day || !year) {
                $scope.createBrewError = "You must provide a brew name, month, day and year.";
            } else {
                var url = BREW_INFO_API_URL + '/create';
                $('#createBrewButton').button('loading');
                var data = {
                    "month": month,
                    "day": day,
                    "year": year,
                    "description": brew_description,
                    "name": brew_name
                };

                var config = {
                    headers: {
                        'Content-Type': 'application/json;charset=utf-8;'
                    }
                };

                $http.post(url, data, config).then(function (response) {
                    if (response.data != null) {
                        $('#createBrewButton').button('reset');
                        $("#createBrewDialog").modal('hide');
                        $scope.createBrewError = undefined;
                    }
                }, function (error) {
                    console.log(error);
                    $('#createBrewButton').button('reset');
                    $scope.createBrewError = "Failed to create brew, please try again.";
                });
            }
        };

        $scope.updateBrew = function (clickType) {
            var url = BREW_INFO_API_URL + '/update';
            $('#brewInfoUpdateButton').button('loading');
            var data = {
                "clickType": clickType
            };

            var config = {
                headers: {
                    'Content-Type': 'application/json;charset=utf-8;'
                }
            };

            $http.post(url, data, config).then(function (response) {
                $('#brewInfoUpdateButton').button('reset');
            }, function (error) {
                console.log(error);
                $('#brewInfoUpdateButton').button('reset');
            });
        };

        $scope.triggerCloudWatch = function (clickType) {
            var url = CLOUDWATCH_API_URL + '/trigger';
            $('#cloudWatchButton').button('loading');
            var data = {
                "clickType": clickType
            };

            var config = {
                headers: {
                    'Content-Type': 'application/json;charset=utf-8;'
                }
            };

            $http.post(url, data, config).then(function (response) {
                $('#cloudWatchButton').button('reset');
            }, function (error) {
                console.log(error);
                $('#cloudWatchButton').button('reset');
            });
        };

        var chartOptions = {
            chart: {
                zoomType: 'x'
            },

            exporting: {
                enabled: false
            },

            xAxis: {
                type: 'datetime',
                labels: {
                    align: 'right',
                    rotation: -30
                }
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
            }
        };

        // update to have different ranges for temp and outpus
        $scope.renderCharts = function(selectedBrew, brewDate, lowerRange, upperRange) {
            if (selectedBrew.brewDate) $scope.brewDate = moment(selectedBrew.brewDate).format("MMM Do YYYY");
            if (selectedBrew.brewCompleteDate) $scope.brewCompleteDate = moment(selectedBrew.brewCompleteDate).format("M/D h:mm a");
            if (selectedBrew.yeastPitch) $scope.yeastPitch = moment(selectedBrew.yeastPitch).format("M/D h:mm a");
            if (selectedBrew.crashStart) $scope.crashStart = moment(selectedBrew.crashStart).format("M/D h:mm a");

            var pathVar = '';

            if (brewDate > 0) {
                pathVar = '/'.concat(moment(brewDate).format("x"))
            }

            var rangeString = '';
            if (lowerRange > 0) {
                rangeString = rangeString.concat("lowerRange=").concat(moment(lowerRange).format("x"));
            }

            if (upperRange > 0) {
                if (rangeString != '') {
                    rangeString = rangeString.concat("&");
                }

                rangeString = rangeString.concat("upperRange=").concat(moment(upperRange).format("x"));
            }

            if (rangeString != '') {
                rangeString = "?".concat(rangeString);
            }

            $http.get('/temp/history'.concat(pathVar).concat(rangeString)).
            then(function (response) {
                chartOptions.chart.type ='';
                chartOptions.title = "Temperature History";
                chartOptions.yAxis = {
                    title: { text: "Temperature (F)" },
                    softMin: 60,
                    softMax: 78,
                    startOnTick: false,
                    plotBands: [{
                        from: 64,
                        to: 74,
                        color: 'rgba(68, 170, 213, 0.1)',
                        label: {
                            text: 'Ale Range',
                            style: {
                                color: '#606060'
                            },
                            textAlign: 'left'
                        }
                    }]
                };

                chartOptions.tooltip = {
                    shared: true,
                    crosshairs: true,
                    dateTimeLabelFormats: '%A, %b %e, %H:%M:%S.%L'
                };

                chartOptions.series =  response.data;
                chart = $('#temp-history').highcharts(chartOptions);
            });

            $http.get('/output/history'.concat(pathVar).concat(rangeString)).
            then(function (response) {
                chartOptions.chart.type ='column';
                chartOptions.title = "Output History";
                chartOptions.yAxis = {
                    title: { text: "On/Off" },
                    softMin: 0,
                    softMax: 1,
                    startOnTick: false
                };

                chartOptions.tooltip = {
                    crosshairs: true,
                    formatter: function () {
                        return '<b>' + Highcharts.dateFormat('%A, %b %e, %H:%M:%S.%L', this.x) + '</b>'
                            + '<br/>' + this.series.name + ': ' + ((this.y == 1) ? 'ON' : 'OFF');
                    }
                };
                chartOptions.series =  response.data;
                chart = $('#output-history').highcharts(chartOptions);
            });
        };
    });