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
/**
 * A drop in replacement which replaces the original ajax utils with added ppr functionality
 * we use it to strip down the code for the minimal version to the core
 */
_MF_SINGLTN(_PFX_XHR + "_PartialSubmitUtils", myfaces._impl.xhrCore._AjaxUtils, {
    _AjaxUtils: myfaces._impl.xhrCore._AjaxUtils,

    constructor_: function() {
        this._callSuper("constructor_");
        //we replace the original class with our new implementation
        myfaces._impl.xhrCore._AjaxUtils = this;
    },

    /**
     * determines fields to submit
     * @param {Object} targetBuf - the target form buffer receiving the data
     * @param {Node} parentItem - form element item is nested in
     * @param {Array} partialIds - ids fo PPS
     */
    encodeSubmittableFields : function(targetBuf, parentItem, partialIds) {
        if (!parentItem) throw "NO_PARITEM";

        if (partialIds) {
            this.encodePartialSubmit(parentItem, false, partialIds, targetBuf);
        } else {
            this._callSuper("encodeSubmittableFields", targetBuf, parentItem, partialIds);
        }

    },

    /**
     * checks recursively if contained in PPS
     * the algorithm is as follows we have an issuing item
     * the parent form of the issuing item and a set of child ids which do not
     * have to be inputs, we scan now for those ids and all inputs which are childs
     * of those ids
     *
     * Now this algorithm is up for discussion because it is relatively complex
     * but for now we will leave it as it is.
     *
     * @param {Node} node - the root node of the partial page submit  (usually the form)
     * @param {boolean} submitAll - if set to true, all elements within this node will
     * be added to the partial page submit
     * @param {Array} partialIds - an array of partial ids which should be used for the submit
     * @param {Object} targetBuf a target string buffer which receives the encoded elements
     */
    encodePartialSubmit : function(node, submitAll, partialIds, targetBuf) {
        var _Lang = this._Lang;
        var _Impl = this.attr("impl");
        var _Dom = this._Dom;

        var partialIdsFilter = function(curNode) {
            if (curNode.nodeType != 1) return false;
            if (submitAll && node != curNode) return true;

            var id = curNode.id || curNode.name;

            return (id && _Lang.contains(partialIds, id)) || id == _Impl.P_VIEWSTATE;
        };

        //shallow scan because we have a second scanning step, to find the encodable childs of
        //the result nodes, that way we can reduce the number of nodes
        var nodes = _Dom.findAll(node, partialIdsFilter, false);

        var allowedTagNames = {"input":true, "select":true, "textarea":true};

        if (nodes && nodes.length) {
            for (var cnt = 0; cnt < nodes.length; cnt++) {
                //we can shortcut the form any other nodetype
                //must get a separate investigation
                var subNodes = (nodes[cnt].tagName.toLowerCase() == "form") ?
                        node.elements :
                        _Dom.findByTagNames(nodes[cnt], allowedTagNames, true);

                if (subNodes && subNodes.length) {
                    for (var cnt2 = 0; cnt2 < subNodes.length; cnt2++) {
                        this.encodeElement(subNodes[cnt2], targetBuf);
                    }
                } else {
                    this.encodeElement(nodes[cnt], targetBuf);
                }
            }
        }

        this.appendViewState(node, targetBuf);
    },

    /**
     * appends the viewstate element if not given already
     *
     * @param parentNode
     * @param targetBuf
     *
     * TODO dom level2 handling here, for dom level2 we can omit the check and readd the viewstate
     */
    appendViewState: function(parentNode, targetBuf) {
        var _Dom = this._Dom;
        var _Impl = this.attr("impl");

        //viewstate covered, do a preemptive check
        if (targetBuf.hasKey && targetBuf.hasKey(_Impl.P_VIEWSTATE)) return;

        var viewStates = _Dom.findByName(parentNode, _Impl.P_VIEWSTATE);
        if (viewStates && viewStates.length) {
            for (var cnt2 = 0; cnt2 < viewStates.length; cnt2++) {
                this.encodeElement(viewStates[cnt2], targetBuf);
            }
        }
    }

});
