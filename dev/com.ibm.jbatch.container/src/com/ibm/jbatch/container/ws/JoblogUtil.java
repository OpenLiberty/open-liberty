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
package com.ibm.jbatch.container.ws;

import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.jbatch.container.RASConstants;
import com.ibm.websphere.ras.annotation.Trivial;

public class JoblogUtil {
	
    /**
     * The name of the batch container's special logger that, by default, logs only to the joblog.
     */
    public static final String JobLogLoggerName = "com.ibm.ws.batch.JobLogger";
    
	private final static Logger jobLogger = Logger.getLogger(JobLogLoggerName);
	
	private static volatile boolean includeServerLogging = true;

	
	
	/**
	 * Logs the message to joblog and trace. 
	 * 
	 * If level > FINE, this method will reduce the level to FINE while logging to trace
	 * to prevent the message to be logged in console.log and messages.log file
	 * 	
	 * Joblog messages will be logged as per original logging level
	 * 
	 * Use this method when you don't want a very verbose stack in messages.log and console.log
	 *
	 *  @param level Original logging level the message was logged at by the code writing the log msg.
     *  @param msgKey Message key.
     *  @param params Message params (fillins)
     *  @param traceLogger Logger to use to attempt to log message to trace.log (whether it will or not
     *  depends on config)
	 */
	@Trivial
	public static void logToJobLogAndTraceOnly(Level level, String msg, Object[] params, Logger traceLogger){
		String formattedMsg = getFormattedMessage(msg, params, "Job event.");
		logRawMsgToJobLogAndTraceOnly(level, formattedMsg, traceLogger);
	}
	
	/**
	 * logs the message to joblog and trace. 
	 * 
	 * If Level > FINE, this method will reduce the level to FINE while logging to trace.log
	 * to prevent the message to be logged in console.log and messages.log file
	 * 
	 * Joblog messages will be logged as per supplied logging level
	 * 
	 * Use this method when you don't want a very verbose stack in messages.log and console.log
	 * 
	 *  @param level Original logging level the message was logged at by the code writing the log msg.
     *  @param rawMsg Message is complete, it has already been translated with parameters a filled in,
     *  or it is a raw, non-translated message like an exception or similar trace.
     *  @param traceLogger Logger to use to attempt to log message to trace.log (whether it will or not
     *  depends on config)
	 */
	@Trivial
	public static void logRawMsgToJobLogAndTraceOnly(Level level, String msg, Logger traceLogger){
		if(level.intValue() > Level.FINE.intValue()){
			traceLogger.log(Level.FINE, msg);
			logToJoblogIfNotTraceLoggable(Level.FINE, msg, traceLogger);
		}
		else{
			traceLogger.log(level, msg);
			logToJoblogIfNotTraceLoggable(level, msg, traceLogger);
		}

		}	
	
	/**
	 * logs the message to joblog and trace. 
	 * Joblog and trace.log messages will be logged as per supplied logging level
	 * 
	 */
	@Trivial
	public static void logToJobLogAndTrace(Level level, String msg, Object[] params, Logger traceLogger){
		String formattedMsg = getFormattedMessage(msg, params, "Job event.");
		logRawMsgToJobLogAndTrace(level, formattedMsg, traceLogger);
	}
	
	/**
	 * logs the message to joblog and trace. 
	 * Joblog and trace.log messages will be logged as per supplied logging level
	 * 
	 *  @param level logging level the message was logged at by the code writing the log msg.
     *  @param rawMsg Message is complete, it has already been translated with parameters a filled in,
     *  or it is a raw, non-translated message like an exception or similar trace.
     *  @param traceLogger Logger to use to attempt to log message to trace.log (whether it will or not
     *  depends on config)
	 */
	@Trivial
	public static void logRawMsgToJobLogAndTrace(Level level, String msg, Logger traceLogger){
		logToJoblogIfNotTraceLoggable(level, msg, traceLogger);
		logToTrace(level, msg, traceLogger);

	}

	@Trivial
	private static void logToTrace(Level level, String msg, Logger traceLogger){
		
			traceLogger.log(level, msg);
	}
	
	@Trivial
	private static void logToTrace(Level level, String msg, Object[] params, Logger traceLogger) {
		String formattedMsg = getFormattedMessage(msg, params, "Job event.");
		traceLogger.log(level, formattedMsg);

	}
	
	/**
	 * if property includeServerLogging = true (default) in the server.xml, 
	 * then all the messages logged to trace.log are also logged to the joblog.
	 *
	 * So logging to trace is enough for the message to be in both trace.log and the joblog.
	 * 
	 * if property includeServerLogging = false in the server.xml, 
	 * then none of the messages logged to trace.log are logged to the joblog.
	 *
	 * So printing to trace.log and joblogs has to be done separately.
	 */
	@Trivial
	private static void logToJoblogIfNotTraceLoggable(Level level, String msg, Logger traceLogger){
		if(includeServerLogging){
			if(!traceLogger.isLoggable(level)){
				jobLogger.log(level,msg);
			}
		}
		else{
			jobLogger.log(level, msg);
		}
	}
	

	/**
     * @return a formatted msg with the given key from the resource bundle.
     */
	@Trivial
    private static String getFormattedMessage(String msgKey, Object[] fillIns, String defaultMsg) {
        ResourceBundle resourceBundle = ResourceBundle.getBundle(RASConstants.BATCH_MSG_BUNDLE);
        
        if (resourceBundle == null) {
            return defaultMsg;
        }
        
        String msg = resourceBundle.getString(msgKey);
        
        return (msg != null) ? MessageFormat.format( msg, fillIns ) : defaultMsg;
    }

	public static void setIncludeServerLogging(boolean include) {
		includeServerLogging = include;
	}
	
}
