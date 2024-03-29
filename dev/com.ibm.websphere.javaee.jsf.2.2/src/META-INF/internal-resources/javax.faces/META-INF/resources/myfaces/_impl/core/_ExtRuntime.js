/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
(function(){var A=myfaces._impl.core._Runtime;A.require=function(B){if(A.exists(B)){return ;}var C=A.getGlobalConfig("myfacesScriptRoot","");this.loadScriptEval(C+"/"+B.replace(/\./g,"/")+".js");};A.delegateObj=function(F,E,B,G){if(!A.isString(F)){throw Error("new class namespace must be of type String");}if("function"!=typeof F){F=A._reserveClsNms(F,B);if(!F){return null;}}var D=F.prototype;for(var C in E){(function(I,H){if(I&&typeof H=="function"){D[I]=function(){return H.apply(E,arguments);};}})(C,E[C]);}D._delegateObj=E;D.constructor=F;D._callDelegate=function(H){var J=(arguments.length==1)?[]:Array.prototype.slice.call(arguments,1);var I=this._delegateObj[H].apply(this._delegateObj,J);if("undefined"!=I){return I;}};A._applyFuncs(F,B,true);A._applyFuncs(F,G,false);return F;};A.pluginClass=function(B,C,F){var D=A.fetchNamespace(B);if(!D){throw new Error("The class namespace "+B+" is not existent");}if(!F){var E=B+"."+(""+A._classReplacementCnt++);A.reserveNamespace(E,D);return A.extendClass(B,E,C);}else{if(C.constructor_){newCls.prototype.constructor=C.constructor_;}A._applyFuncs(D,C,true);}};A.singletonDelegateObj=function(D,C,B,E){if(A._reservedNMS[D]){return ;}return A._makeSingleton(A.delegateObj,D,C,B,E);};A.loadScript=function(G,D,F,E,C){var B=A.browser;if(!B.isFF&&!B.isWebkit&&!B.isOpera>=10){A.loadScriptEval(G,D,F,E);}else{A.loadScriptByBrowser(G,D,F,E,C);}};A._reserveClsNms=function(E,B){var D=null;var C="undefined";if(C!=typeof B&&null!=B){D=(C!=typeof null!=B["constructor_"]&&null!=B["constructor_"])?B["constructor_"]:function(){};}else{D=function(){};}if(!A.reserveNamespace(E,D)){return null;}E=A.fetchNamespace(E);return E;};})();