/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * Various JVM Statistic gathering methods used by the resource classes.
 * 
 * @author Tim Mitchell <tim_mitchell@uk.ibm.com>
 * @module resources/stats/_pmiStats
 * 
 * @return {Object} Containing all the PMI stats methods
 */
define(
    [ 'dojo/Deferred', 'dojo/request', 'jsExplore/resources/stats/_mbeanUtils', 'js/common/tr' ],
    function(Deferred, request, mbeanUtils, tr) {

      return {
        getServletStats: __getServletStats,

        getThreadPoolStats: __getThreadPoolStats,

        getJAXWSStats: __getJAXWSStats,

        getServerSessions: __getServerSessions,

        getSessionStats: __getSessionStats,

        getConnectionPoolStats: __getConnectionPoolStats,

        getServletsForApp : __getServletsForApp,

        getSessionsForApp : __getSessionsForApp,

        getJAXWSServiceQNamesForApp : __getJAXWSServiceQNamesForApp, 

        getDataSourcesForServer : __getDataSourcesForServer,

        getThreadPoolExecutorsForServer : __getThreadPoolExecutorsForServer,

        isPMIEnabled: __isPMIEnabled
      };

      /**
       * This method is called to see if the PMI monitor feature is enabled or not. If we haven't previously 
       * checked, then we check to see the PMI LibertyFeature service exists in the Service Registry, and if so we know it has 
       * been enabled. We cache this check so we only check the service once. 
       * TODO we need to add code to use the WebSockets to dynamically update the property if the feature is deprovisioned.
       */
      function __isPMIEnabled(server) {
        var enabled = false;
        mbeanUtils.isLibertyFeatureActive(server, "monitor-1.0").then(function(response) {
          if (response === true) {
            enabled = true;
          }
        });

        console.log("PMI enabled for server " + server.id + " is " + enabled);
        return enabled;
      };

      /**
       * This method checks to see whether the URL should be encoded. Currently if there is a "/" in the string, 
       * then we encode the URL. 
       */
      function __encodeURL(url) {
        var result = url;
        if (url.indexOf("/") >= 0 || url.indexOf(":") >= 0 || url.indexOf(" ") >= 0 || url.indexOf("=") >= 0)
          result =  encodeURIComponent(url);

        return result;
      }

      /**
       * This method is used to get the Servlet Stats for a Web Application. The values returned from the Mbean are things like responseTime,
       * number of requests, 
       * 
       * @param {Server}
       *          server - The server resource that we can use to find the host, user dir and servername to send the mbean request to.
       * @param {String}
       *          servletName - The <applicationName>.<servletName> String that represents the servlet to look up.
       * @return {Promise} response - The result of the operation. If an error occurred, the result will contain not only the full err
       *         object, but also a errMsg property that can be used to display a prepared message regarding the result.
       */
      function __getServletStats(server, servletName) {
        var url = mbeanUtils.getMBeanPrefixURL() + 'WebSphere:type=ServletStats,name=' + __encodeURL(servletName) + '/attributes?&attribute=RequestCountDetails&attribute=ResponseTimeDetails';
        var deferred = new Deferred();
        var successFunction = function(response) {
          var newResponse = '{"RequestCount": ' + response[0].value.value.reading.count + ', ' +
          '"RequestLastTimestamp": ' + response[0].value.value.reading.timestamp + ', ' +
          '"RequestUnit": "' + response[0].value.value.reading.unit + '", ' +
          '"ResponseCount": ' + response[1].value.value.reading.count + ', ' +
          '"ResponseMax": ' + response[1].value.value.reading.maximumValue + ', ' +
          '"ResponseMean": ' + response[1].value.value.reading.mean + ', ' +
          '"ResponseMin": ' + response[1].value.value.reading.minimumValue + ', ' +
          '"ResponseStdDev": ' + response[1].value.value.reading.standardDeviation + ', ' +
          '"ResponseLastTimestamp": ' + response[1].value.value.reading.timestamp + ', ' +
          '"ResponseTotal": ' + response[1].value.value.reading.total + ', ' +
          '"ResponseUnit": "' + response[1].value.value.reading.unit + '", ' +
          '"ResponseVariance": ' + response[1].value.value.reading.variance + '}';

          deferred.resolve(newResponse, true);
        };
        return mbeanUtils.invokeGetMBeanOperation(server, url, false, deferred, successFunction);
      };

      /**
       * This method is used to get the thread pool information. It list the poolsize as well as the active threads.
       * 
       * @param {Server}
       *          server - The server resource that we can use to find the host, user dir and servername to send the mbean request to.
       * @param {String}
       *          threadPoolName - A String that represents the ThreadPool to look up.
       * @return {Promise} response - The result of the operation. If an error occurred, the result will contain not only the full err
       *         object, but also a errMsg property that can be used to display a prepared message regarding the result.
       */
      function __getThreadPoolStats(server, threadPoolName) {
        var url = mbeanUtils.getMBeanPrefixURL() + 'WebSphere:type=ThreadPoolStats,name=' + __encodeURL(threadPoolName) + '/attributes?attribute=PoolSize&attribute=ActiveThreads';
        return mbeanUtils.invokeGetMBeanOperation(server, url, false);
      };

      /**
       * This method is used to get the loaded class count. It returns the number of loaded classes, the number of unloaded classes, and the
       * Total loaded classes.
       * 
       * org.apache.cxf:bus.id=HelloWorldWeb-Server-Bus,type=Performance.Counter.Server,service=\"{http://lab1.ws.ibm.com/}HelloWorldService\",port=\"HelloWorldPort\",operation=\"sayHello\
       * 
       * 
       * @param {Server}
       *          server - The server resource that we can use to find the host, user dir and servername to send the mbean request to.
       * @param {String}
       *          performanceServerMBean - A boolean to indicate whether we need to access the Performance.Counter.Server (true), 
       *          or Performance.Counter.Client (false). 
       * @param {String}
       *          busName - A String representing the bus of the JAXWS Application endpoint.
       * @param {String}
       *          serviceName - A String representing the Service of the JAXWS Application endpoint.
       * @param {String}
       *          portName - A String representing the Port of the JAXWS Application endpoint.
       * @param {String}
       *          operation (optional) - A String representing the operation of the JAXWS Application endpoint.
       * @return {Promise} response - The result of the operation. If an error occurred, the result will contain not only the full err
       *         object, but also a errMsg property that can be used to display a prepared message regarding the result.
       */
      function __getJAXWSStats(server, performanceServerMBean, busName, serviceName, portName, operation) {

        var url = mbeanUtils.getMBeanPrefixURL() + 'org.apache.cxf:';

        // Set the type of the MBean to either client or server.
        if (performanceServerMBean)
          url += "type=Performance.Counter.Server";
        else 
          url += "type=Performance.Counter.Client";

        // If we have a busName passed in, add the Mbean property.
        if (busName) {
          url+= __encodeURL(",bus.id=" + busName);
        }

        // If we have a portName passed in, add the Mbean property.
        if (portName) {
          if (portName.indexOf("\"") !== 0)
            portName = "\"" + portName  + "\"";
          url+= __encodeURL(",port=" + portName);
        }

        // If we have a ServiceName passed in, add the Mbean property.
        if (serviceName) {
          if (serviceName.indexOf("\"") !== 0)
            serviceName = "\"" + serviceName  + "\"";
          url+= __encodeURL(",service=" + serviceName);
        }

        // If we have an operation passed in, add the Mbean property.
        if (operation) {
          if (operation.indexOf("\"") !== 0)
            operation = "\"" + operation  + "\"";
          url+= __encodeURL(",operation=" + operation);
        }

        return mbeanUtils.invokeGetMBeanOperation(server, url + "/attributes", false);
      };

      /**
       * This method is used to get the list of sessions for a server. It returns an array of 
       * session names that can then be used to call getSessionStats().
       * 
       * @param {Server}
       *          server - The server resource that we can use to find the host, user dir and servername to send the mbean request to.
       * @return {Promise} response - The result of the operation. If an error occurred, the result will contain not only the full err
       *         object, but also a errMsg property that can be used to display a prepared message regarding the result.
       */
      function __getServerSessions(server) {
        return __getListOfMbeans(server, 'WebSphere:type=SessionStats,name=*');
      };

      /**
       * This method is used to get the Session information for an Application endpoint. It returns information such as the session count,
       * the session creation count, the number of live sessions, and failure information.
       * 
       * @param {Server}
       *          server - The server resource that we can use to find the host, user dir and servername to send the mbean request to.
       * @param {String}
       *          urlEndpoint - The application url endpoint to view the stats for.
       * @return {Promise} response - The result of the operation. If an error occurred, the result will contain not only the full err
       *         object, but also a errMsg property that can be used to display a prepared message regarding the result.
       */
      function __getSessionStats(server, urlEndpoint) {
        var url = mbeanUtils.getMBeanPrefixURL() + 'WebSphere:type=SessionStats,name=' + __encodeURL(urlEndpoint) + '/attributes?attribute=ActiveCount&attribute=LiveCount&attribute=CreateCount&attribute=InvalidatedCountbyTimeout&attribute=InvalidatedCount';
        return mbeanUtils.invokeGetMBeanOperation(server, url, false);
      };


      /**
       * This method is used to get the Connection Pool stats for a particular datasource. It returns information such as
       * the number of connections created, connections deleted, the number of managed connection objects in use, the number of connection
       * objects in use, the average waittime and the number of free connections in use.
       * 
       * @param {Server}
       *          server - The server resource that we can use to find the host, user dir and servername to send the mbean request to.
       * @param {String}
       *          dataSource - The name of the datasource to get the stats for, e.g. jdbc/TradeDataSource
       * @return {Promise} response - The result of the operation. If an error occurred, the result will contain not only the full err
       *         object, but also a errMsg property that can be used to display a prepared message regarding the result.
       */
      function __getConnectionPoolStats(server, dataSource) {
        var url = mbeanUtils.getMBeanPrefixURL() + 'WebSphere:type=ConnectionPoolStats,name=' + __encodeURL(dataSource) + '/attributes?attribute=CreateCount&attribute=DestroyCount&attribute=ManagedConnectionCount&attribute=WaitTime&attribute=ConnectionHandleCount&attribute=FreeConnectionCount';
        return mbeanUtils.invokeGetMBeanOperation(server, url, false);
      };

      /**
       * Utility that returns a list of servlets for the specified application resource
       * 
       * @param {String}
       *         appName - The application name for which to query available servlets
       *         
       * @param {Server}
       *         server - The Server resource where the application is installed
       *         
       *  @return {Promise}
       *         response - The Deferred object which resolves to an array of servlet names
       */
      function __getServletsForApp(appName, server) {
        return __getListOfMbeans(server, 'WebSphere:type=ServletStats,name=' + appName + '*');
      };

      /**
       * Utility that returns a list of sessions, represented by what looks like a URI,
       * for the specified application resource 
       * 
       * @param {String}
       *         appName - The application name for which to query available sessions
       *         
       * @param {Server}
       *         server - The Server resource where the application is installed
       *         
       *  @return {Promise}
       *         response - The Deferred object which resolves to an array of session names
       */
      function __getSessionsForApp(appName, server) {
        // TODO: For SessionStats, we return a URI/session ID that might not be affiliated with the app
        // because the application name doesn't appear at all in the SessionStats objectName. This metrics
        // work investigation is still ongoing
        return __getListOfMbeans(server, 'WebSphere:type=SessionStats,name=*');
      };

      /** Utility that returns list of JAX-WS services and ports
       * for specified application
       * 
       * @param {String}
       *         appName - The application's file name for which to query available services.
       *         Note that this is not the same as the app's configured name.
       */
      function __getJAXWSServiceQNamesForApp(appName, server) {
        // TODO: still under construction
      }


      function __getListOfMbeans(server, objectName) {

        var deferred = new Deferred();

        if (server === undefined || server.type !== 'server' && server.type !== 'standaloneServer') {
          tr.throwMsg('A server resource is needed');
        }

        console.log("objectName to get MBean =", objectName);

        mbeanUtils.getMBeans(server, objectName).then(function(response) {
          console.log("response =", response);
          var newResponse = [];
          for (var i = 0; i < response.length; i++) {
            if (response[i].indexOf("name=") > -1) {
              var item = response[i].substring(response[i].indexOf("name=") + 5, response[i].length);
              newResponse[i] = item;
            }
          }
          deferred.resolve(newResponse, true);
        }, function(err) {
          console.error("Something went wrong:", err);
          deferred.resolve(err, true);
        });

        return deferred;
      };

      /**
       * Utility that returns a list of datasources for the specified server resource
       * 
       * @param {Server}
       *         server - The server for which to query available datasources
       *         
       *  @return {Promise}
       *         response - The Deferred object which resolves to an array of datasource names
       */
      function __getDataSourcesForServer(server) {
        return __getListOfMbeans(server, 'WebSphere:type=ConnectionPoolStats');
      };

      /**
       * Utility that returns a list of thread pool executors for the specified server resource
       * 
       * @param {Server}
       *         resource - The server for which to query available thread pool executors
       *         
       *  @return {Promise}
       *         response - The Deferred object which resolves to an array of thread pool executors
       */
      function __getThreadPoolExecutorsForServer(server) {
        return __getListOfMbeans(server, 'WebSphere:type=ThreadPoolStats');
      };
    });