/*******************************************************************************
 * Copyright (c) 2016, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.test.servlet;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.ejb.EJBAccessException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.security.auth.Subject;

import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.WSSecurityException;


@SuppressWarnings("serial")
public class SecuredServletWithCacheKey  extends BaseServlet  {
	
	private static String CUSTOM_PUBLIC_CRED = "CustomPublicCredential";

	/**
	 * Invokes test name found in "test" parameter passed to servlet.
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String test = request.getParameter("test");
		String serFileName = request.getParameter("serFileName");
		String contextAllowedRole = request.getParameter("contextAllowedRole");
		String contextDeniedRole = request.getParameter("contextDeniedRole");
		String originalAllowedRole = request.getParameter("originalAllowedRole");
		String originalDeniedRole = request.getParameter("originalDeniedRole");
		String cacheKey = request.getParameter("cacheKey");
		String cacheMiss = request.getParameter("cacheMiss");
		PrintWriter out = response.getWriter();

		try {
			out.println("SecuredServletWithCacheKey is starting " + test + "<br>");
			System.out.println("-----> " + test + " starting");
			if(cacheKey!= null){
				getClass().getMethod(test, HttpServletRequest.class, HttpServletResponse.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class).invoke(this, request, response, serFileName, contextAllowedRole, contextDeniedRole, originalAllowedRole, originalDeniedRole, cacheKey, cacheMiss);
			}
			else{
				getClass().getMethod(test, HttpServletRequest.class, HttpServletResponse.class, String.class).invoke(this, request, response, serFileName);
			}
			System.out.println("<----- " + test + " successful");
			out.println(test + " " + SUCCESS_MESSAGE);
		} catch (Throwable x) {
			if (x instanceof InvocationTargetException)
				x = x.getCause();
			System.out.println("<----- " + test + " failed:");
			x.printStackTrace(System.out);
			out.println("<pre>ERROR in " + test + ":");
			x.printStackTrace(out);
			out.println("</pre>");
		} finally {
			out.flush();
			out.close();
		}
	}

	/**
	 * Serializes this servlet's security context and save for later use
	 */
	public void testSerializeSecurityContextWithCacheKey(HttpServletRequest request, HttpServletResponse resp, String serFileName) throws Exception {
		String cacheKey = null;
		Executor contextualExecutor = securityContextSvc.createContextualProxy(new CurrentThreadExecutor(), Executor.class);
		try {
			// Save Cache key for current thread context for later comparison
			cacheKey = CacheKeyFactory.getCacheKeyFromSubject(WSSubject.getCallerSubject());
			resp.getWriter().println("Starting cache key: " + cacheKey);
		} catch(WSSecurityException wse) {
			System.out.println("error getting cache key " + wse.toString() );
			resp.getWriter().print("failed");
			throw wse;
		}
		ObjectOutputStream outfile = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(serFileName)));

		try {
			outfile.writeObject( contextualExecutor );
		} catch (Exception e){
			System.out.println("Error writing contextual task " + e.toString() );
			resp.getWriter().print("FAILED");
			throw e;
		} finally {
			outfile.close();
		}
	}

	/**
	 * Work Item 141678 FAT: Validate the security context for a user with a custom cache key
	 */
	public void testDeserializeSecurityContextWithCacheKey(HttpServletRequest request, HttpServletResponse resp, String serFileName, final String contextAllowedRole, final String contextDeniedRole, String originalAllowedRole, String originalDeniedRole, final String cacheKey, final String cacheMiss) throws Exception {
		final String successMsgAllowed = contextAllowedRole + BEAN_SUCCESS_MESSAGE;
		final String successMsgDenied = contextDeniedRole + BEAN_FAILURE_MESSAGE;

		InputStream input = request.getServletContext().getResourceAsStream(serFileName);
		ObjectInputStream objectInput = new ObjectInputStream(input);
		final Executor contextualExecutor;

		try {
			contextualExecutor = (Executor) objectInput.readObject();//this will call Security code's readObject() 
		} finally {												     //in com.ibm.ws.security.context.internal.SecurityContextImpl
			objectInput.close();
		}

		// Apply the deserialized context on the *current* thread.
		contextualExecutor.execute(new Runnable() {
			@Override
			public void run() {
				// check cache key from results and compare to original cache key
				String subjectCacheKey=null;
				Subject callerSubject = null;
				try {
					callerSubject = WSSubject.getCallerSubject();
					subjectCacheKey = CacheKeyFactory.getCacheKeyFromSubject(callerSubject);
				} catch (WSSecurityException e) {
					throw new RuntimeException("Caught exception getting cache key from the caller subect: " + e);
				}
				Set<Object> publicCreds = callerSubject.getPublicCredentials();
				boolean foundCustomPublicCred = false;
				for (Object publicCred:publicCreds){
					if(!foundCustomPublicCred){
						foundCustomPublicCred = publicCred.equals(CUSTOM_PUBLIC_CRED);
					}
				}
				if (cacheMiss.equalsIgnoreCase("true")){
					//confirm current subject does not have custom public credential, since the subject was not found in the cache and it can't be recreated during the deserialize login
					if(foundCustomPublicCred) {
						throw new RuntimeException("Current caller subject should not contain the custom public credential from the deserialized security context: " + publicCreds);
					} 
				}else {
					//confirm original cacheKey is same as current subjectCacheKey
					if( (cacheKey == null) || (!cacheKey.equals(subjectCacheKey))) {
						throw new RuntimeException("Original Cache key: " + cacheKey + " should match cache key after deserialized user login: " + subjectCacheKey);
					}
					//confirm current subject has custom public credential, since the subject was found in the cache
					if(!foundCustomPublicCred){
						throw new RuntimeException("Current caller subject should contain the custom public credential from the deserialized security context, but instead is: " + publicCreds);
					}
				}	
				//Confirm access is allowed
				String resultAllowed = null;
				try {
					if(contextAllowedRole.equalsIgnoreCase(ROLE1_NAME)){
						resultAllowed = bean.role1();
					}
					else if (contextAllowedRole.equalsIgnoreCase(ROLE2_NAME)){
						resultAllowed = bean.role2();
					}
					else if (contextAllowedRole.equalsIgnoreCase(ROLE3_NAME)){
						resultAllowed = bean.role3();
					}
				} catch (Throwable x) {
					throw new RuntimeException("The user should have been allowed access to the role: " + contextAllowedRole + ". Unexpected exception: " + x);
				}
				if(!successMsgAllowed.equals(resultAllowed)){
					throw new RuntimeException("The user should have been allowed access to the role: " + contextAllowedRole + ". The expected success message was: " + successMsgAllowed + ", but instead was: " + resultAllowed);
				}

				//Confirm access is denied
				String resultDenied = null;
				try {
					if(contextDeniedRole.equalsIgnoreCase(ROLE1_NAME)){
						resultDenied = bean.role1();
					}
					else if (contextDeniedRole.equalsIgnoreCase(ROLE2_NAME)){
						resultDenied = bean.role2();
					}
					else if (contextAllowedRole.equalsIgnoreCase(ROLE3_NAME)){
						resultDenied = bean.role3();
					}
				} catch (EJBAccessException x) {
					resultDenied = contextDeniedRole + BEAN_FAILURE_MESSAGE;
				} catch (Throwable x) {
					throw new RuntimeException("The user should have been denied access to the role : " + contextDeniedRole + " with EJBAccessException. Instead, got unexpected exception: " + x);
				}
				if(!successMsgDenied.equals(resultDenied)){
					throw new RuntimeException("The user should have been denied access to the role: " + contextDeniedRole + ". The expected success message was: " + successMsgDenied + ", but instead was: " + resultDenied);
				}
			}
		});

		//Now that the original ctx is restored, 
		//confirm role permissions are reversed for original user
		confirmSecurityContextRestored(originalAllowedRole, originalDeniedRole);
	}
}
