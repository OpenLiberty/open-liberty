/*******************************************************************************
 * Copyright (c) 2014, 2026 IBM Corporation and others.
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
import java.security.PrivilegedAction;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.security.auth.Subject;

import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;

import csiv2.sample.shared.RemoteTestBean;

@Stateless
@Remote(RemoteTestBean.class)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
/**
 *  The EJB will be called by a servlet to which the security subject will be returned
 */
public class DownstreamBean implements RemoteTestBean {
    private Subject mySubject;

    /**
     * This method will return the Security Subject to the calling servlet
     * 
     * @return Subject
     */
    public Subject returnSecuritySubject() {
        try {
            mySubject = WSSubject.getCallerSubject();
            System.out.println("The calling servlet's subject is returned, which inturn "
                               + "used to call the action");
            Subject.doAs(mySubject, action);
        } catch (WSSecurityException e) {
            e.printStackTrace();
        }
        return mySubject;
    }

    private final PrivilegedAction<?> action = new PrivilegedAction<Object>() {
        @Override
        public Object run() {
            return null;
        }
    };

    @Override
    @RolesAllowed("Admin")
    public String test() throws RemoteException {
        Subject callerSubject = null;
        try {
            callerSubject = WSSubject.getCallerSubject();
        } catch (WSSecurityException e) {
            throw new RemoteException("There was a problem getting the caller subject.", e);
        }
        return "callerSubject: " + callerSubject;
    }
}
