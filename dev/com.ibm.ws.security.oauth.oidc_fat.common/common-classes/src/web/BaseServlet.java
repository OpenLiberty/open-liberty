/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.io.IOException;
import java.util.Enumeration;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Collection;
import java.io.InputStream ;
import java.io.StringWriter ;
import java.io.BufferedReader ;
import java.io.InputStreamReader ;

import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.wsspi.security.token.AttributeNameConstants;
import com.ibm.wsspi.security.token.SingleSignonToken;
import com.ibm.wsspi.security.oauth20.token.WSOAuth20Token ;

/**
 * Base servlet which all of our test servlets extend.
 */
public abstract class BaseServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private String servletName;

    BaseServlet(String servletName) {
        this.servletName = servletName;
    }

    protected void updateServletName(String servletName) {
        this.servletName = servletName;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res)
                    throws ServletException, IOException {
        if ("CUSTOM".equalsIgnoreCase(req.getMethod()))
            doCustom(req, res);
        else
            super.service(req, res);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
        handleRequest("GET", req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
        handleRequest("POST", req, resp);
    }

    private void doCustom(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
        handleRequest("CUSTOM", req, resp);
    }

    /**
     * Common logic to handle any of the various requests this servlet supports.
     * The actual business logic can be customized by overriding performTask.
     * 
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    protected void handleRequest(String type, HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
        PrintWriter writer = resp.getWriter();
        writer.println("ServletName: " + servletName);
        writer.println("Request type: " + type);

        StringBuffer sb = new StringBuffer();
        try {
            performTask(type, req, resp, sb);
        } catch (Throwable t) {
            t.printStackTrace(writer);
        }

        writer.write(sb.toString());
        writer.flush();
        writer.close();
    }

    /**
     * Default action for the servlet if not overridden.
     * 
     * @param req
     * @param resp
     * @param writer
     * @throws ServletException
     * @throws IOException
     */
    protected void performTask(String type,
    						   HttpServletRequest req,
                               HttpServletResponse resp, StringBuffer sb)
                    throws ServletException, IOException {
        printProgrammaticApiValues(type, req, sb);
    }

    /**
     * Gets the SSO token from the subject.
     * 
     * @param subject {@code null} is not supported.
     * @return
     */
    private SingleSignonToken getSSOToken(Subject subject) {
        SingleSignonToken ssoToken = null;
        Set<SingleSignonToken> ssoTokens = subject.getPrivateCredentials(SingleSignonToken.class);
        Iterator<SingleSignonToken> ssoTokensIterator = ssoTokens.iterator();
        if (ssoTokensIterator.hasNext()) {
            ssoToken = ssoTokensIterator.next();
        }
        return ssoToken;
    }

    /**
     * Print the various programmatic API values we care about.
     * 
     * @param req
     * @param writer
     */
	protected void printProgrammaticApiValues(String type,
			HttpServletRequest req, StringBuffer sb) {
	           
        Enumeration headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = (String) headerNames.nextElement();
            String value = req.getHeader(key);
            writeLine(sb, "Header key: " + key + " with value: " + value);        }

		// print all the parms even it is "POST"
		Enumeration e = req.getParameterNames();
		if (e.hasMoreElements()) {
			writeLine(sb, "All Parameters");
			while (e.hasMoreElements()) {
				String name = (String) e.nextElement();
				writeLine(
						sb,
						"Param: " + name + " with value: "
								+ req.getParameter(name));
			}
		}
		if (type.equals("POST")) {

			try {
				Collection<Part> myParts = req.getParts();
				writeLine(sb, "All Parameters");
				for (Iterator<Part> myIt = myParts.iterator(); myIt.hasNext();) {
					Part p = myIt.next();
					InputStream inst = p.getInputStream();
					StringBuilder inputStringBuilder = new StringBuilder();
					BufferedReader bufferedReader = new BufferedReader(
							new InputStreamReader(inst, "UTF-8"));
					String line = bufferedReader.readLine();
					Boolean firstOneDone = false;
					while (line != null) {
						if (firstOneDone) {
							inputStringBuilder.append('\n');
							firstOneDone = true;
						}
						inputStringBuilder.append(line);
						line = bufferedReader.readLine();
					}
					writeLine(sb, "Param: " + p.getName() + " with value: "
							+ inputStringBuilder.toString());
				}
			} catch (Exception exc) {
				writeLine(sb, "Exception occurred: " + exc.toString());

			}
		}
		writeLine(sb, "getAuthType: " + req.getAuthType());
		writeLine(sb, "getRemoteUser: " + req.getRemoteUser());
		writeLine(sb, "getUserPrincipal: " + req.getUserPrincipal());

		if (req.getUserPrincipal() != null) {
			writeLine(sb, "getUserPrincipal().getName(): "
					+ req.getUserPrincipal().getName());
		}
		writeLine(sb, "isUserInRole(Employee): " + req.isUserInRole("Employee"));
		writeLine(sb, "isUserInRole(Manager): " + req.isUserInRole("Manager"));
		String role = req.getParameter("role");
		if (role == null) {
			writeLine(sb,
					"You can customize the isUserInRole call with the follow paramter: ?role=name");
		}
		writeLine(sb, "isUserInRole(" + role + "): " + req.isUserInRole(role));

		Cookie[] cookies = req.getCookies();
		writeLine(sb, "Getting cookies");
		if (cookies != null && cookies.length > 0) {
			for (int i = 0; i < cookies.length; i++) {
				writeLine(sb, "cookie: " + cookies[i].getName() + " value: "
						+ cookies[i].getValue());
			}
		}
		writeLine(sb, "getRequestURL: " + req.getRequestURL().toString());

		try {
			// Get the CallerSubject
			Subject callerSubject = WSSubject.getCallerSubject();
			writeLine(sb, "MY callerSubject: \n" + callerSubject);
			writeLine(sb, "break 1");

			// Get the public credential from the CallerSubject
			if (callerSubject != null) {
				WSCredential callerCredential = callerSubject
						.getPublicCredentials(WSCredential.class).iterator()
						.next();
				if (callerCredential != null) {
					writeLine(sb, "callerCredential One: " + callerCredential);
				} else {
					writeLine(sb, "callerCredential Two: null");
				}
				// WSOAuth20Token callerToken =
				// callerSubject.getPrivateCredentials(com.ibm.wsspi.security.oauth20.token.WSOAuth20Token.class).iterator().next()
				// ;
				Set<WSOAuth20Token> callerToken = callerSubject
						.getPrivateCredentials(com.ibm.wsspi.security.oauth20.token.WSOAuth20Token.class);
				writeLine(
						sb,
						"priv cred: "
								+ callerSubject
										.getPrivateCredentials(
												com.ibm.wsspi.security.oauth20.token.WSOAuth20Token.class)
										.toString());
				writeLine(
						sb,
						"priv cred size: "
								+ callerSubject
										.getPrivateCredentials(
												com.ibm.wsspi.security.oauth20.token.WSOAuth20Token.class)
										.size());
				writeLine(sb, "default priv cred: "
						+ callerSubject.getPrivateCredentials().toString());
				writeLine(sb, "default priv cred size: "
						+ callerSubject.getPrivateCredentials().size());
				if (callerToken != null) {
					writeLine(sb, "callerToken One: " + callerToken);
				} else {
					writeLine(sb, "callerToken Two: null");
				}
			} else {
				writeLine(sb, "callerCredential Three: null");
				writeLine(sb, "callerToken Three: null");
			}
			writeLine(sb, "break 2");

			// getInvocationSubject for RunAs tests
			Subject runAsSubject = WSSubject.getRunAsSubject();
			writeLine(sb, "RunAs subject: " + runAsSubject);

			// Check for cache key for hashtable login test. Will return null
			// otherwise
			String customCacheKey = null;
			if (callerSubject != null) {
				String[] properties = { AttributeNameConstants.WSCREDENTIAL_CACHE_KEY };
				SubjectHelper subjectHelper = new SubjectHelper();
				Hashtable<String, ?> customProperties = subjectHelper
						.getHashtableFromSubject(callerSubject, properties);
				if (customProperties != null) {
					customCacheKey = (String) customProperties
							.get(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);
				}
				if (customCacheKey == null) {
					SingleSignonToken ssoToken = getSSOToken(callerSubject);
					if (ssoToken != null) {
						String[] attrs = ssoToken
								.getAttributes(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);
						if (attrs != null && attrs.length > 0) {
							customCacheKey = attrs[0];
						}
					}
				}
			}
			writeLine(sb, "customCacheKey: " + customCacheKey);

		} catch (NoClassDefFoundError ne) {
			// For OSGI App testing (EBA file), we expect this exception for all
			// packages that are not public
			writeLine(sb, "NoClassDefFoundError for SubjectManager: " + ne);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

    /**
     * "Writes" the msg out to the client. This actually appends the msg
     * and a line delimiters to the running StringBuffer. This is necessary
     * because if too much data is written to the PrintWriter before the
     * logic is done, a flush() may get called and lock out changes to the
     * response.
     * 
     * @param sb Running StringBuffer
     * @param msg Message to write
     */
    void writeLine(StringBuffer sb, String msg) {
        sb.append(msg + "\n");
    }

}
