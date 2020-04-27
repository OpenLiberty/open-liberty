/*jshint strict:false*/

var resources =   angular.module('collectiveManagementHandler', []).controller('MainCtrl', function($scope,$http,$timeout) {
  
  /**
   * get Data from a requested URL
   * 
   * @params apilink: URL to fetch data from
   * @returns data fetched on request
   * 
   */
  
  var getRequestData = (function(apilink){
    return{ getReq : function(apilink)
      {
          return $http.get(apilink);
      }
    };
  }());
  
  /**
   * Activates success or failure alert. Does not returns null
   * 
   * @params alertType: takes "success" or "failure" to initialize the alert
   * 
   */
  var sendAlert = (function(alertType){
    if(alertType === "success"){
    $timeout(function () { $scope.SuccessAlert = true; }, 3000);
      $scope.SuccessAlert = false;
      }
    else{
      $timeout(function () { $scope.FailAlert = true; }, 3000);
      $scope.SuccessAlert = false;
    }
  });
  
  $scope.SuccessMessage = "Success";
  $scope.FailMessage = "Failed";
  $scope.SuccessAlert = true;
  $scope.FailAlert = true;
  $scope.hosts = [];
  $scope.updateHostName = "";
  var allData = {};
  
  
  /**
   * 
   * initialize the data for the cards i.e. the host names and server data
   * 
   */
  var getHostsDataCards=(function(){
    var api = "/ibm/api/collective/v1/search?type=host";
    getRequestData.getReq(api).then(function(hostData) {
      var dothishere = true;
      var j=0;
      hostData.data.hosts.list.forEach(function(eachHostName){
        if(dothishere === false){
        var hostServer = [];
        var counter = 0;
        var serversOnHost=[];
        var runningServerCount = eachHostName.servers.up;
        var totalServerCount = eachHostName.servers.up+eachHostName.servers.down+eachHostName.servers.unknown;
        //console.log($scope.hosts.length);
        $scope.hosts.push({hostName : eachHostName.name,hostData:serversOnHost,runningServers:runningServerCount,totalServers:totalServerCount});//,runningSize:runningSize,stoppedSize:stoppedSize,unknownSize:unknownSize});
        }
        else{
          j+=1;
          if(j===100){
            dothishere = false;
          }
        }
      });
    });
  });
  
  angular.element(document).ready(function () {
    getHostsDataCards();
  });
  
  /**
   * 
   * initialize the filter bar data and first 100 cards
   * 
   */
  $scope.getHostsData = (function(){
    $scope.hosts = [];
    var api = "/ibm/api/collective/v1/search?type=host";
    getRequestData.getReq(api).then(function(hostData) {
      allData = hostData;
      $scope.running = hostData.data.hosts.allServersRunning;
      $scope.stopped = hostData.data.hosts.allServersStopped;
      $scope.partial = hostData.data.hosts.someServersRunning;
      $scope.total = hostData.data.hosts.allServersRunning + hostData.data.hosts.allServersUnknown + hostData.data.hosts.allServersStopped + hostData.data.hosts.someServersRunning + hostData.data.hosts.noServers;
      var i = 0;
      var dothis = true;
      hostData.data.hosts.list.forEach(function(eachHostName){
        if(dothis === true){
        var hostServer = [];
        var counter = 0;
        var serversOnHost=[];
        var runningServerCount = eachHostName.servers.up;
        var totalServerCount = eachHostName.servers.up+eachHostName.servers.down+eachHostName.servers.unknown;
        //console.log($scope.hosts.length);
        var runningSize = eachHostName.servers.up*180/totalServerCount;
        var unknownSize = eachHostName.servers.unknown*180/totalServerCount;
        var stoppedSize = eachHostName.servers.down*180/totalServerCount;
        $scope.hosts.push({hostName : eachHostName.name,hostData:serversOnHost,runningServers:runningServerCount,totalServers:totalServerCount,runningSize:runningSize,stoppedSize:stoppedSize,unknownSize:unknownSize});
        i+=1;
        if(i==100){
          //console.log("hello");
           dothis = false;
          }
        }
      });
    });
  })();
  

  
/*
 * 
 * Can be used to load the bar 
 * 
 *///var runningSize = 170*eachHostName.servers.up/totalServerCount;
  //var stoppedSize = 170*eachHostName.servers.down/totalServerCount;
  //var unknownSize = 170*eachHostName.servers.unknown/totalServerCount;
  /*for(var i=0;i<eachHostName.servers.ids.length;i++)
  {
    serversOnHost.push({serverName : eachHostName.servers.ids[i].split(",")[2]});
  }*/
  
  
  
  $scope.registerHost = (function(hostToRegister){
    var request = $http({
      url: "/IBMJMXConnectorREST/mbeans/WebSphere:feature=collectiveController,name=CollectiveRegistration,type=CollectiveRegistration/operations/registerHost",
      method: "POST",
      data:'{"params":[{"value":"' + hostToRegister.name + '","type":"java.lang.String"},{"value":{"rpcUser":"' + hostToRegister.username + '","rpcUserPassword":"' + hostToRegister.password + '"},"type":{"className":"java.util.HashMap","simpleKey":true,"entries":[{"key":"rpcUser","keyType":"java.lang.String","value":"java.lang.String"},{"key":"rpcUserPassword","keyType":"java.lang.String","value":"java.lang.String"}]}}],"signature":["java.lang.String","java.util.Map"]}',
      headers:{'Content-Type':'application/json'}
    });
    request.success(function(){
      console.log("success");
      //$scope.getHostsData();
      $scope.hosts.push({hostName : hostToRegister.name,runningServers:0,totalServers:0});
      $scope.total += 1;
      hostToRegister.name = "";
      hostToRegister.username = "";
      hostToRegister.password = "";
      sendAlert("success");
    });
    
  });
  
  $scope.setPopup = (function(){
    
  })
  $scope.setUpdateHostName = (function(hostname){
    $scope.updateHostName = hostname;
    $scope.showUpdatePopup = ! $scope.showUpdatePopup;
  });
  
  $scope.updateHost = (function(updateHostData){
    var request = $http({
      url: "/IBMJMXConnectorREST/mbeans/WebSphere:feature=collectiveController,name=CollectiveRegistration,type=CollectiveRegistration/operations/updateHost",
      method:"POST",
      data : '{"params":[{"value":"' + $scope.updateHostName + '","type":"java.lang.String"},{"value":{"rpcUser":"' + updateHostData.user + '","rpcUserPassword":"' + updateHostData.pwd + '"},"type":{"className":"java.util.HashMap","simpleKey":true,"entries":[{"key":"rpcUser","keyType":"java.lang.String","value":"java.lang.String"},{"key":"rpcUserPassword","keyType":"java.lang.String","value":"java.lang.String"}]}}],"signature":["java.lang.String","java.util.Map"]}',
      headers:{'Content-type' : 'application/json'}
    });
   request.success(function(){
     sendAlert("success");
   });
   request.error(function(){
     sendAlert("fail");
   });
  });
  
  $scope.getHostServerData = (function(hostname){
    var api = "/ibm/api/collective/v1/hosts";
    getRequestData.getReq(api+"/"+hostname).then(function(hostServerData){
    //console.log("ids: "+hostServerData.data.servers.ids);
    var serversOnHost = [];
    for(var i=0;i<hostServerData.data.servers.ids.length;i++)
    {
      serversOnHost.push({serverName : hostServerData.data.servers.ids[i].split(",")[2],serverData:hostServerData.data.servers.ids[i]});
    }
    $scope.hosts.forEach(function(eachHost){
      
      if(eachHost.hostName===hostname){
        eachHost.hostData=serversOnHost;
        eachHost.runningServers=hostServerData.data.servers.up;
        eachHost.totalServers=hostServerData.data.servers.up+hostServerData.data.servers.down+hostServerData.data.servers.unknown;
        
      }
    });
  });
  });
  
  
  $scope.removeServer = (function(hostname,serverList){
    console.log(serverList);
    serverList.forEach(function(servername){
      console.log(servername);
      var rmServer = (function(fullinfo){
        var info = fullinfo.split(",");
        var request = $http({
          url:"/IBMJMXConnectorREST/mbeans/WebSphere:feature=collectiveController,name=CollectiveRegistration,type=CollectiveRegistration/operations/remove",
          method: "POST",
          data:'{"params":[{"value":"' + info[0] + '","type":"java.lang.String"},{"value":"' + info[1] + '","type":"java.lang.String"},{"value":"' + info[2] + '","type":"java.lang.String"}],"signature":["java.lang.String","java.lang.String","java.lang.String"]}',
          headers: {'Content-Type':'application/json'}
        });
        request.success(function(){
          $scope.getHostServerData(hostname);
          sendAlert("success");
        });
        request.error(function(){
          sendAlert("fail");
        });
      });
      $scope.hosts.forEach(function(eachHost){
        if(eachHost.hostName===hostname){
          eachHost.hostData.forEach(function(server){
            console.log(server.serverData);
            if(servername===server.serverData.split(",")[2]){
              rmServer(server.serverData);}
          });
          
          }
        });
    });
  });
  
  
  $scope.unregisterHost = (function(hostname){
    var request = $http({
      url: "/IBMJMXConnectorREST/mbeans/WebSphere:feature=collectiveController,name=CollectiveRegistration,type=CollectiveRegistration/operations/unregisterHost",
      method: "POST",
      data: '{"params":[{"value":"' + hostname + '","type":"java.lang.String"}],"signature":["java.lang.String"]}',
      headers: {'Content-Type': 'application/json'}
    });
    request.success(function() {
           var index = -1;
           $scope.hosts.forEach(function(eachHost){
             if(eachHost.hostName===hostname){
               index = $scope.hosts.indexOf(eachHost);}
             });
           $scope.hosts.splice(index, 1);
           var api = "/ibm/api/collective/v1/hosts";
           getRequestData.getReq(api).then(function(hostData) {
             //console.log(hostData.data);
             $scope.running = hostData.data.allServersRunning;
             $scope.stopped = hostData.data.allServersStopped;
             $scope.partial = hostData.data.someServersRunning;
             $scope.total = hostData.data.allServersRunning + hostData.data.allServersUnknown  + hostData.data.allServersStopped + hostData.data.someServersRunning + hostData.data.noServers;
             });
           sendAlert("success");
     });
    request.error(function(){
      sendAlert("fail");
      console.error("Error while unregistering the host");
    });
  }); 
  
  
});