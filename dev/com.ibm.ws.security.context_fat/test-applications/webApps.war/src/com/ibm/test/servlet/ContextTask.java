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


import javax.ejb.*;
import com.ibm.ws.ejb.session.BasicStatelessBean;
import java.util.ArrayList;
import java.io.Serializable;



/*
 * Runnable task used to access secured EJB method*/
public class ContextTask implements Runnable, Serializable {
    
	private String status;
	private boolean failOnAuthentication;
	
	private static final long serialVersionUID = -1L;
	ArrayList<Exception> errorResults;

	private BasicStatelessBean bean;
	public  boolean  done;
	
	public ContextTask(UnsecuredServlet caller) {
		status = "failed";
		bean = caller.getBean();
		failOnAuthentication = true;
	    done = false;
	    errorResults =  new ArrayList<Exception>();	
	}
	
	
	//  true = test fails if user is allowed access
	//  false = test fails of user is denied access
	public void setFailOnAuthentication(boolean flag) {
	    failOnAuthentication = flag;
	}
	
	@Override
	public void run() {

		//Call to Secured EJB Method
		runBeanAccessTest();
		
	}
	
	// Status for test
    public String getStatus() {
    	return status;
    }
    
    public ArrayList<Exception> getErrorResults() {
    	return errorResults;
    }
    //Actual call to Secured bean method. 
    private void runBeanAccessTest() {
    	

    	try {
    	    
    		System.out.println("running secure Bean Access Method"); 
    		
    		// Call method. In current security context should not have access to the method
    		// EJBAccessException should be thrown.
    		if( bean != null ) {
    		  bean.role1();
    		  System.out.println("Successfully accessed role1 method");
    	    } else
    			throw new Exception("Bean is null");
    		if( failOnAuthentication ) {
    		    System.out.println("Able to access bean method role1. User is in the role1 group");
    			status="FAILURE";
    		} else {
    		   System.out.println("Settin Status to Success");
    		   status = "SUCCESS";
    		}
    	}
    	catch( EJBAccessException  e ) {
          if( failOnAuthentication)    		
    		status = "SUCCESS";
          else {
        	errorResults.add(e);
        	status = "failed";
          }
    	}
    	catch(Exception e) {
    		
    		System.out.println("Error getting or Calling EJB " + e.getClass().getName() + " " + e.toString() );
    		errorResults.add(e);
    		status="failed";
    	}
    	
    	
    }
    

}
