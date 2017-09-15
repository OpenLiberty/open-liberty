/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.request.probe.bci.internal;

/**
 *
 */
public class RequestProbeConstants {

    public static final String TYPE_STRING = "Ljava/lang/String;";
    public static final String TYPE_LONG = "J";

    //Meta Data Helper Class and method details, which will be used to query the client object which has implemented 'current' RequestProbeTransformDescriptor Class.
    public static final String TO_HELPER_CLASS = "com/ibm/wsspi/request/probe/bci/RequestProbeHelper";
    public static final String TO_HELPER_GET_CLIENT_METHOD = "getObjForInstrumentation";
    public static final String TO_HELPER_GET_CLIENT_METHOD_DESC = "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Lcom/ibm/wsspi/request/probe/bci/RequestProbeTransformDescriptor;";
   

    //Details to call  getContextInfo() on given RequestProbeTransformDescriptor
    public static final String CLIENT_INTERFACE_CLASS = "com/ibm/wsspi/request/probe/bci/RequestProbeTransformDescriptor";
    public static final String CLIENT_INTERFACE_CLASS_GETCONTEXTINFO_METHOD = "getContextInfo";
    public static final String CLIENT_INTERFACE_CLASS_GETCONTEXTINFO_METHOD_DESC = "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/String;";
	public static final String REQ_CONTEXT_CLASS = "com/ibm/wsspi/requestContext/RequestContext";
	public static final String RC_HELPER_GET_RC_METHOD = "getRequestContext";
	public static final String RC_HELPER_GET_RC_METHOD_DESC = "(Lcom/ibm/wsspi/requestContext/Event;)Lcom/ibm/wsspi/requestContext/RequestContext;";
	public static final String ON_EVENT_EXIT_SET_CURRENT_METHOD = "onEventExitSetCurrent";
	public static final String ON_EVENT_EXIT_SET_CURRENT_METHOD_DESC = "(Lcom/ibm/wsspi/requestContext/Event;)V";
	
	public static final String EVENT_CONTEXT_INFO_SEPARATOR = " | ";


}
