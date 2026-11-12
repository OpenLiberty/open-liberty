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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ejb.EJBAccessException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Secured servlet.
 */
@SuppressWarnings("serial")
public class SecuredServlet extends BaseServlet {

    
    private static final ExecutorService unmanagedExecutor = Executors.newSingleThreadExecutor();

    
    
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
        PrintWriter out = response.getWriter();

        try {
            out.println("SecuredServlet is starting " + test + "<br>");
            System.out.println("-----> " + test + " starting");
            if(contextAllowedRole!=null && contextDeniedRole != null && originalAllowedRole!= null && originalDeniedRole != null){
            	getClass().getMethod(test, HttpServletRequest.class, HttpServletResponse.class, String.class, String.class, String.class, String.class, String.class).invoke(this, request, response, serFileName, contextAllowedRole, contextDeniedRole, originalAllowedRole, originalDeniedRole);
            }
            else if(originalAllowedRole != null && originalDeniedRole != null) {
            	getClass().getMethod(test, HttpServletRequest.class, HttpServletResponse.class, String.class, String.class, String.class).invoke(this, request, response, serFileName, originalAllowedRole, originalDeniedRole);
            }
            else {
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
     * Serializes this servlet's security context.
     */
    public void testSerializeSecuredServletSecurityContext(HttpServletRequest request, HttpServletResponse response, String serFileName) throws Exception {
        Executor contextualExecutor = securityContextSvc.createContextualProxy(new CurrentThreadExecutor(), Executor.class);
        ObjectOutputStream outfile = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(serFileName)));
        try {
            outfile.writeObject(contextualExecutor);//this will call Security code's writeObject() 
        } finally {									//in com.ibm.ws.security.context.internal.SecurityContextImpl
            outfile.close();
        }
    }
    
    /**
     * Takes a security context and applies it to the current thread. Confirms deserialized user allowed access to contextAllowedRole and denied to contextDeniedRole.
     * After the thread exits, confirms appropriate access for the original user on the thread: access is allowed to originalAllowedRole, but denied
     * to originalDeniedRole. (ensuring security context has been restored).
     */
    public void testUserCtxToAccessBeanRolesAndConfirmCtxIsRestored(HttpServletRequest request, HttpServletResponse response, String serFileName, final String contextAllowedRole, final String contextDeniedRole, String originalAllowedRole, String originalDeniedRole) throws Exception {
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
    
    /**
     * Takes a security context and applies it to a thread. Confirms deserialized user allowed access to contextAllowedRole and denied to contextDeniedRole.
     * After the thread exits, confirms appropriate access for the original user on the thread: access is allowed to originalAllowedRole, but denied
     * to originalDeniedRole. (ensuring security context has been restored).
     */
    public void testUnauthenticatedCtxAndConfirmCtxIsRestored(HttpServletRequest request, HttpServletResponse response, String serFileName, String originalAllowedRole, String originalDeniedRole) throws Exception {
    	String successMsg = ROLE1_NAME + BEAN_FAILURE_MESSAGE + " " + ROLE2_NAME + BEAN_FAILURE_MESSAGE + " " + ROLE3_NAME + BEAN_FAILURE_MESSAGE;
    	InputStream input = request.getServletContext().getResourceAsStream(serFileName);
        ObjectInputStream objectInput = new ObjectInputStream(input);
        final Executor contextualExecutor;

        try {
            contextualExecutor = (Executor) objectInput.readObject();//this will call Security code's readObject() 
        } finally {												     //in com.ibm.ws.security.context.internal.SecurityContextImpl
            objectInput.close();
        }

        // Submit work on an unmanaged thread.
        Object result = unmanagedExecutor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                final List<Object> results = new LinkedList<Object>();
                // Apply the deserialized context on the thread.
                contextualExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                    	//Confirm access is denied to all roles
                        try {
                        	results.add(bean.role1());
                        } catch (EJBAccessException x) {
                            results.add(ROLE1_NAME + BEAN_FAILURE_MESSAGE);
                        } catch (Throwable x) {
                        	results.add(x);
                        }
                        try {
                        	results.add(bean.role2());
                        } catch (EJBAccessException x) {
                            results.add(ROLE2_NAME + BEAN_FAILURE_MESSAGE);
                        } catch (Throwable x) {
                        	results.add(x);
                        }
                        try {
                            results.add(bean.role3());
                        } catch (EJBAccessException x) {
                            results.add(ROLE3_NAME + BEAN_FAILURE_MESSAGE);
                        } catch (Throwable x) {
                        	results.add(x);
                        }
                    }
                });
                Object result1 = results.get(0);
                Object result2 = results.get(1);
                Object result3 = results.get(2);
                
                if (result1 instanceof Exception)
                    throw (Exception) result1;
                else if (result1 instanceof Error)
                    throw (Error) result1;
                else if (result2 instanceof Exception)
                    throw (Exception) result2;
                else if (result2 instanceof Error)
                    throw (Error) result2;
                else if (result3 instanceof Exception)
                    throw (Exception) result3;
                else if (result3 instanceof Error)
                    throw (Error) result3;
                else
                    return result1 + " " + result2 + " " + result3;
            }
        }).get();

        if (!successMsg.equals(result))
            throw new Exception("Unexpected result: " + result);
        
        //Now that the original ctx is restored, 
        //confirm original user role permissions
        confirmSecurityContextRestored(originalAllowedRole, originalDeniedRole);
    }

	
    
    /**
     * Calls an secured bean method only accessible to users under role1.
     */
    public String callRole1BeanMethod(HttpServletRequest request, HttpServletResponse response, String serFileName) throws Exception {
    	return bean.role1();
    }
    
    /**
     * Calls an secured bean method only accessible to users under role1. 
     * It deserializes the specified context before the call is made from an naked thread.
     */
    public void deserializeUser1Role1CtxAndCallRole1BeanMethod(HttpServletRequest request, HttpServletResponse response, String serFileName) throws Exception {
        final String successMsg = ROLE1_NAME + BEAN_SUCCESS_MESSAGE;
        InputStream input = request.getServletContext().getResourceAsStream(serFileName);
        ObjectInputStream objectInput = new ObjectInputStream(input);
        final Executor contextualExecutor;

        try {
            contextualExecutor = (Executor) objectInput.readObject();//this will call Security code's readObject() 
        } finally {												     //in com.ibm.ws.security.context.internal.SecurityContextImpl
            objectInput.close();
        }

        // Submit work on a unmanaged thread.
        Object result = unmanagedExecutor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                final List<Object> results = new LinkedList<Object>();
                
                // Make sure the caller cannot access the resource on the current thread.
                try {
                	bean.role1();
                	throw new Exception("The user should not be authorized to call secure bean method role1.");
                } catch (EJBAccessException x) {/* Expected*/} 
                
                // Apply the deserialized context on the current thread and make sure the called can now access the resource.
                contextualExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            results.add(bean.role1());
                        } catch (Throwable x) {
                            results.add(x);
                        }
                    }
                });
                Object result = results.get(0);
                if (result instanceof Exception)
                    throw (Exception) result;
                else if (result instanceof Error)
                    throw (Error) result;
                
                // Make sure the caller, once again, cannot access the resource on the current thread.
                try {
                	bean.role1();
                	throw new Exception("The user should not be authorized to call secure bean method role1.");
                } catch (EJBAccessException x) {/* Expected*/}
                
                return result;
            }
        }).get();

        if (!successMsg.equals(result))
            throw new Exception("Unexpected result: " + result);
    }
}
