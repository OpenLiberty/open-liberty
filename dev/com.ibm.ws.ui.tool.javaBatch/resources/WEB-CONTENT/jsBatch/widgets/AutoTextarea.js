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

define(['dojo/_base/declare',
        'dijit/form/Textarea'
], function (declare, Textarea) {
       
        var pFontSize = "14px";
        var textAreaHeight = 28; // standard textbox height for one row
        var textboxWidthPadding = 20;
        var defautTextboxWidth = 254;


        var AutoTextarea = declare("AutoTextarea", [Textarea], {

            // get the text length in pixels
            // by temporarily put the text on the div then remove 
            measureText: function(pText) {
                var lDiv = document.createElement('div');

                document.body.appendChild(lDiv);

                lDiv.style.fontSize = pFontSize;
                lDiv.style.position = "absolute";
                lDiv.style.left = -1000;
                lDiv.style.top = -1000;

                lDiv.innerHTML = pText;

                var lResult = {
                    width: lDiv.clientWidth,
                    height: lDiv.clientHeight
                };

                document.body.removeChild(lDiv);
                lDiv = null;

                return lResult;
            },
            
            isTextOverflow: function(textArea) {
                var result = false;

                var text = textArea.value;
                var textObj = this.measureText(text);
                var textWidth = textObj.width;
                //console.log("text width ", textWidth);                
                var clientWidth = textArea.clientWidth;
                //console.log("clientWidth ", clientWidth);
                // sometimes clientWidth return 0, set default
                if (clientWidth === 0) {
                    clientWidth = defautTextboxWidth;
                }
                var textboxWidth = (clientWidth - textboxWidthPadding);
                //console.log("textBoxWidth " , textboxWidth);
                if (textWidth > textboxWidth) {
                    result = true;
                } 
                //console.log("isOverflow ", result);
                return result;
            },
        
            resize: function() {
                // summary: need to see if we can shrink to one row rather than default of 2;
                // Dojo seems to indicate this is an issue and allocating 2 rows for empty text
                this.inherited(arguments);
                 
                console.log("=============================");
                var ta = this.textbox;
                var h = parseInt(ta.style.height, 10);
                //console.log("height ", h);               
                              
                //console.log("styleHeight " , ta.style.height);                 
                //console.log("param value ", ta.value);               
                //console.log("placeholder ", ta.placeholder);
                //console.log("clientWidth ", ta.clientWidth);            
                var rows = (ta.value.match(/\n/g) || []).length + 1;
                //console.log("row ", rows);
                
                // check if textbox is empty then set to default height - 1 rows  
                if (!isNaN(h)) {
                    if (ta.value === "" && h > textAreaHeight) {
                        // setting height to single line
                        //console.log("reset height on empty text");
                        ta.style.height = textAreaHeight + "px";
                    } else {
                        var text = ta.value;  
                        var lines = text.split(/\r|\r\n|\n/);
                        var count = lines.length;
                        //console.log("lines count ", count);
                        
                        var isTextOverflow = this.isTextOverflow(ta);
                        // if there is one row and text is not overflow to the 2nd line, then set height
                        if (count === 1 && !isTextOverflow && h > textAreaHeight) {
                            //console.log("reset height on 1 row");
                            ta.style.height = textAreaHeight + "px";
                        }
                        //else if (count > 1) {
                        //  var tHeight = (22*count);
                        //  console.log("reset height to ", tHeight);
                        //  ta.style.height = tHeight + "px";
                        //}
                    }
                }               
            }                 
               
        });
       
        return AutoTextarea;
});