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
_MF_CLS(_PFX_XHR+"_IFrameRequest",myfaces._impl.xhrCore._AjaxRequest,{MF_PART_IFRAME:"javax.faces.transport.iframe",MF_PART_FACES_REQUEST:"javax.faces.request",constructor_:function(A){this._callSuper("constructor_",A);},getFormData:function(){var D=new myfaces._impl.xhrCore.engine.FormData(this._sourceForm);D.append(this.MF_PART_IFRAME,"true");D.append(this.MF_PART_FACES_REQUEST,"partial/ajax");var F=decodeURIComponent(jsf.getViewState(this._sourceForm));F=F.split("&");for(var E=0,B=F.length;E<B;E++){var G=F[E];var A=G.split("=");var C=A[0];if(!this._Dom.getNamedElementFromForm(this._sourceForm,C)){D.append(C,A[1]);}}return D;},_formDataToURI:function(){return"";},_getTransport:function(){return new myfaces._impl.xhrCore.engine.IFrame();}});