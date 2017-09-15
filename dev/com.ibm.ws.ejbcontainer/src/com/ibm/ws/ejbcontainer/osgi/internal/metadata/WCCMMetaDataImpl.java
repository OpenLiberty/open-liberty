/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal.metadata;

import java.util.Map;

import com.ibm.tx.jta.embeddable.GlobalTransactionSettings;
import com.ibm.tx.jta.embeddable.LocalTransactionSettings;
import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.ejbcontainer.osgi.internal.BeanInitDataImpl;
import com.ibm.ws.metadata.ejb.BeanInitData;
import com.ibm.ws.metadata.ejb.WCCMMetaData;
import com.ibm.ws.resource.ResourceRefConfigList;

public class WCCMMetaDataImpl extends WCCMMetaData {

    private final ResourceRefConfigList ivResourceRefConfigList;

    public com.ibm.ws.javaee.dd.ejbext.EnterpriseBean enterpriseBeanExtension;
    public com.ibm.ws.javaee.dd.commonbnd.RefBindingsGroup refBindingsGroup;
    public Map<String, com.ibm.ws.javaee.dd.commonbnd.Interceptor> interceptorBindings;

    public WCCMMetaDataImpl(ResourceRefConfigList resourceRefConfigList) {
        ivResourceRefConfigList = resourceRefConfigList;
    }

    @Override
    public ResourceRefConfigList createResRefList() {
        return ivResourceRefConfigList;
    }

    @Override
    public void initialize(BeanInitData bid) {
        super.initialize(bid);

        // Obtain either the ManagedBean or EJB bindings and extensions references... but not both.
        BeanInitDataImpl beanInit = (BeanInitDataImpl) bid;

        enterpriseBeanExtension = beanInit.enterpriseBeanExt;
        refBindingsGroup = beanInit.beanBnd;
        if (bid.ivType == InternalConstants.TYPE_MANAGED_BEAN) {
            interceptorBindings = beanInit.getModuleInitData().managedBeanInterceptorBindings;
        } else {
            interceptorBindings = beanInit.getModuleInitData().ejbJarInterceptorBindings;
        }

    }

    @Override
    public boolean isStartEJBAtApplicationStart() {
        return enterpriseBeanExtension != null &&
               enterpriseBeanExtension.getStartAtAppStart() != null &&
               enterpriseBeanExtension.getStartAtAppStart().getValue();
    }

    @Override
    public LocalTransactionSettings createLocalTransactionSettings()
    {
        return new LocalTranConfigDataImpl(enterpriseBeanExtension);
    }

    @Override
    public GlobalTransactionSettings createGlobalTransactionSettings()
    {
        return new GlobalTranConfigDataImpl(enterpriseBeanExtension);
    }

}
