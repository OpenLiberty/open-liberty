/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
import java.rmi.RemoteException;
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
import com.ibm.websphere.security.CustomRegistryException;
import com.ibm.websphere.security.EntryNotFoundException;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.intfc.WSSecurityService;
import com.ibm.ws.ui.internal.v1.ICatalog;
import com.ibm.ws.ui.internal.v1.ICatalogService;
import com.ibm.ws.ui.internal.v1.IFeatureToolService;
import com.ibm.ws.ui.internal.v1.IToolbox;
import com.ibm.ws.ui.internal.v1.IToolboxService;
import com.ibm.ws.ui.persistence.IPersistenceProvider;
import com.ibm.ws.ui.persistence.InvalidPOJOException;

/**
 * Implements both the ICatalogService and IToolboxService. The fundamental job
 * of these services is to load the Catalog and Toolbox instances, and this logic
 * only needs to be in one place. This same instance is referred to by the interfaces
 * it implements via the OSGi layer - that its the same implementation and class
 * instance is irrelevant.
 */
@Component(service = { ICatalogService.class, IToolboxService.class },
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM" })
public class POJOLoaderService implements ICatalogService, IToolboxService {
    private static final TraceComponent tc = Tr.register(POJOLoaderService.class);

    // DS dependencies
    private WSSecurityService securityService;
    private IFeatureToolService featureToolService;
    private IPersistenceProvider persistenceProviderFile;
    private IPersistenceProvider persistenceProviderCollective;

    /**
     * The cached Catalog singleton instance.
     */
    private ICatalog catalog;

    /**
     * The cached Toolbox instances.
     */
    private Map<String, IToolbox> toolboxes = new HashMap<String, IToolbox>();

    @Reference(service = WSSecurityService.class)
    protected synchronized void setWSSecurityService(final WSSecurityService securityService) {
        this.securityService = securityService;
    }

    protected synchronized void unsetWSSecurityService(final WSSecurityService securityService) {
        if (this.securityService == securityService) {
            this.securityService = null;
        }
    }

    /**
     * When we get a new persistence provider, clear the old cached instances.
     */
    private void clearCachedInstances() {
        this.catalog = null;
        if (this.toolboxes != null) {
            this.toolboxes.clear();
        }
    }

    /**
     * The injection point for the IFeatureToolService that allows us to get feature tools.
     * 
     * @param variableRegistryService - The variableRegistry service
     */
    @Reference(service = IFeatureToolService.class)
    protected synchronized void setIFeatureToolService(IFeatureToolService featureToolService) {
        this.featureToolService = featureToolService;
    }

    protected synchronized void unsetIFeatureToolService(IFeatureToolService featureToolService) {
        if (this.featureToolService == featureToolService) {
            this.featureToolService = null;
        }
    }

    /**
     * Only set the FILE persistence provider. This service is required.
     * 
     * @param provider
     */
    @Reference(service = IPersistenceProvider.class, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY,
               cardinality = ReferenceCardinality.MANDATORY, target = "(com.ibm.ws.ui.persistence.provider=FILE)")
    protected synchronized void setIPersistenceProviderFILE(final IPersistenceProvider provider) {
        persistenceProviderFile = provider;

        Tr.info(tc, "STORAGE_INITIALIZED", "FILE");
        clearCachedInstances();
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

        Tr.info(tc, "STORAGE_INITIALIZED", "COLLECTIVE");
        clearCachedInstances();
    }

    protected synchronized void unsetIPersistenceProviderCOLLECTIVE(final IPersistenceProvider provider) {
        if (persistenceProviderCollective == provider) {
            persistenceProviderCollective = null;
        }
        Tr.info(tc, "STORAGE_INITIALIZED", "FILE");
        clearCachedInstances();
    }

    @Activate
    protected synchronized void activate() {
        clearCachedInstances();
    }

    @Deactivate
    protected synchronized void deactive() {
        clearCachedInstances();
        toolboxes = null;
    }

    /**
     * Gets the effective instance of the IPersistenceProvider.
     * 
     * @return The instance of IPersistenceProvider
     */
    private IPersistenceProvider getPersist() {
        if (persistenceProviderCollective != null) {
            return persistenceProviderCollective;
        } else {
            return persistenceProviderFile;
        }
    }

    /**
     * Load the Catalog from the specified persistence provider.
     * 
     * @return Returns a loaded, validated Catalog instance or {@code null} if the instance was not available (or was invalid).
     */
    @FFDCIgnore(FileNotFoundException.class)
    private Catalog loadCatalogFromPersistence(final IPersistenceProvider persist) {
        try {
            Catalog catalog = persist.load(Catalog.PERSIST_NAME, Catalog.class);
            catalog.setIPersistenceProvider(getPersist());
            catalog.setIToolboxService(this);
            catalog.setIFeatureToolService(featureToolService);
            catalog.initialProcessFeatures();
            catalog.validateSelf();
            Tr.info(tc, "LOADED_PERSISTED_CATALOG");
            return catalog;
        } catch (FileNotFoundException e) {
            // This is an expected code path. If the catalog persisted
            // data is not there, just use the default instance!
            if (tc.isEventEnabled()) {
                Tr.event(tc, "The persisted instance is not available. This is an expected code path and is likely fine.\n" +
                             "If the file should have been there, we'll see isDefault=true (rather than isDefault=false).\n" +
                             "That is the indicator to the client something may be wrong.");
            }
        } catch (JSONMarshallException e) {
            String msg = e.getMessage();
            if (msg != null && msg.equals("Fatal problems occurred while mapping content")) {
                // The persisted JSON representation is not a catalog.
                // This is an unexpected code path. We should FFDC here.
                Tr.error(tc, "UNABLE_TO_LOAD_CATALOG_CONTENT");
            } else if (msg != null && msg.equals("Unable to parse non-well-formed content")) {
                // The file contents are not in a valid JSON format.
                // This is an unexpected code path. We should FFDC here.
                Tr.error(tc, "UNABLE_TO_LOAD_CATALOG_SYNTAX");
            }
        } catch (IOException e) {
            // A general I/O error occurred while accessing the persisted data.
            // This is the unexpected code path. We should FFDC here.
            Tr.error(tc, "UNABLE_TO_LOAD_CATALOG_ACCESS");
        } catch (InvalidPOJOException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught an InvalidPOJOException while trying to load the catalog from persisted storage", e);
            }
            // The persisted Catalog instance is not valid, use the default instance
            Tr.error(tc, "LOADED_CATALOG_NOT_VALID");
        }

        return null;
    }

    /**
     * Load the Catalog from the FILE persistence provider.
     * 
     * @return Returns a loaded, validated Catalog instance or {@code null} if the instance was not available (or was invalid).
     */
    private Catalog loadCatalogFromFile() {
        return loadCatalogFromPersistence(persistenceProviderFile);
    }

    /**
     * Creates the default Catalog instance.
     * 
     * @return The default catalog instance
     */
    private Catalog createDefaultCatalog() {
        Tr.info(tc, "LOADED_DEFAULT_CATALOG");
        Catalog catalog = new Catalog(getPersist(), this, featureToolService);
        catalog.initialProcessFeatures();
        return catalog;
    }

    /**
     * Load the Catalog from the COLLECTIVE persistence provider. If the COLLECTIVE
     * persistence provider does not have a copy, the FILE persistence provider will
     * be checked.
     * 
     * @return Returns a loaded, validated Catalog instance or the default instance if the persisted copy was not available (or was invalid).
     */
    private Catalog loadCatalogFromCollective() {
        Catalog catalog = loadCatalogFromPersistence(persistenceProviderCollective);
        if (catalog == null) {
            catalog = loadCatalogFromFile();
            if (catalog != null) {
                Tr.info(tc, "CATALOG_FILE_PROMOTED_TO_COLLECTIVE", "FILE", "COLLECTIVE");
            } else {
                catalog = createDefaultCatalog();
            }
            // When the collective does not have a persisted catalog, always persist it
            catalog.storeCatalog();
        }
        return catalog;
    }

    /**
     * Loads the catalog. Where the catalog is loaded from is transparent.
     * 
     * @return Returns a loaded, validated Catalog instance or the default instance if the persisted copy was not available (or was invalid).
     */
    private ICatalog loadCatalog() {
        if (persistenceProviderCollective != null) {
            return loadCatalogFromCollective();
        } else {
            ICatalog catalog = loadCatalogFromFile();
            if (catalog == null) {
                catalog = createDefaultCatalog();
            }
            return catalog;
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized ICatalog getCatalog() {
        if (catalog == null) {
            catalog = loadCatalog();
        }
        return catalog;
    }

    /**
     * Grabs the latest UserRegistry. We can not cache this since the security
     * configuration may have changed.
     * 
     * @return
     * @throws WSSecurityException
     */
    private String getUserDisplayName(final String userId) {
        try {
            return securityService.getUserRegistry(null).getUserDisplayName(userId);
        } catch (EntryNotFoundException e) {
            // unexpected path
            Tr.error(tc, "UNABLE_TO_LOAD_DISPLAY_NAME_FOR_USER", userId);
        } catch (CustomRegistryException e) {
            Tr.error(tc, "UNABLE_TO_LOAD_DISPLAY_NAME_FOR_USER", userId);
        } catch (RemoteException e) {
            Tr.error(tc, "UNABLE_TO_LOAD_DISPLAY_NAME_FOR_USER", userId);
        } catch (WSSecurityException e) {
            Tr.error(tc, "UNABLE_TO_LOAD_DISPLAY_NAME_FOR_USER", userId);
        }
        return userId;
    }

    /**
     * Load the Toolbox for the given user from the specified persistence provider.
     * 
     * @param userId The userId of the toolbox to load
     * @return Returns a loaded, validated Toolbox instance or {@code null} if the instance was not available (or was invalid)
     */
    @FFDCIgnore(FileNotFoundException.class)
    private Toolbox loadToolboxFromPersistence(final IPersistenceProvider persist, final String userId) {
        try {
            String persistedName = Toolbox.PERSIST_NAME + "-" + userId;
            Toolbox toolbox = persist.load(persistedName, Toolbox.class);
            toolbox.setCatalog(getCatalog());
            toolbox.setPersistenceProvider(getPersist());
            toolbox.validateSelf();
            Tr.info(tc, "LOADED_PERSISTED_TOOLBOX", userId);
            return toolbox;
        } catch (FileNotFoundException e) {
            // This is an expected code path. If the toolbox persisted
            // data is not there, just use the default instance!
            if (tc.isEventEnabled()) {
                Tr.event(tc, "The persisted instance is not available. This is an expected code path and is likely fine.\n" +
                             "If the file should have been there, we'll see isDefault=true (rather than isDefault=false).\n" +
                             "That is the indicator to the client something may be wrong.");
            }
        } catch (JSONMarshallException e) {
            String msg = e.getMessage();
            if (msg != null && msg.equals("Fatal problems occurred while mapping content")) {
                // The persisted JSON representation is not a toolbox.
                // This is an unexpected code path. We should FFDC here.
                Tr.error(tc, "UNABLE_TO_LOAD_TOOLBOX_CONTENT", userId);
            } else if (msg != null && msg.equals("Unable to parse non-well-formed content")) {
                // The file contents are not in a valid JSON format.
                // This is an unexpected code path. We should FFDC here.
                Tr.error(tc, "UNABLE_TO_LOAD_TOOLBOX_SYNTAX", userId);
            }
        } catch (IOException e) {
            // A general I/O error occured while accessing the persisted data.
            // This is the unexpected code path. We should FFDC here.
            Tr.error(tc, "UNABLE_TO_LOAD_TOOLBOX_ACCESS", userId);
        } catch (InvalidPOJOException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught an InvalidPOJOException while trying to load the catalog from persisted storage", e);
            }
            // The persisted Toolbox instance is not valid, use the default instance
            Tr.error(tc, "LOADED_TOOLBOX_NOT_VALID", userId);
        }

        return null;
    }

    /**
     * Load the Toolbox for the given user from the FILE persistence provider.
     * 
     * @return Returns a loaded, validated Catalog instance or {@code null} if the instance was not available (or was invalid).
     */
    private Toolbox loadToolboxFromFile(final String userId) {
        return loadToolboxFromPersistence(persistenceProviderFile, userId);
    }

    /**
     * Creates the default Toolbox instance for the specified user.
     * 
     * @param userId The userId of the toolbox to create
     * @return The default toolbox instance for the user
     */
    private Toolbox createDefaultToolbox(final String userId) {
        Tr.info(tc, "LOADED_DEFAULT_TOOLBOX", userId);
        return new Toolbox(getCatalog(), getPersist(), userId, getUserDisplayName(userId));
    }

    /**
     * Load the Catalog from the COLLECTIVE persistence provider. If the COLLECTIVE
     * persistence provider does not have a copy, the FILE persistence provider will
     * be checked.
     * 
     * @param userId The userId of the toolbox to load
     * @return Returns a loaded, validated Toolbox instance or the default instance if a persisted copy was not available (or was invalid).
     */
    private Toolbox loadToolboxFromCollective(final String userId) {
        Toolbox toolbox = loadToolboxFromPersistence(persistenceProviderCollective, userId);
        if (toolbox == null) {
            toolbox = loadToolboxFromFile(userId);
            if (toolbox != null) {
                Tr.info(tc, "TOOLBOX_FILE_PROMOTED_TO_COLLECTIVE", userId, "FILE", "COLLECTIVE");
            } else {
                toolbox = createDefaultToolbox(userId);
            }
            // When the collective does not have a persisted toolbox, always persist it
            toolbox.storeToolbox();
        }
        return toolbox;
    }

    /**
     * Loads the toolbox for the specified user. Where the toolbox is loaded from is transparent.
     * 
     * @return Returns a loaded, validated Toolbox instance or the default instance if the persisted copy was not available (or was invalid).
     */
    private IToolbox loadToolbox(final String userId) {
        if (persistenceProviderCollective != null) {
            return loadToolboxFromCollective(userId);
        } else {
            IToolbox toolbox = loadToolboxFromFile(userId);
            if (toolbox == null) {
                toolbox = createDefaultToolbox(userId);
            }
            return toolbox;
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized IToolbox getToolbox(final String userId) {
        IToolbox toolbox = toolboxes.get(userId);
        if (toolbox == null) {
            toolbox = loadToolbox(userId);
            setToolbox(userId, toolbox);
        }
        return toolbox;
    }

    /**
     * Used to override the IToolbox instance for a give user.
     * <p>
     * Primarily used for unit testing
     * 
     * @param activatedInstance If null, remove the cached Toolbox
     */
    public synchronized void setToolbox(final String userId, final IToolbox activatedInstance) {
        if (activatedInstance == null) {
            toolboxes.remove(userId);
        } else {
            toolboxes.put(userId, activatedInstance);
        }
    }

    /**
     * Resets all of the toolboxes to their default values.
     * Called by {@link Catalog#reset()}
     */
    @Override
    public synchronized void resetAllToolboxes() {
        for (IToolbox tb : toolboxes.values()) {
            tb.reset();
        }
    }

    /**
     * Removes the tool entry from all of the toolboxes.
     */
    @Override
    public synchronized void removeToolEntryFromAllToolboxes(final String id) {
        for (IToolbox tb : toolboxes.values()) {
            tb.deleteToolEntry(id);
        }
    }

}
