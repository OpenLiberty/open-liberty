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
//  CHANGE HISTORY
//Defect        Date        Modified By         Description
//--------------------------------------------------------------------------------------
//PK86423		05/13/2009	anupag	       		NPE in logp method
//
package com.ibm.wsspi.webcontainer.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class WebContainerLogger extends Logger {

	private Logger delegateLogger;
	private static Hashtable<String,WebContainerLogger> map = new Hashtable<String,WebContainerLogger>();
	
	/**
	 * getLogger method will create an instance of WebContainerLogger with the appropriate Logger object
	 * @param name - the name of the logger
	 * @param resourceBundleName - the message repository
	 * @return - an instance of WebContainerLogger
	 */
	public static Logger getLogger(final String name, final String resourceBundleName){
		WebContainerLogger tempLogger = map.get(name);
		if (tempLogger==null){
			tempLogger = (WebContainerLogger) AccessController.doPrivileged(
			    new PrivilegedAction<WebContainerLogger>() {
    				public WebContainerLogger run() {
    					return new WebContainerLogger(
    					    Logger.getLogger(name,resourceBundleName),
    					    name, resourceBundleName);
				}
			});
			map.put(name, tempLogger);
		}
			return tempLogger;
	}

	//Never call this method.  Instead use getLogger() to return a Logger of type WebContainerLogger.
	protected WebContainerLogger(String name, String resourceBundleName) {
		super (name, resourceBundleName);
	}
	
	protected WebContainerLogger(Logger logger,String name, String resourceBundleName) {
		super(name, null);
		delegateLogger = logger;
	}
	
	//Override every method from Logger to be called on the delegateLogger.
	
	public void logp(Level level, String sourceClass, String sourceMethod,String msg) {
		delegateLogger.logp(level,sourceClass,sourceMethod,msg);
	}
	
	public void logp(Level level, String sourceClass, String sourceMethod,String msg, Object param1) {
		delegateLogger.logp(level,sourceClass,sourceMethod,msg,param1);
	}
	
	 public void logp(Level level, String sourceClass, String sourceMethod,String msg, Throwable thrown) {
		 delegateLogger.logp(level,sourceClass,sourceMethod,msg,thrown);
	 }
	 
	 public ResourceBundle getResourceBundle() {
			return delegateLogger.getResourceBundle();
	}
	 
	 public String getResourceBundleName() {
			return delegateLogger.getResourceBundleName();
	 }
	 
	 public void setFilter(Filter newFilter) throws SecurityException {
		 delegateLogger.setFilter(newFilter);
	 }
	 
	 public Filter getFilter() {
		 return delegateLogger.getFilter();
	 }
	 
	 public void log(LogRecord record) {
		 delegateLogger.log(record);
	 }
	 
	 private void doLog(LogRecord lr) {
		 delegateLogger.log(lr);
	 }
	 
	 public void log(Level level, String msg) {
		 delegateLogger.log(level,msg);
	 }
	 
	 public void log(Level level, String msg, Object param1) {
		 delegateLogger.log(level,msg,param1);
	 }
	 
	 public void log(Level level, String msg, Object params[]) {
		 delegateLogger.log(level,msg,params);
	 }
	 
	 public void log(Level level, String msg, Throwable thrown) {
		 delegateLogger.log(level, msg, thrown);
	 }
	 
	 public void logrb(Level level, String sourceClass, String sourceMethod, 
				String bundleName, String msg) {
		 delegateLogger.logrb(level, sourceClass, sourceMethod, bundleName, msg);
	 }
	 
	 public void logrb(Level level, String sourceClass, String sourceMethod,
				String bundleName, String msg, Object param1) {
		 delegateLogger.logrb(level, sourceClass, sourceMethod, bundleName, msg, param1);
	 }
	 
	 public void logrb(Level level, String sourceClass, String sourceMethod,
				String bundleName, String msg, Object params[]) {
		 delegateLogger.logrb(level, sourceClass, sourceMethod, bundleName, msg, params);
	 }
	 
	 public void logrb(Level level, String sourceClass, String sourceMethod,
				String bundleName, String msg, Throwable thrown) {
		 delegateLogger.logrb(level, sourceClass, sourceMethod, bundleName, msg, thrown);
	 }
	 
	 public void entering(String sourceClass, String sourceMethod) {
		 delegateLogger.entering(sourceClass, sourceMethod);
	 }
	 
	 public void entering(String sourceClass, String sourceMethod, Object param1) {
		 delegateLogger.entering(sourceClass, sourceMethod, param1);
	 }
	 
	 public void entering(String sourceClass, String sourceMethod, Object params[]) {
		 delegateLogger.entering(sourceClass, sourceMethod, params);
	 }
	 
	 public void exiting(String sourceClass, String sourceMethod) {
		 delegateLogger.exiting(sourceClass, sourceMethod);
	 }
	 
	 public void exiting(String sourceClass, String sourceMethod, Object result) {
		 delegateLogger.exiting(sourceClass, sourceMethod, result);
	 }
	 
	 public void throwing(String sourceClass, String sourceMethod, Throwable thrown) {
		 delegateLogger.throwing(sourceClass, sourceMethod, thrown);
	 }
	 
	 public void severe(String msg) {
		 delegateLogger.severe(msg);
	 }
	 
	 public void warning(String msg) {
		 delegateLogger.warning(msg);
	 }
	 
	 public void info(String msg) {
		 delegateLogger.info(msg);
	 }
	 
	 public void config(String msg) {
		 delegateLogger.config(msg);
	 }
	 
	 public void fine(String msg) {
		 delegateLogger.fine(msg);
	 }
	 
	 public void finer(String msg) {
		 delegateLogger.finer(msg);
	 }
	 
	 public void finest(String msg) {
		 delegateLogger.finest(msg);
	 }
	 
	 public void setLevel(Level newLevel) throws SecurityException {
		 delegateLogger.setLevel(newLevel);
	 }
	 
	 public Level getLevel() {
		 return delegateLogger.getLevel();
	 }
	 
	 public boolean isLoggable(Level level) {
		 return delegateLogger.isLoggable(level);
	 }
	 
	 public String getName() {
		 return delegateLogger.getName();
	 }
	 
	 public synchronized void addHandler(Handler handler) throws SecurityException {
		 delegateLogger.addHandler(handler);
	 }
	 
	 public synchronized void removeHandler(Handler handler) throws SecurityException {
		 delegateLogger.removeHandler(handler);
	 }
	  
	 public synchronized Handler[] getHandlers() {
		 return delegateLogger.getHandlers();
	 }
	 
	 public synchronized void setUseParentHandlers(boolean useParentHandlers) {
		 delegateLogger.setUseParentHandlers(useParentHandlers);
	 }
	 
	 public synchronized boolean getUseParentHandlers() {
		 return delegateLogger.getUseParentHandlers();
	 }
	 
	 public Logger getParent() {
		 return delegateLogger.getParent();
	 }
	 
	 public void setParent(Logger parent) {
		 delegateLogger.setParent(parent);
	 }
	 
	/**
	* If last object in argument is a throwable, then the full stack trace is used to replace the throwable.
	*This is because Throwable.toString() method would only output the title of the exception.
	*Then the parent Logger.logp() method is called.
	 */
	public void logp(Level level, String sourceClass, String sourceMethod,
		String msg, Object params[]) 
	{
			//PK86423 Start
			//int lastParam = params.length-1;
			//if(params.length>0 && params[lastParam] instanceof Throwable){
			//	params[lastParam]=throwableToString((Throwable) params[lastParam]);
			//}
			if(params!=null && params.length>0 && params[params.length-1] instanceof Throwable){
				params[params.length-1]=throwableToString((Throwable) params[params.length-1]);
			}
			//PK86423 End
			delegateLogger.logp(level,sourceClass,sourceMethod,msg,params);
	}


	 /**
	  * Accepts a throwable and returns a String representation.
	  * @param t
	  * @return string representation of stack trace
	  */
	public static String throwableToString(Throwable t)
	{
		StringWriter s = new StringWriter();
		PrintWriter p = new PrintWriter(s);
		t.printStackTrace(p);
		return s.toString();
	}
	
}
