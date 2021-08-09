/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.TruncatableThrowable;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer.servlet.DefaultErrorReporter;

/**
 * This class contains utility methods to assist in reporting useful
 * information to the end user in the case of an error in application
 * code.
 */
public class ApplicationErrorUtils {

    private static final TraceComponent tc = Tr.register(ApplicationErrorUtils.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);
    private static final TraceNLS nls = TraceNLS.getTraceNLS(ApplicationErrorUtils.class, WebContainerConstants.NLS_PROPS);

    /*
     * Returns a stack for the provided exception, trimmed to exclude
     * internal classes (except for the first ones in a group)
     */
    public static String getTrimmedStackHtml(Throwable ex) {
        StringBuilder buffer = new StringBuilder();
        TruncatableThrowable niceException = new TruncatableThrowable(ex);

        // add link back to tools if property is set
        String toolsLink = System.getProperty("was4d.error.page");
        // For security, only add obvious branding when using the tools 
        // We may wish to check the removeServerHeader instead, in future
        boolean isAnonymous = toolsLink == null;

        BufferedReader reader = null;
        try {
            final String errorHtml;
            if (!isAnonymous) {
                errorHtml = "pretty-error.html";
            } else {
                errorHtml = "minimal-error.html";
            }
            reader = new BufferedReader(new InputStreamReader(ApplicationErrorUtils.class.getResourceAsStream(errorHtml), "UTF-8"));
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if (line.contains("{toolsLink}")) {
                    line = line.replace("{toolsLink}", "http://" + toolsLink + "/");
                }
                buffer.append(line);
                buffer.append("\r\n");
                if (line.contains("id=\"box\"")) {
                    // Guess we should have NLSed this title - only show it when tools are active
                    if (toolsLink != null) {
                        buffer.append("<div id=\"title\">Application Error</div>");
                    }

                    StackTraceElement topSte = niceException.getStackTraceEliminatingDuplicateFrames()[0];
                    String failingClassAndLine = topSte.getClassName() + "." + topSte.getMethodName() + "():" + topSte.getLineNumber();

                    // add the error message with link to failing source line
                    final String linkOpener;
                    final String linkCloser;
                    if (toolsLink != null) {
                        linkOpener = "<a href=\"javascript:IDELink('" + failingClassAndLine + "')\">";
                        linkCloser = "</a>";
                    } else {
                        linkOpener = "";
                        linkCloser = "";
                    }
                    final String key = "SRVE0777E";
                    // The message would normally format the exception, but that's the opposite of what we 
                    // want, so pass through a blank argument as the last parameter
                    String exceptionText = "";

                    // Rely on our knowledge of the message parameters, and the fact they won't change order in different locales,
                    // to tack anchor elements onto the beginning and end of what will turn into the formatted class name
                    String defaultString = "SRVE0777E: Exception thrown by application class ''{0}.{1}:{2}''\n{3}";
                    String formatted = nls.getFormattedMessage(key, new Object[] { linkOpener + topSte.getClassName(), topSte.getMethodName(), topSte.getLineNumber() + linkCloser,

                    exceptionText }, defaultString);
                    // If we're trying to not advertise the server identity, strip off the key from the message
                    if (isAnonymous) {
                        int keyIndex = formatted.indexOf(key);
                        if (keyIndex >= 0) {
                            // Strip off the key and the colon
                            formatted = formatted.substring(keyIndex + key.length() + 1);
                        }
                    }
                    buffer.append("\n<div id=\"error\">");
                    buffer.append(formatted);
                    buffer.append("</div>");
                    // print exception name and message
                    buffer.append("\n<div id=\"code\">");
                    printException(niceException, toolsLink, buffer);
                    buffer.append("\n</div>\n");
                }
            }
        } catch (UnsupportedEncodingException e) {
            // This can't happen because JVMs are required to support UTF-8
        } catch (IOException e) {
            // TODO handle this
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // I don't care about this since we are doing tidy up.
                }
            }
        }

        return buffer.toString();
    }

    private static void printException(TruncatableThrowable niceException, String toolsLink, StringBuilder buffer) {
        StackTraceElement[] trimmedStackTrace = niceException.getStackTraceEliminatingDuplicateFrames();

        String clazzName = niceException.getWrappedException().getClass().getName();
        String escapeMessage = DefaultErrorReporter.encodeChars(niceException.getMessage());
        String message = (escapeMessage != null) ? (clazzName + ": " + escapeMessage) : clazzName;
        buffer.append("\n" + message + "<br>");
        // print trimmed stack with links
        buffer.append("\n<div id=\"stack\">");
        if (trimmedStackTrace.length > 0) {
            for (StackTraceElement stackElement : trimmedStackTrace) {
                String toString = TruncatableThrowable.printStackTraceElement(stackElement);

                if (toolsLink != null && toString.indexOf('(') > 0) {
                    // Reformat as a link, since this is a vanilla stack frame
                    String methodName = stackElement.getMethodName();
                    String failingClassAndMethod = stackElement.getClassName() + "." + methodName;
                    buffer.append("at ");

                    buffer.append(failingClassAndMethod);
                    String fileName = stackElement.getFileName();
                    if (fileName != null) {
                        buffer.append("(<a href=\"javascript:IDELink('");
                        buffer.append(failingClassAndMethod + "():" + stackElement.getLineNumber());
                        buffer.append("')\">");
                        buffer.append(fileName);
                        buffer.append(":");
                        buffer.append(stackElement.getLineNumber());
                        buffer.append("</a>)");
                    }

                } else {
                    buffer.append(toString);
                }

                buffer.append("<br>");
            }
        }
        buffer.append("\n</div>");

        // Now recurse for any causes 
        TruncatableThrowable cause = niceException.getCause();
        if (cause != null) {
            buffer.append(TruncatableThrowable.CAUSED_BY);
            printException(cause, toolsLink, buffer);
        }
    }

    public static void issueAppExceptionMessage(String key, Throwable ex) {
        StackTraceElement[] stackTrace = ex.getStackTrace();
        if (stackTrace != null && stackTrace.length > 0) {
            StackTraceElement topSte = stackTrace[0];
            Tr.error(tc, key, topSte.getClassName(), topSte.getMethodName(), topSte.getLineNumber(), new TruncatableThrowable(ex));
        } else {
            Tr.error(tc, key, "unknown", "unknown", "unknown", new TruncatableThrowable(ex));

        }
    }

}
