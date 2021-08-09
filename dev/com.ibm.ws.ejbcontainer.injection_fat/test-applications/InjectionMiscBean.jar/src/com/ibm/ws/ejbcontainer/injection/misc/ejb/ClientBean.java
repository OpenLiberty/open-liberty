/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.injection.misc.ejb;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;

@Stateless
@Remote(ClientRemote.class)
@EJB(name = "java:global/env/ejb/ClientBeanRef", beanInterface = ClientRemote.class, beanName = "ClientBean")
public class ClientBean {
    @EJB(lookup = "java:global/env/ejb/ClientBeanRef")
    ClientRemote bean;

    public void test() {
        if (bean == null) {
            throw new EJBException("injected bean was null");
        }
        try {
            ClientRemote lookupbean = (ClientRemote) new InitialContext().lookup("java:global/env/ejb/ClientBeanRef");
            if (lookupbean == null) {
                throw new EJBException("looked up bean was null");
            }
        } catch (NamingException e) {
            e.printStackTrace();
            throw new EJBException(e);
        }
    }
}
