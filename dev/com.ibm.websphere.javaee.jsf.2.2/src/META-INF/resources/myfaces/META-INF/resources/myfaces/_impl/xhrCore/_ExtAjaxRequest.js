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
myfaces._impl.xhrCore._AjaxRequest=_MF_CLS(_PFX_XHR+"_ExtAjaxRequest",myfaces._impl.xhrCore._AjaxRequest,{constructor_:function(A){this._callSuper("constructor_",A);},getFormData:function(){var C=this._AJAXUTIL,A=this._context.myfaces,B=null;if(!this._partialIdsArray||!this._partialIdsArray.length){B=this._callSuper("getFormData");if(this._source&&A&&A.form){C.appendIssuingItem(this._source,B);}}else{B=this._Lang.createFormDataDecorator(new Array());C.encodeSubmittableFields(B,this._sourceForm,this._partialIdsArray);if(this._source&&A&&A.form){C.appendIssuingItem(this._source,B);}}return B;}});