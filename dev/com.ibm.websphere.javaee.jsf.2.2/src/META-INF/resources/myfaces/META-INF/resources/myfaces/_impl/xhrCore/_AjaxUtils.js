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
_MF_SINGLTN(_PFX_XHR+"_AjaxUtils",_MF_OBJECT,{encodeSubmittableFields:function(E,A,C){if(!A){throw"NO_PARITEM";}if(C){this.encodePartialSubmit(A,false,C,E);}else{var B=A.elements.length;for(var D=0;D<B;D++){this.encodeElement(A.elements[D],E);}}},appendIssuingItem:function(A,B){if(A&&A.type&&A.type.toLowerCase()=="submit"){B.append(A.name,A.value);}},encodeElement:function(F,E){if(!F.name){return ;}var G=this._RT;var B=F.name;var D=F.tagName.toLowerCase();var H=F.type;if(H!=null){H=H.toLowerCase();}if(((D=="input"||D=="textarea"||D=="select")&&(B!=null&&B!=""))&&!F.disabled){if(D=="select"){if(F.selectedIndex>=0){var A=F.options.length;for(var I=0;I<A;I++){if(F.options[I].selected){var C=F.options[I];E.append(B,(C.getAttribute("value")!=null)?C.value:C.text);}}}}if((D!="select"&&H!="button"&&H!="reset"&&H!="submit"&&H!="image")&&((H!="checkbox"&&H!="radio")||F.checked)){if("undefined"!=typeof F.files&&F.files!=null&&G.getXHRLvl()>=2&&F.files.length){E.append(B,F.files[0]);}else{E.append(B,F.value);}}}}});