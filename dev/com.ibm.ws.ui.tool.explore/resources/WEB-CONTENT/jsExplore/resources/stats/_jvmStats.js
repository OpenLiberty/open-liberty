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
 * @module resources/stats/_jvmStats
 * 
 * @return {Object} Containing all the stats methods
 */
define(
    [ 'dojo/Deferred', 'dojo/request', "jsExplore/resources/stats/_mbeanUtils" ],
    function(Deferred, request, mbeanUtils) {
      
      return {
          getHeapMemoryUsage: __getHeapMemoryUsage,

          getThreads: __getThreads,

          getLoadedClasses: __getLoadedClasses,

          getCPUUsage: __getCPUUsage
      };

      /**
       * This method is used to get the Heap Memory Usage. It returns the Heap Usage in mb, the max heap size, and the
       * committed(non-virtual) memory.
       * 
       * @param {Server}
       *          server - The server resource that we can use to find the host, user dir and servername to send the mbean request to.
       * @return {Promise} response - The result of the operation. If an error occurred, the result will contain not only the full err
       *         object, but also a errMsg property that can be used to display a prepared message regarding the result.
       */
      function __getHeapMemoryUsage(server) {
        var url = mbeanUtils.getMBeanPrefixURL() + 'java.lang:type=Memory/attributes/HeapMemoryUsage';
        var deferred = new Deferred();
        var successFunction = function(response) {
          var newResponse = '{"Used": ' + response.value.used + ', ' +
                            '"Committed": ' + response.value.committed + ', ' +
                            '"Max": ' + response.value.max + '}';

          deferred.resolve(newResponse, true);
        };
        return mbeanUtils.invokeGetMBeanOperation(server, url, false, deferred, successFunction);
      };

      /**
       * This method is used to get the active thread count. It returns the number of active threads, the peak thread count and the total
       * number of started threads.
       * 
       * @param {Server}
       *          server - The server resource that we can use to find the host, user dir and servername to send the mbean request to.
       * @return {Promise} response - The result of the operation. If an error occurred, the result will contain not only the full err
       *         object, but also a errMsg property that can be used to display a prepared message regarding the result.
       */
      function __getThreads(server) {
        var url = mbeanUtils.getMBeanPrefixURL() + 'java.lang:type=Threading/attributes?attribute=ThreadCount&attribute=PeakThreadCount&attribute=TotalStartedThreadCount';
        return mbeanUtils.invokeGetMBeanOperation(server, url, false);
      };

      /**
       * This method is used to get the loaded class count. It returns the number of loaded classes, the number of unloaded classes, and the
       * Total loaded classes.
       * 
       * @param {Server}
       *          server - The server resource that we can use to find the host, user dir and servername to send the mbean request to.
       * @return {Promise} response - The result of the operation. If an error occurred, the result will contain not only the full err
       *         object, but also a errMsg property that can be used to display a prepared message regarding the result.
       */
      function __getLoadedClasses(server) {
        var url = mbeanUtils.getMBeanPrefixURL() + 'java.lang:type=ClassLoading/attributes?attribute=LoadedClassCount&attribute=UnloadedClassCount&attribute=TotalLoadedClassCount';
        return mbeanUtils.invokeGetMBeanOperation(server, url, false);
      };

      /**
       * This method is used to get the CPU usage. It returns a double which represents the percentage CPU usage at this moment in time. The
       * double is a value between 0 and 1.0.
       * 
       * @param {Server}
       *          server - The server resource that we can use to find the host, user dir and servername to send the mbean request to.
       * @return {Promise} response - The result of the operation. If an error occurred, the result will contain not only the full err
       *         object, but also a errMsg property that can be used to display a prepared message regarding the result.
       */
      function __getCPUUsage(server) {
        var url = mbeanUtils.getMBeanPrefixURL() + 'java.lang:type=OperatingSystem/attributes?attribute=ProcessCpuLoad&attribute=SystemCpuLoad';
        return mbeanUtils.invokeGetMBeanOperation(server, url, false);
      };
    });
