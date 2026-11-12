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

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBAccessException;
import javax.enterprise.concurrent.ContextService;
import javax.servlet.http.HttpServlet;

import com.ibm.ws.ejb.session.BasicStatelessBean;

@SuppressWarnings("serial")
public class BaseServlet extends HttpServlet{
    /**
     * Message written to servlet to indicate that is has been successfully invoked.
     */
    protected static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";
    protected static final String BEAN_SUCCESS_MESSAGE = "SuccessfulCallToMethod";
    protected static final String BEAN_FAILURE_MESSAGE = "UnsuccessfulCallToMethod";
    protected static final String ROLE1_NAME = "role1";
    protected static final String ROLE2_NAME = "role2";
    protected static final String ROLE3_NAME = "role3";
    
    @Resource(lookup = "concurrent/securityContextSvc")
    protected ContextService securityContextSvc;

    @EJB(beanName = "BasicStatelessBean")
    protected BasicStatelessBean bean;
    
    /**
	 * @throws Exception
	 */
	protected void confirmSecurityContextRestored(String allowedRole, String deniedRole) throws Exception {
		//confirm access denied
        String beanResult = null;
        try {
        	if(deniedRole.equals(ROLE1_NAME)){
        		bean.role1(); 
        	} 
        	else if (deniedRole.equalsIgnoreCase(ROLE2_NAME)){
        		bean.role2();
        	}
        	else if (deniedRole.equalsIgnoreCase(ROLE3_NAME)){
            	bean.role3();
            }
        } catch (EJBAccessException x) {
        	beanResult = deniedRole + BEAN_FAILURE_MESSAGE;
        } 
        
        if(!beanResult.equals(deniedRole + BEAN_FAILURE_MESSAGE))
        	throw new Exception("Call to " + deniedRole + " method should have failed");
        
        //confirm access allowed
        if(allowedRole.equals(ROLE1_NAME)){
        	beanResult = bean.role1(); 
        } 
        else if (allowedRole.equalsIgnoreCase(ROLE2_NAME)){
        	beanResult = bean.role2();
    	}
    	else if (allowedRole.equalsIgnoreCase(ROLE3_NAME)){
    		beanResult = bean.role3();
        }
        if (!beanResult.equals(allowedRole + BEAN_SUCCESS_MESSAGE)){
        	throw new Exception("Call to " + allowedRole + " method should have succeeded.");
        }
	}
}
