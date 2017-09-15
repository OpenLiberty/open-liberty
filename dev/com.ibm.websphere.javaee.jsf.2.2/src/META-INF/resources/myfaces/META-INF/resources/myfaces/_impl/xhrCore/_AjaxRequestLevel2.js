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
_MF_CLS(_PFX_XHR+"_MultipartAjaxRequestLevel2",myfaces._impl.xhrCore._AjaxRequest,{_sourceForm:null,constructor_:function(A){this._callSuper("constructor_",A);},getFormData:function(){var A;if(this._context._mfInternal.xhrOp==="multipartQueuedPost"){A=new FormData(this._sourceForm);this._AJAXUTIL.appendIssuingItem(this._source,A);}else{this._AJAXUTIL.encodeSubmittableFields(A,this._sourceForm,null);this._AJAXUTIL.appendIssuingItem(this._source,A);}return A;},_applyContentType:function(A){},_formDataToURI:function(A){return"";},_getTransport:function(){return new XMLHttpRequest();}});_MF_CLS(_PFX_XHR+"_AjaxRequestLevel2",myfaces._impl.xhrCore._AjaxRequest,{_sourceForm:null,constructor_:function(A){this._callSuper("constructor_",A);},_getTransport:function(){return new XMLHttpRequest();}});