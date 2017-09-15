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
package com.ibm.ws.webcontainer.webapp;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.EmptyStackException;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.servlet.error.ServletErrorReport;
import com.ibm.ws.webcontainer.exception.IncludeFileNotFoundException;
import com.ibm.ws.webcontainer.servlet.DefaultErrorReporter;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

public class WebAppErrorReport extends ServletErrorReport     // 96236
{
	protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.webapp");
	private static final String CLASS_NAME="com.ibm.ws.webcontainer.webapp.WebAppErrorReport";
    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3257007635676148784L;

	public WebAppErrorReport()
    {
        super();
    }
    /**
     * Constructs a new WebAppErrorReport with the specified message.
     *
     * @param message Message of exception
     */

    public WebAppErrorReport(String message)
    {
        super(message);
    }

    /**
     * Constructs a new WebAppErrorReport with the specified message
     * and root cause.
     *
     * @param message Message of exception
     * @param rootCause Exception that caused this exception to be raised
     */

    public WebAppErrorReport(String message, Throwable rootCause)
    {
        super(message, rootCause);
    }

    /**
     * Constructs a new WebAppErrorReport with the specified message
     * and root cause.
     *
     * @param rootCause Exception that caused this exception to be raised
     */

    public WebAppErrorReport(Throwable rootCause)
    {
        super(rootCause);
    }

    public void setErrorCode(int sc)
    {
        super.setErrorCode(sc);
    }

    public void setTargetServletName(String servletName)
    {
        super.setTargetServletName(servletName);
    }

    public StackTraceElement[] getStackTrace()
    {
        Throwable th = getRootCause();
        if (th != null)
			return th.getStackTrace();
		else
			return null;
    }

    public void printFullStackTrace(PrintWriter out)
    {
        printFullStackTrace(out, this);
    }

    @SuppressWarnings("unchecked")
    public static void printFullStackTrace(PrintWriter out, ServletException e)
    {
        Stack errorStack = new Stack();
        errorStack.push(e);
        while (e != null)
        {
            Throwable th = e.getRootCause();
            if (th instanceof ServletException)
            {
                e = (ServletException)th;
            } else
            {
                e = null;
            }
            if (th != null)
            {
                errorStack.push(th);
            }
        }
        try
        {
            while (true)
            {
                Throwable th = (Throwable)errorStack.pop();

                if (th instanceof ServletErrorReport)                              // 96236
                    th.printStackTrace(out);                                       // 96236
                else                                                               // 96236
                {                                                                  // 96236
                    StringWriter sw = new StringWriter();                          // 96236
                    PrintWriter pw = new PrintWriter(sw);                          // 96236
                    th.printStackTrace(pw);                                        // 96236
                    pw.flush();                                                    // 96236
                    out.print(DefaultErrorReporter.encodeChars(sw.toString()));    // 96236
                }                                                                  // 96236

                out.println("");
            }
        }
        catch (EmptyStackException ex)
        {
          com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(ex, "com.ibm.ws.webcontainer.webapp.WebAppErrorReport.printFullStackTrace", "126");
        }
    }
    
    //begin  PK10057
    public static ServletErrorReport constructErrorReport(Throwable th, String path) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
            logger.logp(Level.FINE, CLASS_NAME,"constructErrorReport", "constructing error report for path -->" + path + " Throwable -->" + th);
        }

        WebAppErrorReport r = new WebAppErrorReport(th);
        r.setTargetServletName(path);

        Throwable rootCause = th;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
        if (WCCustomProperties.SERVLET_30_FNF_BEHAVIOR&&rootCause instanceof IncludeFileNotFoundException) {
            r.setErrorCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        else if (rootCause instanceof FileNotFoundException) {
            r.setErrorCode(HttpServletResponse.SC_NOT_FOUND);
        }
        else if (rootCause instanceof UnavailableException) {
            UnavailableException ue = (UnavailableException) rootCause;
            if (ue.isPermanent()) {
                r.setErrorCode(HttpServletResponse.SC_NOT_FOUND);
            } else {
                r.setErrorCode(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            }
        }
        else {
            r.setErrorCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
            logger.logp(Level.FINE, CLASS_NAME,"constructErrorReport", "returning new servlet error report");
        }

        return r;
    }
    
    /**
     * @param th
     * @param wrapper
     */
    public static ServletErrorReport constructErrorReport(Throwable th, RequestProcessor requestProcessor) {
        WebAppErrorReport r = new WebAppErrorReport(th);
        // Begin 269504, Unvailable permanently servlets fail with NPE on second httpResponse
        if (requestProcessor != null&&requestProcessor.getName()!=null)
        {
            	return constructErrorReport(th,requestProcessor.getName());
        }
        else {
        	return constructErrorReport(th,"ServletNameNotFound");
        }
    }


}
