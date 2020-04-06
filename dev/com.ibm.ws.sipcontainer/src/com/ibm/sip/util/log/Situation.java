/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.sip.util.log;

/**
 * @author Nitzan, Jun 23, 2005
 * Only exists to support compatibility with code written to use LWP API
 */

public class Situation
{
    public static final String SITUATION_AVAILABLE = "Available";
	public static final String SITUATION_CONFIGURE = "Configure";
	public static final String SITUATION_CONNECT = "Connect";
	public static final String SITUATION_CONNECT_AVAILABLE = "Connect.AVAILABLE";
	public static final String SITUATION_CONNECT_CLOSED = "Connect.CLOSED";
	public static final String SITUATION_CONNECT_FREED = "Connect.FREED";
	public static final String SITUATION_CONNECT_INUSE = "Connect.INUSE";
	public static final String SITUATION_CREATE = "Create";
	public static final String SITUATION_DEPENDENCY = "Dependency";
	public static final String SITUATION_DEPENDENCY_MET = "Dependency.MET";
	public static final String SITUATION_DEPENDENCY_NOT_MET = "Dependency.NOT MET";
	public static final String SITUATION_DESTROY = "Destroy";
	public static final String SITUATION_FEATURE = "Feature";
	public static final String SITUATION_FEATURE_AVAILABLE = "Feature.AVAILABLE";
	public static final String SITUATION_FEATURE_NOT_AVAILABLE = "Feature.NOT AVAILABLE";
	public static final String SITUATION_REPORT = "Report";
	public static final String SITUATION_REPORT_DEBUG = "Report.DEBUG";
	public static final String SITUATION_REPORT_LOG = "Report.LOG";
	public static final String SITUATION_REPORT_HEARTBEAT = "Report.HEARTBEAT";
	public static final String SITUATION_REPORT_PERFORMANCE = "Report.PERFORMANCE";
	public static final String SITUATION_REPORT_SECURITY = "Report.SECURITY";
	public static final String SITUATION_REPORT_STATUS = "Report.STATUS";
	public static final String SITUATION_REPORT_TRACE = "Report.TRACE";
	public static final String SITUATION_REPORT_TRACE_TRACKING = "Report.TRACE.TRACKING";
	public static final String SITUATION_REQUEST = "Request";
	public static final String SITUATION_REQUEST_COMPLETED = "Request.COMPLETED";
	public static final String SITUATION_REQUEST_INITIATED = "Request.INITIATED";
	public static final String SITUATION_SECURITY = "Security";
	public static final String SITUATION_SECURITY_ACCESS = "Security.ACCESS";
	public static final String SITUATION_SECURITY_AUTHENTICATION = "Security.AUTHENTICATION";
	public static final String SITUATION_SECURITY_AUTHORIZATION = "Security.AUTHORIZATION";
	public static final String SITUATION_START = "Start";
	public static final String SITUATION_START_INITIATED = "Start.START INITIATED";
	public static final String SITUATION_START_RESTART = "Start.RESTART INITIATED";
	public static final String SITUATION_START_COMPLETED = "Start.START COMPLETED";
	public static final String SITUATION_STOP = "Stop";
	public static final String SITUATION_STOP_INITIATED = "Stop.STOP INITIATED";
	public static final String SITUATION_STOP_ABORT_INITIATED = "Stop.ABORT INITIATED";
	public static final String SITUATION_STOP_PAUSE_INITIATED = "Stop.PAUSE INITIATED";
	public static final String SITUATION_STOP_COMPLETED = "Stop.STOP COMPLETED";
	public static final String SITUATION_UNKNOWN = "Other";
	public static final String SITUATION_REMOVE = "Destroy";
}