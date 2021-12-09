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
/* jshint strict: false */
define(["dojo/_base/declare", 
        "dijit/layout/ContentPane",
        "dijit/registry",
        "dojo/i18n!jsExplore/nls/explorerMessages"
        ], function( declare, ContentPane, registry, i18n){

      return declare("GraphWarningMessage", ContentPane, {
        
        panelClass: "",
        panelIconClass: "statsUnavailableMessageIcon",
        panelMsgClass: "statsUnavailableMessage",
        dialogPanelClass: "statsUnavailableDialogContentPane",
        dialogIconClass: "statsUnavailableMessageIconDialog",
        dialogMsgClass: "statsUnavailableMessageDialog",
        
        
        iconClass: null,
        msgClass: null,
        iconPane: null, 
        messagePane: null,
        mainPanelClass: null,
        
        constructor : function(params) {          // resourceName
          this.messageText = params.messageText;
          if (params.dialog) {
            this.iconClass = this.dialogIconClass;
            this.msgClass = this.dialogMsgClass;
            this.mainPanelClass = this.dialogPanelClass;
          } else {
            this.iconClass = this.panelIconClass;
            this.msgClass = this.panelMsgClass;
            this.mainPanelClass = this.panelClass;
          }
        },
      
        postCreate: function() {
        
          this.set("class", this.mainPanelClass);
          this.messagePane = new ContentPane({
            "class" : this.msgClass + " statusGraphTitle",
            content: this.messageText
          });
        
          this.iconPane = new ContentPane({
            img: this.iconToDisplay,
            "class" : this.iconClass
          });
          
          this.addChild(this.iconPane);
          this.addChild(this.messagePane);
        },
      
      setTextMessage: function(textMessage) {
        this.messagePane.set("content", textMessage);
      }
    });
});