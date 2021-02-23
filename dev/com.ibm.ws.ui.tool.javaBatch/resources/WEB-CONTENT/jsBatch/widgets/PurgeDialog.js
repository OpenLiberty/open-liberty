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
/* jshint strict: false */
define([
  'dojo/_base/declare',
  'dijit/_WidgetBase',
  'dijit/_TemplatedMixin',
  'dijit/_WidgetsInTemplateMixin',
  'js/widgets/ConfirmDialog',
  'jsBatch/utils/ID',
  'dojo/query',
  'dojo/dom', 'dojo/dom-construct',
  'dojo/i18n!jsBatch/nls/javaBatchMessages'
], function(declare, 
            _WidgetBase, _TemplatedMixin, _WidgetsInTemplateMixin, 
            ConfirmDialog, 
            ID,
            query, dom, domConstruct,
            i18n)
{
   return declare([ConfirmDialog, _TemplatedMixin ], {
    // This dialog extends the ConfimDialog.  It has a options node which provides an option for 
    // the purge action to purge the job store only and not the associated logs.  
    constructor: function(args) {
      this.inherited(arguments);
      this.hasOptions = true;    // Indicate this dialog needs an options section.
    },

    buildRendering: function() {
      this.inherited(arguments);
      var targetNode = query("div[data-dojo-attach-point='optionsNode']", this.contentNode.domNode)[0];
      var optionsNode = new (declare([_WidgetBase, _TemplatedMixin, _WidgetsInTemplateMixin], {
        templateString: '<div class="jobStoreToggle">' +
                        '  <input role="checkbox" type="checkbox" id=${jobStoreToggleID} aria-checked="false"></input>' +
                        '  <label for=${jobStoreToggleID}>${jobStoreToggleLabel}</label>' +
                        '</div>',
        jobStoreToggleID: ID.JOB_STORE_ONLY_TOGGLE_ID,                        
        jobStoreToggleLabel: i18n.PURGE_JOBSTORE_ONLY
      }))();
      optionsNode.startup();
      domConstruct.place(optionsNode.domNode, targetNode);
    },
    
    postCreate: function() {
      this.inherited(arguments);
      
      var jobStoreOnlyCheckbox = dom.byId(ID.JOB_STORE_ONLY_TOGGLE_ID);
      jobStoreOnlyCheckbox.onchange = function(e) {
        if (e.target.checked) {
          e.target.setAttribute('aria-checked', 'true');
        } else {
          e.target.setAttribute('aria-checked', 'false');
        }
      };

    }
    
  });
});
