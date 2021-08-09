/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.token.mapping;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import com.ibm.json.java.JSONObject;
import com.ibm.wsspi.security.oauth20.UserCredentialResolver;
import com.ibm.wsspi.security.oauth20.UserIdentityException;

public class OAuth20TokenMappingResolver implements UserCredentialResolver {
    private final Properties properties = new Properties();
    static final String client_id = "client_id";
    static String resolverId = null;
    
    
    String userName = "spi_resolved_user";
    List<String> groups = new ArrayList<String>();
    String realm = "_realm";
    String uniqueId;
    JSONObject json = null;
    
    public OAuth20TokenMappingResolver(Dictionary<String, Object> serviceProps) {
        saveDictionary(serviceProps);
    }
    
    public void handleToken(String token) throws UserIdentityException {
    	
		System.out.println("token is : " + token.toString());
		JSONObject jobj = null;
		try {
			jobj = JSONObject.parse(token);
		} catch (Exception e) {
			throw new UserIdentityException("cannot parse the token string", e.getCause());
		}
		if (jobj != null) {
			if ((jobj.get("active")) != null && ((Boolean) jobj.get("active")) == true){
				userName = "spi_resolved_user";
				realm = resolverId + "_realm";
				uniqueId = "user:" + resolverId + "_realm/" + userName;
				groups = new ArrayList<String>();
				groups.add("Employee");
				groups.add(resolverId + "_group");
			}
			else if(jobj.get("active") == null) {
				if (jobj.get("at_hash") != null) {
					userName = "spi_resolved_user_for_rp";
					realm = "spi_resolved_realm_for_rp";
					uniqueId = "user:" + realm + userName;
					groups = new ArrayList<String>();
					groups.add("Employee");
					
				}
				else {
					//this can happen if the validation method is userInfo
					//
					userName = "";
					realm = "";
					uniqueId = "";
					groups = null;
				}
				
			}
			
		} else {
			throw new UserIdentityException("Cannot process the supplied token string!");
			//userName = "";
			//realm = "";
			//uniqueId = "";
			//groups = null;
		}
    	
    	 	
    }

    /** {@inheritDoc} */
    @Override
    public String mapToUser(String token) throws UserIdentityException {
    	handleToken(token);
    	System.out.println("mapToUser() " + userName);
        return userName;
    	//throw new UserIdentityException("just feel like throwing this exception!!!");
    }

    void saveDictionary(Dictionary<String, ?> original) {
        Enumeration<String> keys = original.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            properties.put(key, original.get(key));
        }
        resolverId = (String) original.get("id");
    }

    /** {@inheritDoc} */
    @Override
    public List<String> mapToGroups(String token) throws UserIdentityException {
    	handleToken(token);
    	if (groups != null) {
          System.out.println("mapToGroups() 'Employee', '" + resolverId + "_group" + "'");
    	}
    	else {
    		System.out.println("mapToGroups() returns null for groups..");
    	}
        //List<String> groups = new ArrayList<String>();
        //groups.add("Employee");
        //groups.add(resolverId + "_group");
    	
        return groups;
    }

    /** {@inheritDoc} */
    @Override
    public String mapToUserUniqueID(String token) throws UserIdentityException {
    	handleToken(token);
        System.out.println("mapToUserUniqueID() " + uniqueId);
        return uniqueId;
        //return "user:" + resolverId + "_realm/" + userName;
    }

    /** {@inheritDoc} */
    @Override
    public String mapToRealm(String token) throws UserIdentityException {
    	handleToken(token);
        System.out.println("mapToRealm() " + realm);
        //return resolverId + "_realm";
        return realm;
    }
}
