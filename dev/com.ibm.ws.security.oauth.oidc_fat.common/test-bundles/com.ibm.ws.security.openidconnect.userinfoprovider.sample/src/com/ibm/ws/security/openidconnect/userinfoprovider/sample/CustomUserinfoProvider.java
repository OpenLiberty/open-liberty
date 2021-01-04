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
package com.ibm.ws.security.openidconnect.userinfoprovider.sample;

import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.security.oauth20.AuthnContext;
import com.ibm.wsspi.security.openidconnect.UserinfoProvider;

public class CustomUserinfoProvider implements UserinfoProvider {

    private Properties properties = new Properties();
    static final String client_id = "client_id";
    static String resolverId = null;

	public CustomUserinfoProvider(Dictionary<String, Object> serviceProps) {
		super();
		saveDictionary(serviceProps);
	}

	@Override
	public String getUserInfo(AuthnContext authnContext) {
		if(authnContext.getUserName().equals("oidcu3")) return null;
		if(authnContext.getUserName().equals("oidcu2")) return "text";
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("access_token", authnContext.getAccessToken());
		jsonObject.put("created_at", new Date(authnContext.getCreatedAt()).toString());
		jsonObject.put("expires_in", authnContext.getExpiresIn());
		jsonObject.put("user_name", authnContext.getUserName());
		JSONArray groupIds = new JSONArray();
		groupIds.add("customGroup");
		jsonObject.put("groupIds", groupIds);
		return jsonObject.toString();
	}
	
    void saveDictionary(Dictionary<String, ?> original) {
        Enumeration<String> keys = original.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            properties.put(key, original.get(key));
        }
        resolverId = (String) original.get("id");
    }

}
