/**
 * Created by dbo on 08/06/16.
 */

var app = angular.module('marketplace');

app.controller('Controller', function ($scope, $http, $routeParams, $cookieStore, AuthService, $location, $rootScope) {

    var ip = "";
    // ip = "http://172.20.30.52:5083/";
     // ip = "http://localhost:8082/";
    

    var url = ip + 'api/v1/vnf-packages';
    var urlImage = ip + 'api/v1/images';

    $scope.URL = url;

    $scope.alerts = [];

    $scope.currentPage = 0;
    $scope.pageSize = 5;
    $scope.objs = [];

    $scope.credential = {
        "username": '',
        "password": '',
        "grant_type": "password"
    };

    if (angular.isUndefined($cookieStore.get('logged'))) {
        $rootScope.logged = false;
    } else {
        $rootScope.logged = $cookieStore.get('logged');
    }

    $scope.login = function (credential) {
        console.log("credentials: ")
        console.log(credential);
        console.log(AuthService.login(credential, ip));
        setTimeout(showLoginError, 2000);
    };


    /**
     * Checks if the user is logged
     * @returns {boolean}
     */
    $scope.loggedF = function () {
        return $rootScope.logged;
    };

    if ($rootScope.logged)
    //console.log('Ok Logged');
        $location.replace();
    $scope.username = $cookieStore.get('userName');

    //console.log($scope.username);

    $scope.setCurrent = function(num) {
        //console.log(num);
        if(num>=0 && num<$scope.numberOfPages)
        $scope.currentPage = num;            
    };

    $scope.tracker = function(id, index) {
                return id + '_' + index;
    };

    $scope.getNumber = function(num) {
        return new Array(num);   
    }

    /**
     * Delete the session of the user
     * @returns {undefined}
     */
    $scope.logout = function () {
        AuthService.logout();
    };

    function showLoginError() {
        $scope.$apply(function () {
            $scope.loginError = angular.isUndefined($cookieStore.get('logged'));
            //console.log($scope.loginError);
        });
    }

    // loadTable();

    $scope.loadTable = function () {
        $scope.images = [];
        $scope.objs = [];
        //console.log($routeParams.packageId);
        if (angular.isUndefined($routeParams.packageId)) {
            $http.getWithToken(url)
                .success(function (data) {
                    $scope.objs = data;
                    console.log(data);
                    $scope.numberOfPages = Math.ceil($scope.objs.length/$scope.pageSize);
                    
                    //$scope.numberOfPages = function(){
                    //    return Math.ceil($scope.objs.length/$scope.pageSize);                
                    //};
                })
                .error(function (data, status) {
                    showError(data, status);
                });
        }
        else {
            $scope.packageId = $routeParams.packageId;

            $http.get(url + $routeParams.packageId)
                .success(function (data) {
                    $scope.package = data;
                    console.log(data);
                })
                .error(function (data, status) {
                    showError(data, status);
                });

        }
    }

    $scope.removeImage = function(id) {
        $http.delete(urlImage + '/' + id)
            .success(function (res) {
                console.log("Delete success")
                showOk("Image Deleted");
                $scope.loadTableImages();
            })
    }

    $scope.removePackage = function(id) {
        $http.delete(url + '/' + id)
            .success(function (res) {
                console.log("Delete success")
                showOk("VNF Package Deleted");
                $scope.loadTable();
            })
    }

    $scope.loadTableImages = function () {
        $scope.images = [];
        $scope.objs = [];
            $http.getWithToken(urlImage)
                .success(function (data) {
                    $scope.images = data;
                    console.log("data is: ", data);
                    $scope.numberOfPages = Math.ceil($scope.objs.length/$scope.pageSize);

                    //$scope.numberOfPages = function(){
                    //    return Math.ceil($scope.objs.length/$scope.pageSize);
                    //};
                })
                .error(function (data, status) {
                    showError(data, status);
                });
    }


    function showError(data, status) {
        $scope.alerts.push({
            type: 'alert-danger',
            msg: '<strong>ERROR: HTTP status</strong>: ' + status + ' <strong>response data</strong> : ' + JSON.stringify(data)
        });
        $('.modal').modal('hide');
        if (status === 401) {
            console.log(status + ' Status unauthorized')
            AuthService.logout();
        }
    }

    function showOk(msg) {
        window.setTimeout(function() {
            $(".alert").fadeTo(3500, 0).slideUp(500, function(){
                $(this).remove();
                $scope.alerts.remove(msg)
            });
        }, 7500);
        $scope.alerts.push({type: 'alert-success', msg: msg});
        $scope.loadTable();
        $('.modal').modal('hide');
    }


    $scope.init = function () {

        //console.log(document.querySelector("#template"));
        //console.log($routeParams.packageId);
        if (angular.isUndefined($routeParams.packageId)) {
            var previewNode = document.querySelector("#template");
            previewNode.id = "";
            var previewTemplate = previewNode.parentNode.innerHTML;
            previewNode.parentNode.removeChild(previewNode);

            var header = {};

            if ($cookieStore.get('token') !== '')
                header = {'Authorization': 'Bearer ' + $cookieStore.get('token')};

            console.log(header);
            var myDropzone = new Dropzone('form#my-dropzone', {
                url: url, // Set the url
                method: "POST",
                parallelUploads: 25,
                maxFilesize: 100, //100MB max file size
                // uploadMultiple: true,
                // addRemoveLinks: true, 
                createImageThumbnails: false,
                // acceptedFiles: 'application/x-tar,.tar,.qvow2,.img',
                autoProcessQueue: false, // Make sure the files aren't queued until manually added
                previewTemplate: previewTemplate,
                previewsContainer: "#previews", // Define the container to display the previews
                headers: header,
                init: function () {
                    var submitButton = document.querySelector("#submit-all");
                    myDropzone = this; // closure

                    submitButton.addEventListener("click", function () {
                        $scope.$apply(function ($scope) {
                            myDropzone.processQueue();
                            $scope.loadTable();
                        });
                    });
                    this.on("queuecomplete", function (file, responseText) {
                        console.log("complete_response:", responseText, file);
                        //if (responseText !== undefined){
                            $scope.$apply(function ($scope) {
                                showOk("VNF Package uploaded");
                            });
                        //}
                    });
                    this.on("error", function (file, responseText) {
                        console.log("error_response:", responseText);
                        $scope.$apply(function ($scope) {
                            //showError(responseText.error_description, responseText.error);
                            if (responseText.error)
                                showError(responseText.message, responseText.error);
                        });
                    });
                }
            });


// Update the total progress bar
            myDropzone.on("totaluploadprogress", function (progress) {
                $('.progress .bar:first').width = progress + "%";
            });

            myDropzone.on("sending", function (file, xhr, formData) {
                // Show the total progress bar when upload starts
                $('.progress .bar:first').opacity = "1";


            });

// Hide the total progress bar when nothing's uploading anymore
            myDropzone.on("queuecomplete", function (progress) {
                $('.progress .bar:first').opacity = "0";

            });

            //-------------------------------------

            var imageDropzone = new Dropzone('form#image-dropzone', {
                url: urlImage, // Set the url
                method: "POST",
                parallelUploads: 2,
                maxFilesize: 4096, //4GB max file size
                // uploadMultiple: true,
                // addRemoveLinks: true,
                paramName: 'image', 
                createImageThumbnails: false,
                // acceptedFiles: 'application/x-tar,.tar,.qvow2,.img',
                autoProcessQueue: false, // Make sure the files aren't queued until manually added
                previewTemplate: previewTemplate,
                previewsContainer: "#image-previews", // Define the container to display the previews
                headers: header,
                init: function () {
                    var submitButtonImages = document.querySelector("#submit-all-images");
                    imageDropzone = this; // closure

                    submitButtonImages.addEventListener("click", function () {
                        $scope.$apply(function ($scope) {
                            imageDropzone.processQueue();
                            $scope.loadTableImages();
                        });
                    });
                    this.on("queuecomplete", function (file, responseText) {
                        console.log("complete_response:", responseText, file);
                        //if (responseText !== undefined){
                            $scope.$apply(function ($scope) {
                                showOk("Image uploaded");
                            });
                        //}
                    });
                    this.on("error", function (file, responseText) {
                        console.log("error_response:", responseText);
                        $scope.$apply(function ($scope) {
                            //showError(responseText.error_description, responseText.error);
                            if (responseText.error)
                                showError(responseText.message, responseText.error);
                        });
                    });
                }
            });


// Update the total progress bar
            imageDropzone.on("totaluploadprogress", function (progress) {
                $('.progress .bar:first').width = progress + "%";
            });

            imageDropzone.on("sending", function (file, xhr, formData) {
                // Show the total progress bar when upload starts
                $('.progress .bar:first').opacity = "1";
            });

// Hide the total progress bar when nothing's uploading anymore
            imageDropzone.on("queuecomplete", function (progress) {
                $('.progress .bar:first').opacity = "0";
            });


            //-------------------------------------


            $(".cancel").onclick = function () {
                myDropzone.removeAllFiles(true);
                imageDropzone.removeAllFiles(true);
            };
        }
        if ($scope.loggedF()) 
            $scope.loadTable();

    };


});


app.filter('startFrom', function() {
    return function(input, start) {
        start = +start; //parse to int
        return input.slice(start);
    }
});

