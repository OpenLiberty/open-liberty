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
define(["dojo/_base/declare",
        "dojo/dom-class",
        "dijit/_Widget", 
        "dijit/_TemplatedMixin",
        "dijit/_WidgetsInTemplateMixin",
        "dijit/Dialog",
        "jsShared/utils/imgUtils",
        "dojo/text!./templates/DownloadDialog.html", 
        "dojo/i18n!jsBatch/nls/javaBatchMessages",
        "dojo/_base/lang",
        "dojo/on",
        "dojo/dom",
        "dojo/domReady!"],
        function(
            declare,
            domClass,
            Widget,
            TemplatedMixin,
            WidgetsInTemplateMixin,
            Dialog,
            imgUtils,
            template,
            i18n,
            lang,
            on,
            dom) {
  //"use strict";
  /* jshint strict: false */  
  return declare("downloadDialog", [Dialog], {
    constructor: function() {
    },

    baseClass: "downloadDialog acDialog",
    title: 'default',
    descriptionIcon: '',
    description: 'default',
    
    hide: function() {
      this.inherited(arguments).then(lang.hitch(this, function(){
        this.destroyRecursive();
      }));
    },
    
    cancelButtonLabel : i18n.CLOSE,
    downloadToggleCheckbox : i18n.INCLUDE_ALL_LOGS,
    downloadButtonText: i18n.DOWNLOAD,
    downloadButtonAriaLabel: i18n.DOWNLOAD_ARIA,
    downloadButtonFunction: undefined,
    downloadButtonClass: 'downloadButton',
      
    postCreate : function(){
      
      this.inherited(arguments);
      
      domClass.add(this.containerNode, "acDialogContentPane");
      domClass.add(this.titleBar, "acDialogTitleBar");
      domClass.add(this.titleNode, "acDialog_title");
      
      this.closeButtonNode.classList.add("cancelIcon");
      
      var me = this;
      // build the content from the template and inputs
      console.log("me: ", this);
      
      var contents = new (declare([Widget, TemplatedMixin, WidgetsInTemplateMixin], {
        //these are created for the HTML template to use
        templateString: template,

        descriptionIcon: imgUtils.getSVGSmallName('download-grey'),
        description: me.get("description"),

        cancelButtonLabel : me.get("cancelButtonLabel"),
        downloadToggleCheckbox: me.get("downloadToggleCheckbox"),
        downloadButtonText: me.get("downloadButtonText"),
        downloadButtonAriaLabel: me.get("downloadButtonAriaLabel"),
        downloadButtonClass: me.get("downloadButtonClass"),
        downloadButtonTabIndex: 0

      }))();
      contents.startup();

      this.set("content", contents);
      
      var downloadSingleLogZip = function() {
        try {
          me.singleZipDownloadFunction();
        } catch(e) {
          console.log('Failed to invoke the singleZipDownload', e);
        }
      };
      
      var downloadAllLogsZip = function() {
        try {
          me.allZipDownloadFunction();
        } catch(e) {
          console.log('Failed to invoke the allZipDownload', e);
        }
      };
      
      me.downloadSignal = on(contents.downloadButton, "click", function(){
        if (dom.byId('zipToggle').checked === true) {
          downloadAllLogsZip();
        } else {
          downloadSingleLogZip();
        }
          
          // Clean up the dialog DOM
          me.destroyRecursive();
      });
      
      var includeAllFilesCheckbox = dom.byId('zipToggle');
      includeAllFilesCheckbox.onchange = function(e) {
        if (e.target.checked) {
          e.target.setAttribute('aria-checked', 'true');
        } else {
          e.target.setAttribute('aria-checked', 'false');
        }
      };
    }
  });
});
