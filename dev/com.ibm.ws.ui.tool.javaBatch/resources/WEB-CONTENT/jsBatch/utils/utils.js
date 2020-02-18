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
/*****************************************************************************
  * module:
  *   jsBatch/utils/utils
  * summary:
  *   A Util class that contains a list of utilities specific to the
  *   Java Batch Tool
  *
  * @return {Object} Containing all the utils methods
  ****************************************************************************/
define([ 'dojo/_base/lang',
         'dojo/date/locale',
         'dojo/dom',    
         'dojox/html/entities',
         'js/widgets/MessageDialog',
         'jsShared/utils/imgUtils',
         'jsBatch/utils/ID',
         'jsBatch/utils/hashUtils',
         'jsBatch/utils/viewToHash',
         'dojo/i18n!js/widgets/nls/widgetsMessages',
         'dojo/i18n!jsBatch/nls/javaBatchMessages'
       ],

function(lang, locale, dom, entities, MessageDialog,
         imgUtils, batchID, hashUtils, viewToHash, widgetMessagei18n, i18n) {
  "use strict";

  return {
    stateStatusToLabel: __stateStatusToLabel,
    stateToImage: __stateToImage,
    statusToImage: __statusToImage,
    showErrorPopup: __showErrorPopup,
    errorMessageRestFail: __errorMessageRestFail,
    extractServerParts: __extractServerParts,
    formatDate: __formatDate,
    getDate: __getDate,
    getFormattedDate: __getFormattedDate,
    getFormattedTime: __getFormattedTime,
    showPopupRelatedToDatabaseConfigurationIssues : __showPopupRelatedToDatabaseConfigurationIssues,
    showPopupForIgnoredSearchCriterias : __showPopupForIgnoredSearchCriterias,
    showPopupForAuthorizationIssues : __showPopupForAuthorizationIssues    
  };

  function __errorMessageRestFail(defaultMessage, err){
    var message = defaultMessage;
    var details = "";
    if(err.response.text){
      details = err.response.text;
    } else {
      details = err;
    }

    __showErrorPopup(message, details);
  }


  /**
   * Call this method to show an error popup that overlays the entire browser.
   * @param msg - this must be a translated message!
   * @param details - optional parameter to add additional text to popup
   */
  function __showErrorPopup(msg, details) {
    if(! details) {
      details = "";
    }

    var errorMessageDialog = new MessageDialog({
      id : batchID.ERROR_DIALOG,
      title : i18n.ERROR,
      messageText : msg,
      additionalText : details, // dojo will break if undefined
      messageDialogIcon : "#status-alert-gray-small",
      okButtonLabel: i18n.GO_TO_DASHBOARD,
      okFunction : function() {
        viewToHash.updateView("");
      },
      hide: function() {
        this.destroyRecursive();
        history.back(-1);
      }

    });
    errorMessageDialog.placeAt(document.body);
    errorMessageDialog.startup();
    errorMessageDialog.show();
  }

  /**
   * Given the state/status value, return the string used to represent that state/status value.
   *
   * @param value - String - state/status value
   */
  function __stateStatusToLabel(value) {
    switch (value) {
      case 'SUBMITTED':
        return i18n.SUBMITTED;
      case 'DISPATCHED':
        return i18n.DISPATCHED;
      case 'COMPLETED':
        return i18n.COMPLETED;
      case 'STOPPED':
        return i18n.STOPPED;
      case 'FAILED':
        return i18n.FAILED;
      case 'ABANDONED':
        return i18n.ABANDONED;
      case 'JMS_CONSUMED':
        return i18n.JMS_CONSUMED;
      case 'JMS_QUEUED':
        return i18n.JMS_QUEUED;
      case 'STARTED':
        return i18n.STARTED;
      case 'STARTING':
        return i18n.STARTING;
      case 'STOPPING':
        return i18n.STOPPING;
      default:
        console.error('State/Status value unknown: ' + value);
        return value;
    }
  }

  /**
   * Return the SVG used to represent the state value.
   *
   * @param value - String - state value
   */
  function __stateToImage(value) {
    switch (value) {
      case 'SUBMITTED':
        return imgUtils.getSVGSmall('submitted');
      case 'DISPATCHED':
        return imgUtils.getSVGSmall('dispatched');
      case 'COMPLETED':
        return imgUtils.getSVGSmall('completed');
      case 'STOPPED':
        return imgUtils.getSVGSmall('stopped');
      case 'FAILED':
        return imgUtils.getSVGSmall('failed');
      case 'ABANDONED':
        return imgUtils.getSVGSmall('abandoned');
      case 'JMS_CONSUMED':
        return imgUtils.getSVGSmall('jmsconsumed');
      case 'JMS_QUEUED':
        return imgUtils.getSVGSmall('jmsqueued');
      default:
        console.error('State value unknown: ' + value);
        return "";
    }
  }

  /**
   * Return the SVG or GIF (for animation) used to represent the status value.
   *
   * @param value - String - status value
   */
  function __statusToImage(value) {
    switch (value) {
      case 'STARTED':
        return imgUtils.getSVGSmall('started');
      case 'STARTING':
        return "images/status-starting.gif";
      case 'STOPPING':
        return "images/status-stopping.gif";
      case 'COMPLETED':
        return imgUtils.getSVGSmall('completed');
      case 'STOPPED':
        return imgUtils.getSVGSmall('stopped');
      case 'FAILED':
        return imgUtils.getSVGSmall('failed');
      case 'ABANDONED':
        return imgUtils.getSVGSmall('abandoned');
      default:
        console.error('Status value unknown: ' + value);
        return "";
    }
  }

  /**
   * Extracts the server parts from the server ID.  Returns the
   * Host, User Directory, and Server Name.
   *
   * @param serverId - string - 'host/userDir/serverName' of the server Id returned
   *                            by the Java Batch API
   *
   * @return obj - {
   *                host,
   *                userDir,
   *                serverName
   *               }
   */
  function __extractServerParts(serverId) {
    var serverObj = { host: '',
                      userDir: '',
                      serverName: ''
                    };

    if (!serverId) {
      return serverObj;
    }

    // Replace any backward slashes ('\') with a forward slash ('/')
    var server = serverId.replace(/\\/g, "/");

    // Host is part up to the first '/'
    var firstSlashIndex = server.indexOf('/');
    if (firstSlashIndex > -1) {
        serverObj.host = server.substring(0, firstSlashIndex);
    }

    // Server Name is part following the last '/'
    var lastSlashIndex = server.lastIndexOf('/');
    if (lastSlashIndex > -1) {
        serverObj.serverName = server.substring(lastSlashIndex+1);
    }

    // User Directory is part inbetween!
    if (firstSlashIndex > -1 && lastSlashIndex > -1 && (firstSlashIndex !== lastSlashIndex)) {
        var userDir = server.substring(firstSlashIndex +1, lastSlashIndex);
        if (userDir.indexOf(':/') === 1) {
            // Convert drive letter to upper case for use with the Collective APIs.
            userDir = userDir.substring(0,1).toUpperCase() + userDir.substring(1);
        }
        serverObj.userDir = userDir;
    }

    return serverObj;
  }

  /**
   * Dates in the Batch database are stored in UTC format.   However, the REST API
   * returns dates using the following:
   *      return SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS Z");  (per Scott Kurz)
   *
   * For display on the UI, we will use formatLength "medium", which in en-us is
   * in the format of
   *      Oct 6, 2016, 3:05:08 PM
   *
   * @param dateString - date string
   * @param linkable - optional: 0 for plain date/time, 1 for clickable date style with hover text
   *                   If not specified, defaults to 0 (as undefined)
   */
  function __formatDate(dateString, linkable) {
    //NOTE: dojo parses this incorrectly. it takes the date/time as is, ignores the given timezone, and tacks on the machine's timezone
    // It just so happens that because it keeps the date/time as is, it keeps the server time which is what we want.
    var date = locale.parse(dateString, { datePattern: "yyyy/MM/dd HH:mm:ss.SSS Z",
                                          selector: 'date'});
    var formattedDate, formattedDateTimeZone, dateTitle, innerHTML;
    if (date) {
      if(dojo.locale === 'ar' || dojo.locale.indexOf('ar-') > -1){
        // Arabic requires special logic for formatting its dates, because Dojo doesn't display correctly with the default formatting or Gregorian bundle formats
        // We have to append two date patterns since the day of the month doesn't appear to the right of the month name even if the datePattern is rearranged.

        // Special unicode characters are needed to allow concatenating rtl strings
        formattedDate = locale.format(date,{
          selector: "date",
          formatLength: "medium",
          datePattern: "MMMM, yyyy'\u200e' dd"
        });
        var time = locale.format(date,{
          selector: "date",
          formatLength: "medium",
          datePattern: "a'\u200e' HH:mm"
        });
        formattedDateTimeZone = locale.format(date,{
          selector: "date",
          formatLength: "long",
          datePattern: "a'\u200e' HH:mm:ss MMMM, yyyy'\u200e' dd z"
          //NOTE: not sure if the timezone (z) should go after time, or at end of dateTime string
        });
        if (!linkable) {
          innerHTML = time + " " + formattedDate;
        } else {
          dateTitle = lang.replace(i18n.SEARCH_ON, [formattedDate, i18n.LAST_UPDATE]);
          innerHTML =  time + "<a title='" + dateTitle + "'> " + formattedDate + "</a>";
        }

      }
      else{
        formattedDate = locale.format(date, { formatLength: "medium", selector: "date" });
        var formattedDateTime = locale.format(date, { formatLength: "medium"});
        formattedDateTimeZone = locale.format(date, { formatLength: "long"});

        var dateStart = formattedDateTime.indexOf(formattedDate);
        if (!linkable || dateStart === -1) { // if we do not want it linkable or if date is not found in string
          // return plain formattedDate for ExecutionGrid
          innerHTML = formattedDateTime;
        } else {
          var dateEnd = dateStart + formattedDate.length;
          dateTitle = lang.replace(i18n.SEARCH_ON, [formattedDate, i18n.LAST_UPDATE]);
          innerHTML = formattedDateTime.slice(0, dateStart) + "<a title='" + dateTitle + "'>" + formattedDate + "</a>" + formattedDateTime.slice(dateEnd);
        }

      }
      return "<span title='" + formattedDateTimeZone + "'>" + innerHTML + "</span>";
    } else {
      return "";
    }
  }

  /**
   * Return server date only, for use in 'lastUpdate' URL parameter
   */
  function __getDate(dateString) {
    var date = locale.parse(dateString, { datePattern: "yyyy/MM/dd HH:mm:ss.SSS Z", selector: 'date'});
    if (date) {
      return locale.format(date, { datePattern: "yyyy-MM-dd", selector: 'date' });
    } else {
      return "";
    }
  }

  /**
   * Return server date only, formatted
   */
  function __getFormattedDate(dateString) {
    var date = locale.parse(dateString, { datePattern: "yyyy/MM/dd HH:mm:ss.SSS Z", selector: 'date'});
    if (date) {
      return locale.format(date, { formatLength: "medium", selector: "date" });
    } else { return ""; }
  }

  /**
   * Return server time only, formatted
   */
  function __getFormattedTime(dateString) {
    var date = locale.parse(dateString, { datePattern: "yyyy/MM/dd HH:mm:ss.SSS Z", selector: 'date'});
    if (date) {
      return locale.format(date, { formatLength: "medium", selector: "time" });
    } else { return ""; }
  }

  /**
   *
   */
  function __showPopupRelatedToDatabaseConfigurationIssues() {
    __showPopupErrorMessageDialog(batchID.ERROR_DIALOG_FOR_IN_MEMORY_DB, i18n.PERSISTENCE_CONFIGURATION_REQUIRED);
  }

  /**
   * show popup dialog with the unauthorized message.
   *
   * @param errorMsg error message to be displayed in the popup dialog
   */
  function __showPopupForAuthorizationIssues(errorMsg) {
    __showPopupErrorMessageDialog(batchID.ERROR_DIALOG_FOR_AUTHORIZATION, errorMsg);
  }

  /**
   *
   */
  function __showPopupForIgnoredSearchCriterias(ignoredFields) {
    ignoredFields = entities.encode(ignoredFields);

    var msg = lang.replace(i18n.IGNORED_SEARCH_CRITERIA, [ignoredFields]);

    var errorMessageDialog = new MessageDialog({
      id : batchID.ERROR_DIALOG_FOR_IGNORED_SEARCH_CRITERIA,
      title : i18n.WARNING,
      displayOkButton : false,
      messageText : msg,
      messageDialogIcon : "#status-alert-gray-small",
      okFunction : function() {
        // Just hide the popup.  Don't go back to the last url.
        this.hide();
      }
    });
    errorMessageDialog.placeAt(document.body);
    errorMessageDialog.startup();
    errorMessageDialog.show();
  }

  /*
   * Show popup dialog using the passed in error Id and message. If the tool is initiated
   * from the toolbox, show button to go to toolbox, otherwise show ok and stay in the dashboard.
   */
  function __showPopupErrorMessageDialog(errorId, errorMsg) {
    var okButtonLabel = __getPopupDialogOKButtonLabel();
    var errorMessageDialog = new MessageDialog({
      id : errorId,
      title : i18n.ERROR,
      messageText : errorMsg,
      messageDialogIcon : "#status-alert-gray-small",
      okButtonLabel: okButtonLabel,
      okFunction : function() {
        __okButtonAction(okButtonLabel);
      },
      hide: function() {
        __okButtonAction(okButtonLabel);
      }
    });
    errorMessageDialog.placeAt(document.body);
    errorMessageDialog.startup();
    errorMessageDialog.show();
  }

  /**
   * If Java Batch is initialized from the toolbox, use Go to Toolbox as the OK button label.
   * Otherwise use OK as the label.
   *
   * @returns OK label to be used in the popup dailog
   */
  function __getPopupDialogOKButtonLabel() {
    var loc = window.parent.location;
    var devOrStandaloneVersion = (loc.href.indexOf('devJavaBatch') > -1) ||
                                 (loc.href.indexOf('ibm/adminCenter/javaBatch') > -1);

    var okButtonLabel = widgetMessagei18n.TOOLBOX_BUTTON_GO_TO;
    if (devOrStandaloneVersion) {
       okButtonLabel = i18n.OK_BUTTON_LABEL;
    }

    return okButtonLabel;
  }

  /**
   * If button label is OK, dismiss the loading spinner and stay in the dashboard. Otherwise
   * remove the hash in the URL to go back to the toolbox.
   *
   * @param okButtonLabel Label used for the ok button
   */
  function __okButtonAction(okButtonLabel) {
    if (okButtonLabel === i18n.OK_BUTTON_LABEL) {
      // remove the loading spinner and stay in the dashboard
      var loadingView = dom.byId(batchID.LOADING_VIEW);
      if (loadingView) {
        loadingView.style.display = "none";
      }
    } else {
      // go back to the toolbox
      hashUtils.removeHash();
    }
  }
  
});
