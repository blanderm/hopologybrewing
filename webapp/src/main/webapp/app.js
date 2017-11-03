const BREW_INFO_API_URL = "https://38i2h2bygi.execute-api.us-west-2.amazonaws.com/api";
const CLOUDWATCH_API_URL = "https://ywh5rko6xh.execute-api.us-west-2.amazonaws.com/api";

app = angular.module('brewing-bcs', ['daterangepicker'])
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
                let enabledProcesses = [];
                $scope.activeProcesses = [];

                for (let i = 0; i < response.data.length; i++) {
                    if (response.data[i]) {
                        enabledProcesses.push(i);
                    }
                }

                for (let j = 0; j < enabledProcesses.length; j++) {
                    $http.get('/process/'.concat(enabledProcesses[j]).concat('/current_state')).
                    then(function (processStateResponse) {
                        let exitConditions = processStateResponse.data.statesObj[processStateResponse.data.current_state.state].exitConditions;

                        if (exitConditions != null) {
                            for (let k = 0; k < exitConditions.length; k++) {
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
            let calculatedTimer = value / 10 / 60 / 60 / 24;
            let strTimer = "";
            let floor;

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

    .controller('tempPidController', function ($scope, $http) {
        $http.get('/temp').then(function (response) {
            // find active process and get current state
            $scope.noProbes = (response.data.length == 0);
            $scope.probes = [];

            for (let i = 0; i < response.data.length; i++) {
                $http.get('/temp/' + i).then(function (response) {
                    $scope.probes[i] = {
                       name : response.data[0].name,
                       reading : response.data[0].data[0],
                       setpoint: response.data[0].setpoint
                    };
                });
            }
        });
    })

    .controller('brewController', function ($scope, $http) {
        $http.get('/brews').
        then(function (response) {
            $scope.brews = response.data.brews;

            $scope.selectedBrew = response.data.brews[response.data.mostRecent];
            if ($scope.selectedBrew) {
                $scope.brewDate = moment($scope.selectedBrew.brewDate).format("MMM Do YYYY");

                if ($scope.selectedBrew.brewCompleteDate) $scope.brewCompleteDate = moment($scope.selectedBrew.brewCompleteDate).format("M/D h:mm a");
                if ($scope.selectedBrew.yeastPitch) $scope.yeastPitch = moment($scope.selectedBrew.yeastPitch).format("M/D h:mm a");
                if ($scope.selectedBrew.crashStart) $scope.crashStart = moment($scope.selectedBrew.crashStart).format("M/D h:mm a");


                let startDate = null;
                let endDate = null;
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
                $scope.chartDatePickerOptions = {
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
                            $scope.renderCharts($scope.selectedBrew, $scope.selectedBrew.brewDate, $scope.chartDatePicker.startDate, $scope.chartDatePicker.endDate)
                        }
                    }
                };

                $('#createBrewDatePicker').daterangepicker({
                    singleDatePicker: true,
                    showDropdowns: true,
                    locale: {
                        format: 'MM/DD/YYYY'
                    },
                    autoApply: true,
                    autoUpdateInput: true,
                    showCustomRangeLabel: false,
                    startDate: moment().format("MM/DD/YYYY")
                }, function(start, end, label) {
                    $scope.createBrewDate = start.format('YYYY-MM-DD');
                    console.log("New date range selected: " + start.format('YYYY-MM-DD') + " to " + end.format('YYYY-MM-DD') + " (predefined range: " + label + ")");
                    console.log($scope.createBrewDate);
                });

                $scope.dateMin =  $scope.selectedBrew.brewDate;
                $scope.dateMax =  ($scope.selectedBrew.brewCompleteDate > 0 ? $scope.selectedBrew.brewCompleteDate : null);
                $scope.chartDatePicker = {startDate: startDate, endDate: endDate};
                $scope.renderCharts($scope.selectedBrew, $scope.selectedBrew.brewDate, startDate, endDate);
            } else {
                $scope.noSelectedBrew = true;
            }
        });

        $http.get(CLOUDWATCH_API_URL + '/list').
        then(function (response) {
            $scope.rules = response.data;
            console.log($scope.rules);
        });

        $scope.createBrew = function (brewName, brewDescription, createBrewDate) {
            if (!brewName || !createBrewDate) {
                $scope.createBrewError = "You must provide a brew name and date.";
            } else {
                console.log(createBrewDate);
                let url = BREW_INFO_API_URL + '/create';
                $('#createBrewButton').button('loading');
                let data = {
                    "name": brewName,
                    "brewDate": (moment(createBrewDate).format("x") - 0)
                };

                if (brewDescription) data["description"] = brewDescription;

                let config = {
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

        $scope.editBrew = function (brew) {
            if (!brew.name) {
                $scope.editBrewError = "You must provide a brew name.";
            } else {
                let url = BREW_INFO_API_URL + '/create';
                $('#editBrewButton').button('loading');
                let addlAttrs = [];
                if (brew.yeastPitch) {
                    addlAttrs.push({
                        "name" : "yeast_pitch",
                        "value" : brew.yeastPitch
                    })
                }

                if (brew.crashStart) {
                    addlAttrs.push({
                        "name" : "crash_start",
                        "value" : brew.crashStart
                    })
                }

                if (brew.brewCompleteDate) {
                    addlAttrs.push({
                        "name" : "brew_completion_date",
                        "value" : brew.brewCompleteDate
                    })
                }

                let data = {
                    "brewDate": (moment(brew.brewDate).format("x") - 0),
                    "name": brew.name,
                    "description": brew.description,
                    "additionalAttributes" : addlAttrs
                };

                let config = {
                    headers: {
                        'Content-Type': 'application/json;charset=utf-8;'
                    }
                };

                $http.post(url, data, config).then(function (response) {
                    if (response.data != null) {
                        $('#editBrewButton').button('reset');
                        $("#editBrewDialog").modal('hide');
                        $scope.editBrewError = undefined;
                    }
                }, function (error) {
                    console.log(error);
                    $('#editBrewButton').button('reset');
                    $scope.editBrewError = "Failed to update brew, please try again.";
                });
            }
        };

        $scope.updateBrew = function (clickType, brewDate) {
            let url = BREW_INFO_API_URL + '/update';
            $('#brewInfoUpdateButton').button('loading');
            let data = {
                "clickType": clickType,
            };

            if (brewDate) data["brewDate"] = (moment(brewDate).format("x") - 0);

            let config = {
                headers: {
                    'Content-Type': 'application/json;charset=utf-8;'
                }
            };

            $http.post(url, data, config).then(function (response) {
                if (confirm("Do you want to update polling?") == true) {
                    url = CLOUDWATCH_API_URL + '/trigger';

                    $http.post(url, data, config).then(function (response) {
                        $('#brewInfoUpdateButton').button('reset');
                    }, function (error) {
                        console.log(error);
                        $('#brewInfoUpdateButton').button('reset');
                    });
                } else {
                    $('#brewInfoUpdateButton').button('reset');
                }
            }, function (error) {
                console.log(error);
                $('#brewInfoUpdateButton').button('reset');
            });
        };

        $scope.triggerCloudWatch = function (clickType) {
            let url = CLOUDWATCH_API_URL + '/trigger';
            $('#cloudWatchButton').button('loading');
            let data = {
                "clickType": clickType
            };

            let config = {
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
            $scope.brewDate = (selectedBrew.brewDate) ? moment(selectedBrew.brewDate).format("MMM Do YYYY") : undefined;
            $scope.brewCompleteDate = (selectedBrew.brewCompleteDate) ? moment(selectedBrew.brewCompleteDate).format("M/D h:mm a") : undefined;
            $scope.yeastPitch = (selectedBrew.yeastPitch) ? moment(selectedBrew.yeastPitch).format("M/D h:mm a") : undefined;
            $scope.crashStart = (selectedBrew.crashStart) ? moment(selectedBrew.crashStart).format("M/D h:mm a") : undefined;

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

app.directive('dateformat', function() {
    return {
        restrict: 'A',
        require: 'ngModel',
        scope: true,
        link: function(scope, el, attrs, ngModel) {

            ngModel.$formatters.push(function(value) { //show the user the date
                return new Date(value).toUTCString();
            });
            ngModel.$parsers.push(function(value){
                return new Date(value).getTime(); //convert back to milliseconds
            });
        }
    };
});