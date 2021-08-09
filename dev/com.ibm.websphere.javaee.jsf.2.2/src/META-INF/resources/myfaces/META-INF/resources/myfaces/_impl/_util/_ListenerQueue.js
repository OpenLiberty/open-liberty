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
_MF_CLS(_PFX_UTIL+"_ListenerQueue",myfaces._impl._util._Queue,{_assertListener:function(A){if("function"!=typeof (A)){var B=myfaces._impl._util._Lang.getMessage("ERR_PARAM_GENERIC",null,"_ListenerQueue",arguments.caller.toString(),"function");throw this._Lang.makeException(new Error(),null,null,this._nameSpace,arguments.caller.toString(),B);}},enqueue:function(A){this._assertListener(A);this._callSuper("enqueue",A);},remove:function(A){this._assertListener(A);this._callSuper("remove",A);},broadcastEvent:function(C){var B=myfaces._impl._util._Lang.objToArray(arguments);var A=function(D){D.apply(null,B);};try{this.each(A);}finally{A=null;}}});