(function() {
	'use strict';

	var MODULE_NAME = 'onmsList.scanreport';

	// $filters that can be used to create human-readable versions of filter values
	angular.module('scanReportListFilters', [ 'onmsListFilters' ])
	.filter('property', function() {
		return function(input) {
			switch (input) {
			case 'id':
				return 'ID';
			case 'timestamp':
				return 'Scan Time';
			case 'location':
				return 'Location';
			case 'applications':
				return 'Application';
			}
			// If no match, return the input
			return input;
		}
	})
	.filter('value', function($filter) {
		return function(input, property) {
			switch (property) {
			case 'timestamp':
				// Return the date in our preferred format
				return $filter('date')(input, 'MMM d, yyyy h:mm:ss a');
			}
			return input;
		}
	})
	.filter('prettyProperty', function($filter) {
		return function(input) {
			// Strip off remote poller property prefix
			input = input.replace('org.opennms.netmgt.poller.remote','');

			// Split on dots or dashes
			input = input.split(/\.|-/);

			var retval = '';
			var first = true;
			for (var i = 0; i < input.length; i++) {
				if (!first) {
					retval += ' ';
				}
				switch(input[i]) {
					// Fully uppercase abbreviations
					case 'id':
					case 'ip':
					case 'os':
						retval += input[i].toUpperCase();
						break;
					// Otherwise, uppercase first letter only
					default:
						retval += input[i].slice(0,1).toUpperCase() + input[i].slice(1);
						break;
				}
				first = false;
			}
			return retval;
		}
	});

	// ScanReport list module
	angular.module(MODULE_NAME, [ 'ngResource', 'onmsList', 'scanReportListFilters' ])

	.directive('scanReportLogs', function($window) {
		return {
			controller: function($log, $scope, ScanReportLogs) {
				$scope.$watch('report', function(report) {
					var response = ScanReportLogs.query({
						id: report.id
					}, 
					function() {
						$log.debug('response = ' + angular.toJson(response));
						if (response.text) {
							$scope.logText = response.text;
						} else {
							$log.warn('Unknown response: ' + angular.toJson(response));
						}
					},
					function(response) {
						switch(response.status) {
						case 404:
							// If we didn't find any elements, then clear the list
							$scope.logText = undefined;
							break;
						case 401:
						case 403:
							// Handle session timeout by reloading page completely
							$window.location.href = $location.absUrl();
							break;
						}
						// TODO: Handle 500 Server Error by executing an undo callback?
					});
				});
			},
			scope: {
				report: '='
			},
			templateUrl: 'js/angular-onmsList-scanreportlogs.html',
			transclude: true
		};
	})

	.directive('scanReportDetails', function($window) {
		return {
			controller: function($scope) {
				// Do something?
			},
			// Use an isolated scope
			scope: {
				report: '='
			},
			templateUrl: 'js/angular-onmsList-scanreportdetails.html',
			transclude: true
		};
	})

	.factory('ScanReportLogs', function($resource, $log, $http, $location) {
		return $resource(BASE_REST_URL + '/scanreports/:id/logs', { id: '@id' },
			{
				'query': { 
					method: 'GET',
					transformResponse: function(data, headers, status) {
						var ret;
						switch(status) {
							case 302: // refresh on redirect
								$window.location.href = $location.absUrl();
								ret = {};
								break;
							case 204: // no content
								ret = {};
								break;
							default:
								ret = {text:data};
						}
						//$log.debug('$resource(logs) returning: ' + angular.toJson(ret));
						return ret;
					}
				}
			}
		);
	})

	/**
	 * ScanReport REST $resource
	 */
	.factory('ScanReports', function($resource, $log, $http, $location) {
		return $resource(BASE_REST_URL + '/scanreports/:id', { id: '@id' },
			{
				'query': { 
					method: 'GET',
					isArray: true,
					// Append a transformation that will unwrap the item array
					transformResponse: appendTransform($http.defaults.transformResponse, function(data, headers, status) {
						// TODO: Figure out how to handle session timeouts that redirect to 
						// the login screen
						/*
						if (status === 302) {
							$window.location.href = $location.absUrl();
							return [];
						}
						*/
						if (status === 204) { // No content
							return [];
						} else {
							// Always return the data as an array
							return angular.isArray(data['scan-report']) ? data['scan-report'] : [ data['scan-report'] ];
						}
					})
				},
				'update': { 
					method: 'PUT'
				}
			}
		);
	})

	/**
	 * ScanReport list controller
	 */
	.controller('ScanReportListCtrl', ['$scope', '$location', '$window', '$log', '$filter', 'ScanReports', function($scope, $location, $window, $log, $filter, ScanReports) {
		$log.debug('ScanReportListCtrl initializing...');

		$scope.selectedScanReport = {};

		$scope.selectScanReport = function(item) {
			$scope.selectedScanReport = item;
		}

		// Set the default sort and set it on $scope.$parent.query
		$scope.$parent.defaults.orderBy = 'timestamp';
		$scope.$parent.defaults.order = 'desc';
		$scope.$parent.query.orderBy = 'timestamp';
		$scope.$parent.query.order = 'desc';

		// Reload all resources via REST
		$scope.$parent.refresh = function() {
			// Fetch all of the items
			ScanReports.query(
				{
					_s: $scope.$parent.query.searchParam, // FIQL search
					limit: $scope.$parent.query.limit,
					offset: $scope.$parent.query.offset,
					orderBy: $scope.$parent.query.orderBy,
					order: $scope.$parent.query.order
				}, 
				function(value, headers) {
					$scope.$parent.items = value;

					var contentRange = parseContentRange(headers('Content-Range'));
					$scope.$parent.query.lastOffset = contentRange.end;
					// Subtract 1 from the value since offsets are zero-based
					$scope.$parent.query.maxOffset = contentRange.total - 1;
					$scope.$parent.setOffset(contentRange.start);
				},
				function(response) {
					switch(response.status) {
					case 404:
						// If we didn't find any elements, then clear the list
						$scope.$parent.items = [];
						$scope.$parent.query.lastOffset = 0;
						$scope.$parent.query.maxOffset = -1;
						$scope.$parent.setOffset(0);
						break;
					case 401:
					case 403:
						// Handle session timeout by reloading page completely
						$window.location.href = $location.absUrl();
						break;
					}
					// TODO: Handle 500 Server Error by executing an undo callback?
				}
			);
		};

		// Save an item by using $resource.$update
		$scope.$parent.update = function(item) {
			var saveMe = ScanReports.get({id: item.id}, function() {
				saveMe.label = item.label;
				saveMe.location = item.location;
				saveMe.properties = item.properties;

				// TODO
				//saveMe.status = item.status;
				// TODO
				//saveMe.properties = item.properties;

				// Read-only fields
				// saveMe.type = item.type;
				// saveMe.date = item.date;

				saveMe.$update({}, function() {
					// If there's a search in effect, refresh the view
					if ($scope.$parent.query.searchParam !== '') {
						$scope.$parent.refresh();
					}
				});
			}, function(response) {
				$log.debug(response);
			});
		};

		$scope.$parent.deleteItem = function(item) {
			var saveMe = ScanReports.get({id: item.id}, function() {
				if ($window.confirm('Are you sure you want to remove scan report \"' + item.id + '\"?')) {
					saveMe.$delete({id: item.id}, function() {
						$scope.refresh();
					});
				}
			}, function(response) {
				if (response.status === 404) {
					// We didn't find the item so it can't be deleted
					// Might as well call refresh()
					$scope.refresh();
				}
			});
		};

		// Refresh the item list;
		$scope.$parent.refresh();

		$log.debug('ScanReportListCtrl initialized');
	}])

	.run(['$rootScope', '$log', function($rootScope, $log) {
		$log.debug('Finished initializing ' + MODULE_NAME);
	}])

	;

	angular.element(document).ready(function() {
		console.log('Bootstrapping ' + MODULE_NAME);
		angular.bootstrap(document, [MODULE_NAME]);
	});
}());
