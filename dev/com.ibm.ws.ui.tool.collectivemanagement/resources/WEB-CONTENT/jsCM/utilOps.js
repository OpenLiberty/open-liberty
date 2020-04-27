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

define(
    [ 'dojo/request' ],
    function(request) {
      "use strict";
      return {
        register : function(host, user, pwd) {
          var url = "/IBMJMXConnectorREST/mbeans/WebSphere:feature=collectiveController,name=CollectiveRegistration,type=CollectiveRegistration/operations/registerHost";
          var options = {
            handleAs : 'json',
            headers : {
              'Content-type' : 'application/json'
            },
            data : '{"params":[{"value":"' + host + '","type":"java.lang.String"},{"value":{"rpcUser":"' + user + '","rpcUserPassword":"' + pwd + 
              '"},"type":{"className":"java.util.HashMap","simpleKey":true,"entries":[{"key":"rpcUser","keyType":"java.lang.String","value":"java.lang.String"},{"key":"rpcUserPassword","keyType":"java.lang.String","value":"java.lang.String"}]}}],"signature":["java.lang.String","java.util.Map"]}'

          };
          request.post(url, options).then(function(response) {
              if (!alert('SUCCESSFUL')) {
					window.location.reload();
              }
          }, function(err) {
            alert('ERROR! ' + err);
          });
        },
        
        update : function(host, user, pwd) {
          var url = "/IBMJMXConnectorREST/mbeans/WebSphere:feature=collectiveController,name=CollectiveRegistration,type=CollectiveRegistration/operations/updateHost";
          var options = {
            handleAs : 'json',
            headers : {
              'Content-type' : 'application/json'
            },
            data : '{"params":[{"value":"' + host + '","type":"java.lang.String"},{"value":{"rpcUser":"' + user + '","rpcUserPassword":"' + pwd + 
              '"},"type":{"className":"java.util.HashMap","simpleKey":true,"entries":[{"key":"rpcUser","keyType":"java.lang.String","value":"java.lang.String"},{"key":"rpcUserPassword","keyType":"java.lang.String","value":"java.lang.String"}]}}],"signature":["java.lang.String","java.util.Map"]}'
          };
          request.post(url, options).then(function(response) {
              if (!alert('SUCCESSFUL')) {
					window.location.reload();
              }
          }, function(err) {
            alert('ERROR!' + err);
          });
        },

        unregister : function(host) {
          var url = "/IBMJMXConnectorREST/mbeans/WebSphere:feature=collectiveController,name=CollectiveRegistration,type=CollectiveRegistration/operations/unregisterHost";
          var options = {
            handleAs : 'json',
            headers : {
              'Content-type' : 'application/json'
            },
            data : '{"params":[{"value":"' + host + '","type":"java.lang.String"}],"signature":["java.lang.String"]}'
          };
          request.post(url, options).then(function(response) {
              if (!alert('SUCCESSFUL')) {
					window.location.reload();
              }
          }, function(err) {
            alert('ERROR!' + err);
          });
        },

        remove : function(rServer) {
          var info = rServer.name.split(",");
          console.log(info[0]);
          console.log(info[1]);
          console.log(info[2]);
          var url = "/IBMJMXConnectorREST/mbeans/WebSphere:feature=collectiveController,name=CollectiveRegistration,type=CollectiveRegistration/operations/remove";
          var options = {
            handleAs : 'json',
            headers : {
              'Content-type' : 'application/json'
            },
            data : '{"params":[{"value":"' + info[0] + '","type":"java.lang.String"},{"value":"' + info[1] + '","type":"java.lang.String"},{"value":"' + info[2] + '","type":"java.lang.String"}],"signature":["java.lang.String","java.lang.String","java.lang.String"]}'
          };
          request.post(url, options).then(function(response) {
              if (!alert('SUCCESSFUL')) {
					window.location.reload();
              }
          }, function(err) {
            alert('ERROR!' + err);
          });
        }
      };
    });