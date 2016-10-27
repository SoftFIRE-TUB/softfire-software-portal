/**
 * Created by dbo on 09/06/16.
 */
require({
    baseUrl: 'js',
    paths: {
        jquery: 'libs/jquery/jquery.min',
        jquery_ui: 'libs/jquery/jquery-ui.min',
        bootstrapJS: "../dist/js/bootstrap.min",
        material: "material/material.min",
        ripples: "material/ripples.min",
        angular: "libs/angular/angular.min",
        angular_route: "libs/angular/angular-route.min",
        angular_cookies: "libs/angular/angular-cookies.min",
        ui_bootstrap: "libs/angular/ui-bootstrap-tpls-0.10.0.min",
        app: "app",
        angular_sanitize: "libs/angular/angular-sanitize.min",
        httpService: "services/httpService",
        authService: "services/authService",
        dropzone: "libs/dropzone",
        mainController: "controllers/mainController"
    },
    shim: {
        jquery: {
            exports: '$'
        },
        ripples: {
            deps: ['jquery', 'material']
        },
        material: {
            deps: ['jquery']
        },
        bootstrapJS: {
            deps: ['jquery']
        },
        angular: {
            exports: 'angular',
            deps: ['jquery', 'bootstrapJS', 'material']
        },
        boot: {
            deps: ['jquery']
        },
        jquery_ui: {
            deps: ['jquery']
        },
        bootstrap: {
            deps: ['app']
        },
        app: {
            deps: ['angular', 'angular_route', 'angular_sanitize', 'ui_bootstrap']
        },
        angular_route: {
            deps: ['angular']
        },
        angular_cookies: {
            deps: ['angular']
        },
        angular_sanitize: {
            deps: ['angular']
        },
        ui_bootstrap: {
            deps: ['angular']
        },
        authService: {
            deps: ['app']
        },
        mainController: {
            deps: ['app', 'httpService', 'authService', 'angular_cookies', 'dropzone','ripples']
        },
        httpService: {
            deps: ['app']
        },
        dropzone: {
            deps: ['jquery'],
            exports: 'Dropzone'
        }

    }
}), require([
    'require',
    'bootstrapJS',
    'material',
    'ripples',
    'angular',
    'angular_route',
    'mainController'

], function (require) {
    return require(['bootstrap']);
});