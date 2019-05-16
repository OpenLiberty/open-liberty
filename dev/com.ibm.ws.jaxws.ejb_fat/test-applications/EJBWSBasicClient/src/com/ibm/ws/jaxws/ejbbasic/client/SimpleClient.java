/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.ejbbasic.client;

import java.io.IOException;

import javax.xml.ws.BindingProvider;

import org.junit.Test;

/**
 *
 */
public class SimpleClient {

    @Test
    public void testUserNotFoundException() throws IOException {
        UserQueryService userQueryService = new UserQueryService();
        try {
            UserQuery userQuery = userQueryService.getUserQueryPort();
            ((BindingProvider) userQuery).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                                                  "http://127.0.0.1:8011/EJBWSBasic/UserQueryService");
            userQuery.getUser("none");
            System.out.println("FAILED UserNotFoundException is expected");
        } catch (UserNotFoundException_Exception e) {
            String userName = e.getFaultInfo().getUserName();
            if (userName.equals("none")) {
                System.out.print("PASS The expected UserNotFoundException is thrown, " + e.getMessage());
            } else {
                System.out.print("FAILED User name none not found in the exception message");
            }
        }
    }
}
