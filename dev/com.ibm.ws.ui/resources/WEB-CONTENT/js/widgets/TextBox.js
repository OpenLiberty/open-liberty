/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
 
 /* jshint strict: false */
define(["dojo/_base/declare",
        "dojo/_base/lang",
        "js/toolbox/toolbox",
        "js/widgets/_ValidationTextBoxMixin",
        "dojox/mobile/TextBox", 
        "dojo/has",
        "dojo/domReady!"],
        function(declare,
                lang,
                toolbox,
                ValidationTextBoxMixin,
                mTextBox,
                has
                ) {
    var TextBox = declare("js.widgets.TextBox", [mTextBox, ValidationTextBoxMixin],{
        postCreate: function(){
            if (has("adminCenter-bidi")){
                // get user preference for textDir and set it (rtl, ltr, auto)
                toolbox.getToolbox().getPreferences().then(lang.hitch(this, function(prefs) {
                    var textDir = prefs[toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION];
                    if (textDir === toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION_CONTEXTUAL){
                        this.set("textDir", "auto");
                    } else if (textDir === toolbox.getToolbox().PREFERENCE_BIDI_TEXT_DIRECTION_RTL){
                        this.set("textDir", "rtl");
                    } else {
                        this.set("textDir", "ltr");
                    }
                }));
            }
            this.inherited(arguments);
        }

    });
    return TextBox;
});
