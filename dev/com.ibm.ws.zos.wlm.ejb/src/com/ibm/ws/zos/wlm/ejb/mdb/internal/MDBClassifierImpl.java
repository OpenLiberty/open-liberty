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
package com.ibm.ws.zos.wlm.ejb.mdb.internal;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ejs.container.MessageEndpointCollaborator;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.zos.core.utils.NativeUtils;
import com.ibm.ws.zos.wlm.Enclave;
import com.ibm.ws.zos.wlm.EnclaveManager;
import com.ibm.ws.zos.wlm.ejb.mdb.MDBClassifier;

/**
 * MDB Classifier class that holds a view of all MDBClassification objects needed for classifying MDB requests.
 * The MDBClassifier is equivalent to the wlmClassification parent element.
 */
@Component(service = { MDBClassifier.class, MessageEndpointCollaborator.class },
           property = { "service.vendor=IBM" })
public class MDBClassifierImpl implements MDBClassifier, MessageEndpointCollaborator {

    /** Trace component. */
    private static final TraceComponent tc = Tr.register(MDBClassifierImpl.class);

    /** The context key representing the enclave token that tracks the MDB work. */
    public static final String KEY_WLM_DATA_MDB_ENCLAVE_TOKEN = "com.ibm.ws.zos.wlm.mdb.enclave.token";

    /** The context key representing the enclave token of a pre-existing enclave. */
    public static final String KEY_WLM_DATA_PRE_MDB_ENCLAVE_TOKEN = "com.ibm.ws.zos.wlm.pre.mdb.enclave.token";

    /** Enclave manager. It is the entry point to the WLM native services. */
    EnclaveManager enclaveManager;

    /** Native utilities. */
    NativeUtils nativeUtils;

    /** Configuration Admin. */
    ConfigurationAdmin configAdmin;

    /** List holding classification data representing a mdbClassification config entry. */
    List<MDBClassification> mdbClassificationsDataList = Collections.synchronizedList(new LinkedList<MDBClassification>());

    /**
     * DS method to activate this component.
     *
     * @param cc         The component context.
     * @param properties : Map containing service properties
     */
    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> properties) {
    }

    /**
     * DS method to deactivate this component.
     *
     * @param cc     The component context.
     * @param reason The deactivation reason.
     */
    @Deactivate
    protected void deactivate(ComponentContext cc, int reason) {
        cleanup();
    }

    /**
     * Sets an instance of the EnclaveManager service.
     *
     * @param enclaveManager The EnclaveManager service instance to set.
     */
    @Reference(service = EnclaveManager.class)
    protected void setEnclaveManager(EnclaveManager enclaveManager) {
        this.enclaveManager = enclaveManager;
    }

    /**
     * Unsets an instance of the EnclaveManager service.
     *
     * @param enclaveManager The EnclaveManager service instance to unset.
     */
    protected void unsetEnclaveManager(EnclaveManager enclaveManager) {
        if (this.enclaveManager == enclaveManager) {
            this.enclaveManager = null;
        }
    }

    /**
     * Sets an instance of the NativeUtils service.
     *
     * @param nativeUtils The NativeUtils service instance to set.
     */
    @Reference(service = NativeUtils.class)
    protected void setNativeUtils(NativeUtils nativeUtils) {
        this.nativeUtils = nativeUtils;
    }

    /**
     * Unsets an instance of the NativeUtils service.
     *
     * @param nativeUtils The NativeUtils service instance to unset.
     */
    protected void unsetNativeUtils(NativeUtils nativeUtils) {
        if (this.nativeUtils == nativeUtils) {
            this.nativeUtils = null;
        }
    }

    /**
     * Sets an instance of the ConfigurationAdmin service.
     *
     * @param configAdmin The ConfigurationAdmin service instance to set.
     */
    @Reference(service = ConfigurationAdmin.class)
    protected void setConfigurationAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    /**
     * Unsets an instance of the ConfigurationAdmin service.
     *
     * @param configAdmin The ConfigurationAdmin service instance to unset.
     */
    protected void unsetConfigurationAdmin(ConfigurationAdmin configAdmin) {
        if (this.configAdmin == configAdmin) {
            this.configAdmin = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void update(Dictionary<?, ?> properties) {
        String[] mdbClassifications = (String[]) properties.get("mdbClassification");

        // Return immediately, if there is nothing to process.
        if (mdbClassifications == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Update is a No-Op.", new Object[] { properties });
            }
            return;
        }

        // Iterate over all mdbClassigication config entries. The list is in the order in which they were configured.
        List<MDBClassification> tempAllMDBClassificationsList = new LinkedList<MDBClassification>();
        for (int i = 0; i < mdbClassifications.length; i++) {
            try {
                Configuration config = getConfigAdmin().getConfiguration(mdbClassifications[i], null);
                Dictionary<?, ?> prop = config.getProperties();
                MDBClassification mdbc = new MDBClassification(prop);
                if (mdbc.isValid()) {
                    tempAllMDBClassificationsList.add(mdbc);
                }
            } catch (Throwable t) {
                // An FFDC will be issued. Move on to the next element.
            }
        }

        // Update the list of mdbClassifications.
        synchronized (mdbClassificationsDataList) {
            mdbClassificationsDataList.clear();
            mdbClassificationsDataList.addAll(tempAllMDBClassificationsList);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void cleanup() {
        synchronized (mdbClassificationsDataList) {
            mdbClassificationsDataList.clear();
        }
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Object> preInvoke(Map<String, Object> context) {
        Enclave preExistingEnclave = null;

        try {
            // Find out if the current request was configured to be tracked by WLM.
            byte[] transactionClass = classifyMDBRequest(context);
            if (transactionClass != null) {

                // Suspend the existing enclave if there is one.
                preExistingEnclave = getEnclaveManager().removeCurrentEnclaveFromThread();
                if (preExistingEnclave != null) {
                    context.put(KEY_WLM_DATA_PRE_MDB_ENCLAVE_TOKEN, preExistingEnclave);
                }

                // Create a new enclave and join it.
                long arrivalTime = Long.valueOf(getNativeUtils().getSTCK());
                Enclave enclave = getEnclaveManager().joinNewEnclave(transactionClass, arrivalTime);
                if (enclave != null) {
                    context.put(KEY_WLM_DATA_MDB_ENCLAVE_TOKEN, enclave);
                    return context;
                }
            }
        } catch (Throwable t) {
            // Catch any unexpected exceptions to prevent the MDB request from failing.
            if (preExistingEnclave != null) {
                try {
                    getEnclaveManager().restoreEnclaveToThread(preExistingEnclave);
                } catch (Throwable tt) {
                    // FFDC will do. There is nothing we can do at this point.
                }
            }
        }

        // If we are here, postInvoke will be skipped.
        return null;

    }

    /** {@inheritDoc} */
    @Override
    public void postInvoke(Map<String, Object> context) {
        try {
            // If an enclave was create to track the mdb request during preInvoke, leave it.
            Enclave enclave = (Enclave) context.get(KEY_WLM_DATA_MDB_ENCLAVE_TOKEN);
            if (enclave != null) {
                getEnclaveManager().leaveEnclave(enclave);
            }
        } catch (Throwable t) {
            // If there is an unexpected exception, there is not much we can do at this point.
            // FFDC will do.
        } finally {
            // If we suspended an existing enclave during preInvoke, resume it.
            Enclave preExistingEnclave = (Enclave) context.get(KEY_WLM_DATA_PRE_MDB_ENCLAVE_TOKEN);
            try {
                if (preExistingEnclave != null) {
                    getEnclaveManager().restoreEnclaveToThread(preExistingEnclave);
                }
            } catch (Throwable t) {
                // If there is an unexpected exception, there is not much we can do at this point.
                // FFDC will do.
            }
        }
    }

    /**
     * Retrieves the transaction class associated with configuration entries that matches the MDB request.
     *
     * @param context            The context map containing data to aid in filtering.
     * @param mdbClassifications The list of mdbClassification objects representing config entries.
     *
     * @return The transaction class associated with configuration entries that matches the MDB request.
     */
    public byte[] classifyMDBRequest(Map<String, Object> context) {
        String activationSpec = (String) context.get(MessageEndpointCollaborator.KEY_ACTIVATION_SPEC_ID);

        if (activationSpec != null) {
            synchronized (mdbClassificationsDataList) {
                Iterator<MDBClassification> i = mdbClassificationsDataList.iterator();
                while (i.hasNext()) {
                    MDBClassification classification = i.next();
                    String configActivationSpec = classification.getActivationSpecId();
                    if (activationSpec.equals(configActivationSpec) || configActivationSpec.equals("*")) {
                        return classification.getTransactionClassBytes();
                    }
                }
            }
        }

        return null;
    }

    /**
     * Returns the EnclaveManager service instance or an IllegalStateException if it is not set.
     * It allows the caller to avoid NullPointerExceptions if this service is suddenly deactivated
     * while also allowing the caller to report the issue.
     *
     * @return The EnclaveManager service instance or an IllegalStateException if it is not present.
     */
    private EnclaveManager getEnclaveManager() {
        if (enclaveManager == null) {
            throw new IllegalStateException("The enclave manager service is not active.");
        }

        return enclaveManager;
    }

    /**
     * Returns the NativeUtils service instance or an IllegalStateException if it is not set.
     * It allows the caller to avoid NullPointerExceptions if this service is suddenly deactivated
     * while also allowing the caller to report the issue.
     *
     * @return The NativeUtils service instance or an IllegalStateException if it is not present.
     */
    private NativeUtils getNativeUtils() {
        if (nativeUtils == null) {
            throw new IllegalStateException("The native utility service is not active.");
        }

        return nativeUtils;
    }

    /**
     * Returns the NativeUtils service instance or an IllegalStateException if it is not set.
     * It allows the caller to avoid NullPointerExceptions if this service is suddenly deactivated
     * while also allowing the caller to report the issue.
     *
     * @return The ConfigurationAdmin service instance or an IllegalStateException if it is not present.
     */
    private ConfigurationAdmin getConfigAdmin() {
        if (configAdmin == null) {
            throw new IllegalStateException("The configuration admin service is not active.");
        }

        return configAdmin;
    }

}
