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

/**
 *MyFaces core javascripting libraries
 *
 *  Those are the central public API functions in the JSF2
 *  Ajax API! They handle the entire form submit and ajax send
 *  and resolve cycle!
 */

/**
 * reserve the root namespace
 */
if ('undefined' != typeof OpenAjax && ('undefined' == typeof jsf || null == typeof jsf)) {
    OpenAjax.hub.registerLibrary("jsf", "www.sun.com", "1.0", null);
}
//just in case openajax has failed (testing environment)
/**
* @ignore
*/
if (!window.jsf) {
	/**
	* @namespace jsf
	*/
    var jsf = new function() {
        /*
         * Version of the implementation for the jsf.js.
         * <p />
         * as specified within the jsf specifications jsf.html:
         * <ul>
         * <li>left two digits major release number</li>
         * <li>middle two digits minor spec release number</li>
         * <li>right two digits bug release number</li>
         * </ul>
		 * @constant
         */
        this.specversion = 220000;
        /**
         * Implementation version as specified within the jsf specification.
         * <p />
         * A number increased with every implementation version
         * and reset by moving to a new spec release number
         *
		 * @constant
         */
        this.implversion = 0;

        /**
         * SeparatorChar as defined by UINamingContainer.getNamingContainerSeparatorChar()
         * @type {Char}
         */
        this.separatorchar = getSeparatorChar();

        /**
         * This method is responsible for the return of a given project stage as defined
         * by the jsf specification.
         * <p/>
         * Valid return values are:
         * <ul>
         *     <li>&quot;Production&quot;</li>
         *     <li>&quot;Development&quot;</li>
         *     <li>&quot;SystemTest&quot;</li>
         *     <li>&quot;UnitTest&quot;</li>
         * </li>
         *
         * @return {String} the current project state emitted by the server side method:
         * <i>javax.faces.application.Application.getProjectStage()</i>
         */
        this.getProjectStage = function() {
            var impl = myfaces._impl.core._Runtime.getGlobalConfig("jsfAjaxImpl", myfaces._impl.core.Impl);
            return impl.getProjectStage();
        };

        /**
         * collect and encode data for a given form element (must be of type form)
         * find the javax.faces.ViewState element and encode its value as well!
         * return a concatenated string of the encoded values!
         *
         * @throws an exception in case of the given element not being of type form!
         * https://issues.apache.org/jira/browse/MYFACES-2110
         */
        this.getViewState = function(formElement) {
            /*we are not allowed to add the impl on a global scope so we have to inline the code*/
            var impl = myfaces._impl.core._Runtime.getGlobalConfig("jsfAjaxImpl", myfaces._impl.core.Impl);
            return impl.getViewState(formElement);
        };

        /**
         * returns the window identifier for the given node / window
         * @param {optional String | DomNode}  the node for which the client identifier has to be determined
         * @return the window identifier or null if none is found
         */
        this.getClientWindow = function() {
            /*we are not allowed to add the impl on a global scope so we have to inline the code*/
            var impl = myfaces._impl.core._Runtime.getGlobalConfig("jsfAjaxImpl", myfaces._impl.core.Impl);
            return (arguments.length)? impl.getClientWindow(arguments[0]) : impl.getClientWindow();
        }

        //private helper functions
        function getSeparatorChar() {
            var impl = myfaces._impl.core._Runtime.getGlobalConfig("jsfAjaxImpl", myfaces._impl.core.Impl);
            return impl.getSeparatorChar();
        }

    };
	//jsdoc helper to avoid warnings, we map later 
	window.jsf = jsf;
}

/**
 * just to make sure no questions arise, I simply prefer here a weak
 * typeless comparison just in case some frameworks try to interfere
 * by overriding null or fiddeling around with undefined or typeof in some ways
 * it is safer in this case than the standard way of doing a strong comparison
 **/
if (!jsf.ajax) {
	/**
	* @namespace jsf.ajax
	*/
    jsf.ajax = new function() {


        /**
         * this function has to send the ajax requests
         *
         * following request conditions must be met:
         * <ul>
         *  <li> the request must be sent asynchronously! </li>
         *  <li> the request must be a POST!!! request </li>
         *  <li> the request url must be the form action attribute </li>
         *  <li> all requests must be queued with a client side request queue to ensure the request ordering!</li>
         * </ul>
         *
         * @param {String|Node} element: any dom element no matter being it html or jsf, from which the event is emitted
         * @param {EVENT} event: any javascript event supported by that object
         * @param {Map} options : map of options being pushed into the ajax cycle
         */
        this.request = function(element, event, options) {
            if (!options) {
                options = {};
            }
            /*we are not allowed to add the impl on a global scope so we have to inline the code*/
            var impl = myfaces._impl.core._Runtime.getGlobalConfig("jsfAjaxImpl", myfaces._impl.core.Impl);
            return impl.request(element, event, options);
        };

		/**
		* Adds an error handler to our global error queue.
		* the error handler must be of the format <i>function errorListener(&lt;errorData&gt;)</i>
		* with errorData being of following format:
		* <ul>
         *     <li> errorData.type : &quot;error&quot;</li>
         *     <li> errorData.status : the error status message</li>
         *     <li> errorData.serverErrorName : the server error name in case of a server error</li>
         *     <li> errorData.serverErrorMessage : the server error message in case of a server error</li>
         *     <li> errorData.source  : the issuing source element which triggered the request </li>
         *     <li> eventData.responseCode: the response code (aka http request response code, 401 etc...) </li>
         *     <li> eventData.responseText: the request response text </li>
         *     <li> eventData.responseXML: the request response xml </li>
        * </ul>
         *
         * @param {function} errorListener error handler must be of the format <i>function errorListener(&lt;errorData&gt;)</i>
		*/
        this.addOnError = function(/*function*/errorListener) {
            var impl = myfaces._impl.core._Runtime.getGlobalConfig("jsfAjaxImpl", myfaces._impl.core.Impl);
            return impl.addOnError(errorListener);
        };

        /**
         * Adds a global event listener to the ajax event queue. The event listener must be a function
         * of following format: <i>function eventListener(&lt;eventData&gt;)</i>
         *
         * @param {function} eventListener event must be of the format <i>function eventListener(&lt;eventData&gt;)</i>
         */
        this.addOnEvent = function(/*function*/eventListener) {
            var impl = myfaces._impl.core._Runtime.getGlobalConfig("jsfAjaxImpl", myfaces._impl.core.Impl);
            return impl.addOnEvent(eventListener);
        };

        /**
         * processes the ajax response if the ajax request completes successfully
         * @param request the ajax request!
         * @param context the ajax context!
         */
        this.response = function(/*xhr request object*/request, context) {
            var impl = myfaces._impl.core._Runtime.getGlobalConfig("jsfAjaxImpl", myfaces._impl.core.Impl);
            return impl.response(request, context);
        };
    }
}

if (!jsf.util) {
	/**
	* @namespace jsf.util
	*/
    jsf.util = new function() {

        /**
         * varargs function which executes a chain of code (functions or any other code)
         *
         * if any of the code returns false, the execution
         * is terminated prematurely skipping the rest of the code!
         *
         * @param {DomNode} source, the callee object
         * @param {Event} event, the event object of the callee event triggering this function
         * @param {optional} functions to be chained, if any of those return false the chain is broken
         */
        this.chain = function(source, event) {
            var impl = myfaces._impl.core._Runtime.getGlobalConfig("jsfAjaxImpl", myfaces._impl.core.Impl);
            return impl.chain.apply(impl, arguments);
        };
    }
}


