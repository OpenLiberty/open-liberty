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
import java.util.Set;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.security.javaeesec.CDIHelper;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;


//TODO: add module name for the error message.
//TODO: add module name for the error message.
//TODO: add module name for the error message.

public class ModulePropertiesUtils {
    private static final TraceComponent tc = Tr.register(ModulePropertiesUtils.class);

    private static ModulePropertiesUtils self = new ModulePropertiesUtils();

    protected ModulePropertiesUtils() {}

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

    public boolean isHttpAuthenticationMechanism() {
        HttpAuthenticationMechanism ham = getHttpAuthenticationMechanism(false);
        if (ham != null) {
            return true;
        }
        return false;
    }

    public HttpAuthenticationMechanism getHttpAuthenticationMechanism() {
        return getHttpAuthenticationMechanism(true);
    }

    private HttpAuthenticationMechanism getHttpAuthenticationMechanism(boolean logError) {
        HttpAuthenticationMechanism ham = null;
        CDI cdi = getCDI();
        if (cdi != null) {
            Instance<ModulePropertiesProvider> mppi = cdi.select(ModulePropertiesProvider.class);
            if (mppi != null && !mppi.isUnsatisfied() && !mppi.isAmbiguous()) {
                List<Class> implClassList = mppi.get().getAuthMechClassList();
                if (implClassList.size() == 1) {
                    Instance<HttpAuthenticationMechanism> hami = cdi.select(implClassList.get(0));
                    if (hami != null && !hami.isUnsatisfied() && !hami.isAmbiguous()) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "HAM from the current CDI : " + hami);
                        }
                        ham = hami.get();
                    } else if (cdi.getBeanManager().equals(CDIHelper.getBeanManager()) == false) {
                        // try module level.
                        Set<HttpAuthenticationMechanism> hams = CDIHelper.getBeansFromCurrentModule(implClassList.get(0));
                        if (hams.size() == 1) {
                            ham = hams.iterator().next();
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, "HAM from the module BeanManager : " + ham);
                            }
                        }
                    }
                    if (ham == null) {
                        Tr.error(tc, "JAVAEESEC_ERROR_NO_HAM", getJ2EEModuleName(), getJ2EEApplicationName());
                    }
                } else {
                    Tr.error(tc, "JAVAEESEC_ERROR_MULTIPLE_HTTPAUTHMECHS", getJ2EEModuleName(), getJ2EEApplicationName(), implClassList);
                }
            } else if (logError) {
                Tr.error(tc, "JAVAEESEC_ERROR_NO_MODULE_PROPS", getJ2EEApplicationName());
            }
        }
        return ham;
    }

    public boolean isImmediateEval(String elExpression) {
        if (elExpression != null) {
            return elExpression.startsWith("#{");
        }
        return false;
    }

    public String extractExpression(String elExpression) {
        if (elExpression != null && (elExpression.startsWith("#{") || elExpression.startsWith("${")) && elExpression.endsWith("}")) {
            return elExpression.substring(2, elExpression.length() -1);
        }
        return elExpression;
    }

    protected CDI getCDI() {
        return CDI.current();
    }

    protected ComponentMetaData getComponentMetaData() {
        return ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
    }
}
