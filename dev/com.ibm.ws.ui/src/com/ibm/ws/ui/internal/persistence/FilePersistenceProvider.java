/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal.persistence;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.jsonsupport.JSON;
import com.ibm.websphere.jsonsupport.JSONFactory;
import com.ibm.websphere.jsonsupport.JSONMarshallException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.ui.persistence.IPersistenceDebugger;
import com.ibm.ws.ui.persistence.IPersistenceProvider;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.FileUtils;

/**
 * Set the service.ranking=0 so that we are considered the "default" provider.
 */
@Component(service = { IPersistenceProvider.class },
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM", "service.ranking:Integer=0", "com.ibm.ws.ui.persistence.provider=FILE" })
public class FilePersistenceProvider implements IPersistenceProvider {
    private static final TraceComponent tc = Tr.register(FilePersistenceProvider.class);

    static final String DEFAULT_PERSIST_LOCATION = "${server.output.dir}/resources/adminCenter-1.0/";

    static final String KEY_LOCATION_SERVICE = "locationServiceRef";
    private final AtomicServiceReference<WsLocationAdmin> locationServiceRef = new AtomicServiceReference<WsLocationAdmin>(KEY_LOCATION_SERVICE);
    protected static final String KEY_JSON_SERVICE = "jsonService";
    private final Map<String, File> fileLockObjs = new HashMap<String, File>();
    // For unittest
    private JSON json;
    private final IPersistenceDebugger fDebug;
    private ComponentContext cc = null;

    /**
     * Zero-argument constructor for use by DS.
     */
    public FilePersistenceProvider() {
        fDebug = new FilePersistenceDebugger();
    }

    /**
     * Unit test constructor. Allows for override of the JSON API object and
     * IFileDebugger.
     * 
     * @param json
     * @param fDebug
     */
    FilePersistenceProvider(final JSON json, final IPersistenceDebugger fDebug) {
        this.json = json;
        this.fDebug = fDebug;
    }

    @Reference(service = WsLocationAdmin.class, name = KEY_LOCATION_SERVICE,
               policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setLocationService(final ServiceReference<WsLocationAdmin> reference) {
        locationServiceRef.setReference(reference);
    }

    protected void unsetLocationService(final ServiceReference<WsLocationAdmin> reference) {
        locationServiceRef.unsetReference(reference);
    }

    protected JSON getJSONService() throws JSONMarshallException {
        // This is an internal JSON object for unittest only
        // not used in real product situations
        if (json != null) {
            return json;
        }

        JSON jsonService = JSONFactory.newInstance();
        return jsonService;
    }

    @Activate
    protected void activate(final ComponentContext context) {
        cc = context;
        locationServiceRef.activate(cc);
    }

    @Deactivate
    protected void deactivate() {
        cc = null;
        locationServiceRef.deactivate(cc);
    }

    /**
     * This method should not be called directly by any method except getPersistenceFileLockObj().
     * <p>
     * This method will create the File object.
     * 
     * @param name The persistence name associated with the File
     * @return The File object which represents the underlying persistent file.
     * @throws IOException If the parent directories could not be created (if necessary)
     * @see {@link #getPersistenceFileLockObj(String)}
     */
    @Trivial
    private File getPersistenceFile(final String name) {
        final WsResource persistenceFile = locationServiceRef.getServiceWithException().resolveResource(DEFAULT_PERSIST_LOCATION + name + ".json");

        final File file = persistenceFile.asFile();
        if (tc.isEventEnabled()) {
            Tr.event(tc, "Persistence for [" + name + "] file: " + file.getAbsolutePath());
        }

        return file;
    }

    /**
     * Gets the persistence file for the given resource. This File object
     * is also the 'lock object' used to synchronize reads and writes to
     * a particular storage name.
     * <p>
     * Access to this method is synchronized. Callers should use the returned
     * object for local synchronization.
     * 
     * @param name The persistence name associated with the File 'lock object'
     * @return The associated File 'lock object'
     * @throws IOException If the File 'lock object' could not be properly created because the parent directories could not be created.
     */
    @Trivial
    private synchronized File getPersistenceFileLockObj(final String name) {
        File lockObj = fileLockObjs.get(name);
        if (lockObj == null) {
            lockObj = getPersistenceFile(name);
            fileLockObjs.put(name, lockObj);
        }
        return lockObj;
    }

    /**
     * Attempts to create the parent files, if needed. If the parent
     * directories do not exist they will be created. If they can not be
     * created, an exception will be thrown to the caller. Parent directories
     * are NOT created by the Rest handler JSON API so we must ensure they are created here.
     * 
     * @param file The File whose parents should be created, if needed.
     * @throws IOException If the parent directories could not be created.
     */
    @Trivial
    private void createParentIfNeeded(final File file) throws IOException {
        final File parentDir = file.getParentFile();
        if (!FileUtils.fileExists(parentDir)) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Parent directories do not exist. Attempting creation.");
            }
            if (!FileUtils.fileMkDirs(parentDir)) {
                throw new IOException("Unable to create required parent directories for " + file.getAbsolutePath());
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void store(final String name, final Object pojo) throws JSONMarshallException, IOException {
        final File file = getPersistenceFileLockObj(name);
        try {
            synchronized (file) {
                createParentIfNeeded(file);
                JSON jsonService = getJSONService();
                jsonService.serializeToFile(file, pojo);
            }
        } catch (JSONMarshallException e) {
            // This should NEVER, ever happen
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Unexpected JSONMarshallException caught while storing data to the file. THIS SHOULD NEVER HAPPEN.", e);
            }
            String msg = e.getMessage();
            if (msg != null && msg.equals("I/O exception of some sort has occurred")) {
                Tr.error(tc, "FILE_PERSISTENCE_STORE_IO_ERROR", file.getAbsolutePath(), e.getMessage());
            }
            throw e;
        } catch (IOException e) {
            Tr.error(tc, "FILE_PERSISTENCE_STORE_IO_ERROR", file.getAbsolutePath(), e.getMessage());
            throw e;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void storePlainText(final String name, final String content) throws JSONMarshallException, IOException {
        final File file = getPersistenceFileLockObj(name);
        try {
            synchronized (file) {
                createParentIfNeeded(file);
                org.apache.commons.io.FileUtils.writeStringToFile(file, content, "UTF-8");
            }
        } catch (IOException e) {
            Tr.error(tc, "FILE_PERSISTENCE_STORE_IO_ERROR", file.getAbsolutePath(), e.getMessage());
            throw e;
        }
    }

    /**
     * {@inheritDoc} <p>
     * FileNotFound exception is expected. Do not do anything in that flow.
     * Otherwise, catch each other type of problem and log a meaningful error
     * message so the customer could possibly try to resolve it. This will also
     * cause an FFDC to get generated, which is what we want as these situations
     * should <b>never</b> happen in normal flows.
     */
    @FFDCIgnore(FileNotFoundException.class)
    @Override
    public <T> T load(final String name, final Class<T> clazz) throws JSONMarshallException, IOException {
        final File file = getPersistenceFileLockObj(name);
        try {
            synchronized (file) {
                JSON jsonService = getJSONService();
                return jsonService.parse(file, clazz);
            }
        } catch (JSONMarshallException e) {
            String msg = e.getMessage();
            if (msg != null && msg.equals("Unable to parse non-well-formed content")) {
                Tr.error(tc, "FILE_PERSISTENCE_BAD_JSON", file.getAbsolutePath(), fDebug.getFileContents(file));
            } else if (msg != null && msg.equals("Fatal problems occurred while mapping content")) {
                Tr.error(tc, "FILE_PERSISTENCE_WRONG_CLASS", file.getAbsolutePath(), clazz.getCanonicalName(), fDebug.getFileContents(file));
            }
            throw e;
        } catch (FileNotFoundException e) {
            // This is an expected error flow, do not FFDC or log anything
            throw e;
        } catch (IOException e) {
            Tr.error(tc, "FILE_PERSISTENCE_IO_ERROR", file.getAbsolutePath(), e.getMessage());
            throw e;
        }

    }

    /**
     * {@inheritDoc} <p>
     * FileNotFound exception is expected. Do not do anything in that flow.
     * Otherwise, catch each other type of problem and log a meaningful error
     * message so the customer could possibly try to resolve it. This will also
     * cause an FFDC to get generated, which is what we want as these situations
     * should <b>never</b> happen in normal flows.
     */
    @FFDCIgnore(FileNotFoundException.class)
    @Override
    public String loadPlainText(final String name) throws FileNotFoundException, IOException {
        final File file = getPersistenceFileLockObj(name);
        try {
            synchronized (file) {
                return org.apache.commons.io.FileUtils.readFileToString(file, "UTF-8");
            }
        } catch (FileNotFoundException e) {
            // This is an expected error flow, do not FFDC or log anything
            throw e;
        } catch (IOException e) {
            Tr.error(tc, "FILE_PERSISTENCE_IO_ERROR", file.getAbsolutePath(), e.getMessage());
            throw e;
        }

    }

    /** {@inheritDoc} */
    @Override
    public long getLastModified(final String name) {
        final File file = getPersistenceFileLockObj(name);
        synchronized (file) {
            return FileUtils.fileLastModified(file);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean delete(String name) throws IOException {
        final File file = getPersistenceFileLockObj(name);

        synchronized (file) {
            return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                @Override
                public Boolean run() {
                    boolean deleted = file.delete();
                    File parent = file.getParentFile();
                    String[] sa = parent.list();
                    if (sa.length == 0)
                    {
                        if (parent.delete() == false)
                        {
                            Tr.warning(tc, "TOOLDATA_PARENT_DIR_DELETE_RESULT_FALSE", parent.getAbsolutePath());
                        }
                    }
                    return deleted;
                }

            });
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean exists(String name)
    {
        final File file = getPersistenceFileLockObj(name);

        synchronized (file) {
            return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                @Override
                public Boolean run() {
                    return file.exists();
                }

            });
        }

    }
}
