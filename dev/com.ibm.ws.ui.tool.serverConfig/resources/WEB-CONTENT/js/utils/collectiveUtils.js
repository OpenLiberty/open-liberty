/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

var collectiveUtils = (function() {
    "use strict";

    var checkCollectiveEnvironment = function() {
        return $.ajax({
            url: "/ibm/api/collective/v1"
        });
    };
    
    
    var retrieveCollectiveServerList = function() {
        var deferred = new $.Deferred();
        $.ajax({
            url: "/ibm/api/collective/v1/search?type=server",
            dataType: "json",
            cache: false,
            success: function(data) {
                deferred.resolve(data.servers.list);
            },
            error: function() {
                deferred.reject();
            }
        });    
        return deferred;
    };
    
    
    var checkServerId = function(serverId) {        
        return $.ajax({
            url: "/ibm/api/collective/v1/servers/" + core.encodeServerId(serverId)
        });
    };
    
    
    var retrieveHostServerId = function() {
        var deferred = new $.Deferred();
        $.ajax({
            url: "/IBMJMXConnectorREST/mbeans/WebSphere%3Afeature%3Dkernel%2Cname%3DServerInfo/attributes",
            dataType: "json",
            cache: false,
            success: function(data) {
                var host = null;
                var userDir = null;
                var name = null;
                data.forEach(function(entry) {
                    switch(entry.name) {
                    case "DefaultHostname":
                        host = entry.value.value;
                        break;
                    case "UserDirectory":
                        userDir = entry.value.value;
                        if(userDir[userDir.length - 1] === "/") {
                            userDir = userDir.substring(0, userDir.length - 1);
                        }
                        break;
                    case "Name":
                        name = entry.value.value;
                        break;
                    }
                });
                deferred.resolve(host + "," + userDir + "," + name);
            },
            error: function() {
                deferred.reject();
            }
        });    
        return deferred;
    };


    return {
        checkCollectiveEnvironment: checkCollectiveEnvironment,
        retrieveCollectiveServerList: retrieveCollectiveServerList,
        checkServerId: checkServerId,
        retrieveHostServerId: retrieveHostServerId
    };    
    
})();
