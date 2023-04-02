/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal.v1.pojo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

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
    private final Map<String, Object> syncObjects = new HashMap<String, Object>();

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

    /**
     * Gets the object for the specified tool persisted name to
     * synchronize read/write/delete of the tool data
     *
     * Access to this method is synchronized. Callers should use the returned
     * object for local synchronization.
     *
     * @param encodedPersistedName The tool encoded persisted name to look up or create the synchronization object
     * @return The object used to do synchormization
     */
    private synchronized Object getSyncObject(final String encodedPersistedName) {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "getSyncObject", "encodedPersistedName=" + encodedPersistedName);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "getSyncObject", syncObjects.toString());
        }
        Object syncObj = syncObjects.get(encodedPersistedName);
        if (syncObj == null) {
            syncObj = new Object();
            syncObjects.put(encodedPersistedName, syncObj);
        }
        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "getSyncObject", syncObj);
        }
        return syncObj;
    }

    /**
     * The persisted file name for the tool with user id in it in previous releases is not encrypted because it is sensitive. However, 
     * encryption is now done on the user id to avoid path traversal attack through the id and to provide an unique string.
     *
     * @param toolName The name of the tool
     * @param userId The user id
     * @return The encrypted file path to the tool data
     */
    private String getEncodedPersistedName(final String toolName, final String userId) {
        return toolName + "/" + Toolbox.getEncodedUserId(userId);
    }

     /**
     * Each tool persists its data in non-encrypted persisted file path in previous releases.
     *
     * @param toolName The name of the tool
     * @param userId The user id
     * @return The non-encrypted file path to the tool data
     */
    private String getNonencodedPersistedName(final String toolName, final String userId) {
        return toolName + "/" + userId;
    }

    /**
     * Return the encrypted and non-encrypted persisted names for the given tool and user id.
     *
     * @param toolName The name of the tool
     * @param userId The user id
     * @return The tool file paths including both encrypted and non-encrypted json file names
     */
    private String[] getPersistedNames(final String toolName, final String userId) {
        String[] persistedNames = new String[2];
        persistedNames[0] = getNonencodedPersistedName(toolName, userId);
        persistedNames[1] = getEncodedPersistedName(toolName, userId);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "getPersistedNames", "non-encoded file name is " + persistedNames[0]);
            Tr.debug(tc, "getPersistedNames", "encoded file name is " + persistedNames[1]);
        }
        return persistedNames;
    }

    /**
     * Promote the tool data for the given tool and user from the specified persistence provider to its encrypted persisted name.
     *
     * @param persistProvider The persistence layer
     * @param persistedNames String array containing the non-encrypted and encrypted persisted file paths 
     * @param userId The user id
     * @param toolName The name of the tool
     * @param toolData The tool data to be persisted
     * @return Returns the tool data, or null if the file is not found, or if IOException is thrown.
     */
    private void convertToEncodedPersistedName(final IPersistenceProvider persistProvider, final String[] persistedNames, 
            final String userId, final String toolName, final String toolData) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "convertToEncodedPersistedName", "converting from " + persistedNames[0] + " to " + persistedNames[1]);
        }

        // delete the tool data in the non-encoded file name
        deleteToolDataFromPersistence(persistProvider, persistedNames[0], userId, toolName);
        // create the tool data in the encoded file name
        if (postAndPutToolDataToPersistence(persistProvider, persistedNames[1], userId, toolName, toolData) != null) {
            Tr.info(tc, "TOOL_DATA_PROMOTED_TO_ENCODED_NAME", new Object[] {toolName, userId});
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getToolData(String userId, String toolName) {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "getToolData", new Object[] {"userId=" + userId, "toolName=" + toolName});
        }

        String[] persistedNames = getPersistedNames(toolName, userId);
        synchronized(getSyncObject(persistedNames[1])) {
            final IPersistenceProvider persistenceProvider = getPersist();
            // if the persisted tool data stored in the encoded file name does not exist, then read the data from the non-ecoded file name.
            // If the non-ecoded file name does not exist, then it is safe to assume persisted data not available for this tool.
            // If the non-ecoded file name exists and contains data, change the file name to the encoded name.
            String toolData = loadToolDataFromPersistence(persistenceProvider, persistedNames[1], userId, toolName);
            if (toolData == null) {
                toolData = loadToolDataFromPersistence(persistenceProvider, persistedNames[0], userId, toolName);
                if (toolData == null) {
                    if (tc.isEventEnabled()) {
                        Tr.event(tc, "The persisted tool data is not available. This is an expected code path and is likely fine.\n");
                    }
                } else if (!toolData.equals("IOException")) {
                    convertToEncodedPersistedName(persistenceProvider, persistedNames, userId, toolName, toolData);
                }
            }
            if (toolData != null && toolData.equals("IOException")) {
                // if error other than FileNotFoundException, reset to no toolData before return
                toolData = null;
            }
            if (tc.isEntryEnabled()) {
                Tr.exit(tc, "getToolData", toolData);
            }
            return toolData;
        }
    }

    /**
     * Load the tool data for the given persisted file path from the specified persistence provider.
     *
     * @param persistProvider The persistence layer
     * @param persistedName The persisted file path for the tool
     * @param userId The user id
     * @param toolName the name of the tool
     * @return Returns the tool data, or null if the file is not found, or if IOException is thrown.
     */
    @FFDCIgnore(FileNotFoundException.class)
    private String loadToolDataFromPersistence(final IPersistenceProvider persist, final String persistedName, final String userId, final String toolName) {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "loadToolDataFromPersistence", "persistedName=" + persistedName);
        }
        String toolData = null;
        // synchronized is done by the caller
        try {
            toolData = persist.loadPlainText(persistedName);
            Tr.info(tc, "LOADED_PERSISTED_TOOL_DATA", userId, toolName);
        } catch (FileNotFoundException e) {
            // do nothing and return null toolData
        } catch (IOException e) {
            // A general I/O error occured while accessing the persisted data.
            // This is the unexpected code path. We should FFDC here.
            Tr.error(tc, "UNABLE_TO_LOAD_TOOL_DATA_ACCESS", userId, toolName);
            // signal for error and not to continue loading of different file
            toolData = "IOException";
        }
        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "loadToolDataFromPersistence", toolData);
        }
        return toolData;
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteToolData(String userId, String toolName) {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "deleteToolData", new Object[] {"userId=" + userId, "toolName=" + toolName});
        }
        String[] persistedNames = getPersistedNames(toolName, userId);
        boolean deletedFromCollective = true;
        boolean deletedFromFile = true;
        synchronized(getSyncObject(persistedNames[1])) {
            for (String persistedName : persistedNames) {
                if (persistenceProviderCollective != null)
                    deletedFromCollective  = deleteToolDataFromPersistence(persistenceProviderCollective, persistedName, userId, toolName) && deletedFromCollective;

                if (persistenceProviderFile != null)
                    deletedFromFile = deleteToolDataFromPersistence(persistenceProviderFile, persistedName,  userId, toolName) && deletedFromFile;
            }
        }
        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "deleteToolData", deletedFromCollective && deletedFromFile);
        }

        return deletedFromCollective && deletedFromFile;
    }

    /** {@inheritDoc} */
    @Override
    public String addToolData(String userId, String toolName, String toolData) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "addToolData", new Object[] {"userId=" + userId, "toolName=" + toolName});
        }
        String encodedPersistedName = getEncodedPersistedName(toolName, userId);
        synchronized(getSyncObject(encodedPersistedName)) {
            return postAndPutToolDataToPersistence(getPersist(), encodedPersistedName, userId, toolName, toolData);
        }
    }

    /**
     * Deletes the tool data for the given persisted file path from the specified persistence provider.
     *
     * @param persistProvider The persistence layer
     * @param persistedName The persisted file path for the tool
     * @param userId The user id
     * @param toolName the name of the tool
     * @return Returns <code>true</code> if the tool data is deleted or does not exist. Otherwise return <code>false</code>.
     */
    private boolean deleteToolDataFromPersistence(final IPersistenceProvider persist, final String persistedName, final String userId, final String toolName) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "deleteToolDataFromPersistence", "persistedName=" + persistedName);
        }
        // synchronized is done by the caller
        try {
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
     * Posts the tool data to the given encrypted persisted name using the specified persistence provider.
     *
     * @param persistProvider The persistence layer
     * @param persistedName The encrypted persisted file path for the tool
     * @param userId The userId of the tool data to be saved
     * @param toolName the name of the tool
     * @return Returns tool data string, otherwise null if error
     */
    private String postAndPutToolDataToPersistence(final IPersistenceProvider persist, final String persistedName, final String userId, final String toolName,
                                                   final String toolData) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "postAndPutToolDataToPersistence", "persistedName=" + persistedName);
        }
        // synchronized is done by the caller
        try {
            persist.storePlainText(persistedName, toolData);
            Tr.info(tc, "POSTED_TOOL_DATA", userId, toolName);
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
        String encodedPersistedName = getEncodedPersistedName(toolName, userId);
        synchronized(getSyncObject(encodedPersistedName)) {
            if (persistenceProviderCollective != null) {
                return persistenceProviderCollective.exists(encodedPersistedName);
            } else {
                return persistenceProviderFile.exists(encodedPersistedName);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void promoteIfPossible(String userId, String toolName)
    {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "promoteIfPossible", new Object[] {"userId=" + userId, "toolName=" + toolName});
        }
        // Try promoting the content in the encoded file name from the file persistence layer to collective first. If the file doesn't exist, then
        // promote the content in the non-encoded file name from the file to collective persistence layer.
        String[] persistedNames = getPersistedNames(toolName, userId);
        synchronized(getSyncObject(persistedNames[1])) {
            if (persistenceProviderCollective != null && persistenceProviderCollective.exists(persistedNames[1]) == false) {
                if (!promoteIfPossible(persistedNames[1], persistedNames[1], userId, toolName)) {
                    promoteIfPossible(persistedNames[0], persistedNames[1], userId, toolName);
                }
            }
        }
        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "promoteIfPossible");
        }
    }

    /**
     * Promotes the tool data from file persistence layer to collective if data doesn't exist in collective.
     *
     * @param fromPersistedName The presisted file name in the file persistence layer to be prompted
     * @param toPersistedName The persisted file name to be saved in the collective persistence layer
     * @param userId The userId of the tool data to be saved
     * @param toolName the name of the tool
     * @return Returns tool data string, otherwise null if error
     */
    private boolean promoteIfPossible(final String fromPersistedName, final String toPersistedName, final String userId, final String toolName) {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "promoteIfPossible", new Object[] {"fromPersistedName=" + fromPersistedName, "toPersistedName=" + toPersistedName});
        }
        boolean promoted = false;
 
        if (persistenceProviderFile.exists(fromPersistedName) == true)
        {
            try {
                String data = loadToolDataFromPersistence(persistenceProviderFile, fromPersistedName, userId, toolName);
                if (data != null && !"IOException".equals(data)) {
                    persistenceProviderCollective.storePlainText(toPersistedName, data);
                    promoted = true;
                }
            } catch (IOException e) {
                Tr.error(tc, "UNABLE_TO_PROMOTE_TOOL_DATA_CONTENT", userId, toolName);
            } catch (JSONMarshallException e) {
                Tr.error(tc, "UNABLE_TO_PROMOTE_TOOL_JSON_DATA_CONTENT", e.getMessage());
            }
        }

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "promoteIfPossible", promoted);
        }
        return promoted;
    }
}
