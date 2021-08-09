/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jaspi;

import java.rmi.RemoteException;
import java.util.Hashtable;

import javax.security.auth.callback.CallbackHandler;

import com.ibm.websphere.security.CustomRegistryException;
import com.ibm.websphere.security.EntryNotFoundException;
import com.ibm.ws.webcontainer.security.JaspiService;
import com.ibm.wsspi.security.token.AttributeNameConstants;

/*
 * JASPIC callback handler that does not map the principal or groups to user registry entries.
 * Used for the Bridge Provider for JSR-375 Java EE Security API.
 */
public class NonMappingCallbackHandler extends JaspiCallbackHandler implements CallbackHandler {

    public NonMappingCallbackHandler(JaspiService jaspiService) {
        super(jaspiService);
    }

    @Override
    protected void addCommonAttributes(String realm, String securityName, Hashtable<String, Object> credData) {
        credData.put(AttributeNameConstants.WSCREDENTIAL_REALM, realm);
        credData.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, "user:" + realm + "/" + securityName);
        credData.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, securityName);

    }

    @Override
    protected String mapGroup(String groupFromCallback) throws CustomRegistryException, EntryNotFoundException, RemoteException {
        return groupFromCallback;
    }

}
