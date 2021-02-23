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

define(['jsBatch/views/dashboardView',
        'dojo/_base/lang',
        'dojo/promise/all',
        'dijit/registry',
        'jsBatch/utils/hashUtils',
        'jsBatch/utils/ID',
        'jsBatch/utils/restUtils',
        'jsBatch/utils/viewToHash',
        'jsBatch/utils/utils',
        'jsBatch/views/executionDetailsView',
        'jsBatch/views/jobLogsView',
        'jsBatch/views/loadingView',
        'dojo/json',
        'dojo/topic',
        'dojo/i18n!jsBatch/nls/javaBatchMessages',
        'js/toolbox/toolHash',
        'jsShared/utils/ID'
        ],
        function(dashboardView,
                 lang,
                 all,
                 registry,
                 hashUtils,
                 batchID,
                 restUtils,
                 viewToHash,
                 utils,
                 executionDetailsView,
                 jobLogsView,
                 loadingView,
                 JSON,
                 topic,
                 i18n,
                 toolHash,
                 sharedID) {
  "use strict";

  function startsWith(string, prefix) {
    return ((typeof string === 'string') && (string.indexOf(prefix) === 0));
  }

  return{
    updateView : __updateView
  };


  function __jobLogsCalls(paramsList){
    var jobinstance = paramsList.jobinstance;
    delete paramsList.jobinstance;

    var numberOfParams = Object.keys(paramsList).length;

    if( numberOfParams <= 2 && numberOfParams > 0){
      if(paramsList.jobexecution){
        restUtils.getWithParms(restUtils.JOB_EXECUTION_LOGS_QUERY ,[paramsList.jobexecution]).then(function(response){
          response = JSON.parse(response.data);
          var totalPages = 0;
          var allLinks = {};
          var href = "";
          response.forEach(function(obj){
            href = obj.href;
            if(href.match(/\/ibm\/api\/batch\/jobexecutions\/[0-9]+\/joblogs\?part=part\.[0-9]+\.log&type=text$/)){
              totalPages += 1;
              allLinks[totalPages] = obj.href;
            }
          });
          totalPages = Object.keys(allLinks).length;
          if(totalPages === 0){
            utils.showErrorPopup(i18n.URL_NO_LOGS);
            return 0;
          }
          var page = parseInt(paramsList.page, 10);
          if(page <= 0 || page > totalPages){
            utils.showErrorPopup(i18n.URL_PAGE_PARAMS_ERROR);
            return 0;
          }
          else if(!page){
            page = totalPages;
          }
          var url = allLinks[page];
          var uri = getUri(url);
          restUtils.get(uri).then(function(secondResponse) {
            jobLogsView.updateView({pageId: page, pageText: secondResponse.data, pageCount: totalPages, executionId: paramsList.jobexecution, downloadAsText: url, downloadAsZip: url.slice(0,-4)+"zip", downloadAllAsZip: response[1].href});
            var logFileName = getLogFilenameFromUri(uri);
            setBreadcrumbHelper(jobinstance, paramsList.jobexecution, logFileName, page);
          },
          function(err){
            utils.errorMessageRestFail(i18n.REST_CALL_FAILED, err);
          });

        },
        function(err){
          utils.errorMessageRestFail(i18n.REST_CALL_FAILED, err);
        });
      }
    } else {
      utils.showErrorPopup(i18n.NO_JOB_EXECUTION_URL);
    }
  }


  function __updateView(hash) {
    /*
     * We are switching to a new view, which means any lingering error popups
     * were caused from the previous view.  Since it was the previous view, remove
     * the error popup.  If the new view we are switching to causes an error,
     * this method should generate a new error popup.  This method is to prevent stale
     * popups from lingering on the view.
     */
    clearAnyErrorPopups();

    /*
     * While we wait for REST calls the update to the view, show the loading view.
     * That means hide all the views, but the loading view.
     */
    showLoadingView();

    if(startsWith(hash, hashUtils.getToolId())){
      var uri = hash.split("/");

      var isDashboardViewUrl = startsWith(hash, 'javaBatch/?') || // FIXME: change "/?" to "?"
                    hash === "javaBatch" ||
                    hash === "javaBatch/" ||
                    startsWith(hash, 'javaBatch/search/');

      if(isDashboardViewUrl) {
        goToDashboardView(hash);
        setBreadcrumbHelper(); // clear all breadcrumb
      } else if(startsWith(hash, 'javaBatch/')){
        // We are opening other views
        var view = uri[1];
        if(view === "jobexecutions") {
          var executionId = uri[2];
          var executionDetails = null;
          var jobInstanceId = null;
          var jobInstanceDetails = null;
          var stepsDetails = null;
          if(executionId) {
            restUtils.getWithParms(restUtils.EXECUTION_DETAIL_QUERY,[executionId]).then(function(response){
              executionDetails = JSON.parse(response.data);
              jobInstanceId = executionDetails.instanceId;

              restUtils.getWithParms(restUtils.JOB_INSTANCE_DETAIL_QUERY,[jobInstanceId]).then(function(response){
                  jobInstanceDetails = JSON.parse(response.data);

                  restUtils.getWithParms(restUtils.EXECUTION_STEPS_DETAIL_QUERY,[executionId]).then(function(response){
                    stepsDetails = JSON.parse(response.data);
                    executionDetailsView.updateView({executionId: executionId, executionDetails: executionDetails, jobInstanceDetails: jobInstanceDetails, stepsDetails: stepsDetails});
                    setBreadcrumbHelper(jobInstanceId, executionId);
                  });
              });
            },
            function(err){
              utils.errorMessageRestFail(i18n.REST_CALL_FAILED, err);
            });
          } else {
            utils.showErrorPopup(i18n.MISSING_EXECUTION_ID_PARAM);
          }
        } else if(view === "joblogs"){
          var jobLogsViewQueryParams = ["page","jobinstance","jobexecution"];
          var params = uri[2].replace("?","").split("&");
          var paramsList = {};
          for(var i = 0; i < params.length; i++){
            var temp = params[i].split("=");
            if(isNaN(temp[1])){
              utils.showErrorPopup(lang.replace(i18n.NOT_A_NUMBER,[temp[0]]));
              return 0;
            }
            if(jobLogsViewQueryParams.indexOf(temp[0]) > -1 && !paramsList[temp[0]]){
              paramsList[temp[0]]=temp[1];
            } else if(paramsList[temp[0]]){
              utils.showErrorPopup(lang.replace(i18n.PARAMETER_REPETITION, [temp[0]]));
              return 0;
            }
            else {
              utils.showErrorPopup(lang.replace(i18n.INVALID_PARAMETER, [temp[0]]));
              return 0;
            }
          }

          if(paramsList.jobinstance && paramsList.jobexecution){
            utils.showErrorPopup(i18n.URL_MULTIPLE_ATTRIBUTES);
            return 0;
          }

          if(paramsList.jobinstance) {
            goToMostRecentJobExecution(paramsList);
          } else {
            // We do not know the job instance id, so go get it so we can use
            // the instance id in the breadcrumb

            // This seems the cleanest place in the code flow to get the instance id, but its a performance hit
            // to make a dedicated REST API call just to get the instance id.
            restUtils.getWithParms(restUtils.EXECUTION_DETAIL_QUERY,[paramsList.jobexecution]).then(
                function(response) {
                    var jobExecutionDetails = JSON.parse(response.data);
                    paramsList.jobinstance = jobExecutionDetails.instanceId;
                    __jobLogsCalls(paramsList);
                }
            );
          }

        }else {
          utils.showErrorPopup(i18n.NO_VIEW);
          console.log("You are on a wrong view");
          return 0;
        }
      } else {
        // FIXME: What is this else clause for?!
        restUtils.get(restUtils.URL).then(function(response){
          dashboardView.updateView("", response.data);
        },
        function(err){
          utils.errorMessageRestFail(i18n.REST_CALL_FAILED, err);
        });
      }

    }else{
      utils.showErrorPopup(lang.replace(i18n.WRONG_TOOL_ID, [hashUtils.getToolId(), hash]));
      console.log('The hash did not start with the tool ID ' + hashUtils.getToolId() + ', but instead ' + hash);
      return 0;
    }
  }

  /**
   * This method will attempt to return everything to the right of the domain in the fullUrl.
   */
  function getUri(fullUrl) {
    var uri = "";
    var link = document.createElement('a');
    link.href = fullUrl;
    // Example: https://localhost:9443/ibm/api/batch/jobexecutions/108/joblogs?part=part.1.log&type=text
    // pathname = /ibm/api/batch/jobexecutions/108/joblogs
    // search = ?part=part.1.log&type=text
    uri = link.pathname + link.search;

    if(uri[0] !== '/') {
      // IE does NOT preserve the / when using link.pathname.  This causes the xhr call to act
      // very weird.  The expected behavior is to have have XHR call using the uri and the
      // browser to automatically know the uri relative to the current domain in the browser's url.
      uri = '/' + uri;
    }

    return uri;
  }

  /**
   * here job instance is being replaced by job execution in paramsList because that's how we
   * intend to use our api i.e. if user requests for logs of an instance we show them logs of
   * last executed execution's last page if page is not specified.
   */
  function goToMostRecentJobExecution(paramsList) {
    restUtils.getWithParms(restUtils.JOB_INSTANCE_LOGS_QUERY,[paramsList.jobinstance]).then(function(response){
      response = JSON.parse(response.data);
      var href = response[0].href;
      // here the job execution is being extracted from the response href - get the substring such that
      // we get jobexecution/jobexecutionId and then split on / to get the jobexecutionId
      href = href.substring(href.indexOf("jobexecutions/"),href.indexOf("/joblogs"));
      var jobexecution = href.split("/")[1];
      paramsList.jobexecution = jobexecution;
      changeQueryStringFromInstanceToExecution(jobexecution);
    },
    function(err){
      utils.errorMessageRestFail(i18n.REST_CALL_FAILED, err);
    }).then(function(){
      __jobLogsCalls(paramsList);
    });
  }

  /**
   * This method will change the browser url's query string when using jobinstance={id}
   * When an end user uses
   */
  function changeQueryStringFromInstanceToExecution(jobExecutionId) {
    if(! jobExecutionId) {
      return;
    }

    var currentHash = window.top.location.hash;
    // Isolate "jobinstance={instance_id}" and replace with "jobexecution={execution_id}"
    var questionMarkIndex = currentHash.indexOf('?');
    if(questionMarkIndex === -1) {
      // No query strings present, NOOP
      return;
    }

    var newQueryString = "?";
    var currentQueryString = currentHash.substring(questionMarkIndex + 1); // exclude the ?
    currentQueryString = currentQueryString.split('&');
    for(var i = 0; i < currentQueryString.length; i++) {
      // Cycle through all the existing query string and build a new hash.  During build,
      // make sure to replace jobinstance with jobexecution

      var singleParameter = currentQueryString[i];
      if(i !== 0) {
        // Since we removed the & via the split, add them back
        newQueryString += "&";
      }
      var containsJobInstanceString = singleParameter.indexOf("jobinstance=") !== -1;
      if(containsJobInstanceString) {
        newQueryString += "jobexecution=" + jobExecutionId;
      } else {
        newQueryString += singleParameter;
      }
    }
    // Set the browser to the new hash that will _never_ have jobinstance in the query string!
    var newUrl = window.top.location.protocol + "//" + window.top.location.host + window.top.location.pathname + currentHash.substring(0, questionMarkIndex) + newQueryString;

    // .replace will prevent the previous URL from being saved in the browser's history.  The previous url will contain the jobinstance query string.
    // The jobinstance url will redirect to the jobexecution url.  Without preventing history from
    // keeping of jobinstance url, the back button in a browser will take users back to jobinstance and then an autodirect to jobexecution.
    // The browser history will forever loop between jobinstance and jobexecution when using the back button because we created an infinite
    // loop with the browser history due to the redirect behavior.
    window.top.location.replace(newUrl);
  }

  function goToDashboardView(hash) {
    var query = "?sort=-lastUpdatedTime";
    if(hash.indexOf("?") > -1) {
      // We have existing query params, so we must make sure we add a
      // ampersand (&) between the sort= and existing query string
      query += '&' + hash.substring(hash.indexOf("?") + 1);
    }

    var getJobInstancesQuerySizePromise = restUtils.getJobInstancesQuerySize();
    var getPersistedDataPromise = restUtils.getPersistedData();



    // Add in the query size value for the dashboard view
    all([getJobInstancesQuerySizePromise, getPersistedDataPromise]).then(function(data) {
      var querySize = data[0];
      var persistedData = data[1];
      if (! querySize) {
        querySize = restUtils.JOB_INSTANCES_DEFAULT_QUERY_SIZE;
      }
      query = query + '&pageSize=' + querySize + '&ignoreCase=true';
      dashboardView.persistedData = persistedData;
      dashboardView.updateView(query);

      restUtils.get(restUtils.v4JobInstances, query).then(function(response) {
        handleAnyIgnoredQueryParameters(response);
        // Noticed that the response.data is "[]" when the database is empty
        dashboardView.updateView(null, response.data);
      }, function(err) {
        console.log("---------- err calling rest api", err);
        handleDashboardRestCallError(err);
      });
    });
  } // end of goToDashboardView


  /**
   * Show only the loading view. The other views are made hidden
   */
  function showLoadingView() {
    var breadcrumbContainer = registry.byId(batchID.BREADCRUMB_CONTAINER);
    breadcrumbContainer.getChildren().forEach(function(child){
      if(child.id === batchID.LOADING_VIEW) {
        breadcrumbContainer._showChild(child);
      } else {
      breadcrumbContainer._hideChild(child);
      }
      child.resize();
    });
  }

  /**
   * Destroy the error popup
   */
  function clearAnyErrorPopups() {
    var errorDialogPopup = registry.byId(batchID.ERROR_DIALOG);
    if(errorDialogPopup) {
      errorDialogPopup.destroyRecursive();
    }

    errorDialogPopup = registry.byId(batchID.ERROR_DIALOG_FOR_IGNORED_SEARCH_CRITERIA);
    if(errorDialogPopup) {
      errorDialogPopup.destroyRecursive();
    }

    errorDialogPopup = registry.byId(batchID.ERROR_DIALOG_FOR_IN_MEMORY_DB);
    if(errorDialogPopup) {
      errorDialogPopup.destroyRecursive();
    }
  }

  /**
   * Handle any unsuccessful REST API calls for the dashboard view
   */
  function handleDashboardRestCallError(err) {
    if(err && err.response && err.response.status === 500) {
      var errorMsg = err.response.text;
      var messageKey = "CWWKY0152E";
      var message = "The REST URL search parameters requesting this function are not supported by the Java batch memory-based persistence configuration";
      if(errorMsg && (errorMsg.indexOf(messageKey) > -1)) {
        // Batch REST API indicating persisted database is not configured
        utils.showPopupRelatedToDatabaseConfigurationIssues();
        return;
      }
    } else if (err && err.response && err.response.status === 401) {
      // HTTP 401 is for unauthorized error
      var errorMessage = err.response.text;
      if(errorMessage) {
        // Batch REST API indicating authorization problem. Going back to the toolbox if java batch tool is initiated from the toolbox.
        utils.showPopupForAuthorizationIssues(errorMessage);
        return;
      }
    }

    utils.errorMessageRestFail(i18n.REST_CALL_FAILED, err);
    return;
  }

  /**
   * Show a popup when the server side indicates query parameters were ignored
   * during a REST API call
   */
  function handleAnyIgnoredQueryParameters(response) {
    var header = "X-IBM-Unrecognized-Fields";
    var ignoredFields = response.getHeader("X-IBM-Unrecognized-Fields");
    if(ignoredFields) {
      utils.showPopupForIgnoredSearchCriterias(ignoredFields);
    }
  }

  /**
   * This method is the controller of the breadcrumb and determines what the
   * breadcrumb should contain.
   * @param jobInstanceId
   * @param jobExecutionId
   * @param jobLogId
   * @returns
   */
  function setBreadcrumbHelper(jobInstanceId, jobExecutionId, jobLogId, page) {
    var breadcrumb = registry.byId(sharedID.getBreadcrumbPane());

    if(!jobInstanceId && !jobExecutionId) {
      breadcrumb.resetBreadcrumbPane();
      return;
    }
    breadcrumb.setBreadcrumb(jobInstanceId, jobExecutionId, jobLogId, page);
  }

  function getLogFilenameFromUri(uri) {
    var queryString = uri.substring(uri.indexOf('?') + 1);
    var parameters = queryString.split('&');
    for(var i = 0; i < parameters.length; i++) {
      var singleParam = parameters[i];
      if(singleParam.indexOf("part=") === 0) {
        var keyAndValue = singleParam.split("=");
        return keyAndValue[1]; // return the value, right of the equal sign
      }
    }
    return "Unknown";
  }

});
