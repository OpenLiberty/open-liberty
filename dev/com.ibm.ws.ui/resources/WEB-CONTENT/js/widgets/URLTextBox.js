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
        "js/widgets/TextBox", 
        "dojox/string/BidiComplex",
        "dojo/has",
        "dojo/_base/lang",
        "dojo/domReady!"],
        function(declare,
                TextBox,
                BidiComplex,
                has,
                lang
                ) {
    return declare("js.widgets.URLTextBox", TextBox,{
        postCreate: function(){
            console.log("URLTextBox has bidi:" + has("adminCenter-bidi"));
            if (has("adminCenter-bidi")){
                BidiComplex.attachInput(this.domNode, "URL");
                // need to set value to "" because attachInput makes placeholder disappear
                if (this.value.length === 0){
                    this.set("value", "");
                }
            }
            this.inherited(arguments);
        }

    });
});
