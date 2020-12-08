/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal.v1.pojo;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.jsonsupport.JSONMarshallException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.ui.internal.v1.IToolDataService;
import com.ibm.ws.ui.persistence.IPersistenceProvider;

/**
 * Implements both the IToolDataService. The service is to load/store/delete/update the
 * plain text from/to the storage.
 */
@Component(service = { IToolDataService.class },
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM" })
public class PlainTextLoaderService implements IToolDataService {
    private static final TraceComponent tc = Tr.register(PlainTextLoaderService.class);

    // DS dependencies
    private IPersistenceProvider persistenceProviderFile;
    private IPersistenceProvider persistenceProviderCollective;

    /**
     * Only set the FILE persistence provider. This service is required.
     * 
     * @param provider
     */
    @Reference(service = IPersistenceProvider.class, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY,
               cardinality = ReferenceCardinality.MANDATORY, target = "(com.ibm.ws.ui.persistence.provider=FILE)")
    protected synchronized void setIPersistenceProviderFILE(final IPersistenceProvider provider) {
        persistenceProviderFile = provider;

        Tr.info(tc, "STORAGE_INITIALIZED_PLAINTEXT_LOADER", "FILE");
    }

    protected synchronized void unsetIPersistenceProviderFILE(final IPersistenceProvider provider) {
        if (persistenceProviderFile == provider) {
            persistenceProviderFile = null;
        }
    }

    /**
     * Only set the COLLECTIVE persistence provider. This server is optional.
     * 
     * @param provider
     */
    @Reference(service = IPersistenceProvider.class, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY,
               cardinality = ReferenceCardinality.OPTIONAL, target = "(com.ibm.ws.ui.persistence.provider=COLLECTIVE)")
    protected synchronized void setIPersistenceProviderCOLLECTIVE(final IPersistenceProvider provider) {
        persistenceProviderCollective = provider;

        Tr.info(tc, "STORAGE_INITIALIZED_PLAINTEXT_LOADER", "COLLECTIVE");
    }

    protected synchronized void unsetIPersistenceProviderCOLLECTIVE(final IPersistenceProvider provider) {
        if (persistenceProviderCollective == provider) {
            persistenceProviderCollective = null;
        }
        Tr.info(tc, "STORAGE_INITIALIZED_PLAINTEXT_LOADER", "FILE");
    }

    @Activate
    protected synchronized void activate() {}

    @Deactivate
    protected synchronized void deactive() {}

    /**
     * Gets the effective instance of the IPersistenceProvider.
     * 
     * @return The instance of IPersistenceProvider
     */
    protected synchronized IPersistenceProvider getPersist() {
        if (persistenceProviderCollective != null) {
            return persistenceProviderCollective;
        } else {
            return persistenceProviderFile;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getToolData(String userId, String toolName) {
        String persistedName = toolName + "/" + userId;
        synchronized (persistedName) {
            if (persistenceProviderCollective != null) {
                return loadToolDataFromPersistence(persistenceProviderCollective, userId, toolName);

            } else {
                return loadToolDataFromPersistence(persistenceProviderFile, userId, toolName);
            }
        }
    }

    /**
     * Load the tool data for the given user/tool from the specified persistence provider.
     * 
     * @param userId The user id.
     * @param toolName the name of the tool
     * @return Returns the tool data, or null if the file is not found, or if IOException is thrown.
     */
    @FFDCIgnore(FileNotFoundException.class)
    private String loadToolDataFromPersistence(final IPersistenceProvider persist, final String userId, final String toolName) {
        try {
            String persistedName = toolName + "/" + userId;
            String toolData = persist.loadPlainText(persistedName);
            Tr.info(tc, "LOADED_PERSISTED_TOOL_DATA", userId, toolName);
            return toolData;
        } catch (FileNotFoundException e) {
            // This is an expected code path. If the user/tool has no data
            if (tc.isEventEnabled()) {
                Tr.event(tc, "The persisted tool data is not available. This is an expected code path and is likely fine.\n");
            }
        } catch (IOException e) {
            // A general I/O error occured while accessing the persisted data.
            // This is the unexpected code path. We should FFDC here.
            Tr.error(tc, "UNABLE_TO_LOAD_TOOL_DATA_ACCESS", userId, toolName);
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteToolData(String userId, String toolName) {
        boolean deletedFromCollective = true;
        boolean deletedFromFile = true;
        String persistedName = toolName + "/" + userId;
        synchronized (persistedName) {

            if (persistenceProviderCollective != null)
                deletedFromCollective = deleteToolDataFromPersistence(persistenceProviderCollective, userId, toolName);

            if (persistenceProviderFile != null)
                deletedFromFile = deleteToolDataFromPersistence(persistenceProviderFile, userId, toolName);
        }
        return deletedFromFile && deletedFromCollective;

    }

    /** {@inheritDoc} */
    @Override
    public String addToolData(String userId, String toolName, String toolData) {

        return postAndPutToolDataToPersistence(getPersist(), userId, toolName, toolData);
    }

    /**
     * Deletes the tool data for the given user/tool from the specified persistence provider.
     * 
     * @param userId The user id.
     * @param toolName the name of the tool
     * @return Returns <code>true</code> if the tool data is deleted. Otherwise return <code>false</code>.
     */
    private boolean deleteToolDataFromPersistence(final IPersistenceProvider persist, final String userId, final String toolName) {
        try {
            String persistedName = toolName + "/" + userId;
            boolean ret = true;
            if (persist != null && persist.exists(persistedName))
            {
                ret = persist.delete(persistedName);
                if (ret)
                    Tr.info(tc, "DELETED_PERSISTED_TOOL_DATA", userId, toolName);
            }
            return ret;
        } catch (IOException e) {
            Tr.error(tc, "UNABLE_TO_DELETE_PERSISTED_TOOL_DATA", userId, toolName);
        }
        return false;
    }

    /**
     * Posts the tool data for the given user/tool to the specified persistence provider.
     * 
     * @param userId The userId of the tool data to be saved.
     * @param toolName the name of the tool
     * @return Returns tool data string.
     */
    private String postAndPutToolDataToPersistence(final IPersistenceProvider persist, final String userId, final String toolName,
                                                   final String toolData) {
        try {
            String persistedName = toolName + "/" + userId;
            synchronized (persistedName) {
                persist.storePlainText(persistedName, toolData);
                Tr.info(tc, "POSTED_TOOL_DATA", userId, toolName);
            }
            return toolData;
        } catch (JSONMarshallException e) {
            // This should not occur. FFDC here.
            Tr.error(tc, "UNABLE_TO_POST_TOOL_JSON_DATA_CONTENT", e.getMessage());
        } catch (IOException e) {
            Tr.error(tc, "UNABLE_TO_POST_TOOL_DATA_CONTENT", userId);
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean exists(String userId, String toolName) {
        String persistedName = toolName + "/" + userId;
        synchronized (persistedName) {
            if (persistenceProviderCollective != null) {
                return persistenceProviderCollective.exists(persistedName);
            } else {
                return persistenceProviderFile.exists(persistedName);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void promoteIfPossible(String userId, String toolName)
    {
        String persistedName = toolName + "/" + userId;
        synchronized (persistedName) {
            if (persistenceProviderCollective != null) {
                if (persistenceProviderCollective.exists(persistedName) == false && persistenceProviderFile.exists(persistedName) == true)
                {
                    try {
                        String s = loadToolDataFromPersistence(persistenceProviderFile, userId, toolName);
                        if (s != null)
                            persistenceProviderCollective.storePlainText(persistedName, s);
                    } catch (IOException e)
                    {
                        Tr.error(tc, "UNABLE_TO_PROMOTE_TOOL_DATA_CONTENT", userId, toolName);
                    } catch (JSONMarshallException e) {
                        Tr.error(tc, "UNABLE_TO_PROMOTE_TOOL_JSON_DATA_CONTENT", e.getMessage());
                    }
                }
            }
        }
    }
}
