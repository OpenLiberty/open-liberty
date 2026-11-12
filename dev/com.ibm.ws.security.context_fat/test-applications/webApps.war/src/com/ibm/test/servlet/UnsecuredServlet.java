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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.security.PrivilegedExceptionAction;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBAccessException;
import javax.enterprise.concurrent.ContextService;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.auth.callback.WSCallbackHandlerImpl;
import com.ibm.ws.ejb.session.BasicStatelessBean;


/**
 * Unsercured servlet.
 */
@SuppressWarnings("serial")
public class UnsecuredServlet extends HttpServlet {
    /**
     * Message written to servlet to indicate that is has been successfully invoked.
     */
    private static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";
    private static final String BEAN_SUCCESS_MESSAGE = "SuccessfulCallToMethod";
    private static final String ROLE1_NAME = "role1";
    private static final ExecutorService unmanagedExecutor = Executors.newSingleThreadExecutor();
    
    @EJB(beanName = "BasicStatelessBean")
    private BasicStatelessBean bean;
    
    @Resource(lookup = "concurrent/securityContextSvc")
    private ContextService securityContextSvc;    
    
    private String status;
    /**
     * Invokes test name found in "test" parameter passed to servlet.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("test");
        String serFileName = request.getParameter("serFileName");
        PrintWriter out = response.getWriter();

        try {
            out.println("UnsecuredServlet is starting " + test + "<br>");
            System.out.println("-----> " + test + " starting");
            getClass().getMethod(test, HttpServletRequest.class, HttpServletResponse.class, String.class).invoke(this, request, response, serFileName);
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
     * Calls a secured bean method only accessible to users under role1.
     */
    public String callRole1BeanMethod(HttpServletRequest request, HttpServletResponse response, String serFileName) throws Exception {
    	return bean.role1();
    }
    
    /**
     * Calls an unsecured bean method only accessible by anyone.
     */
    public String callUnsecuredBeanMethod(HttpServletRequest request, HttpServletResponse response, String serFileName) throws Exception {
    	return bean.unsecured();
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
            contextualExecutor = (Executor) objectInput.readObject();
        } finally {
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
    /*
     * Ensure Security context is maintained. Current thread is unauthenticated.  Save the context of the current thread
     * in a runable proxy. Log in a user. Access a secured EJB with the logged in user subject (Access should be granted.
     * Then run the previously created proxy. Access should be denied. Then Access the EJB from the current thread. 
     * Access should be denied. */
    public void testUnAuthenticatedTaskLogin(HttpServletRequest request, HttpServletResponse resp, String serFileName) throws Exception {
    	 
    	System.out.println("\nBegin testUnauthenticatedTaskLogin \n");
    	status = "failed"; 
    	
    	// Create Runnable task that calls EJB secured method 	
    	ContextTask ctt = new ContextTask( this );
    	    	
    	//Create Proxy with current context (No users Authenticated)
    	Runnable cttProxy = securityContextSvc.createContextualProxy(ctt, Runnable.class);
    	  	  	    	
    	// Log User in with Callback
    	try {
  	      Subject subject = UnsecuredServlet.login("user1","TestRealm","user1pwd");
  	      
  	      //Inline class to call bean using authenticated context from login
  	      PrivilegedExceptionAction<Void> RunBeanAsAuthorized = new PrivilegedExceptionAction<Void>() {
  	            @Override
  	            public Void run() throws Exception {
  	                bean.role1();
  	                return null;
  	            }
  	        };
  	      // If we have successfully logged in access the EJB 
  	      // using the logged in subject
  	      if ( subject != null ) {
  	    	 System.out.println("Accessing Bean Method using Programatic login subject (Access Allowed)");
  		     WSSubject.doAs(subject, RunBeanAsAuthorized);
  	         System.out.println("Successfully accessed EJB from login subject");
  	      } else {
  	    	  failTest(resp, "Error Logging in");
  	      }
  	    } catch(Exception e ) {
  	      System.out.println( "Error accessing bean from log in context ");
  	      throw e;
  	    }
  	
      	// run thread using unauthenticated Context. Should prevent access to
    	// EJB method. 
    	 System.out.println("Accessing Bean Method from unauthenticated proxy object (Access Denied)");
    	cttProxy.run();
    	
    	if( !ctt.getStatus().equals("SUCCESS")) {
    		System.out.println("Context Proxy status = " + ctt.getStatus());
    		if( ctt.getErrorResults().size() > 0 )
  	    		 throw ctt.getErrorResults().get(0);
		  	 else 
		  		 throw new Exception("Undefined error executing unauthenticated proxy call to EJB");
        } else {
           System.out.println("Successfully tested Unauthenticated path");
        }
    	// access EJB from current thread
    	// Current thread should also be unauthenticated and access should be denied
    	try {
    		System.out.println("Accessing Bean Method from current thread (Access Denied)");
    		testLocalBean();
    	} catch(EJBAccessException e ) {
    		System.out.println("Successfully denied access in current thread");
    	} catch(Exception e ) {
    		System.out.println("Error accessing EJB from current thread" );
    		throw e;
    	}
        resp.getWriter().print(SUCCESS_MESSAGE);
    }
    
    /*
     * Ensure security context is maintained. Execute a task in the  Current thread which is unauthenticated. Create a context proxy containing this threads
     * security context which accesses a secured EJB.  Deserialize a context proxy containing an authenticated user. Then access a secured EJB Method with the deserialized proxy.
     *  (Access should be allowed). Then execute the proxy created at the beginning of the test. (Access should be denied). Access the EJB from the current 
     *  thread (Access should be denied)  */
    public void testUnAuthenticatedTaskDeserialize(HttpServletRequest request, HttpServletResponse resp, String serFileName) throws Exception {
    	
    	System.out.println("\nBegin testUnauthenticatedTaskDeserialize \n");
    	status = "failed"; 
    	
    	// Create Runnable task that instantiates EJB and called secured method 	
    	ContextTask ctt = new ContextTask( this );
    	
    	//Create Proxy with current context (No users Authenticated)
    	Runnable cttProxy = securityContextSvc.createContextualProxy(ctt, Runnable.class);
    	  	  	
    	//Load up contextProxy using logged in user
    	// with authenticated context
    	System.out.println("Serialized File name = " + serFileName);
    	InputStream input = request.getServletContext().getResourceAsStream(serFileName);
        ObjectInputStream objectInput = new ObjectInputStream(input);
        final Executor authRunnableProxy;
      
          try {
            authRunnableProxy = (Executor) objectInput.readObject();
          } catch(Exception e ) {
        	  System.out.println("Error reading in new object " + e.toString() );
        	  throw e;
          }
          finally {
            objectInput.close();
          }
    	  
    	// Make sure serialized proxy was Successfully deserialized before continuing
    	if ( authRunnableProxy != null ) {
    	  // Run with authenticated proxy 
    	  try {
       		 ContextTask ctAuth = new ContextTask( this );
       		 ctAuth.setFailOnAuthentication(false);
       		 System.out.println("Accessing Bean Method from deserialize authenticated Context (Access Allowed)");
      	     authRunnableProxy.execute( ctAuth );
      	     if( ctAuth.getStatus() != "SUCCESS" ) {
      	    	 if( ctAuth.getErrorResults().size() > 0 )
      	    		 throw ctAuth.getErrorResults().get(0);
      	    	else 
   		  		 throw new Exception("Undefined error executing authenticated proxy call to EJB");
      	     }
       	  } catch (Exception e ) {
       		  System.out.println("Error Executing deserialized proxy " + e.toString() );
       		  throw e;
       	  }
      	  // run thread using unauthenticated Context. Should prevent access to
    	  // EJB method.
    	  System.out.println("Accessing Bean Method from unauthenticated proxy object (Access Denied)");
    	  cttProxy.run();
    	  // Check status
    	  if( !ctt.getStatus().equals("SUCCESS")) {
    		  	 if( ctt.getErrorResults().size() > 0 )
       	    		 throw ctt.getErrorResults().get(0);
    		  	 else 
    		  		 throw new Exception("Undefined error executing unauthenticated proxy call to EJB");
          } else {
        	  System.out.println("Successfully tested Unauthenticated path");
          }
    	  // access EJB from current thread
      	  // Current thread should also be unauthenticated and access should be denied
      	  try {
      		System.out.println("Accessing Bean Method from Current thread (Access Denied)");
      		testLocalBean();
      	  } catch(EJBAccessException e ) {
      		System.out.println("Successfully denied access in current thread");
      	  } catch(Exception e ) {
      		System.out.println("Error accessing EJB from current thread");
      		throw e;	 
      	  }
    	    
    	} else {
    		failTest(resp, "unable to deserialize ContextTask");
    	}
        resp.getWriter().print(SUCCESS_MESSAGE);
    }
    
    // Fail test function
    private void failTest(HttpServletResponse resp, String message) throws Exception {
       
    	System.out.println(message);
    	resp.getWriter().print("failed");
    	throw new Exception(message);
    }
    /*
     * Method to execute a call to the secured EJB method from the current thread*/
    private void testLocalBean() throws Exception {
    	
 
		System.out.println("running secure Bean Access Method in servlet");
		// Call method. 
		// EJBAccessException should be thrown if unauthorized or unauthenticated.  
	 	bean.role1();
 
     	
    }
    
    // Login function
    public static Subject login(String user, String realm, String password)
            throws Exception {
    	
         javax.security.auth.Subject subject = null;
         LoginContext lc = null;


         try
         {
 	    	lc = new LoginContext("WSLogin", new WSCallbackHandlerImpl(user, password));
         } catch (LoginException le)
         {
             System.err.println("Failed to create LoginContext" + le.getMessage());
 	         le.printStackTrace();
             throw le;
         } catch (SecurityException se)
         {
             System.err.println("Failed to create LoginContext" + se.getMessage());
 	         se.printStackTrace();
             throw se;
         }
         try
         {
             // attempt authentication
             lc.login();
         } catch (SecurityException se)
         {
             System.err.println("Failed to login");
 	         se.printStackTrace();
             throw se;
         }

         // get the subject
         subject = lc.getSubject();
         if ( subject == null ) {
             System.err.println("Failed to get the subject from LoginContext");
 	         return null;
         } else {
        	 return subject;
         }

     }
    
    // Called by ContextTask to get the EJB
    public BasicStatelessBean getBean() {
    	return bean;
    }    
}
