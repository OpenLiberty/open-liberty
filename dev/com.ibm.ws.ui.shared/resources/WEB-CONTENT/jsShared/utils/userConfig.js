/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
var userConfig = (function() {
  
    "use strict";

    var persistenceFeatureName = null;
    var tooldata = null; 
    var persistedData = {};
    var isDatabaseEmpty = false;
    var init = false;

    var __init = function(featureName) {
      if (init) {
        return;
      }
      persistenceFeatureName = featureName;
      tooldata = toolData.createToolData(persistenceFeatureName);
      init = true;
      __load(function(){
        console.log("Persistence data loaded successfully.");
      }, function(){
        console.log("No persistence data found");
      });
    } // end of __init()
    
    /**
     * userData 
     */
    var __load = function (resolve, reject) {
      
        tooldata.get(function(response) {
          // Parse the response into a JSON object
          persistedData = JSON.parse(response);
          if(resolve){
            isDatabaseEmpty = false;
            resolve(persistedData);
          }
        },function(error) {
          // If we get a HTTP 404 or 204 (for no content) it means that the persistence file doesn't exist, and we need to set the variable to false.
          if (error.status === 404 || error.status === 204) {
              isDatabaseEmpty = true;
              if(reject){
                reject(error);
              }
          }    
        });
    };

    var __save = function (key, value, resolve) {
      
        persistedData[key] = value;

        // If the database doesn't exist then we need to do a Post. Another process may have
        // created it in the meantime so we need to catch an HTTP 409 and then GET the existing data, and
        // Put the merged data.
        if (isDatabaseEmpty) {
            tooldata.post(persistedData, function(response) {
              console.log("Updated Search persistence data", response);
              isDatabaseEmpty = false;
              if(resolve){
                resolve();
              }
            }, function(error) {
                // If we've hit a 409 it means that there is now data, and we should be doing a put rather than a post.
                if (error.status === 409) {
                    // So we need to get this new data, and merge our persisted data with it.
                    tooldata.get(function(response) {
                        persistedData = JSON.parse(response);
                        persistedData[key] = value;
                        // Once we have get the new data and updated it, we need to try one more time to write it out.  
                        tooldata.put(persistedData, function(response) {
                            console.log("Updated Search persistence data");
                            isDatabaseEmpty = false;
                            if(resolve){
                              resolve();
                            }
                        }, function(error) {
                            console.error("Unable to update persisted data: RC: " + error.status);
                        });
                    }); 
                }
            });
        // If we did have the file, then assume it still exists and Put the new data. We need to cope with the
        // fact that the file may have been deleted 404, in which case we do a Post, and a 412 which indicates that
        // the server side data has changed. In this case we Get the data again, and Put the merged data.
        } else {
            tooldata.put(persistedData, function(response) {
                console.log("Updated Search persistence data success", response);
                isDatabaseEmpty = false;
                if(resolve){
                  resolve();
                }
            }, function(error) {
            	if(!error){
            		return;
            	}
                // If we get a 404 or 204 (for no content) it means that there is no existing file, so we need to use a post to create the file.
                if (error.status === 404 || error.status === 204) {
                    tooldata.post(persistedData, function(response) {
                        console.log("Default data posted to server");
                        isDatabaseEmpty = false;
                        if(resolve){
                          resolve();
                        }
                    });
                // If we get a 412, it means that the data is stale because someone else has updated the
                // tool data since we did our get. In this case we just need to re-get the data, update it again, and then resend.
                } else if (error.status === 412) {
                    var retryData = persistedData;
                    tooldata.get(function(response) {
                        persistedData = JSON.parse(response);
                        persistedData[key] = value;
                        console.log('Retrying update persistence data', persistedData);
                        // Once we have get the new data and updated it, we need to try one more time to write it out.
                        tooldata.put(persistedData, function(response) {
                            console.log("Updated Search persistence data stale", response);
                            isDatabaseEmpty = false;
                            if(resolve){
                              resolve();
                            }
                        }, function (error) {
                            console.error("Unable to update persisted data: RC: " + error.status);  
                        });
                    });
                } else {
                    console.error("Unable to update persisted data: RC: " + error.status);
                }
            });
        }
      }

    /*
     * This method creates the new JSON object to return to the server. This is done in a separate function because when we write out the data
     * we may send back stale data, which will mean that we need to re-get the up to date data and then re-calculate the JSON to send back.
     * 
     */
    var __updateConfig = function(data) {
      var keyToUpdate = data['key'];
  
      // If we don't have any already cached past data our persisted object, we need to create it.
      if (!persistedData.hasOwnProperty(keyToUpdate)) {
        persistedData = new Object();
      }
      persistedData[keyToUpdate] = data[keyToUpdate];
    } // end of __updatePersistedData()
  
    var __hasPastData = function(newData) {
      var keyToUpdate = newData['key'];
  
      // If we don't have any already cached past data our persisted object, we need to create it.
      if (persistedData.hasOwnProperty(keyToUpdate)) {
        return true;
      } else {
        return false;
      }
    } // end of __hasPastData()
    
    return {
      load : __load,
      save : __save,
      update : __updateConfig,
      init : __init
    }; // end of return

})(); 


//This code was lifted from the d3 library.  This logic allows pure javascript to be loaded using html <script> or by AMD loading
if (typeof define === "function" && define.amd) {
define(this.userConfig = userConfig); 
} else if (typeof module === "object" && module.exports) { 
module.exports = userConfig; 
}