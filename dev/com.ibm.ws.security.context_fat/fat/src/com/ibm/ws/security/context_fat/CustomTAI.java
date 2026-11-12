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

package com.ibm.ws.security.context_fat;
/*
 * Custom Trust Associate Interceptor used to return a TAIResult with a custom cache key installed in the subect's public 
 * Credentials.
 * */

import java.util.Hashtable;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Random;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;

import com.ibm.websphere.security.WebTrustAssociationException;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.wsspi.security.tai.TAIResult;
import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;
import com.ibm.wsspi.security.token.AttributeNameConstants;

public class CustomTAI implements TrustAssociationInterceptor {

    
    private Properties initProps = null;
    private static String AUTHORIZATION = "Authorization";
    private static String CUSTOM_PUBLIC_CRED = "CustomPublicCredential";

    @Override
    public void cleanup() {}

    @Override
    public String getType() {
        return "CustomTAI";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public int initialize(Properties props)
                    throws WebTrustAssociationFailedException {
    	
    	System.out.println("Custom TAI initializing");
        if (props != null) {
            initProps = props;
        }
        return 0;
    }

    @Override
    public boolean isTargetInterceptor(HttpServletRequest req)
                    throws WebTrustAssociationException {
        return true;
    }

    @Override
    public TAIResult negotiateValidateandEstablishTrust(HttpServletRequest req, HttpServletResponse rsp)
                    throws WebTrustAssociationFailedException {
    	System.out.println("Custom TAI validate and establish trust");
    	String test = req.getParameter("test");
    	// Get user and password sent with the Http Request
    	String usernpass = new String( Base64.decodeBase64(req.getHeader(AUTHORIZATION).substring(6)));
    	String[] usernpassArray = usernpass.split(":");
    	String user = usernpassArray[0];
    	String password = usernpassArray[1];
    	// Create principal and add user
    	SimplePrincipal principal = new SimplePrincipal(user);
    	TAIResult result = null;
        System.out.println("user = " + user);
        Subject subject = null;
        // Make sure we have a userid and password to work with
        if( user != null & password != null ) {
        	// Create new subject 
        	subject = new Subject();
        	// Group Container
        	ArrayList<String> groups = new ArrayList<String>();
        	// HashTable setup for public Credentials
        	Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
        	// Add unique id
            hashtable.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, user);
            hashtable.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, user);
            // Add Cache key
            String key = null;
            if (test.equals("testSerializeSecurityContextWithCacheKey")){
            	key = user + "MyNonRandomCacheKey";
            } else {
            	key = user + new Integer( new Random().nextInt(1000)).toString();
            }
            System.out.println("Adding cache key to hashtable: " + key);
            hashtable.put(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, key);
            // if user1 then set group to role1
            if( user.equals("user1"))
                groups.add("role1");
            else
               	groups.add("role2");
            //add groups to public credentials
            hashtable.put(AttributeNameConstants.WSCREDENTIAL_GROUPS, groups);
            subject.getPublicCredentials().add(CUSTOM_PUBLIC_CRED);
            subject.getPublicCredentials().add(hashtable);
            // add Princpal to the subject
            subject.getPrincipals().add(principal);
            System.out.println("User not null created subject with simple principal");
            // Create TAIResult
            result = TAIResult.create(HttpServletResponse.SC_OK, user, subject);
        } else {
       	    if(principal != null ) 
       	      try {
       	    	// Create a special TAIResult if no userid and password are passed  
        		result = TAIResult.create(HttpServletResponse.SC_OK, WSSubject.getCallerPrincipal(), WSSubject.getCallerSubject() );
       	      } catch( WSSecurityException wse) {
       	    	  System.out.println("Error getting caller subject. TAI result will send Sever error message");
       	    	  result = null;
       	      }
       	    
        }
        if (result != null) 
          return result;
        else
          // Return error if no TAIResult can be generated	
          return TAIResult.create(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
    
    
}
