/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec;

import java.security.AccessController;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.metadata.ApplicationMetaDataListener;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfigChangeEvent;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfigChangeListener;
import com.ibm.ws.webcontainer.security.internal.WebAppSecurityConfigImpl;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleCoordinator;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 * Controls JSR375 applications.
 */
@Component(service = { ApplicationMetaDataListener.class, WebAppSecurityConfigChangeListener.class },
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "service.vendor=IBM" })
public class ApplicationUtils implements ApplicationMetaDataListener, WebAppSecurityConfigChangeListener {
    private static final TraceComponent tc = Tr.register(ApplicationUtils.class);

    private static final Set<String> appsToRestart = new HashSet<String>();
    private static final String REFERENCE_APP_COORD = "appCoord";
    private static ComponentContext context;

    final static SecureAction priv = AccessController.doPrivileged(SecureAction.get());

    @Activate
    protected void activate(ComponentContext cc) {
        context = cc;
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {}

    @Reference(name = REFERENCE_APP_COORD)
    protected void setAppRecycleCoordinator(ServiceReference<ApplicationRecycleCoordinator> ref) {}

    protected void unsetAppRecycleCoordinator(ServiceReference<ApplicationRecycleCoordinator> ref) {}

    public static void registerApplication(String appName) {
        appsToRestart.add(appName);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, appName + " is added. Number of JSR375 Applications : " + appsToRestart.size());
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.webcontainer.security.WebAppSecurityConfigChangeListener#notifyWebAppSecurityConfigChanged(com.ibm.ws.webcontainer.security.WebAppSecurityConfigChangeEvent)
     */
    @Override
    public void notifyWebAppSecurityConfigChanged(WebAppSecurityConfigChangeEvent event) {
        List<String> attributes = event.getModifiedAttributeList();
        if (isAppRestartRequired(attributes)) {
            recycleApplications();
        }
    }

    @Override
    public void applicationMetaDataCreated(MetaDataEvent<ApplicationMetaData> event) throws MetaDataException {
        // do nothing.
    }

    @Override
    public void applicationMetaDataDestroyed(MetaDataEvent<ApplicationMetaData> event) {
        String appName = event.getMetaData().getJ2EEName().getApplication();
        if (appsToRestart.remove(appName)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, appName + " is removed. Number of JSR375 Applications : " + appsToRestart.size());
            }
        }
    }

    protected static boolean isAppRestartRequired(List<String> delta) {
        if (delta != null &&
            (delta.contains(WebAppSecurityConfigImpl.CFG_KEY_FAIL_OVER_TO_BASICAUTH) ||
             delta.contains(WebAppSecurityConfigImpl.CFG_KEY_LOGIN_FORM_URL) ||
             delta.contains(WebAppSecurityConfigImpl.CFG_KEY_LOGIN_ERROR_URL) ||
             delta.contains(WebAppSecurityConfigImpl.CFG_KEY_ALLOW_FAIL_OVER_TO_AUTH_METHOD) ||
             delta.contains(WebAppSecurityConfigImpl.CFG_KEY_OVERRIDE_HAM) ||
             delta.contains(WebAppSecurityConfigImpl.CFG_KEY_LOGIN_FORM_CONTEXT_ROOT) ||
             delta.contains(WebAppSecurityConfigImpl.CFG_KEY_BASIC_AUTH_REALM_NAME))) {
            return true;
        }
        return false;
    }

    private static void recycleApplications() {
        // No need to recycle apps during server shutdown
        if (FrameworkState.isStopping())
            return;
        if (!appsToRestart.isEmpty()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Recycling JSR375 applications", appsToRestart);
            }
            ApplicationRecycleCoordinator appCoord = (ApplicationRecycleCoordinator) priv.locateService(context, REFERENCE_APP_COORD);
            appCoord.recycleApplications(appsToRestart);
            // in order to avoid any potential memory leak, clear the table when recycling the applications.
            appsToRestart.clear();
        }
    }

    /**
     * This is for unit test.
     */
    protected boolean isApplicationRegistered(String appName) {
        return appsToRestart.contains(appName);
    }

    /**
     * This is for unit test.
     */
    protected int numberOfApplications() {
        return appsToRestart.size();
    }

    /**
     * This is for unit test.
     */
    protected void clearApplications() {
        appsToRestart.clear();
    }
}
