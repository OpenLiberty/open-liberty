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
_MF_CLS(_PFX_XHR+"engine.BaseRequest",_MF_OBJECT,{timeout:0,readyState:0,method:"POST",url:null,async:true,response:null,responseText:null,responseXML:null,status:null,statusText:null,constructor_:function(A){this._callSuper("constructor_",A);this._initDefaultFinalizableFields();this._XHRConst=myfaces._impl.xhrCore.engine.XhrConst;this._Lang.applyArgs(this,A);},open:function(C,A,B){this._implementThis();},send:function(A){this._implementThis();},setRequestHeader:function(A,B){this._implementThis();},abort:function(){this._implementThis();},onloadstart:function(A){},onprogress:function(A){},onabort:function(A){},onerror:function(A){},onload:function(A){},ontimeout:function(A){},onloadend:function(A){},onreadystatechange:function(A){},_implementThis:function(){throw Error("the function needs to be implemented");}});