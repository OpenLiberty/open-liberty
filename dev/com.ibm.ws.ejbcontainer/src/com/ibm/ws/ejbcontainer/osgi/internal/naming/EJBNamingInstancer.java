/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal.naming;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import com.ibm.ejs.container.ContainerEJBException;
import com.ibm.ejs.container.EJSHome;
import com.ibm.ejs.container.EJSWrapperCommon;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ejbcontainer.AmbiguousEJBReferenceException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Takes an EJBBinding and attempts to create an instance of that bean
 *
 */
@Trivial
abstract class EJBNamingInstancer {

    protected volatile boolean homeRuntime;
    protected volatile boolean remoteRuntime;
    private static final TraceComponent tc = Tr.register(EJBNamingInstancer.class);

    @FFDCIgnore(ContainerEJBException.class)
    protected Object initializeEJB(EJBBinding binding, String jndiName) throws NamingException {
        Object instance = null;

        if (binding == null) {
            return null;
        }

        // Home and remote interfaces are not supported in Liberty

        if (binding.isHome() && !homeRuntime) {
            throwCannotInstantiateUnsupported(binding, jndiName,
                                              "JNDI_CANNOT_INSTANTIATE_HOME_CNTR4008E");
        }

        if (!binding.isLocal && !remoteRuntime) {
            throwCannotInstantiateUnsupported(binding, jndiName,
                                              "JNDI_CANNOT_INSTANTIATE_REMOTE_CNTR4009E");
        }

        try {
            EJSHome home = binding.homeRecord.getHomeAndInitialize();

            if (binding.isHome()) {
                EJSWrapperCommon wc = home.getWrapper();
                if (binding.isLocal) {
                    instance = wc.getLocalObject();
                } else {
                    instance = home.createRemoteHomeObject();
                }
            } else {
                // Use interface name to create the business object
                if (binding.isLocal) {
                    instance = home.createLocalBusinessObject(binding.interfaceIndex, null);
                } else {
                    instance = home.createRemoteBusinessObject(binding.interfaceIndex, null);
                }
            }
        } catch (Throwable t) {
            throwCannotInstantiateObjectException(binding, jndiName, t);
        }
        return instance;

    }

    /**
     * Internal method to throw a NameNotFoundException for unsupported
     * Home and Remote interfaces.
     *
     * @param binding
     * @param jndiName
     * @param messageId
     * @throws NameNotFoundException
     */
    private void throwCannotInstantiateUnsupported(EJBBinding binding,
                                                   String jndiName,
                                                   String messageId) throws NameNotFoundException {
        J2EEName j2eeName = getJ2EEName(binding);
        String msgTxt = Tr.formatMessage(tc, messageId,
                                         binding.interfaceName,
                                         j2eeName.getComponent(),
                                         j2eeName.getModule(),
                                         j2eeName.getApplication(),
                                         jndiName);
        throw (new NameNotFoundException(msgTxt));
    }

    /**
     * Internal method that creates a NamingException that contains cause
     * information regarding why a binding failed to resolve. <p>
     *
     * The returned exception will provide similar information as the
     * CannotInstantiateObjectException from traditional WAS.
     */
    private void throwCannotInstantiateObjectException(EJBBinding binding,
                                                       String jndiName,
                                                       Throwable cause) throws NamingException {
        J2EEName j2eeName = getJ2EEName(binding);
        Object causeMsg = cause.getLocalizedMessage();
        if (causeMsg == null) {
            causeMsg = cause.toString();
        }
        String msgTxt = Tr.formatMessage(tc, "JNDI_CANNOT_INSTANTIATE_OBJECT_CNTR4007E",
                                         binding.interfaceName,
                                         j2eeName.getComponent(),
                                         j2eeName.getModule(),
                                         j2eeName.getApplication(),
                                         jndiName,
                                         causeMsg);
        NamingException nex = new NamingException(msgTxt);
        nex.initCause(cause);
        throw nex;
    }

    protected void throwAmbiguousEJBReferenceException(EJBBinding binding, String jndiName) throws NamingException {
        String message;

        if (binding.j2eeNames.size() > 1) {
            message = "The short-form default binding '" + jndiName +
                      "' is ambiguous because multiple beans implement this interface : " +
                      binding.j2eeNames + ". Provide an interface specific binding or use " +
                      "the long-form default binding on lookup.";
        } else {
            message = "The simple-binding-name '" + jndiName +
                      "' for bean " + binding.homeRecord.getJ2EEName() + " is ambiguous because the bean " +
                      "implements multiple interfaces.  Provide an interface specific " +
                      "binding or add #<interface> to the simple-binding-name on lookup.";
        }

        AmbiguousEJBReferenceException amEx = new AmbiguousEJBReferenceException(message);
        throwCannotInstantiateObjectException(binding, jndiName, amEx);
    }

    protected J2EEName getJ2EEName(EJBBinding binding) {
        return binding.homeRecord.getJ2EEName();
    }
}
