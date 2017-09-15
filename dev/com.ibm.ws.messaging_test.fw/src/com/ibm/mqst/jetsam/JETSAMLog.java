/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.mqst.jetsam;

/**
 * @author matrober
 *
 * This interface defines the methods provided by a JETSAMLog
 */
public interface JETSAMLog extends java.io.Serializable
{
	
	// **************** LIFECYCLE METHODS ************************
	public void open(boolean newFile);
	public boolean isOpen();
	public void close();
	public String getFileName();
	
	// ***************** LOGGING METHODS *************************
	public void comment(String text);
	public void comment(Exception e);
	public void comment(Throwable e);
	public void comment(String text, Exception e);
	public void comment(String text, Error e);
	public void error(String text);
	public void error(Exception e);
	public void error(Throwable e);
	public void error(String text, Exception e);
	public void error(String text, Error e);
	public int getErrors();
	public void setErrors(int setErrors);
	public void blankLine();
	public void section(String sectionName);
	public void header(String hdrStr);
	public void timestamp();
	
	// *************** PERFORMANCE METHODS ***********************	
	public void performance(String name);
	public void performanceStats();

}
