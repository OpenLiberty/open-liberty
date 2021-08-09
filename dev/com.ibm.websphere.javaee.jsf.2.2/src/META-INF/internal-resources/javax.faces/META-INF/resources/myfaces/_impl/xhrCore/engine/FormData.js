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
_MF_CLS(_PFX_XHR+"engine.FormData",Object,{form:null,viewstate:null,_appendedParams:{},constructor_:function(A){this.form=A;},append:function(A,B){this._appendedParams[A]=true;if(this.form){this._appendHiddenValue(A,B);}},_finalize:function(){this._removeAppendedParams();},_appendHiddenValue:function(B,D){if("undefined"==typeof D){return ;}var C=myfaces._impl._util._Dom;var A=C.createElement("input",{"type":"hidden","name":B,"style":"display:none","value":D});this.form.appendChild(A);},_removeAppendedParams:function(){if(!this.form){return ;}for(var A=this.form.elements.length-1;A>=0;A--){var B=this.form.elements[A];if(this._appendedParams[B.name]&&B.type=="hidden"){B.parentNode.removeChild(B);delete B;}}this._appendedParams={};}});