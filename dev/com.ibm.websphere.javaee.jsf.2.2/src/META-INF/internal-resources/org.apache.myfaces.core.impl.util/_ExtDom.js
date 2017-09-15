/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
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

/*only quirksmode browsers get the quirks part of the code*/
_MF_SINGLTN && _MF_SINGLTN(_PFX_UTIL + "_ExtDom", myfaces._impl._util._Dom, {

    _Lang:myfaces._impl._util._Lang,
    _RT:myfaces._impl.core._Runtime,

    constructor_:function () {
        this._callSuper("constructor_");
        var _T = this;
        //we only apply lazy if the jsf part is loaded already
        //otherwise we are at the correct position
        if (myfaces._impl.core.Impl) {
            this._RT.iterateClasses(function (proto) {
                if (proto._Dom) {
                    proto._Dom = _T;
                }
            });
        }

        myfaces._impl._util._Dom = _T;
    },

    /**
     * finds the elements by an attached style class
     *
     * @param fragment the source fragment which is the root of our search (included in the search)
     * @param styleClass the styleclass to search for
     * @param deepScan if set to true a deep scan can be performed
     */
    findByStyleClass:function (fragment, styleClass, deepScan) {
        var filter = this._Lang.hitch(this, function (node) {
            var classes = this.getClasses(node);
            var len = classes.length;
            if (len == 0) return false;
            else {
                for (var cnt = 0; cnt < len; cnt++) {
                    if (classes[cnt] === styleClass) return true;
                }
            }
            return false;
        });
        try {
            deepScan = !!deepScan;

            //html5 getElementsByClassname

            //TODO implement this, there is a better way to check for styleclasses
            //check the richfaces code for that one
            /*if (fragment.getElementsByClassName && deepScan) {
             return fragment.getElementsByClassName(styleClass);
             }

             //html5 speed optimization for browsers which do not ,
             //have the getElementsByClassName implemented
             //but only for deep scan and normal parent nodes
             else */
            if (this._Lang.exists(fragment, "querySelectorAll") && deepScan) {
                try {
                    var result = fragment.querySelectorAll("." + styleClass.replace(/\./g, "\\."));

                    if (fragment.nodeType == 1 && filter(fragment)) {
                        result = (result == null) ? [] : result;
                        result = this._Lang.objToArray(result);
                        result.push(fragment);
                    }
                    return result;
                } catch (e) {
                    //in case the selector bombs we have to retry with a different method
                }
            } else {
                //fallback to the classical filter methods if we cannot use the
                //html 5 selectors for whatever reason
                return this._callSuper("findAll", fragment, filter, deepScan);
            }

        } finally {
            //the usual IE6 is broken, fix code
            filter = null;

        }
    },

    /**
     * gets an element from a form with its id -> sometimes two elements have got
     * the same id but are located in different forms -> MyFaces 1.1.4 two forms ->
     * 2 inputHidden fields with ID jsf_tree_64 & jsf_state_64 ->
     * http://www.arcknowledge.com/gmane.comp.jakarta.myfaces.devel/2005-09/msg01269.html
     *
     * @param {String} nameId - ID of the HTML element located inside the form
     * @param {Node} form - form element containing the element
     * @param {boolean} nameSearch if set to true a search for name is also done
     * @param {boolean} localOnly if set to true a local search is performed only (a full document search is omitted)
     * @return {Object}   the element if found else null
     *
     * @deprecated this is pointless, per definition only one element with a single id is allowed
     * in the dom node, multiple names are allowed though but we do not use it that way
     *
     */
    getElementFromForm:function (nameId, form, nameSearch, localOnly) {
        if (!nameId) {
            throw this._Lang.makeException(new Error(), null, null, this._nameSpace, "getElementFromForm", "_Dom.getElementFromForm an item id or name must be given");
        }

        if (!form) {
            return this.byId(nameId);
        }

        var isNameSearch = !!nameSearch;
        var isLocalSearchOnly = !!localOnly;

        //we first check for a name entry!
        if (isNameSearch && this._Lang.exists(form, "elements." + nameId)) {
            return form.elements[nameId];
        }

        //if no name entry is found we check for an Id but only in the form
        var element = this.findById(form, nameId);
        if (element) {
            return element;
        }

        // element not found inside the form -> try document.getElementById
        // (can be null if element doesn't exist)
        if (!isLocalSearchOnly) {
            return this.byId(nameId);
        }

        return null;
    },

    /**
     *
     * @param {Node} form
     * @param {String} nameId
     *
     * checks for a a element with the name or identifier of nameOrIdentifier
     * @returns the found node or null otherwise
     */
    findFormElement:function (form, nameId) {
        this._assertStdParams(form, nameId, "findFormElement");

        if (!form.elements) return null;
        return form.elements[nameId] || this.findById(form, nameId);
    },

    /**
     * finds a corresponding html item from a given identifier and
     * dom fragment
     * the idea is that the fragment can be detached but yet we still
     * can search it
     *
     * @param fragment the dom fragment to find the item for
     * @param itemId the identifier of the item
     */
    findById:function (fragment, itemId) {
        //we have to escape here

        if (fragment.getElementById) {
            return fragment.getElementById(itemId);
        }

        if (fragment.nodeType == 1 && fragment.querySelector) {
            try {
                //we can use the query selector here
                var newItemId = itemId;
                if (fragment.id && fragment.id === itemId) return fragment;
                if (this._Lang.isString(newItemId)) {
                    newItemId = newItemId.replace(/\./g, "\\.").replace(/:/g, "\\:");
                }

                return fragment.querySelector("#" + newItemId);
            } catch (e) {
                //in case the selector bombs we retry manually
            }
        }

        var filter = function (node) {
            return node && node.id && node.id === itemId;
        };
        try {
            return this.findFirst(fragment, filter);
        } finally {
            //ie6 fix code
            filter = null;
        }
    },

    /**
     * findfirst functionality, finds the first element
     * for which the filter can trigger
     *
     * @param fragment the processed fragment/domNode
     * @param filter a filter closure which either returns true or false depending on triggering or not
     */
    findFirst:function (fragment, filter) {
        this._Lang.assertType(filter, "function");

        if (document.createTreeWalker && NodeFilter) {
            return this._iteratorFindFirst(fragment, filter);
        } else {
            return this._recursionFindFirst(fragment, filter);
        }
    },

    /**
     * a simple recusion based find first which iterates over the
     * dom tree the classical way which is also the slowest one
     * but that one will work back to ie6+
     *
     * @param fragment the starting fragment
     * @param filter the filter to be applied to
     */
    _recursionFindFirst:function (fragment, filter) {
        if (filter(fragment)) {
            return fragment;
        }

        if (!fragment.childNodes) {
            return null;
        }

        //sub-fragment usecases
        var child;
        var cnt;
        var childLen = fragment.childNodes.length;
        for (cnt = 0; cnt < childLen; cnt++) {
            child = fragment.childNodes[cnt];
            var item = this._recursionFindFirst(child, filter);
            if (item != null)
                return item;
        }
        return null;
    },

    /**
     * the faster based iterator findFirst which will work
     * on all html5 compliant browsers and a bunch of older ones
     *
     * @param fragment the fragment to be started from
     * @param filter the filter which has to be used
     */
    _iteratorFindFirst:function (fragment, filter) {
        if (filter(fragment)) {
            return fragment;
        }
        //we have a tree walker in place this allows for an optimized deep scan

        var walkerFilter = function (node) {
            return (filter(node)) ? NodeFilter.FILTER_ACCEPT : NodeFilter.FILTER_SKIP;
        };
        var treeWalker = document.createTreeWalker(fragment, NodeFilter.SHOW_ELEMENT, walkerFilter, false);
        if (treeWalker.nextNode()) {
            /** @namespace treeWalker.currentNode */
            return treeWalker.currentNode;
        }
        return null;
    },

    /**
     * a closure based child filtering routine
     * which steps one level down the tree and
     * applies the filter closure
     *
     * @param item the node which has to be investigates
     * @param filter the filter closure
     */
    getFilteredChild:function (item, filter) {

        this._assertStdParams(item, filter, "getFilteredChild");

        var childs = item.childNodes;
        if (!childs) {
            return null;
        }
        for (var c = 0, cLen = childs.length; c < cLen; c++) {
            if (filter(childs[c])) {
                return childs[c];
            }
        }
        return null;
    },

    /**
     * gets the child of an item with a given tag name
     * @param {Node} item - parent element
     * @param {String} childName - TagName of child element
     * @param {String} itemName - name  attribute the child can have (can be null)
     * @Deprecated
     */
    getChild:function (item, childName, itemName) {
        var _Lang = this._Lang;

        function filter(node) {
            return node.tagName
                    && _Lang.equalsIgnoreCase(node.tagName, childName)
                    && (!itemName || (itemName && itemName == node.getAttribute("name")));

        }

        return this.getFilteredChild(item, filter);
    },

    /**
     * fetches the style class for the node
     * cross ported from the dojo toolkit
     * @param {String|Object} node the node to search
     * @returns the className or ""
     */
    getClass:function (node) {
        node = this.byId(node);
        if (!node) {
            return "";
        }
        var cs = "";
        if (node.className) {
            cs = node.className;
        } else {
            if (this.hasAttribute(node, "class")) {
                cs = this.getAttribute(node, "class");
            }
        }
        return cs.replace(/^\s+|\s+$/g, "");
    },

    /**
     * fetches the class for the node,
     * cross ported from the dojo toolkit
     * @param {String|Object}node the node to search
     */
    getClasses:function (node) {
        var c = this.getClass(node);
        return (c == "") ? [] : c.split(/\s+/g);
    },
    _isTable:function (item) {
        return "table" == (item.nodeName || item.tagName).toLowerCase();
    },

    deleteScripts:function (nodeList) {
        if (!nodeList || !nodeList.length) return;
        var len = nodeList.length;
        for (var cnt = 0; cnt < len; cnt++) {
            var item = nodeList[cnt];
            var src = item.getAttribute('src');
            if (src && src.length > 0 && (src.indexOf("/jsf.js") != -1 || src.indexOf("/jsf-uncompressed.js") != -1)) {
                continue;
            }
            this.deleteItem(item);
        }
    }

});

