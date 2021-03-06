angular.module('hello', ['ngRoute', 'ui.bootstrap'])
  .config(function($routeProvider, $httpProvider){
      $routeProvider.when('/', {
          templateUrl: '/home.html',
          controller: 'home',
          controllerAs: 'controller'
      }).when('/login', {
          templateUrl: '/login.html',
          controller: 'navigation',
          controllerAs: 'controller'
      }).when('/searchresult',{
          templateUrl: '/searchresult.html',
          controller: 'search',
          controllerAs: 'controller'
      }).otherwise('/');
      $httpProvider.defaults.headers.common["X-Requested-With"] = "XMLHttpRequest";
  })
  .factory('searchResults', function() {
      var searchResults;
      return {
          getSearchResults: function() {
              return searchResults;
          },
          setSearchResults: function(sr) {
              console.log("setting movie search results via service");
              searchResults = sr;
          }
      };
  })
  .factory('rateMovie', ['$http', function($http){
      return {
          postRatingByUser: function(uid, mid, rating){
              $http.post("/ratemovie", {'userid': uid, 'movieid': mid, 'rating': rating}).then(
                  function(response){
                      console.log("post user rating successfully!");
                  }
              );
          }
      };
  }])
  .controller('home', ['$scope', '$http', '$rootScope', function($scope, $http, $rootScope) {
      var self = this;
      console.log($rootScope.authenticated);
      $scope.authenticated = $rootScope.authenticated;

      $http.get("/topRatingMovies").then(function(response){
          self.movies = response.data;
          console.log("get movies from /topRatingMovies ", self.movies);
          console.log(self, $scope);
      });
  }])
  .controller('search', ['$scope', '$http', 'searchResults', function($scope, $http, searchResults) {
      var self = this;
      this.searchmovies = searchResults.getSearchResults();
      console.log("search controller gets back a collection of this ", this.searchmovies);
  }])
  .controller('navigation', ['$scope', '$http', '$rootScope', '$location', 'searchResults', function($scope, $http, $rootScope, $location, searchResults){
      var self = this;
      self.query = { search: "" };
      var authenticate = function(credentials, callback){
          if(!credentials){
            return;
          }
          //var headers = credentials ? {authorization : "Basic " + btoa(credentials.username + ":" + credentials.password)} : {};

          console.log("credentials: ", credentials );

          $http.post( '/authenticate', credentials ).then(function(response){
              if(response.data){
                  $rootScope.authenticated = true;
                  $rootScope.uid = response.data;
                  console.log( "user id is stored in root scope. It is ", $rootScope.uid );
              }else{
                  $rootScope.authenticated = false;
              }
              callback && callback();
          }, function(){
              console.log("rejecting a promise with response", response.data);
              $rootScope.authenticated = false;
              callback && callback();
          });
      }

      if(!$rootScope.authenticated){
          authenticate(self.credentials);
      }
      self.credentials = {};
      //$rootScope.authenticated = true;

      self.login = function(){
          authenticate(self.credentials, function(){
              if($rootScope.authenticated){
                  $location.path("/");
                  self.error = false;
              } else {
                  $location.path("/login");
                  self.error = true;
                  self.error_message = "Wrong combination of username and password.";
              }
          });
      }

      self.register = function(){
          var cr = self.credentials;
          $http.post("/register", cr).then(function(response){
              id = response.data;
              if( id === -1 ){
                  $location.path("/");
                  self.error = true;
                  self.credentials = {};
                  $rootScope.authenticated = false;
              }else{
                  $location.path("/");
                  $rootScope.authenticated = true;
              }
          })
      }

      self.logout = function(){
          $http.post('/logout', {}).finally(function() {
              $rootScope.authenticated = false;
              $location.path("/");
          });
      }

      self.generalSearch = function(){
          console.log("general search for: ", self.query.search);
          $http.post('search', self.query).then(function(response){
              console.log(response.data);
              searchResults.setSearchResults(response.data);
              $location.path('/searchresult');
          });
      }
  }])
  .directive('movieCard', ['rateMovie', '$rootScope', function(rateMovie, $rootScope){
      return {
          restrict: 'E',
          transclude: true,
          templateUrl: 'moviecard.html',
          scope: {
              movie:"="
          }
      }
  });
