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
package com.ibm.ws.webcontainer.servlet;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.EmptyStackException;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ejs.ras.TraceNLS;

import com.ibm.websphere.servlet.error.ServletErrorReport;

/**
 * DefaultErrorReporter is the default webapp error handler used for reporting
 * webapp request processing errors to clients.
 *
 * @deprecated
 * 105840 - DPJ - 6/20/01
 */
public class DefaultErrorReporter extends HttpServlet
{
    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3763096349528044856L;

	private static TraceNLS nls = TraceNLS.getTraceNLS(DefaultErrorReporter.class, "com.ibm.ws.webcontainer.resources.Messages");

    public void service(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
    {
        PrintWriter out;
        try
        {
            out = response.getWriter();
        }
        catch (IllegalStateException e)
        {
            //occurs when a servlet gets the response OutputStream instead of the PrintWriter
          com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, "com.ibm.ws.webcontainer.servlet.DefaultErrorReporter.service", "48", this);
            out = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), response.getCharacterEncoding()));
        }

        ServletErrorReport error = (ServletErrorReport)request.getAttribute(ServletErrorReport.ATTRIBUTE_NAME);

        reportErrorAsHTML(out, error, request.getRequestURL().toString());
    }

    public static void reportErrorAsHTML(PrintWriter out, ServletErrorReport error, String url)
    {
        out.println("<HTML>\n<HEAD><TITLE>" +
                    nls.getString("Error.Report","Error Report") +
                    "</TITLE></HEAD>\n<BODY>");

        if (error == null)
        {
            out.println(nls.getString("No.Error.to.Report","No Error to Report"));
        }
        else
        {
            out.println("<H1>Error " + error.getErrorCode() + "</H1>");

            out.println("<H3>" +
                        nls.getString("error.occured.processing.request", "An error has occurred while processing request: ") +
                        encodeChars(url) +                     // 96236
                        "</H3>");

            out.println("<H3><B>" +
                        nls.getString("Message","Message:") +
                        // begin 110817
                        // ServletErrorReport by default encodes the chars for getMessage().
                        //  "</B> " + encodeChars(error.getMessage()) +   // 96236
                        "</B> " + error.getMessage() + 
                        // end 110817
                        "</H3><BR>");

            out.println("<B>" +
                        nls.getString("Target.Servlet","Target Servlet:") + 
                        " </B>" + encodeChars(error.getTargetServletName()) +     // 96236
                        "<BR>");

            out.println("<B>" + nls.getString("StackTrace","StackTrace:") + " </B>");

            printFullStackTrace(out, error); 
        }

        out.println("\n</BODY>\n</HTML>");

        out.flush();
    }

    /**
     * method rewritten for defect 105840
     *
     */
    public static void printFullStackTrace(PrintWriter out, ServletException e)
    {
        // determine the absolute root exception
        Throwable th = null;

        while (e != null)
        {
            th = e.getRootCause();

            if (th == null)
            {
                th = e;
                break;
            }
            else
                e = th instanceof ServletException ? (ServletException)th : null;
        }

        // now print the root exception
        try
        {
            // if th is a ServletErrorReport, it's already encoded...if not, encode it
            if (th instanceof ServletErrorReport)
                out.println("<HR width=\"100%\">\n" + th.getMessage() + "<BR>");
            else
                out.println("<HR width=\"100%\">\n" + encodeChars(th.getMessage())+ "<BR>");

            // print the stack trace to a string writer so we can encode it if necessary and format
            // its output
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            th.printStackTrace(pw);
            pw.flush();
            String traceOut;

            // if it's a ServletErrorReport, it's already encoded...if not, encode it
            if (th instanceof ServletErrorReport)
                traceOut = sw.toString();
            else
                traceOut = encodeChars(sw.toString());

            // split the stack lines so they'll appear right in the browser
            StringTokenizer st = new StringTokenizer(traceOut, "\n");

            while (st.hasMoreTokens())
            {
                out.println(st.nextToken());
                out.println("<BR>&nbsp;&nbsp;&nbsp;&nbsp;");
            }

            out.println("<BR>");
        }
        catch (EmptyStackException ex)
        {
          com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(ex, "com.ibm.ws.webcontainer.servlet.DefaultErrorReporter.printFullStackTrace", "158");
        }
    }


    /**
     * method added for defect 105840
     *
     * This method prints the stack trace without the error message information
     *
     */
    public static void printShortStackTrace(PrintWriter out, ServletException e)
    {
        // determine the absolute root exception
        Throwable th = null;

        while (e != null)
        {
            th = e.getRootCause();

            if (th == null)
            {
                th = e;
                break;
            }
            else
                e = th instanceof ServletException ? (ServletException)th : null;
        }

        // now print the trace
        try
        {
            // print the stack trace to a string writer so we can encode it if necessary and format
            // its output
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            th.printStackTrace(pw);
            pw.flush();
            String traceOut;

            // if it's a ServletErrorReport, it's already encoded...if not, encode it
            if (th instanceof ServletErrorReport)
                traceOut = sw.toString();
            else
                traceOut = encodeChars(sw.toString());

            // split the stack lines so they'll appear right in the browser
            StringTokenizer st = new StringTokenizer(traceOut, "\n");

            while (st.hasMoreTokens())
            {
                out.println(st.nextToken());
                out.println("<BR>&nbsp;&nbsp;&nbsp;&nbsp;");
            }

            out.println("<BR>");
        }
        catch (EmptyStackException ex)
        {
          com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(ex, "com.ibm.ws.webcontainer.servlet.DefaultErrorReporter.printShortStackTrace", "217");
        }
    }

/*  public static void printFullStackTrace(PrintWriter out, ServletException e)
    {
        Stack errorStack = new Stack();
        errorStack.push(e);

        while (e != null)
        {
            Throwable th = e.getRootCause();

            if (th instanceof ServletException)   // 96236
            {
                // 96236
                e = (ServletException)th;         // 96236
            }                                     // 96236
            else                                  // 96236
            {
                // 96236
                e = null;                         // 96236
            }                                     // 96236

            if (th != null)
            {
                errorStack.push(th);
            }
        }
        try
        {
            Throwable th = (Throwable)errorStack.pop();

            for (int i=1; th != null; i++)
            {
                if (i == 1)
                {
                    // if th is a ServletErrorReport, it's already encoded...if not, encode it
                    if (th instanceof ServletErrorReport)                                  // 96236
                    {
                        // 96236
                        out.println("<HR width=\"100%\">\n<B>" +                           // 96236
                                    nls.getString("Root.Error", "Root Error-") +           // 96236
                                    i + "</B>: " +                                         // 96236
                                    th.getMessage() + "<BR>");                             // 96236
                    }                                                                      // 96236
                    else                                                                   // 96236
                    {
                        // 96236
                        out.println("<HR width=\"100%\">\n<B>" +                           // 96236
                                    nls.getString("Root.Error", "Root Error-") +           // 96236
                                    i + "</B>: " +                                         // 96236
                                    encodeChars(th.getMessage()) + "<BR>");                // 96236
                    }                                                                      // 96236
                }
                else
                {
                    // if th is a ServletErrorReport, it's already encoded...if not, encode it
                    if (th instanceof ServletErrorReport)                                  // 96236
                    {
                        // 96236
                        out.println("<HR width=\"100%\">\n<B>" +                           // 96236
                                    nls.getString("Wrapped.Error","Wrapped Error-") +      // 96236
                                    i + "</B>: " +                                         // 96236
                                    th.getMessage() + "<BR>");                             // 96236
                    }                                                                      // 96236
                    else                                                                   // 96236
                    {
                        // 96236
                        out.println("<HR width=\"100%\">\n<B>" +                           // 96236
                                    nls.getString("Wrapped.Error","Wrapped Error-") +      // 96236
                                    i + "</B>: " +                                         // 96236
                                    encodeChars(th.getMessage()) + "<BR>");                // 96236
                    }                                                                      // 96236
                }

                // print the stack trace to a string writer so we can encode it if necessary and format
                // its output
                StringWriter sw = new StringWriter();                                      // 96236
                PrintWriter pw = new PrintWriter(sw);                                      // 96236
                th.printStackTrace(pw);                                                    // 96236
                pw.flush();                                                                // 96236
                String traceOut;                                                           // 96236
                                                                                           // 96236
                // if it's a ServletErrorReport, it's already encoded...if not, encode it  // 96236
                if (th instanceof ServletErrorReport)                                      // 96236
                    traceOut = sw.toString();                                              // 96236
                else                                                                       // 96236
                    traceOut = encodeChars(sw.toString());                                 // 96236
                                                                                           // 96236
                // split the stack lines so they'll appear right in the browser            // 96236
                StringTokenizer st = new StringTokenizer(traceOut, "\n");                  // 96236
                                                                                           // 96236
                while (st.hasMoreTokens())                                                 // 96236
                {
                    // 96236
                    out.println(st.nextToken());                                           // 96236
                    out.println("<BR>&nbsp;&nbsp;&nbsp;&nbsp;");                           // 96236
                }                                                                          // 96236

                out.println("<BR>");                                                       // 96236

                th = (Throwable)errorStack.pop();
            }
        }
        catch (EmptyStackException ex)
        {
        }
    } */

    // 96236 - this method was added for defect 96236
    public static String encodeChars(String iString)
    {
        if (iString == null)
            return "";

        int strLen = iString.length(), i;

        if (strLen < 1)
            return iString;

        // convert any special chars to their browser equivalent specification
        StringBuffer retString = new StringBuffer(strLen * 2);

        for (i = 0; i < strLen; i++)
        {
            switch (iString.charAt(i))
            {
                case '<':
                    retString.append("&lt;");
                    break;

                case '>':
                    retString.append("&gt;");
                    break;

                case '&':
                    retString.append("&amp;");
                    break;

                case '\"':
                    retString.append("&quot;");
                    break;

                case '+':
                    retString.append("&#43;");
                    break;

                case '(':
                    retString.append("&#40;");
                    break;

                case ')':
                    retString.append("&#41;");
                    break;

                case '\'':
                    retString.append("&#39;");
                    break;

                case '%':
                    retString.append("&#37;");
                    break;

                case ';':
                    retString.append("&#59;");
                    break;

                default:
                    retString.append(iString.charAt(i));
                    break;
            }
        }

        return retString.toString();
    }
}

