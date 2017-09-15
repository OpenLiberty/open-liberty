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
_MF_CLS(_PFX_XHR+"_AjaxRequestQueue",myfaces._impl._util._Queue,{_curReq:null,enqueue:function(A){if(this._curReq==null){this._curReq=A;this._curReq.send();}else{this._callSuper("enqueue",A);if(A._queueSize!=this._size){this.setQueueSize(A._queueSize);}}},processQueue:function(){this._curReq=this.dequeue();if(this._curReq){this._curReq.send();}},cleanup:function(){this._curReq=null;this._callSuper("cleanup");}});