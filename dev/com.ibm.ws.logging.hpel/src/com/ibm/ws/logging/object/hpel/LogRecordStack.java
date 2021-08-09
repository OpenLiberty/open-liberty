/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.object.hpel;

import java.util.HashMap;
import java.util.Map;

import com.ibm.ejs.ras.hpel.HpelHelper;
import com.ibm.websphere.logging.hpel.LogRecordContext;

/**
 * Utility class to pass MDC values of records published by asynchronous loggers
 * to the handler. Publishing code is responsible for both setting values and cleaning
 * it up afterwards.
 */
public class LogRecordStack {

	/**
	 * Utility class containing thread specific information asynchronous loggers
	 * need to pass to the handler.
	 */
	public static class StackInfo {
		private final Map<String,String> extensions;
		private final int threadId;

		public StackInfo() {
			extensions = collectExtensions();
			threadId = HpelHelper.getIntThreadId();
		}
		
		private static Map<String, String> collectExtensions() {
			 Map<String, String> extensions = new HashMap<String,String>();
			 LogRecordContext.getExtensions(extensions);
			 return extensions;
		}
	}
    
	 private final static ThreadLocal<StackInfo> MDC = new ThreadLocal<StackInfo>();

	 /**
	  * Returns current stack information. It is called by log handler to get
	  * thread specific information which is either passed for the record in a
	  * request or obtained from the thread directly.
	  * 
	  * @return {@link StackInfo} instance setup in the current thread
	  */
	 public static StackInfo getStack() {
		 StackInfo result = MDC.get();
		 return result == null ? new StackInfo() : result;
	 }
	 
	 /**
	  * Returns thread id from the current stack. It is called by log handler
	  * to get thread id which is either passed for the record in a request or
	  * obtained from the thread directly.
	  * 
	  * @return thread id from the stack.
	  */
	 public static int getThreadID() {
		 StackInfo result = MDC.get();
		 return result == null ? HpelHelper.getIntThreadId() : result.threadId;
	 }
	 
	 /**
	  * Returns context extensions from the current stack. It is called by log handler
	  * to get extensions which are either passed for the record in a request or
	  * obtained from the thread directly.
	  * 
	  * @return extensions from the stack.
	  */
	 public static Map<String,String> getExtensions() {
		 StackInfo result = MDC.get();
		 if (result == null) {
			 return StackInfo.collectExtensions();
		 } else {
			 return result.extensions;
		 }
	 }
	 
     /**
      * Sets current stack information. It is called by publishing code for records
      * logged with asynchronous loggers to pass on values effective at the time
      * log request was made.
      * @param stack new {@link StackInfo} to setup in the current thread
      */
	 public static void setStack(StackInfo stack) {
		 MDC.set(stack);
	 }
	 
	 /**
	  * Clears current map from the stack. It is called by publishing code after
	  * all records logged with asynchronous loggers were set to log handler.
	  */
	 public static void clear() {
		 MDC.remove();
	 }
}
