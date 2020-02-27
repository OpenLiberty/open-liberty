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

package com.ibm.ws.event.logging.internal;

public interface EventLoggingConstants {

    /** Property name for sampleRate of Event Logging **/
    public static final String EL_SAMPLING_RATE = "sampleRate";
    
    /** Property name for log mode of Event Logging(entry, exit, entryExit) **/
    public static final String EL_LOG_MODE = "logMode";
    
    /** Property name for event types this Event Logging accepts **/
    public static final String EL_EVENT_TYPES = "eventTypes";

    /** Property name for minimum duration of a request, before we dump exit entries to log **/
    public static final String EL_MIN_DURATION = "minDuration";
    
    /** Property name to indicates if context information details will be included in output **/
    public static final String EL_INCLUDE_CONTEXT_INFO = "includeContextInfo";
    
    /** Property name to indicate separator used for splitting event log message **/
    /**Example : requestID=AAAm7gXHICd_AAAAAAAAAAc # type=websphere.datasource.psExecuteUpdate#pattern=jdbc/TradeDataSource | update orderejb set orderstatus = ?, completiondate = ? where orderid = ?**/
    public static final String EVENT_LOG_MESSAGE_SEPARATOR = " # ";
    
}
