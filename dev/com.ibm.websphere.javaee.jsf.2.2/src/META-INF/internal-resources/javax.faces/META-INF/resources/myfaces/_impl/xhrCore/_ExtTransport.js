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
_MF_SINGLTN(_PFX_XHR+"_ExtTransports",myfaces._impl.xhrCore._Transports,{constructor_:function(){this._callSuper("constructor_");myfaces._impl.xhrCore._Transports=this;this.updateSingletons("transport",this);},xhrPost:function(E,D,C,A){C._mfInternal.xhrOp="xhrPost";var B=this._getArguments(E,D,C,A);delete B.xhrQueue;(new (this._getAjaxReqClass(C))(B)).send();},xhrGet:function(E,D,C,A){C._mfInternal.xhrOp="xhrGet";var B=this._getArguments(E,D,C,A);B.ajaxType="GET";delete B.xhrQueue;(new (this._getAjaxReqClass(C))(B)).send();},xhrQueuedGet:function(E,D,C,A){C._mfInternal.xhrOp="xhrQueuedGet";var B=this._getArguments(E,D,C,A);B.ajaxType="GET";this._q.enqueue(new (this._getAjaxReqClass(C))(B));},multipartPost:function(E,D,C,A){C._mfInternal.xhrOp="multipartPost";var B=this._getArguments(E,D,C,A);delete B.xhrQueue;(new (this._getMultipartReqClass(C))(B)).send();},multipartGet:function(E,D,C,A){C._mfInternal.xhrOp="multiPartGet";var B=this._getArguments(E,D,C,A);B.ajaxType="GET";delete B.xhrQueue;(new (this._getMultipartReqClass(C))(B)).send();},multipartQueuedGet:function(E,D,C,A){C._mfInternal.xhrOp="multipartQueuedGet";var B=this._getArguments(E,D,C,A);B.ajaxType="GET";this._q.enqueue(new (this._getMultipartReqClass(C))(B));}});