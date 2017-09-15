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
package com.ibm.jbatch.container.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public class ExceptionMatcher {
	private static final String className = ExceptionMatcher.class.getName();
	private static Logger logger = Logger.getLogger(ExceptionMatcher.class.getPackage().getName());
	
	private List<Object> exceptionHierarchy;
	private Set<String> _skipIncludeExceptions;
	private Set<String> _skipExcludeExceptions;
	
	public ExceptionMatcher(Set<String> _skipIncludeExceptions, Set<String> _skipExcludeExceptions) {
		this._skipIncludeExceptions = _skipIncludeExceptions;
		this._skipExcludeExceptions = _skipExcludeExceptions;
	}
	
	/**
	 * Check whether given exception is in skippable/retryable exception list
	 * @param e - the exception to check
	 * @return true or false 
	 */
    @FFDCIgnore(ClassNotFoundException.class)
	public boolean isSkippableOrRetryable(Exception e) {
		final String mName = "isSkippableOrRetryable";
		String exClassName = e.getClass().getName();
		
		boolean retVal = false;
		try {
			ClassLoader tccl = Thread.currentThread().getContextClassLoader();
			Class<?> excClass = tccl.loadClass(e.getClass().getName());
			exceptionHierarchy = new ArrayList<Object>();
			// Gets the List of Super Classes from excClass as the bottom
			getSuperClasses(excClass);
			
			boolean inExclude = isInElementSet(_skipExcludeExceptions, excClass);
			boolean inInclude = isInElementSet(_skipIncludeExceptions, excClass);
			
			if(inInclude && inExclude) {
				// If the Exception is in both elements, find which is the nearest tag
				// Find distance to nearest exclude
				int includeCount = getDistanceToElement(_skipIncludeExceptions);
				int excludeCount = getDistanceToElement(_skipExcludeExceptions);
				
				if(includeCount == excludeCount) {
				    retVal = false;
				} else if (includeCount < excludeCount) {
				    retVal = true;
				} else {
				    retVal = false;
				}
			} else if (inInclude && !inExclude) {
				// If inInclude = true and inExclude = false we will skip
			    retVal = true;
			} else {
				// Default Behavior
			    retVal = false;
			}
		} catch (ClassNotFoundException cnf) {
			logger.logp(Level.FINE, className, mName, cnf.getLocalizedMessage());				
		}
		
		if(logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, className, mName, mName+": " + retVal + ": " + exClassName);
		
		return retVal;
	}
	
	/**
	 * Determines if the given exception is a subclass of an element
	 * @param skipList - Set of elements
	 * @param excClass - The thrown exception
	 * @return True or False if skipList contains an element that is a superclass of the thrown exception
	 * @throws ClassNotFoundException
	 */
	public boolean isInElementSet(Set<String> skipList, Class<?> excClass) throws ClassNotFoundException {
		//String mName = "isInElementSet(Set<String> skipList, Class<?> excClass)";
		boolean retVal = false;
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		for(String value : skipList) {
			Class<?> clazz = tccl.loadClass(value);
			if(clazz.isAssignableFrom(excClass)) {
				retVal = true;
				break;
			}
		}
		return retVal;
	}
	
	/**
	 * Finds and returns the closest distance of the include/exclude element from the
	 * given exception. The exception hierarchy is derived from the given exception
	 * as it's base and all it's superclasses up to and including Object.
	 * @param skipList - Set of elements to find
	 * @return The distance to the nearest element
	 * @throws ClassNotFoundException
	 */
	public int getDistanceToElement(Set<String> skipList) throws ClassNotFoundException {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		
		int count;
		int closest = Integer.MAX_VALUE;
		for(String exception : skipList) {
			for(int i = 0; i < exceptionHierarchy.size(); i++) {
				Class<?> clazz = (Class<?>) exceptionHierarchy.get(i);
				Class<?> exClazz = tccl.loadClass(exception);
				if(clazz.equals(exClazz)) {
					count = i;
					// Retain the closest tag distance
					if(count < closest) {
						closest = count;
					}
					break;
				}
			}
		}
		return closest;
	}
	
	/**
	 * Builds a superclass hierarchy based on the given exception
	 * @param objClass - the thrown exception
	 */
	public void getSuperClasses(Class<?> objClass) {
		exceptionHierarchy.add(objClass);
		getSuperClass(objClass);
	}
	
	/**
	 * Helper method to getSuperClasses(Class<?> objClass);
	 * @param objClass - the thrown exception
	 */
	public void getSuperClass(Class<?> objClass) {
		Class<?> clazz = objClass.getSuperclass();
		if(clazz != null) {
			exceptionHierarchy.add(clazz);
			getSuperClass(clazz);
		}
	}
}
