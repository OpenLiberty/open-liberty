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
/* 
 * create a server type card using template
 */
define([
        "dojo/_base/declare", 
        "dojo/dom",
        "dijit/_WidgetBase", 
        "dijit/_TemplatedMixin",
        "dijit/_WidgetsInTemplateMixin",
        "dojox/string/BidiComplex",
        "jsExplore/widgets/shared/BaseCard",
        "jsExplore/resources/utils",
        "dojo/text!./templates/AppOnServerCard.html",
        "jsShared/utils/imgUtils"
        ], 
        function( 
            declare, 
            dom,
            WidgetBase, 
            _TemplatedMixin, 
            _WidgetsInTemplateMixin, 
            BidiComplex,
            BaseCard,
            utils,
            template,
            imgUtils
        ) {

  var Card = declare("AppOnServerCard", [ BaseCard, WidgetBase, _TemplatedMixin, _WidgetsInTemplateMixin ], {
    templateString : template,
    cardBottomServerDivId : "",
    cardBottomServerImgAlt : "",
    cardBottomServerImgId : "",
    cardBottomServerTitle : "",
    cardBottomServerTitleId : "",
    cardBottomServerDir : "",
    cardBottomUserDirDivId : "",
    cardBottomUserDirDisplayTitle : "",
    cardBottomUserDirClass : "",
    cardBottomServerIcon : imgUtils.getSVGSmallName('server'),
    cardBottomHostIcon : imgUtils.getSVGSmallName('host'),

    postMixInProperties : function() {     
      this._setCardResourceIcon(false);
      this._setCardLabel();
      this._setResourceState();
      this._setServerData();
      this._setCardBottomUserDir(this.resource.server.userdir);
      this._setHostData(this.resource.server);
      this._init();
    },

    postCreate : function() {
      this.inherited(arguments);
      this._setCardResourceIcon(true);
      this.connect(this.domNode, "onclick", this.onClick);
    },

    /*
     * Setup the server data related to the resource.
     */
    _setServerData: function() {
      this.cardBottomServerDivId = this.id + this.resource.server.id + "server";
      this.cardBottomServerDir = utils.getStringTextDirection(this.resource.server.name);
      this.cardBottomServerTitleId = this.cardBottomServerDivId + "Title";
      this.cardBottomServerTitle = this.resource.server.name;
      this.cardBottomServerImgId = this.cardBottomServerDivId + "Img";
      this.cardBottomServerImgAlt = this.resource.server.name;
    },

    /*
     * Setup the user dir related to the resource server.
     */
    _setCardBottomUserDir: function(userDir) {
      this.cardBottomUserDirDivId = this.id + this.resource.server.id + "usrDir";
      this.cardBottomUserDirTitleId = this.cardBottomUserDirDivId + "Title";
      this.cardBottomUserDirTitle = userDir.replace(/"/g, "&quot;");
      if (utils.getBidiTextDirectionSetting() !== "ltr") {
        this.cardBottomUserDirDisplayTitle = BidiComplex.createDisplayString(userDir, "FILE_PATH");
      } else {
        this.cardBottomUserDirDisplayTitle = userDir;
      }
      this.cardBottomUserDirClass = "cardBottomListedStatUserDir";           
      var userDirLength = this._getStringPixel(userDir);    
      // max width of the server card top is 162px
      if (userDirLength < 175) {          
        this.cardBottomUserDirClass = "cardBottomListedStatUserDirNoTruncate";    
      }     
    },

    update: function() {
      this._updateState(this.resource.state);
    }

  });
  return Card;
});
