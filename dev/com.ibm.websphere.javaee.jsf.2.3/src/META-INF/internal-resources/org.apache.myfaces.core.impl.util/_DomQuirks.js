/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
if (_MF_SINGLTN) {
    /***
     Dom.js version for non html5 browsers
     */
    _MF_SINGLTN(_PFX_UTIL + "DomQuirks", myfaces._impl._util._Dom, /** @lends myfaces._impl._util._DomQuirks.prototype */ {

        IE_QUIRKS_EVENTS:{
            "onabort":true,
            "onload":true,
            "onunload":true,
            "onchange":true,
            "onsubmit":true,
            "onreset":true,
            "onselect":true,
            "onblur":true,
            "onfocus":true,
            "onkeydown":true,
            "onkeypress":true,
            "onkeyup":true,
            "onclick":true,
            "ondblclick":true,
            "onmousedown":true,
            "onmousemove":true,
            "onmouseout":true,
            "onmouseover":true,
            "onmouseup":true
        },

        constructor_:function () {

            var b = myfaces._impl.core._Runtime.browser;

            if (b.isIEMobile && b.isIE <= 6) {
                //winmobile hates add onLoad, and checks on the construct
                //it does not eval scripts anyway
                myfaces.config = myfaces.config || {};
                myfaces.config._autoeval = false;
                return;
            } else {
                //for whatever reason autoeval is set here on Firefox 3.5 only
                // could be a firebug problem, by setting setters
                //and getters no assignment was revealed.
                if ('undefined' != typeof myfaces.config && 'undefined' != typeof myfaces.config._autoeval) {
                    delete myfaces.config._autoeval;
                }
            }
            this._callSuper("constructor_");
            myfaces._impl._util._Dom = this;

            var _Lang = this._Lang;
            //by w3c spec the script eval is turned off it is only
            //on on legacy browsers
            var scriptEval = _Lang.hitch(this, this.isManualScriptEval);

            if (window) {
                this._RT.addOnLoad(window, scriptEval);
            }
            //safety fallback if the window onload handler is overwritten and not chained
            if (document.body) {
                this._RT.addOnLoad(document.body, scriptEval);
            }

            //we register ourselves into the existing classes lazily
            var _T = this;
            if (myfaces._impl.core.Impl) {
                this._RT.iterateClasses(function (proto) {
                    if (proto._Dom) proto._Dom = _T;
                });
            }
        },

        /**
         * Checks whether the browser is dom compliant.
         * Dom compliant means that it performs the basic dom operations safely
         * without leaking and also is able to perform a native setAttribute
         * operation without freaking out
         *
         *
         * Not dom compliant browsers are all microsoft browsers in quirks mode
         * and ie6 and ie7 to some degree in standards mode
         * and pretty much every browser who cannot create ranges
         * (older mobile browsers etc...)
         *
         * We dont do a full browser detection here because it probably is safer
         * to test for existing features to make an assumption about the
         * browsers capabilities
         */
        isDomCompliant:function () {
            if ('undefined' == typeof this._isCompliantBrowser) {
                this._isCompliantBrowser = !!((window.Range
                        && typeof Range.prototype.createContextualFragment == 'function')
                                                            //createContextualFragment hints to a no quirks browser but we need more fallbacks
                        || document.querySelectoryAll       //query selector all hints to html5 capabilities
                        || document.createTreeWalker);      //treewalker is either firefox 3.5+ or ie9 standards mode
            }
            return this._isCompliantBrowser;
        },

        /**
         * now to the evil browsers
         * of what we are dealing with is various bugs
         * first a simple replaceElement leaks memory
         * secondly embedded scripts can be swallowed upon
         * innerHTML, we probably could also use direct outerHTML
         * but then we would run into the script swallow bug
         *
         * the entire mess is called IE6 and IE7
         *
         * @param item
         * @param markup
         */
        _outerHTMLNonCompliant:function (item, markup) {

            var b = this._RT.browser;

            try {
                //check for a subtable rendering case

                var evalNodes = this._buildEvalNodes(item, markup);

                if (evalNodes.length == 1) {
                    var ret = evalNodes[0];
                    this.replaceElement(item, evalNodes[0]);
                    return ret;
                } else {
                    return this.replaceElements(item, evalNodes);
                }

            } finally {

                var dummyPlaceHolder = this.getDummyPlaceHolder();
                //now that Microsoft has finally given
                //ie a working gc in 8 we can skip the costly operation
                if (b.isIE && b.isIE < 8) {
                    this._removeChildNodes(dummyPlaceHolder, false);
                }
                dummyPlaceHolder.innerHTML = "";
            }

        },

        replaceElement:function (item, evalNode) {

            var _Browser = this._RT.browser;
            if (!_Browser.isIE || _Browser.isIE >= 8) {
                //standards conform no leaking browser
                item.parentNode.replaceChild(evalNode, item);
            } else {
                this._callSuper("replaceElement", item, evalNode);
            }
        },

        /**
         * builds the ie nodes properly in a placeholder
         * and bypasses a non script insert bug that way
         * @param markup the marku code
         */
        _buildNodesNonCompliant:function (markup) {

            //now to the non w3c compliant browsers
            //http://blogs.perl.org/users/clinton_gormley/2010/02/forcing-ie-to-accept-script-tags-in-innerhtml.html
            //we have to cope with deficiencies between ie and its simulations in this case
            var probe = this.getDummyPlaceHolder();//document.createElement("div");

            probe.innerHTML = "<table><tbody><tr><td><div></div></td></tr></tbody></table>";

            //we have customers using html unit, this has a bug in the table resolution
            //hence we determine the depth dynamically
            var depth = this._determineDepth(probe);

            this._removeChildNodes(probe, false);
            probe.innerHTML = "";

            var dummyPlaceHolder = this.getDummyPlaceHolder();//document.createElement("div");

            //fortunately a table element also works which is less critical than form elements regarding
            //the inner content
            dummyPlaceHolder.innerHTML = "<table><tbody><tr><td>" + markup + "</td></tr></tbody></table>";
            var evalNodes = dummyPlaceHolder;

            for (var cnt = 0; cnt < depth; cnt++) {
                evalNodes = evalNodes.childNodes[0];
            }
            var ret = (evalNodes.parentNode) ? this.detach(evalNodes.parentNode.childNodes) : null;

            if ('undefined' == typeof evalNodes || null == evalNodes) {
                //fallback for htmlunit which should be good enough
                //to run the tests, maybe we have to wrap it as well
                dummyPlaceHolder.innerHTML = "<div>" + markup + "</div>";
                //note this is triggered only in htmlunit no other browser
                //so we are save here
                //TODO Fix this (probaby ret is needed here, check the history)
                evalNodes = this.detach(dummyPlaceHolder.childNodes[0].childNodes);
            }

            this._removeChildNodes(dummyPlaceHolder, false);
            //ie fix any version, ie does not return true javascript arrays so we have to perform
            //a cross conversion
            return ret;

        },

        _determineDepth:function (probe) {
            var depth = 0;
            var newProbe = probe;
            for (; newProbe &&
                           newProbe.childNodes &&
                           newProbe.childNodes.length &&
                           newProbe.nodeType == 1; depth++) {
                newProbe = newProbe.childNodes[0];
            }
            return depth;
        },

        //now to another nasty issue:
        //for ie we have to walk recursively over all nodes:
        //http://msdn.microsoft.com/en-us/library/bb250448%28VS.85%29.aspx
        //http://weblogs.java.net/blog/driscoll/archive/2009/11/13/ie-memory-management-and-you
        //http://home.orange.nl/jsrosman/
        //http://www.quirksmode.org/blog/archives/2005/10/memory_leaks_li.html
        //http://www.josh-davis.org/node/7
        _removeNode:function (node, breakEventsOpen) {
            if (!node) return;
            var b = this._RT.browser;
            if (this.isDomCompliant()) {
                //recursive descension only needed for old ie versions
                //all newer browsers cleanup the garbage just fine without it
                //thank you
                if ('undefined' != typeof node.parentNode && null != node.parentNode) //if the node has a parent
                    node.parentNode.removeChild(node);
                return;
            }

            //now to the browsers with non working garbage collection
            this._removeChildNodes(node, breakEventsOpen);

            try {
                //outer HTML setting is only possible in earlier IE versions all modern browsers throw an exception here
                //again to speed things up we precheck first
                if (!this._isTableElement(node)) {
                    //we do not do a table structure innnerhtml on table elements except td
                    //htmlunit rightfully complains that we should not do it
                    node.innerHTML = "";
                }
                if (b.isIE && 'undefined' != typeof node.outerHTML) {//ie8+ check done earlier we skip it here
                    node.outerHTML = '';
                } else {
                    node = this.detach(node)[0];
                }
                if (!b.isIEMobile) {
                    delete node;
                }
            } catch (e) {
                //on some elements we might not have covered by our table check on the outerHTML
                // can fail we skip those in favor of stability
                try {
                    // both innerHTML and outerHTML fails when <tr> is the node, but in that case
                    // we need to force node removal, otherwise it will be on the tree (IE 7 IE 6)
                    this.detach(node);
                    if (!b.isIEMobile) {
                        delete node;
                    }
                } catch (e1) {
                }
            }
        },

        /**
         * recursive delete child nodes
         * node, this method only makes sense in the context of IE6 + 7 hence
         * it is not exposed to the public API, modern browsers
         * can garbage collect the nodes just fine by doing the standard removeNode method
         * from the dom API!
         *
         * @param node  the node from which the childnodes have to be deletd
         * @param breakEventsOpen if set to true a standard events breaking is performed
         */
        _removeChildNodes:function (node, breakEventsOpen) {
            if (!node) return;

            //node types which cannot be cleared up by normal means
            var disallowedNodes = this.TABLE_ELEMS;

            //for now we do not enable it due to speed reasons
            //normally the framework has to do some event detection
            //which we cannot do yet, I will dig for options
            //to enable it in a speedly manner
            //ie7 fixes this area anyway
            //this.breakEvents(node);

            var b = this._RT.browser;
            if (breakEventsOpen) {
                this.breakEvents(node);
            }

            for (var cnt = node.childNodes.length - 1; cnt >= 0; cnt -= 1) {
                var childNode = node.childNodes[cnt];
                //we cannot use our generic recursive tree walking due to the needed head recursion
                //to clean it up bottom up, the tail recursion we were using in the search either would use more time
                //because we had to walk and then clean bottom up, so we are going for a direct head recusion here
                if ('undefined' != typeof childNode.childNodes && node.childNodes.length)
                    this._removeChildNodes(childNode);
                try {
                    var nodeName = (childNode.nodeName || childNode.tagName) ? (childNode.nodeName || childNode.tagName).toLowerCase() : null;
                    //ie chokes on clearing out table inner elements, this is also covered by our empty
                    //catch block, but to speed things up it makes more sense to precheck that
                    if (!disallowedNodes[nodeName]) {
                        //outer HTML setting is only possible in earlier IE versions all modern browsers throw an exception here
                        //again to speed things up we precheck first
                        if (!this._isTableElement(childNode)) {    //table elements cannot be deleted
                            childNode.innerHTML = "";
                        }
                        if (b.isIE && b.isIE < 8 && 'undefined' != typeof childNode.outerHTML) {
                            childNode.outerHTML = '';
                        } else {
                            node.removeChild(childNode);
                        }
                        if (!b.isIEMobile) {
                            delete childNode;
                        }
                    }
                } catch (e) {
                    //on some elements the outerHTML can fail we skip those in favor
                    //of stability

                }
            }
        },
        /**
         * cross ported from dojo
         * fetches an attribute from a node
         *
         * @param {String} node the node
         * @param {String} attr the attribute
         * @return the attributes value or null
         */
        getAttribute:function (/* HTMLElement */node, /* string */attr) {
            //	summary
            //	Returns the value of attribute attr from node.
            node = this.byId(node);
            // FIXME: need to add support for attr-specific accessors
            if ((!node) || (!node.getAttribute)) {
                // if(attr !== 'nwType'){
                //	alert("getAttr of '" + attr + "' with bad node");
                // }
                return null;
            }
            var ta = typeof attr == 'string' ? attr : new String(attr);

            // first try the approach most likely to succeed
            var v = node.getAttribute(ta.toUpperCase());
            if ((v) && (typeof v == 'string') && (v != "")) {
                return v;	//	string
            }

            // try returning the attributes value, if we couldn't get it as a string
            if (v && v.value) {
                return v.value;	//	string
            }



            if (node.getAttribute(ta)) {
                return node.getAttribute(ta);	//	string
            } else if (node.getAttribute(ta.toLowerCase())) {
                return node.getAttribute(ta.toLowerCase());	//	string

            }
            //there used to be a getAttributeNode check here for really old
            //browsers, I had to remove it because of a firefox warning
            //which uses a regexp scan for this method to be deprecated

            return null;	//	string
        },

        /**
         * bugfixing for ie6 which does not cope properly with setAttribute
         */
        setAttribute:function (node, attr, val) {

            this._assertStdParams(node, attr, "setAttribute");

            //quirks mode and ie7 mode has the attributes problems ie8 standards mode behaves like
            //a good citizen
            var _Browser = this._RT.browser;
            //in case of ie > ie7 we have to check for a quirks mode setting
            if (!_Browser.isIE || _Browser.isIE > 7 && this.isDomCompliant()) {
                this._callSuper("setAttribute", node, attr, val);
                return;
            }

            /*
             Now to the broken browsers IE6+.... ie7 and ie8 quirks mode

             we deal mainly with three problems here
             class and for are not handled correctly
             styles are arrays and cannot be set directly
             and javascript events cannot be set via setAttribute as well!

             or in original words of quirksmode.org ... this is a mess!

             Btw. thank you Microsoft for providing all necessary tools for free
             for being able to debug this entire mess in the ie rendering engine out
             (which is the Microsoft ie vms, developers toolbar, Visual Web Developer 2008 express
             and the ie8 8 developers toolset!)

             also thank you http://www.quirksmode.org/
             dojotoolkit.org and   //http://delete.me.uk/2004/09/ieproto.html
             for additional information on this mess!

             The lowest common denominator tested within this code
             is IE6, older browsers for now are legacy!
             */
            attr = attr.toLowerCase();

            if (attr === "class") {
                //setAttribute does not work for winmobile browsers
                //firect calls work
                node.className = val;
            } else if (attr === "name") {
                //the ie debugger fails to assign the name via setAttr
                //in quirks mode
                node[attr] = val;
            } else if (attr === "for") {
                if (!_Browser.isIEMobile || _Browser.isIEMobile >= 7) {
                    node.setAttribute("htmlFor", val);
                } else {
                    node.htmlFor = val;
                }
            } else if (attr === "style") {
                node.style.cssText = val;
            } else {
                //check if the attribute is an event, since this applies only
                //to quirks mode of ie anyway we can live with the standard html4/xhtml
                //ie supported events
                if (this.IE_QUIRKS_EVENTS[attr]) {
                    if (this._Lang.isString(attr)) {
                        var c = document.body.appendChild(document.createElement('span'));
                        try {
                            c.innerHTML = '<span ' + attr + '="' + val + '"/>';
                            node[attr] = c.firstChild[attr];
                        } finally {
                            document.body.removeChild(c);
                        }
                    }
                } else {
                    //unknown cases we try to catch them via standard setAttributes
                    if (!_Browser.isIEMobile || _Browser.isIEMobile >= 7) {
                        node.setAttribute(attr, val);
                    } else {
                        node[attr] = val;
                    }
                }
            }
        },

        getDummyPlaceHolder:function () {
            this._callSuper("getDummyPlaceHolder");

            //ieMobile in its 6.1-- incarnation cannot handle innerHTML detached objects so we have
            //to attach the dummy placeholder, we try to avoid it for
            //better browsers so that we do not have unecessary dom operations
            if (this._RT.browser.isIEMobile && created) {
                this.insertFirst(this._dummyPlaceHolder);

                this.setAttribute(this._dummyPlaceHolder, "style", "display: none");

            }

            return this._dummyPlaceHolder;
        },

        /**
         * classical recursive way which definitely will work on all browsers
         * including the IE6
         *
         * @param rootNode the root node
         * @param filter the filter to be applied to
         * @param deepScan if set to true a deep scan is performed
         */
        _recursionSearchAll:function (rootNode, filter, deepScan) {
            var ret = [];
            //fix the value to prevent undefined errors

            if (filter(rootNode)) {
                ret.push(rootNode);
                if (!deepScan) return ret;
            }

            //
            if (!rootNode.childNodes) {
                return ret;
            }

            //subfragment usecases

            var retLen = ret.length;
            var childLen = rootNode.childNodes.length;
            for (var cnt = 0; (deepScan || retLen == 0) && cnt < childLen; cnt++) {
                ret = ret.concat(this._recursionSearchAll(rootNode.childNodes[cnt], filter, deepScan));
            }
            return ret;
        },

        /**
         * break the standard events from an existing dom node
         * (note this method is not yet used, but can be used
         * by framework authors to get rid of ie circular event references)
         *
         * another way probably would be to check all attributes of a node
         * for a function and if one is present break it by nulling it
         * I have to do some further investigation on this.
         *
         * The final fix is to move away from ie6 at all which is the root cause of
         * this.
         *
         * @param node the node which has to be broken off its events
         */
        breakEvents:function (node) {
            if (!node) return;
            var evtArr = this.IE_QUIRKS_EVENTS;
            for (var key in evtArr) {
                if (!evtArr.hasOwnProperty(key)) continue;
                if (key != "onunload" && node[key]) {
                    node[key] = null;
                }
            }
        },

        isManualScriptEval:function () {

            if (!this._Lang.exists(myfaces, "config._autoeval")) {

                //now we rely on the document being processed if called for the first time
                var evalDiv = document.createElement("div");
                this._Lang.reserveNamespace("myfaces.config._autoeval");
                //null not swallowed
                myfaces.config._autoeval = false;

                var markup = "<script type='text/javascript'> myfaces.config._autoeval = true; </script>";
                //now we rely on the same replacement mechanisms as outerhtml because
                //some browsers have different behavior of embedded scripts in the contextualfragment
                //or innerhtml case (opera for instance), this way we make sure the
                //eval detection is covered correctly
                this.setAttribute(evalDiv, "style", "display:none");

                //it is less critical in some browsers (old ie versions)
                //to append as first element than as last
                //it should not make any difference layoutwise since we are on display none anyway.
                this.insertFirst(evalDiv);

                //we remap it into a real boolean value
                if (this.isDomCompliant()) {
                    this._outerHTMLCompliant(evalDiv, markup);
                } else {
                    //will not be called placeholder for quirks class
                    this._outerHTMLNonCompliant(evalDiv, markup);
                }


            }

            return  !myfaces.config._autoeval;
        },

        getNamedElementFromForm:function (form, elementName) {
			var browser = this._RT.browser;
            if(browser.isIE && browser.isIE < 8) {
                if(!form.elements) return null;
                for(var cnt = 0, l = form.elements.length; cnt < l; cnt ++) {
                    var element = form.elements[cnt];
                    if(element.name == elementName) {
                        return element;
                    }
                }
                return null;
            } else {
                return form[elementName];
            }
        }

    });

}
