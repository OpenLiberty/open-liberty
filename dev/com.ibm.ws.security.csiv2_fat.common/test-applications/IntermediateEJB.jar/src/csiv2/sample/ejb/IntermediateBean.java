/*******************************************************************************
 * Copyright (c) 2015, 2026 IBM Corporation and others.
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
package csiv2.sample.ejb;

import java.rmi.RemoteException;
import java.util.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;

import csiv2.sample.shared.IntermediateTestBean;
import csiv2.sample.shared.RemoteTestBean;

@Stateless
@Remote(IntermediateTestBean.class)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
/**
 *  The EJB will be called by a servlet and it will in turn call a remote EJB.
 */
public class IntermediateBean implements IntermediateTestBean {

    private static final Logger logger = Logger.getLogger(IntermediateBean.class.getName());

    @Override
    @RolesAllowed("Admin")
    public String callRemoteTestBean(String port) throws RemoteException {
        try {
            RemoteTestBean bean = getBean(port);
            return bean.test();
        } catch (Exception e) {
            throw new RemoteException("There was a problem invoking the remote bean.", e);
        }
    }

    private RemoteTestBean getBean(String port) throws Exception {
        logger.info("Looking up RemoteTestBean bean using JNDI.");
        return narrow(jndiResolve(port, "ejb/global/DownstreamApp/DownstreamEJB/DownstreamBean!" + RemoteTestBean.class.getName()), RemoteTestBean.class);
    }

    private Object jndiResolve(String port, String name) throws Exception {
        String url = "corbaname::localhost:" + port + "/NameService#" + name;
        logger.info("Using corbaname url: " + url);
        Object obj = new InitialContext().lookup(url);
        logger.info("Looked up stub for ejb: " + obj);
        return obj;
    }

    @SuppressWarnings("unchecked")
    private static <T> T narrow(Object from, Class<T> to) {
        return (T) PortableRemoteObject.narrow(from, to);
    }

}
