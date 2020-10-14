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
package com.ibm.wsspi.persistence.internal;

import static org.eclipse.persistence.config.PersistenceUnitProperties.SCHEMA_GENERATION_CREATE_ACTION;
import static org.eclipse.persistence.config.PersistenceUnitProperties.SESSION_EVENT_LISTENER_CLASS;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.EntityManager;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.eclipse.persistence.jpa.PersistenceProvider;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.persistence.InMemoryMappingFile;
import com.ibm.wsspi.persistence.PersistenceServiceUnitConfig;
import com.ibm.wsspi.persistence.internal.eclipselink.PsSessionEventListener;
import com.ibm.wsspi.persistence.internal.util.DoPrivHelper;
import com.ibm.wsspi.persistence.internal.util.PersistenceClassLoader;

public final class PUInfoImpl implements PersistenceUnitInfo {
    private final static String SCHEMA_GENERATION_DATABASE_ACTION;

    static {
        final String emName = EntityManager.class.getName();
        final boolean isJakarta = emName.startsWith("jakarta");
        final String prefix = isJakarta ? "jakarta" : "javax";
        SCHEMA_GENERATION_DATABASE_ACTION = prefix + ".persistence.schema-generation.database.action";
    }

    private final DataSource _jtaDataSource;
    private final DataSource _nonJtaDataSource;
    private final List<String> _classes;
    private final List<String> _mappingFileNames;
    private final List<URL> _inMemoryMappingFileURLs;
    private final Properties _props;
    private final String _name;
    private final static AtomicInteger _nameGenerator = new AtomicInteger(0);

    private final PersistenceClassLoader _appLoader;

    private final InMemoryUrlStreamHandler _inMemHandler;
    private final URL _puRoot;

    PUInfoImpl(PersistenceServiceUnitConfig conf, InMemoryUrlStreamHandler inMemHandler, URL bundleRoot) {
        _inMemHandler = inMemHandler;
        _puRoot = bundleRoot;

        _jtaDataSource = conf.getJtaDataSource();
        _nonJtaDataSource = conf.getNonJaDataSource();
        _classes = new ArrayList<String>(conf.getClasses());
        _name = "persistence_service_pu-" + _nameGenerator.incrementAndGet();
        _inMemoryMappingFileURLs = new ArrayList<URL>();
        _mappingFileNames = new ArrayList<String>(conf.getMappingFileNames());

        /**
         * This is pretty gross, but EclipseLink doesn't do a good job handling complex
         * ClassLoading environments. Consumer provided loader, Persistence service bundle loader,
         * EclipseLink bundle loader
         */
        _appLoader = DoPrivHelper.newPersistenceClassLoader(conf.getConsumerLoader(), PersistenceProvider.class, PUInfoImpl.class);

        List<InMemoryMappingFile> inMemoryMappingFiles = copyInMemoryMappingFiles(conf.getInMemoryMappingFiles());

        // Register file: based files with the ClassLoader
        for (String file : _mappingFileNames) {
            _appLoader.registerFileResource(file);
        }
        List<String> inMemNames = processInMemoryMappingFiles(inMemoryMappingFiles);
        // Now that we've registered the in-memory files, we can add them to the list of files
        // that normally only exist on the file system.
        _mappingFileNames.addAll(inMemNames);

        _props = new Properties();
        if (conf.getCreateOrUpdateTables()) {
            _props.put(SCHEMA_GENERATION_DATABASE_ACTION, SCHEMA_GENERATION_CREATE_ACTION);
            // DataSource privDs = conf.getPrivilegedDataSource();
            // if (privDs != null) {
            // // 151909
            // // _props.put(SchemaGeneration.CONNECTION, privDs);
            // }
        }
        _props.putAll(conf.getProperties());

        Boolean allow = conf.getAllowUnicode();
        if (allow == null || allow.equals(Boolean.FALSE)) {
            _props.put(SESSION_EVENT_LISTENER_CLASS, PsSessionEventListener.class.getName());
        }

        _props.putAll(conf.getProperties());
    }

    /**
     * Private method that takes a list of InMemoryMappingFiles and copies them by assigning a new
     * id while sharing the same underlying byte array. This method avoids unnecessarily create
     * identical byte arrays. Once all references to a byte array are deleted, the garbage
     * collector will cleanup the byte array.
     */
    private List<InMemoryMappingFile> copyInMemoryMappingFiles(List<InMemoryMappingFile> copyIMMF) {
        List<InMemoryMappingFile> immf = new ArrayList<InMemoryMappingFile>();
        for (InMemoryMappingFile file : copyIMMF) {
            immf.add(new InMemoryMappingFile(file.getMappingFile()));
        }
        return immf;
    }

    /**
     * Private copy constructor which is used by createCopyWithNewName.
     */
    private PUInfoImpl(PUInfoImpl source) throws IllegalArgumentException {
        _puRoot = source._puRoot;
        _jtaDataSource = source._jtaDataSource;
        _nonJtaDataSource = source._nonJtaDataSource;
        _classes = Collections.unmodifiableList(source._classes);
        _mappingFileNames = Collections.unmodifiableList(source._mappingFileNames);
        _inMemoryMappingFileURLs = Collections.unmodifiableList(source._inMemoryMappingFileURLs);
        _appLoader = source._appLoader;
        _name = "persistence_service_pu-" + _nameGenerator.incrementAndGet();
        _inMemHandler = source._inMemHandler;

        _props = new Properties();
        _props.putAll(source._props);
    }

    @Override
    public void addTransformer(ClassTransformer arg0) {
        // noop -- static weaving
    }

    @Override
    public boolean excludeUnlistedClasses() {
        return true;
    }

    @Override
    @Trivial
    public ClassLoader getClassLoader() {
        return _appLoader;
    }

    @Override
    @Trivial
    public List<URL> getJarFileUrls() {
        return Collections.emptyList();
    }

    @Override
    @Trivial
    public DataSource getJtaDataSource() {
        return _jtaDataSource;
    }

    @Override
    @Trivial
    public List<String> getManagedClassNames() {
        return _classes != null ? _classes : Collections.<String> emptyList();
    }

    @Override
    @Trivial
    public List<String> getMappingFileNames() {
        return _mappingFileNames != null ? _mappingFileNames : Collections.<String> emptyList();
    }

    @Override
    @Trivial
    public ClassLoader getNewTempClassLoader() {
        // Shouldn't happen
        throw new UnsupportedOperationException();
    }

    @Override
    @Trivial
    public DataSource getNonJtaDataSource() {
        return _nonJtaDataSource;
    }

    @Override
    @Trivial
    public String getPersistenceProviderClassName() {
        return PersistenceProvider.class.getName();
    }

    @Override
    @Trivial
    public String getPersistenceUnitName() {
        return _name;
    }

    @Override
    @Trivial
    public URL getPersistenceUnitRootUrl() {
        return _puRoot;
    }

    @Override
    @Trivial
    public String getPersistenceXMLSchemaVersion() {
        return "2.1";
    }

    @Override
    @Trivial
    public Properties getProperties() {
        return _props != null ? _props : new Properties();
    }

    @Override
    @Trivial
    public SharedCacheMode getSharedCacheMode() {
        return SharedCacheMode.NONE;
    }

    @Override
    @Trivial
    public PersistenceUnitTransactionType getTransactionType() {
        return PersistenceUnitTransactionType.JTA;
    }

    @Override
    @Trivial
    public ValidationMode getValidationMode() {
        return ValidationMode.NONE;
    }

    /**
     * Cleanup resources. Need to be sure to clean up InMemoryUrlStreamHandler as that could easily
     * leak.
     */
    public void close() {
        _inMemHandler.deregister(_inMemoryMappingFileURLs);
    }

    /**
     * Creates a copy of this PUInfoImpl object, with a new name. The copy is intended to be used
     * by the PersistenceProvider to generate schema. The name of the PUInfoImpl will differ from
     * the name of this PUInfoImpl. The PersistenceProvider appears to be ignoring repeated
     * requests for schema with the same name.
     *
     * @return A copy of this PUInfoImpl with a modified name.
     */
    PersistenceUnitInfo createCopyWithNewName() {
        return new PUInfoImpl(this);
    }

    /**
     * Registers all mapping files with : ResourceResolver and InMemoryURLStreamHandler
     *
     * Returns a List containing the names of all InMemoryMappingFiles.
     */
    @Trivial
    private List<String> processInMemoryMappingFiles(List<InMemoryMappingFile> immfs) {
        List<String> res = new ArrayList<String>();
        for (InMemoryMappingFile immf : immfs) {
            // Register the file with our ClassLoader
            URL url = _appLoader.registerInMemoryResource(immf);
            // Register the <URL -> File> mapping with the handler so it can return a stream
            // when requested to do so.
            _inMemHandler.register(url, immf);
            // Save a reference to the URL so we can cleanup later.
            _inMemoryMappingFileURLs.add(url);
            res.add(immf.getName());
        }

        return res;
    }
}
