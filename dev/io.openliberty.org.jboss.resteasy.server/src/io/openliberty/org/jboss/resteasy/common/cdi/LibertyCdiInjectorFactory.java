/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.org.jboss.resteasy.common.cdi;

import java.util.Map;
import java.util.WeakHashMap;

import javax.enterprise.inject.spi.BeanManager;

import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import org.jboss.resteasy.cdi.CdiInjectorFactory;


@SuppressWarnings("restriction")
public class LibertyCdiInjectorFactory extends CdiInjectorFactory {

    public static CDIService cdiService;
    
    //lookupBeanManager gets called from super's ctor, so must be initialized via getBeanManagers()
    private Map<ComponentMetaData, BeanManager> beanManagers;

    @Override
    protected BeanManager lookupBeanManager() {
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        BeanManager beanMgr = getBeanManagers().get(cmd);
        if (beanMgr == null) {
            beanMgr = cdiService.getCurrentModuleBeanManager();
            synchronized (beanManagers) {
                beanManagers.put(cmd, beanMgr);
            }
        }
        if (beanMgr != null) {
            return beanMgr;
        }
        try {
            return super.lookupBeanManager();
        } catch (Exception ex) {
            return null;
        }
    }

    private Map<ComponentMetaData, BeanManager> getBeanManagers() {
        if (beanManagers == null) {
            synchronized(this) {
                if (beanManagers == null) {
                    beanManagers = new WeakHashMap<ComponentMetaData, BeanManager>();
                }
            }
        }
        return beanManagers;
    }
}
