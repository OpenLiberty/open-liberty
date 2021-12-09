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
define(['dijit/registry', 'dojo/_base/lang', 'dojo/on', 'dojo/keys', 'jsBatch/utils/utils', 'dojo/request',
        'dijit/layout/BorderContainer', 'dijit/layout/ContentPane', 'dijit/form/Button', 'jsShared/utils/imgUtils', 'dijit/form/TextBox',
        'dojo/i18n!jsBatch/nls/javaBatchMessages', 'jsBatch/utils/viewToHash', 'jsBatch/widgets/DownloadDialog',
        'dojo/dom-construct', 'dojo/dom-class', 'jsBatch/utils/ID',
        'dojo/domReady!'], 
    
function(registry, lang, on, keys, utils, request,
         BorderContainer, ContentPane, Button, imgUtils, TextBox, i18n, viewToHash, DownloadDialog,
         domConstruct, domClass, ID) {

  'use strict';
  /* jshint loopfunc:true */

  return {
    viewId: ID.JOB_LOGS_PAGE,
    updateView: function(params){
      var breadcrumbContainer = registry.byId(ID.BREADCRUMB_CONTAINER);
      var joblogsPage = registry.byId(this.viewId);
      if (!joblogsPage) {
        joblogsPage = this.initPage(params);
        breadcrumbContainer.addChild(joblogsPage);
      }
      
      breadcrumbContainer.getChildren().forEach(function(child){
        if(child.id === joblogsPage.id) {
          breadcrumbContainer._showChild(child);
        } else {
          breadcrumbContainer._hideChild(child);
        }
        child.resize();
      });
      
      domConstruct.create("pre", {innerHTML: params.pageText, tabIndex:'0', style: {wordWrap:'break-word', whiteSpace:'pre-wrap'}}, ID.getJoblogsContentPane(), "only");
      
      var downloadButton = registry.byId(ID.DOWNLOAD_BUTTON);
      downloadButton.onClick = function(){
        var download = imgUtils.getSVG('download-grey');
        var widgetId = ID.DOWNLOAD_BOX;
        try {
          var results = registry.byId(widgetId);
          results.destroy();
       }
       catch(e)
       {
           // no widget
       }
        var downloadDialog = new DownloadDialog({
          id : widgetId,
          title : i18n.FILE_DOWNLOAD,
          description : i18n.DOWNLOAD_DIALOG_DESCRIPTION,
          descriptionIcon : download,
          downloadButtonText: i18n.DOWNLOAD,
          singleZipDownloadFunction : function() {
            window.open(params.downloadAsZip,'_self');
          },
          allZipDownloadFunction : function() {
            window.open(params.downloadAllAsZip, '_self');
          }
        });
        downloadDialog.placeAt(document.body);
        downloadDialog.startup();
        downloadDialog.show();
      };
      
      var bottom = registry.byId(ID.BOTTOM_BUTTON);
      bottom.onClick = function(){
        viewToHash.updateView("joblogs/?jobexecution="+params.executionId);
      };
      
      var top = registry.byId(ID.TOP_BUTTON);
      top.onClick = function(){
        viewToHash.updateView("joblogs/?jobexecution="+params.executionId+"&page=1");
      };
      
      var pageNumberBox = registry.byId(ID.PAGE_NUMBER_BOX);
      var totalPageNumberCount = registry.byId(ID.PAGE_NUMBER_TOTAL);
      totalPageNumberCount.domNode.innerHTML = "<span class='totalPages'>/ "+params.pageCount+"</span>";
      pageNumberBox.set("value", params.pageId);
      on(pageNumberBox, "keydown", function(event) {
        if(event.keyCode === keys.ENTER){
          if(isNaN(pageNumberBox.get("value"))){
            utils.showErrorPopup(i18n.NOT_A_NUMBER,[pageNumberBox.get("value")]);
          } else {
            viewToHash.updateView("joblogs/?jobexecution="+params.executionId+"&page="+pageNumberBox.get("value"));
          }
        }
      });
      
      var next = registry.byId(ID.NEXT_BUTTON);
      next.onClick = function(){
        if(params.pageId < params.pageCount){
          viewToHash.updateView("joblogs/?jobexecution="+params.executionId+"&page="+(params.pageId+1).toString());
        }
      };
      
      var previous = registry.byId(ID.PREVIOUS_BUTTON);
      previous.onClick = function(){
        if(params.pageId > 1){
          viewToHash.updateView("joblogs/?jobexecution="+params.executionId+"&page="+(params.pageId-1).toString());
        }
      };
      
      var logText = registry.byId(ID.getJoblogsContentPane()).domNode;
      if(params.pageId === params.pageCount){
        logText.scrollTop = logText.scrollHeight;
      } else {
        logText.scrollTop = 0;
      }
      
      on(window, 'resize', function() { 
        joblogsPage.resize();
      });
      
    },
  
    initPage: function(params){
      
      var jobLogsBorderContainer = new BorderContainer({
        baseClass: "jobLogsContainer",
        id: this.viewId,
        title: i18n.JOBLOGS_LOG_CONTENT
      });
      
      var joblogs = new ContentPane({
        id: ID.getJoblogsContentPane(),
        region: "center",
        title: i18n.JOBLOGS,
        content: ' ',
        baseClass: 'logContentPane'
      });
      
      var pageNavigationBar = new ContentPane({
        id: ID.getJoblogsNavigationBar(),
        baseClass: "navigationBar",
        region: "bottom",
        title: i18n.LOGS_NAVIGATION_BAR,
        content:' '
      });
      
      var downloadButton = new Button({
        label : i18n.DOWNLOAD,
        id: ID.DOWNLOAD_BUTTON,
        iconClass: 'navigationButtons',
        postCreate: function(){
          this.iconNode.innerHTML = imgUtils.getSVG('download-grey');
        },
        onFocus : function(){
          this.set("style", "background:#eeeeee");
        },
        onBlur : function(){
          this.set("style", "background:#e1e1e1");
        }
      });
      
      var topButton = new Button({
        label : i18n.LOG_TOP,
        showLabel: false,
        id: ID.TOP_BUTTON,
        iconClass: 'navigationButtons',
        postCreate: function(){
          this.iconNode.innerHTML = imgUtils.getSVG('double-caret');
        },
        onFocus : function(){
          this.set("style", "background:#eeeeee;");
        },
        onBlur : function(){
          this.set("style", "background:#e1e1e1");
        }
      });
      
      var endButton = new Button({
        label : i18n.LOG_END,
        showLabel: false,
        id: ID.BOTTOM_BUTTON,
        iconClass: 'navigationButtons',
        postCreate: function(){
          this.iconNode.innerHTML = imgUtils.getSVG('double-caret');
        },
        onFocus : function(){
          this.set("style", "background:#eeeeee");
        },
        onBlur : function(){
          this.set("style", "background:#e1e1e1");
        }
      });
      
      var previousButton = new Button({
        label: i18n.PREVIOUS_PAGE,
        showLabel: false,
        id: ID.PREVIOUS_BUTTON,
        iconClass: 'navigationButtons',
        postCreate: function(){
          this.iconNode.innerHTML = imgUtils.getSVG('caret');
        },
        onFocus : function(){
          this.set("style", "background:#eeeeee");
        },
        onBlur : function(){
          this.set("style", "background:#e1e1e1");
        }
      });
      
      var nextButton = new Button({
        label : i18n.NEXT_PAGE,
        showLabel: false,
        id: ID.NEXT_BUTTON,
        iconClass: 'navigationButtons',
          postCreate: function(){
            this.iconNode.innerHTML = imgUtils.getSVG('caret');
          },
        onFocus : function(){
          this.set("style", "background:#eeeeee");
        },
        onBlur : function(){
          this.set("style", "background:#e1e1e1");
        }
      });
      
      var pageNumberBox = new TextBox({
        baseClass: "pageNumberBox dijitTextBox",
        name: "pageNumber",
        value: "",
        id: ID.PAGE_NUMBER_BOX,
        title: i18n.PAGENUMBER
      });
      
      var pageTotal = new ContentPane({
        baseClass: "logsNavigation",
        id: ID.PAGE_NUMBER_TOTAL
       });
            
      var navPages = new ContentPane({
       baseClass: "pageNavigation"
      });
      
      var buttonsNav = new ContentPane({
        baseClass: "navigationButton" 
      });
      
      //This is to remove all the dojo button styling from the buttons
      domClass.remove(previousButton.domNode.children[0],"dijitButtonNode");
      domClass.remove(nextButton.domNode.children[0],"dijitButtonNode");
      domClass.remove(topButton.domNode.children[0],"dijitButtonNode");
      domClass.remove(endButton.domNode.children[0],"dijitButtonNode");
      domClass.remove(downloadButton.domNode.children[0],"dijitButtonNode");
      
      navPages.addChild(topButton);
      navPages.addChild(previousButton);
      navPages.addChild(pageNumberBox);
      navPages.addChild(pageTotal);
      navPages.addChild(nextButton);
      navPages.addChild(endButton);
      pageNavigationBar.addChild(navPages);
      
      
      buttonsNav.addChild(downloadButton);
      
      pageNavigationBar.addChild(buttonsNav);
      
      
      jobLogsBorderContainer.addChild(joblogs);
      jobLogsBorderContainer.addChild(pageNavigationBar);
      
      return jobLogsBorderContainer;
    }
  };
});