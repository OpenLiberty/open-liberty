/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.webcontainerext;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.StringTokenizer;

import com.ibm.websphere.servlet.error.ServletErrorReport;
import com.ibm.websphere.servlet.response.ResponseUtils;
import com.ibm.ws.jsp.JspCoreException;

// Change history:
// JSPErrorReport created for Defect 211450
// Defect 218240 "Improve JSP Error Page." 2007/07/21 Scott Johnson
// Defect 315405  2005/10/21  jsp container fails to call JSPErrorReport.setTargetServletName

public class JSPErrorReport extends ServletErrorReport {
    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3256721801324344372L;
	/**
     * Constructs a new JSPErrorReport with the specified message
     * and root cause.
     *
     * @param message Message of exception
     * @param rootCause Exception that caused this exception to be raised
     */

    private static String separatorString = System.getProperty("line.separator");  // Defect 211450
    
    public JSPErrorReport(String message, Throwable rootCause)
    {
        super(message, rootCause);
    }
    /**
     * Returns a detailed message about the error.
     */
    public String getMessage()
    {
        return super.getMessage();
    }
    public String getUnencodedMessage()
    {
    	return super.getUnencodedMessage();
    }
    public String getMessageAsHTML()
    {
        StringTokenizer st = new StringTokenizer(super.getMessage(), separatorString);        
        String retMsg="";
        while (st.hasMoreTokens()) {
        	retMsg+= st.nextToken()+"<BR>";
        }
        
        // get the root cause and print its stack trace to StringWriter for formatting.
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
    	Throwable t=null;
    	Throwable rootCause=this;
    	while ((t = rootCause.getCause()) != null) {
				rootCause=t;            		  
    	}
    	rootCause.printStackTrace(pw);
        pw.flush();
        String traceOut = sw.toString();
        // split the stack lines so they'll appear right in the browser
        st = new StringTokenizer(traceOut, separatorString);
        StringBuffer sb=new StringBuffer();
        while (st.hasMoreTokens())
        {
            sb.append(ResponseUtils.encodeDataString(st.nextToken()));
            sb.append("<BR>");
        }
        sb.append("<BR>");

        StringBuffer returnMessage = new StringBuffer();
        returnMessage.append("<HTML><HEAD>");
        returnMessage.append("<title>"+JspCoreException.getMsg("JSPG0229")+"</title>");
        returnMessage.append("<style type=\"text/css\">#mybox{padding: 0.5em;border: noborder; border-width: thin; width: 100%;}</style>");
        returnMessage.append("<style type=\"text/css\">h2 { text-align: justify;color:#5555FF;font-size:15pt;font-family: Verdana, Helvitica, sans-serif;font-weight:bold}</style>");
        returnMessage.append("</HEAD><BODY>");
        returnMessage.append("<h2>"+JspCoreException.getMsg("JSPG0229")+"</h2>");
        returnMessage.append("<TABLE BORDER=2  BGCOLOR=\"#DDDDFF\">");
        returnMessage.append("<TR VALIGN=\"BOTTOM\"><TD BGCOLOR=\"#C2B0D6\" ><B><FONT FACE=\"Verdana, Helvitica, sans-serif\"  COLOR=\"black\" SIZE=\"4PT\">"+JspCoreException.getMsg("JSPG0230")+"&nbsp;&nbsp;&nbsp;"+new Integer(this.getErrorCode()).toString()+"</B><BR><BR></TD></TR>");
        returnMessage.append("<TR><TD><B>"+JspCoreException.getMsg("JSPG0231")+"</B>");
        returnMessage.append("<div id=\"mybox\">"+"<PRE>"+retMsg+"</PRE>"+"</div></TD></TR>");
        returnMessage.append("<TR><TD><B>"+JspCoreException.getMsg("JSPG0232")+"</B>");
        returnMessage.append("<div id=\"mybox\"><PRE>"+sb.toString()+"</PRE></div></TD></TR>");
        returnMessage.append("</TABLE></BODY></HTML>");
        
        return returnMessage.toString();
    }
    //  Defect 315405  begin
    public void setTargetServletName(String servletName)
    {
    	super.setTargetServletName(servletName);
    }
    //  Defect 315405  end
}
