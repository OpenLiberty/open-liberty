/*******************************************************************************
 * Copyright (c) 1997, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Date;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.webcontainer.webapp.WebAppRequestDispatcher;
import com.ibm.wsspi.webcontainer.WCCustomProperties;


/**
 * DirectoryBrowsingServlet
 *
 * This servlet implements directory browsing behavior.  It is automatically loaded
 * if directory browsing is enabled for a web module.
 *
 * version 1.0 - 07/24/01
 */
public class DirectoryBrowsingServlet extends HttpServlet
{

    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3258125864872195895L;
	private static TraceNLS nls = TraceNLS.getTraceNLS(DirectoryBrowsingServlet.class, "com.ibm.ws.webcontainer.resources.Messages");
	
    /**
     * Handle the GET Method
     */
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        PrintWriter out;
        String  title = "Index of ";
        String dirName = (String)req.getAttribute("com.ibm.servlet.engine.webapp.dir.browsing.path");
        String reqURI = (String)req.getAttribute("com.ibm.servlet.engine.webapp.dir.browsing.uri");

        // PK81387 Start

        //I believe dirName is now the local dir, not the file system path
        File dir = null;
        URL dirURL = null;
        //this is a servlet, we only have access to the ServletContextFacade
        ServletContext context = getServletConfig().getServletContext();   
        String dirNameFileSystemPath = context.getRealPath(dirName);
        boolean fileSystem = false;
        //dirNameFileSystemPath!=null is not a good enough check to determine if you're in a container
        //if we have a container and there have been jsps compiled, it gives you the directory in the workarea
        if (dirNameFileSystemPath!=null) {
            fileSystem = true;
            dirName=dirNameFileSystemPath;
            dir = new File(dirNameFileSystemPath);

            // get path to war directory
            String contextRealPath = context.getRealPath("/");
            
            int idx=dirName.lastIndexOf(contextRealPath);
            
            if (idx!=-1) {
            	// subtract the war directory from teh reqiested directory
            	String matchString=dirName.substring(idx+contextRealPath.length());
            	
            	matchString=matchString.replace(File.separator,"/");
            	
            	// Ensure matchString starts with "/" so WSUtil.resolveURI processes leading "."s
            	if (!matchString.startsWith("/")) {
            	   matchString="/"+matchString;
            	}
            	
            	// remove a trailing "/" so tthat uriCaseCheck does a vlaid check.
            	if (matchString.endsWith("/")) {
            		matchString=matchString.substring(0, matchString.length()-1);
            	}
            	     
            	// checkWEB-INF unless esposeWebInfoOnDispatch is set and we are in a dispatched request
            	boolean checkWEBINF = !WCCustomProperties.EXPOSE_WEB_INF_ON_DISPATCH || (req.getAttribute(WebAppRequestDispatcher.DISPATCH_NESTED_ATTR)==null) ;
            	
            	try {
            		
            		if (!com.ibm.wsspi.webcontainer.util.FileSystem.uriCaseCheck(dir, matchString,checkWEBINF))
            		{
                        resp.sendError(404, nls.getString("File.not.found", "File not found"));
                        return;
            		}	
            	} catch (java.lang.IllegalArgumentException exc) {
            		// Must be traversing back directories
                    resp.sendError(404, nls.getString("File.not.found", "File not found"));
                    return;
            	}
             } else {
                 // Must be traversing back directories
                 //dirNameFileSystemPath was normalized in Liberty, whereas in tWAS it contained the root path followed by whatever was entered
                 resp.sendError(404, nls.getString("File.not.found", "File not found"));
                 return;
             }
        }
        else { //container
            dirURL = context.getResource(dirName);
            if (dirURL == null) {
                resp.sendError(404, nls.getString("File.not.found", "File not found"));
                return;
            }
        }
            // PK81387 End	
            if (!reqURI.endsWith("/"))
                reqURI += '/';
    
            title += reqURI;
    
            // make sure we can access it
            if (fileSystem && !dir.canRead())
                resp.sendError(404, nls.getString("File.not.found", "File not found"));
        // set the content type
		// set the content type as UTF-8 as filenames are encoded in UTF-8
        resp.setContentType("text/html; charset=UTF-8");

        // write the output
        out = resp.getWriter();

        out.println("<HTML><HEAD><TITLE>");
        out.println(title);
        out.println("</TITLE></HEAD><BODY>");
        out.println("<H1 align=\"left\">" + title + "</H1>");
        out.println("<HR size=\"3\"><TABLE cellpadding=\"2\"><TBODY><TR bgcolor=\"#d7ffff\">");

        // output the table headers
        out.println("<TH width=\"250\" nowrap><P align=\"left\">Name</P></TH>");
        out.println("<TH width=\"250\" nowrap><P align=\"left\">Last Modified</P></TH>");
        out.println("<TH width=\"150\" nowrap><P align=\"left\">Size</P></TH>");
        out.println("<TH width=\"300\" nowrap><P align=\"left\">Description</P></TH></TR>");

        // output a row for each file in the directory
        if (fileSystem) {
            fillTableRows(dir, reqURI, out);
        } else {
            fillTableRows(context, dirName, reqURI, out);
        }

        // finish the page
        out.println("</TBODY></TABLE></BODY></HTML>");

        // close it up
        out.close();
    }

    // Pass the POST request to the GET method
    public void doPost (HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        doGet(req,resp);
    }

    private void fillTableRows(ServletContext context, String dir, String reqURI, PrintWriter out)
    {
        Set<String>resourcePaths = context.getResourcePaths(dir);
        
        // for each file, output a table row
        for (String s:resourcePaths) {
            //dir.length() contains the trailing /, so just get the substring of dir.length()
            s=s.substring(dir.length()); //remove the directory with the slash
            if (s.endsWith("/")) { // a directory
                if (s.equalsIgnoreCase("META-INF/") ||
                    s.equalsIgnoreCase("WEB-INF/")) {
                    continue;
                }
                //unknown last modified time
                printDirectory(out, reqURI, s, "-");
            } else {
                if (s.endsWith(".jsp") ||
                                s.endsWith(".jsv") ||
                                s.endsWith(".jsw"))
                            {
                                continue;
                            }
                //unknown last modified time
                printFile(out, reqURI, s, -1, "-");
            }
        }
    }

    
    private void fillTableRows(File dir, String reqURI, PrintWriter out)
    {
        File[] files = dir.listFiles();
        int fc = 0;
        Date date;

        // for each file, output a table row
        while (fc < files.length)
        {
            if (files[fc].isDirectory())
            {
                // it's a directory...make sure it's not a configuration dir
                if (files[fc].getName().equalsIgnoreCase("META-INF") ||
                    files[fc].getName().equalsIgnoreCase("WEB-INF"))
                {
                    fc++;
                    continue;
                }
                date = new Date(files[fc].lastModified());
                String lastModifiedDateString = date.toString();
                printDirectory(out, reqURI, files[fc].getName(), lastModifiedDateString);

            }
            else
            {
                // it's a file...make sure it's not a jsp type
                if (files[fc].getName().endsWith(".jsp") ||
                    files[fc].getName().endsWith(".jsv") ||
                    files[fc].getName().endsWith(".jsw"))
                {
                    fc++;
                    continue;
                }
                
                date = new Date(files[fc].lastModified());
                String lastModifiedDateString = date.toString();
                printFile(out, reqURI, files[fc].getName(), files[fc].length(), lastModifiedDateString);
            }
            // increment file counter for while
            fc++;
        }
    }
    
    private void printDirectory(PrintWriter out, String urlString, String fileNameString, String dateString) {
        // not a config dir...put out a table row
        out.println("<TR><TD nowrap>");

        // create the link
        out.println("<A href=\"" + urlString + fileNameString + "\">");
        out.println("<B>" + fileNameString + "</B></A></TD>");

        // show last modified date
        out.println("<TD nowrap>" + dateString + "</TD>");

        // show the dir size (just a dash) and description
        out.println("<TD nowrap>-</TD><TD nowrap>Directory</TD></TR>");
    }
    
    private void printFile(PrintWriter out, String urlString, String fileNameString, long fileSize, String dateString) {
        // not a jsp type...put out a file link
        out.println("<TR><TD nowrap>");

        // create the link
        out.println("<A href=\"" + urlString + fileNameString + "\">");
        out.println(fileNameString + "</A></TD>");

        // show last modified date
        out.println("<TD nowrap>" + dateString + "</TD>");

        // show the file size
        if (fileSize==-1) { //unknown file size
            out.println("<TD nowrap>-</TD>");
        } else {
            out.println("<TD nowrap>" + fileSize + "</TD>");
        }

        out.println("<TD nowrap>File</TD></TR>");

    }
}
