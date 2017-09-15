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
_MF_SINGLTN&&_MF_SINGLTN(_PFX_UTIL+"_ExtLang",myfaces._impl._util._Lang,{_RT:myfaces._impl.core._Runtime,constructor_:function(){this._callSuper("constructor_");var A=this;if(myfaces._impl.core.Impl){this._RT.iterateClasses(function(B){if(B._Lang){B._Lang=A;}});}myfaces._impl._util._Lang=A;},consumeEvent:function(A){A=A||window.event;(A.stopPropagation)?A.stopPropagation():A.cancelBubble=true;},escapeString:function(B,A){return B.replace(/([\.$?*|:{}\(\)\[\]\\\/\+^])/g,function(C){if(A&&A.indexOf(C)!=-1){return C;}return"\\"+C;});},trimStringInternal:function(A,B){return this.strToArray(A,B).join(B);},createErrorMsg:function(D,B,J){var I=[];var G=this.hitch(this,this.keyValToStr),L=this.hitch(this,this.getMessage),A=this.hitch(I,I.push);A(G(L("MSG_AFFECTED_CLASS"),D));A(G(L("MSG_AFFECTED_METHOD"),B));var E=J.name;var H=J.message;var K=J.description;var M=J.number;var F=J.lineNumber;if(J){var C="undefined";A(G(L("MSG_ERROR_NAME"),E?E:C));A(G(L("MSG_ERROR_MESSAGE"),H?H:C));A(G(L("MSG_ERROR_DESC"),K?K:C));A(G(L("MSG_ERROR_NO"),C!=typeof M?M:C));A(G(L("MSG_ERROR_LINENO"),C!=typeof F?F:C));}return I.join("");}});