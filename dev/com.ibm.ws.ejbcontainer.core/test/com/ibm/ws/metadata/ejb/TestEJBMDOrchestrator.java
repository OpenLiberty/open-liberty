/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.metadata.ejb;

import java.util.List;
import java.util.Map;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.ContainerException;
import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.csi.EJBModuleMetaDataImpl;
import com.ibm.websphere.cpi.Persister;
import com.ibm.websphere.csi.EJBModuleConfigData;
import com.ibm.ws.ejbcontainer.failover.SfFailoverCache;
import com.ibm.ws.javaee.dd.ejb.EnterpriseBean;
import com.ibm.ws.managedobject.ManagedObjectFactory;
import com.ibm.ws.managedobject.ManagedObjectService;
import com.ibm.ws.resource.ResourceRefConfigList;
import com.ibm.wsspi.injectionengine.JNDIEnvironmentRefType;

public class TestEJBMDOrchestrator extends EJBMDOrchestrator {
    @Override
    public void processEJBJarBindings(ModuleInitData mid, EJBModuleMetaDataImpl mmd) throws EJBConfigurationException {}

    @Override
    public Persister createCMP11Persister(BeanMetaData bmd, String defaultDataSourceJNDIName) throws ContainerException {
        return null;
    }

    @Override
    protected void setActivationLoadPolicy(BeanMetaData bmd) throws EJBConfigurationException {}

    @Override
    protected void setConcurrencyControl(BeanMetaData bmd) {}

    @Override
    protected void checkPinPolicy(BeanMetaData bmd) {}

    @Override
    protected void getIsolationLevels(int[] isoLevels, int type, String[] methodNames, Class<?>[][] methodParamTypes, List<?> isoLevelList, EnterpriseBean enterpriseBean) {}

    @Override
    protected void getReadOnlyAttributes(boolean[] readOnlyAttrs, int type, String[] methodNames, Class<?>[][] methodParamTypes, List<?> accessIntentList,
                                         EnterpriseBean enterpriseBean) {}

    @Override
    public void processGeneralizations(EJBModuleConfigData moduleConfig, EJBModuleMetaDataImpl mmd) throws EJBConfigurationException {}

    @Override
    protected String getFailoverInstanceId(EJBModuleMetaDataImpl mmd, SfFailoverCache statefulFailoverCache) {
        return null;
    }

    @Override
    protected boolean getSFSBFailover(EJBModuleMetaDataImpl mmd, EJSContainer container) {
        return false;
    }

    @Override
    protected void processEJBExtensionsMetadata(BeanMetaData bmd) {}

    @Override
    protected boolean processSessionExtensionTimeout(BeanMetaData bmd) {
        return false;
    }

    @Override
    protected void processSessionExtension(BeanMetaData bmd) throws EJBConfigurationException {}

    @Override
    protected void processEntityExtension(BeanMetaData bmd) throws EJBConfigurationException {}

    @Override
    protected void processZOSMetadata(BeanMetaData bmd) {}

    @Override
    protected ManagedObjectService getManagedObjectService() throws EJBConfigurationException {
        return null;
    }

    @Override
    protected <T> ManagedObjectFactory<T> getManagedBeanManagedObjectFactory(BeanMetaData bmd, Class<T> klass) throws EJBConfigurationException {
        return null;
    }

    @Override
    protected <T> ManagedObjectFactory<T> getInterceptorManagedObjectFactory(BeanMetaData bmd, Class<T> klass) throws EJBConfigurationException {
        return null;
    }

    @Override
    protected <T> ManagedObjectFactory<T> getEJBManagedObjectFactory(BeanMetaData bmd, Class<T> klass) throws EJBConfigurationException {
        return null;
    }

    @Override
    protected void populateBindings(BeanMetaData bmd,
                                    Map<JNDIEnvironmentRefType, Map<String, String>> allBindings,
                                    Map<String, String> envEntryValues,
                                    ResourceRefConfigList resRefList) throws EJBConfigurationException {}

}
