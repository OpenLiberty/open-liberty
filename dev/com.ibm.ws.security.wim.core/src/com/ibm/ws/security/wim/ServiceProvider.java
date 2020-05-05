/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim;

import java.rmi.RemoteException;
import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.wim.Service;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.model.Root;

/**
 * The service provider for the virtual member manager service.
 */
public class ServiceProvider implements Service {

    private static final TraceComponent tc = Tr.register(ServiceProvider.class);

    private static final AtomicServiceReference<ConfigManager> configRef = new AtomicServiceReference<ConfigManager>("vmmConfiguration");
    private static final AtomicServiceReference<ProfileManager> profileManager = new AtomicServiceReference<ProfileManager>("profileservice");
    private volatile boolean activated = false;

    protected void setProfileservice(ServiceReference<ProfileManager> ref) {
        profileManager.setReference(ref);
    }

    public static ProfileManager getProfileService() {
        return profileManager.getService();
    }

    protected void unsetProfileservice(ServiceReference<ProfileManager> ref) {
        profileManager.unsetReference(ref);
    }

    public void activate(ComponentContext cc, Map<String, Object> properties) {
        profileManager.activate(cc);
        configRef.activate(cc);
        activated = true;
        updateFederatedManagerService();

    }

    public void deactivate(ComponentContext cc) {
        activated = false;
        Tr.info(tc, "FEDERATED_MANAGER_SERVICE_STOPPED");
        profileManager.deactivate(cc);
        configRef.deactivate(cc);
    }

    protected void setConfiguration(ServiceReference<ConfigManager> reference) {
        configRef.setReference(reference);
    }

    protected void unsetConfiguration(ServiceReference<ConfigManager> reference) {
        configRef.unsetReference(reference);
    }

    /**
     * Federated Manager is ready when all of these required services have been registered.
     */
    private void updateFederatedManagerService() {
        if (!activated) {
            return;
        }

        if (profileManager.getReference() != null) {
            Tr.info(tc, "FEDERATED_MANAGER_SERVICE_READY");
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Some required federated manager services are not available.");
            }
        }
    }

    @Override
    public Root createRootObject() throws WIMException {
        final String METHODNAME = "createRootObject";

        Root result = null;
        result = new Root();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " " + result);
        }
        return result;
    }

    @Override
    public Root get(Root root) throws WIMException {
        final String METHODNAME = "get";
        Root result = null;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " " + root);
        }
        result = getProfileService().get(root);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " " + result);
        }
        return result;
    }

    @Override
    public Root search(Root root) throws WIMException {
        final String METHODNAME = "search";
        Root result = null;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " " + root);
        }
        result = getProfileService().search(root);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " " + result);
        }
        return result;

    }

    @Override
    public Root login(Root root) throws WIMException {
        final String METHODNAME = "login";
        Root result = null;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " " + root);
        }
        result = getProfileService().login(root);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " " + result);
        }
        return result;
    }

    @Override
    public Root delete(Root root) throws WIMException {
        // final String METHODNAME = "delete";

        // f116502
        Root result = null;
        // TxHandle txHandle = null;
        // boolean success = false;
        // boolean useGlobalTransaction = true;
        // useGlobalTransaction = isUseGlobalTransaction(root);
        // d119203
        // txControl = (TransactionControl) jtaHelper.getTransactionControl(txControl, environmentManager);
        // boolean useTransaction = jtaHelper.useTransaction(txControl, environmentManager);
        try {
            /*
             * if (useTransaction && !useGlobalTransaction) {
             * if (tc.isDebugEnabled()) {
             * Tr.debug(tc, METHODNAME + " useGlobalTransaction is false, so initiating new transaction");
             * }
             * txHandle = txControl.preinvoke(false, true);
             * } else if (useTransaction) {
             * if (tc.isDebugEnabled()) {
             * Tr.debug(tc, METHODNAME + " useGlobalTransaction is true, so using same global transaction");
             * }
             * txHandle = txControl.preinvoke(false, false);
             * }
             */
            result = getProfileService().delete(root);
            // success = true;
        } catch (Exception toCatch) {
            // jtaHelper.handleException(toCatch);
        } finally {
            /*
             * if (useTransaction) {
             * jtaHelper.closeTransaction(METHODNAME, txControl, txHandle, success);
             * }
             */
        }
        return result;
    }

    @Override
    public Root create(Root root) throws WIMException {
        Root result = null;

        // boolean success = false;
        // boolean useGlobalTransaction = true;
        // useGlobalTransaction = isUseGlobalTransaction(root);

        // txControl = (TransactionControl) jtaHelper.getTransactionControl(txControl, environmentManager);
        // boolean useTransaction = jtaHelper.useTransaction(txControl, environmentManager);
        try {
/*
 * if (useTransaction && !useGlobalTransaction) {
 * if (tc.isDebugEnabled()) {
 * Tr.debug(tc, METHODNAME + " useGlobalTransaction is false, so initiating new transaction");
 * }
 * txHandle = txControl.preinvoke(false, true);
 * } else if (useTransaction) {
 * if (tc.isDebugEnabled()) {
 * Tr.debug(tc, METHODNAME + " useGlobalTransaction is true, so using same global transaction");
 * }
 * txHandle = txControl.preinvoke(false, false);
 * }
 */
            result = getProfileService().create(root);
            // success = true;
        } catch (Exception toCatch) {
            toCatch.getMessage();
            // jtaHelper.handleException(toCatch);
        } finally {
            /*
             * if (useTransaction) {
             * jtaHelper.closeTransaction(METHODNAME, txControl, txHandle, success);
             * }
             */
        }
        return result;
    }

    public String getRealmName() throws WIMException {
        String result = null;
        result = getProfileService().getDefaultRealmName();
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.security.wim.ProfileServiceLite#update(com.ibm.wsspi.security.wim.model.Root)
     */
    @Override
    public Root update(Root root) throws WIMException, RemoteException {
        // TODO Auto-generated method stub
        return null;
    }
}
