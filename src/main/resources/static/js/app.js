/**
 * Created by dbo on 09/06/16.
 */


var app = angular.module('marketplace', ['ngRoute', 'ngSanitize', 'ui.bootstrap', 'ngCookies']);

app.config(function ($routeProvider, $locationProvider) {

    $routeProvider
        .when('/', {
            templateUrl: 'list.html',
            controller: ''
        })
        .when('/:packageId', {
            templateUrl: 'info.html',
            controller: 'Controller'
        }).otherwise({
        redirectTo: '/'
    });
    $locationProvider.html5Mode(false);
});
