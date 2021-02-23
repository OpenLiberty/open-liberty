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
    "dijit/_WidgetBase", 
    "dijit/_TemplatedMixin",
    "dijit/_WidgetsInTemplateMixin",
    "jsExplore/widgets/shared/BaseCard",
    "dojo/text!./templates/ClusterCard.html",
    "dojo/i18n!../nls/explorerMessages",
    'jsShared/utils/imgUtils'
    ], 
    function(
    declare, 
    WidgetBase, 
    _TemplatedMixin, 
    _WidgetsInTemplateMixin, 
    BaseCard,
    template, 
    i18n,
    imgUtils
    ) {

  var Card = declare("ClusterCard", [ BaseCard, WidgetBase, _TemplatedMixin, _WidgetsInTemplateMixin ], {
    templateString : template,
    cardBottomAppIcon : imgUtils.getSVGSmallName('app'),
    cardBottomServerIcon : imgUtils.getSVGSmallName('server'),

    postMixInProperties : function() {  
      this._setCardResourceIcon(false);
      this._setCardLabel();
      this._setResourceState();
      this._setServerData();
      this._setAppData();
      this._init();
    },

    postCreate : function() {
      this.inherited(arguments);
      this._setCardResourceIcon(true);
      this.connect(this.domNode, "onclick", this.onClick);
    },
    
    update: function() {
      this._updateState(this.resource.state);
      this._updateServerData(this.resource);
      this._updateAppData(this.resource);
    }
    
  });
  return Card;
});
