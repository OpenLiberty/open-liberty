/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
_MF_SINGLTN(_PFX_XHR+"_Transports",_MF_OBJECT,{_PAR_ERRORLEVEL:"errorlevel",_PAR_QUEUESIZE:"queuesize",_PAR_PPS:"pps",_PAR_TIMEOUT:"timeout",_PAR_DELAY:"delay",_q:new myfaces._impl.xhrCore._AjaxRequestQueue(),xhrQueuedPost:function(D,C,B,A){B._mfInternal.xhrOp="xhrQueuedPost";this._q.enqueue(new (this._getAjaxReqClass(B))(this._getArguments(D,C,B,A)));},multipartQueuedPost:function(E,D,C,A){C._mfInternal.xhrOp="multipartQueuedPost";var B=this._getArguments(E,D,C,A);this._q.enqueue(new (this._getMultipartReqClass(C))(B));},_getArguments:function(A,C,B,I){var G=myfaces._impl.core._Runtime,E=myfaces._impl._util._Lang,F=E.hitch(this,this._applyConfig),D=G.getLocalOrGlobalConfig,H={"source":A,"sourceForm":C,"context":B,"passThrough":I,"xhrQueue":this._q};F(H,B,"alarmThreshold",this._PAR_ERRORLEVEL);F(H,B,"queueSize",this._PAR_QUEUESIZE);F(H,B,"timeout",this._PAR_TIMEOUT);if(D(B,this._PAR_PPS,false)&&E.exists(I,myfaces._impl.core.Impl.P_EXECUTE)&&I[myfaces._impl.core.Impl.P_EXECUTE].length>0){H["partialIdsArray"]=I[myfaces._impl.core.Impl.P_EXECUTE].split(" ");}return H;},_applyConfig:function(A,E,D,C){var F=myfaces._impl.core._Runtime;var B=F.getLocalOrGlobalConfig;if(B(E,C,null)!=null){A[D]=B(E,C,null);}},_getMultipartReqClass:function(A){if(this._RT.getXHRLvl()>=2){return myfaces._impl.xhrCore._MultipartAjaxRequestLevel2;}else{return myfaces._impl.xhrCore._IFrameRequest;}},_getAjaxReqClass:function(A){if(this._RT.getXHRLvl()<2){return myfaces._impl.xhrCore._AjaxRequest;}else{return myfaces._impl.xhrCore._AjaxRequestLevel2;}}});