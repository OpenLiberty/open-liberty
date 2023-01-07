/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package com.ibm.ws.security.oauth20.plugins;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.ibm.ws.security.oauth20.util.OidcOAuth20Util;

public class OidcBaseClientSerializer implements JsonSerializer<OidcBaseClient> {

    @Override
    public JsonElement serialize(OidcBaseClient client, Type typeOfClient, JsonSerializationContext context) {
        // Create deep copy
        OidcBaseClient deepCopyClient = client.getDeepCopy();

        // Mask password in new deep copy
        deepCopyClient.setClientSecret("*");

        return (new JsonParser()).parse(OidcOAuth20Util.GSON_RAW.toJson(deepCopyClient));
    }
}
