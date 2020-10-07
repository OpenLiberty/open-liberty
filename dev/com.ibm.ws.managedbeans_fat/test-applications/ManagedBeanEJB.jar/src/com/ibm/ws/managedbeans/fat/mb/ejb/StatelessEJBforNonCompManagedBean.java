/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.managedbeans.fat.mb.ejb;

import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;

@Stateless
public class StatelessEJBforNonCompManagedBean {
    public void verifyNonCompLookup() {
        for (String name : new String[] { "java:global/env/tsr", "java:app/env/tsr", "java:module/env/tsr" }) {
            try {
                new InitialContext().lookup(name);
            } catch (NamingException e) {
                throw new EJBException(e);
            }
        }
    }
}
