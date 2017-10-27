/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.properties;

import java.util.Iterator;
import java.util.List;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;


//TODO: add module name for the error message.
//TODO: add module name for the error message.
//TODO: add module name for the error message.

public class ModulePropertiesUtils {
    private static final TraceComponent tc = Tr.register(ModulePropertiesUtils.class);

    private static ModulePropertiesUtils self = new ModulePropertiesUtils();

    private ModulePropertiesUtils() {}

    public static ModulePropertiesUtils getInstance() {
        return self;
    }

    public String getJ2EEModuleName() {
        ComponentMetaData cmd = getComponentMetaData();
        if (cmd != null) {
            return cmd.getModuleMetaData().getJ2EEName().getModule();
        } else {
            return null;
        }
    }

    public String getJ2EEApplicationName() {
        ComponentMetaData cmd = getComponentMetaData();
        if (cmd != null) {
            return cmd.getJ2EEName().getApplication();
        } else {
            return null;
        }
    }

    public boolean isOneHttpAuthenticationMechanism(CDI cdi) {
        HttpAuthenticationMechanism ham = getHttpAuthenticationMechanism(cdi);
        if (ham != null) {
            return true;
        }
        return false;
    }

    public HttpAuthenticationMechanism getHttpAuthenticationMechanism(CDI cdi) {
        HttpAuthenticationMechanism ham = null;
        if (cdi != null) {
            Instance<HttpAuthenticationMechanism> hami = cdi.select(HttpAuthenticationMechanism.class);
            if (hami != null) {
                int size = countSize(hami);
                if (size > 0) {
                    Instance<ModulePropertiesProvider> mppi = cdi.select(ModulePropertiesProvider.class);
                    if (mppi != null) {
                        if (!mppi.isUnsatisfied() && !mppi.isAmbiguous()) {
                            List<Class> implClassList = mppi.get().getAuthMechClassList();
                            if (implClassList.size() == 1) {
                                hami = cdi.select(implClassList.get(0));
                                if (hami != null && !hami.isUnsatisfied() && !hami.isAmbiguous()) {
                                    if (tc.isDebugEnabled()) {
                                        Tr.debug(tc, "HAM set by ModuleProperties: " + hami);
                                    }
                                    ham = hami.get();
                                } else {
                                    Tr.error(tc, "JAVAEESEC_ERROR_NO_MPP");
                                }
                            } else {
                                // more than one. do some additional work.
                                StringBuffer sb = new StringBuffer();
                                int count = 0;
                                for(Class clz : implClassList) {
                                    hami = cdi.select(clz);
                                    if (hami != null && !hami.isUnsatisfied() && !hami.isAmbiguous()) {
                                        if (tc.isDebugEnabled()) {
                                            Tr.debug(tc, "HAMs set by ModuleProperties: " + hami);
                                        }
                                        ham = hami.get();
                                        count++;
                                        sb.append(hami).append(" ");
                                    }
                                }
                                if (count == 0) {
                                    Tr.error(tc, "JAVAEESEC_ERROR_NO_HAM", getJ2EEModuleName(), getJ2EEApplicationName());
                                } else if (count > 1) {
                                    ham = null;
                                    Tr.error(tc, "JAVAEESEC_ERROR_MULTIPLE_HTTPAUTHMECHS", getJ2EEModuleName(), getJ2EEApplicationName(), sb.toString());
                                }
                            }
                        } else {
                            Tr.error(tc, "JAVAEESEC_ERROR_NO_MODULE_PROPS", getJ2EEApplicationName());
                        }
                    }
                } else {
                    Tr.error(tc, "JAVAEESEC_ERROR_NO_HAM", getJ2EEModuleName(), getJ2EEApplicationName());
                }
            }
        }
        return ham;
    }


    private int countSize(Instance<?> instance) {
        int count = 0;
        Iterator iterator = instance.iterator();
        while(iterator.hasNext()) {
            iterator.next();
            count++;
        }
        return count;
    }

    protected ComponentMetaData getComponentMetaData() {
        return ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
    }
}
