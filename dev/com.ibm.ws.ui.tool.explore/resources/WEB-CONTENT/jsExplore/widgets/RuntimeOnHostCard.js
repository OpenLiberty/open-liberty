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
        "dojo/text!./templates/RuntimeOnHostCard.html",
        "jsShared/utils/imgUtils"
        ], 
        function(
            declare, 
            WidgetBase, 
            _TemplatedMixin, 
            _WidgetsInTemplateMixin, 
            BaseCard,
            template,
            imgUtils
        ) {

  var Card = declare("RuntimeOnHostCard", [ BaseCard, WidgetBase, _TemplatedMixin, _WidgetsInTemplateMixin ], {
    templateString : template,
    cardBottomServerIcon : imgUtils.getSVGSmallName('server'),
    cardTopHostTitle: "",
    cardTopHostTitleId: "",

    postMixInProperties : function() {  
      this._setCardResourceIcon(false);
      this._setCardLabel();
      this._setUserDir(this.resource.path);
      this._setServerData();
      this.cardTopHostTitle = this.resource.host.name;
      this.cardTopHostTitleId = this.id + "hostTitle";
      this._init();
    },

    postCreate : function() {
      this.inherited(arguments);
      this._setCardResourceIcon(true);
      this.connect(this.domNode, "onclick", this.onClick);
    },

    update: function() {
      this._updateServerData(this.resource);
    }

  });
  return Card;
});
