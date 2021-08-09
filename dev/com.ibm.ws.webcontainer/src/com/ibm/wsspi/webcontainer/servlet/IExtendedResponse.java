/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import com.ibm.websphere.servlet.response.IResponse;
import com.ibm.wsspi.webcontainer.util.IResponseOutput;

/**
 * 
 * 
 * IExtendedResponse is an spi for websphere additions to the standard
 * ServletResponse methods
 * 
 * @ibm-private-in-use
 * 
 * @since   WAS7.0
 * 
 */

public interface IExtendedResponse extends ServletResponseExtended, IResponseOutput
{
    public Vector[] getHeaderTable();
    public void addSessionCookie(Cookie cookie);
    public void removeCookie(String cookieName);
    //Begin:248739
    //Add methods for DRS-Hot failover to set internal headers without checking
    //if the request is an include.
    public void setInternalHeader(String name, String s);
    public void setHeader(String name, String s, boolean checkInclude);
    //End:248739
    //PQ97429
    public void sendRedirect303(String location) throws IOException;    
    //PQ97429
    public IResponse getIResponse();
    //340473
    public int getStatusCode();
    
    public void registerOutputMethodListener(IOutputMethodListener listener);
    public void fireWriterRetrievedEvent(PrintWriter pw);
    public void fireOutputStreamRetrievedEvent(ServletOutputStream sos);
	public void initForNextResponse(IResponse res);
	
	public void start();
	
	public void finish() throws ServletException, IOException;
	
    public void destroy();
	public void closeResponseOutput(boolean b); //557339 FP7001FVT: Server timeout FFDC after 5 mins, reply intermittent
	
	public boolean isOutputWritten();
    
}
