/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal.metadata;

import org.osgi.framework.ServiceRegistration;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.ejs.container.EJBMethodInfoImpl;
import com.ibm.websphere.csi.LocalTranConfigData;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.metadata.extended.IdentifiableComponentMetaData;
import com.ibm.ws.ejbcontainer.osgi.BeanRuntime;
import com.ibm.ws.ejbcontainer.osgi.MDBRuntime;
import com.ibm.ws.ejbcontainer.osgi.internal.EJBRuntimeImpl;

public class OSGiBeanMetaData extends BeanMetaData implements IdentifiableComponentMetaData {
    private static final TraceComponent tc = Tr.register(OSGiBeanMetaData.class);

    public final BeanRuntime beanRuntime;
    public final String systemHomeBindingName;
    public ServiceRegistration<?> mbeanServiceReg;

    /**
     * @param slotSize
     */
    public OSGiBeanMetaData(int slotSize, BeanRuntime beanRuntime, String systemHomeBindingName) {
        super(slotSize);
        this.beanRuntime = beanRuntime;
        this.systemHomeBindingName = systemHomeBindingName;
    }

    @Override
    public EJBMethodInfoImpl createEJBMethodInfoImpl(int slotSize) {
        return new OSGiEJBMethodMetaDataImpl(slotSize);
    }

    @Override
    public void validate() throws EJBConfigurationException {
        super.validate();

        if (_localTran.getBoundary() == LocalTranConfigData.BOUNDARY_ACTIVITY_SESSION) {
            Tr.error(tc, "RESOLVER_ACTIVITY_SESSION_NOT_SUPPORTED_CNTR4113E",
                     enterpriseBeanName,
                     j2eeName.getModule(),
                     j2eeName.getApplication(),
                     "ACTIVITY_SESSION");

            String message = Tr.formatMessage(tc, "RESOLVER_ACTIVITY_SESSION_NOT_SUPPORTED_CNTR4113E",
                                              enterpriseBeanName,
                                              j2eeName.getModule(),
                                              j2eeName.getApplication(),
                                              "ACTIVITY_SESSION");
            throw new EJBConfigurationException(message);
        }
    }

    public MDBRuntime getMDBRuntime() {
        return (MDBRuntime) beanRuntime;
    }

    /**
     * @see com.ibm.ws.container.service.metadata.extended.IdentifiableComponentMetaData#getPersistentIdentifier()
     */
    @Override
    public String getPersistentIdentifier() {
        return EJBRuntimeImpl.getMetaDataIdentifierImpl(j2eeName);
    }
}
