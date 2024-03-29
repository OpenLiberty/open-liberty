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
_MF_SINGLTN(_PFX_UTIL+"_Logging",_MF_OBJECT,{_ERR:"error",_INF:"info",_DEB:"debug",_LOG:"log",_WRN:"warn",_Lang:myfaces._impl._util._Lang,constructor_:function(){this._callSuper("constructor_");this._browser=myfaces._impl.core._Runtime.browser;this.logLevels={};this.stackTraceLevels={};var B=this.logLevels;var E=this.stackTraceLevels;var D=[this._ERR,this._INF,this._DEB,this._LOG,this._WRN];for(var A=0;A<D.length;A++){var C=D[A];B[C]=true;E[C]=false;}E[this._ERR]=true;},_log:function(J,F){var I=this._Lang.objToArray(arguments[1]).join("|");var G=window.console;if(G&&G[J]){G[J](I);if(this.stackTraceLevels[J]&&G.trace){G.trace();}}var E=document.getElementById("myfaces.logging");if(E){var C=document.createElement("div");var H=this._browser;if(!H.isIE||H.isIE>7){C.setAttribute("class","consoleLog "+J);}else{C.className="consoleLog "+J;}E.appendChild(C);var A=this._Lang.objToArray(arguments[1]);var D=[];for(var B=0;B<A.length;B++){D.push("<div class='args args_"+B+"'>");D.push(A[B]);D.push("</div>");}C.innerHTML="<div class='args argsinfo'>"+J.toUpperCase()+"</div>"+D.join("");E.scrollTop=E.scrollHeight;}},logError:function(){if(!this.logLevels[this._ERR]){return ;}this._log(this._ERR,arguments);},logWarn:function(){if(!this.logLevels[this._WRN]){return ;}this._log(this._WRN,arguments);},logInfo:function(){if(!this.logLevels[this._INF]){return ;}this._log(this._INF,arguments);},logDebug:function(){if(!this.logLevels[this._DEB]){return ;}this._log(this._DEB,arguments);},logTrace:function(){if(!this.logLevels[this._LOG]){return ;}this._log("log",arguments);}});