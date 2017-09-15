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
_MF_SINGLTN(_PFX_XHR+"_PartialSubmitUtils",myfaces._impl.xhrCore._AjaxUtils,{_AjaxUtils:myfaces._impl.xhrCore._AjaxUtils,constructor_:function(){this._callSuper("constructor_");myfaces._impl.xhrCore._AjaxUtils=this;},encodeSubmittableFields:function(C,A,B){if(!A){throw"NO_PARITEM";}if(B){this.encodePartialSubmit(A,false,B,C);}else{this._callSuper("encodeSubmittableFields",C,A,B);}},encodePartialSubmit:function(D,M,E,G){var F=this._Lang;var J=this.attr("impl");var I=this._Dom;var B=function(N){if(N.nodeType!=1){return false;}if(M&&D!=N){return true;}var O=N.id||N.name;return(O&&F.contains(E,O))||O==J.P_VIEWSTATE;};var A=I.findAll(D,B,false);var L={"input":true,"select":true,"textarea":true};if(A&&A.length){for(var C=0;C<A.length;C++){var K=(A[C].tagName.toLowerCase()=="form")?D.elements:I.findByTagNames(A[C],L,true);if(K&&K.length){for(var H=0;H<K.length;H++){this.encodeElement(K[H],G);}}else{this.encodeElement(A[C],G);}}}this.appendViewState(D,G);},appendViewState:function(A,F){var D=this._Dom;var E=this.attr("impl");if(F.hasKey&&F.hasKey(E.P_VIEWSTATE)){return ;}var C=D.findByName(A,E.P_VIEWSTATE);if(C&&C.length){for(var B=0;B<C.length;B++){this.encodeElement(C[B],F);}}}});