/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.wsspi.persistence.internal;

import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.hibernate.jpa.HibernatePersistenceProvider;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.persistence.InMemoryMappingFile;
import com.ibm.wsspi.persistence.PersistenceServiceUnitConfig;
import com.ibm.wsspi.persistence.internal.jta.LibertyJtaPlatformJakarta;
import com.ibm.wsspi.persistence.internal.util.DoPrivHelper;
import com.ibm.wsspi.persistence.internal.util.PersistenceClassLoader;

/**
 * Programmatic implementation of {@link PersistenceUnitInfo} for Hibernate 8.x.
 * Eliminates the need for a persistence.xml on disk.
 * Passed directly to HibernatePersistenceProvider.createContainerEntityManagerFactory().
 */
public final class HibernatePUInfoImpl implements PersistenceUnitInfo {

    private static final String SCHEMA_GENERATION_DATABASE_ACTION =
        "jakarta.persistence.schema-generation.database.action";
    private static final String SCHEMA_GENERATION_CREATE_ACTION = "create";

    /** Hibernate built-in Liberty JTA platform class name (available since Hibernate 5.2.13) */
    private static final String WEBSPHERE_LIBERTY_JTA_PLATFORM =
        "org.hibernate.engine.transaction.jta.platform.internal.WebSphereLibertyJtaPlatform";
    /** Hibernate JtaPlatform interface */
    private static final String JTA_PLATFORM_INTERFACE =
        "org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform";

    private final DataSource _jtaDataSource;
    private final DataSource _nonJtaDataSource;
    private final List<String> _classes;
    private final List<String> _mappingFileNames;
    private final List<URL> _inMemoryMappingFileURLs;
    private final Properties _props;
    private final String _name;
    private static final AtomicInteger _nameGenerator = new AtomicInteger(0);

    private final PersistenceClassLoader _appLoader;
    private final InMemoryUrlStreamHandler _inMemHandler;
    private final URL _puRoot;

    HibernatePUInfoImpl(PersistenceServiceUnitConfig conf, InMemoryUrlStreamHandler inMemHandler, URL bundleRoot) {
        _inMemHandler = inMemHandler;
        _puRoot = bundleRoot;

        _jtaDataSource = conf.getJtaDataSource();
        _nonJtaDataSource = conf.getNonJaDataSource();
        _classes = new ArrayList<>(conf.getClasses());
        _name = "persistence_service_pu-" + _nameGenerator.incrementAndGet();
        _inMemoryMappingFileURLs = new ArrayList<>();
        _mappingFileNames = new ArrayList<>(conf.getMappingFileNames());

        /**
         * Build a composite classloader with Hibernate's bundle as the parent.
         *
         * Parent: HibernatePersistenceProvider classloader (thirdparty bundle).
         *   → owns all Hibernate internals: ANTLR, ByteBuddy stubs, jboss-logging, etc.
         *   → class lookups for Hibernate-internal types resolve here first, avoiding
         *     NoClassDefFoundError caused by type-identity mismatches across OSGi bundles.
         *
         * Delegates (tried in findClass() after parent fails):
         *   [0] consumer bundle classloader — loads entity classes (e.g. Person)
         *   [1] service bundle classloader  — loads PersistenceClassLoader itself, JTA helpers
         *
         * This is the inverse of the previous layout where consumer was parent.
         * Consumer-as-parent caused ANTLR and other Hibernate-internal classes to be
         * resolved through the wrong classloader path, producing NoClassDefFoundError.
         */
        _appLoader = DoPrivHelper.newPersistenceClassLoader(
                HibernatePersistenceProvider.class.getClassLoader(),  // parent = thirdparty bundle
                conf.getConsumerLoader(),                              // delegate[0] = entity classes
                HibernatePUInfoImpl.class.getClassLoader());           // delegate[1] = service bundle

        // Register file-based mapping files with the classloader
        for (String file : _mappingFileNames) {
            _appLoader.registerFileResource(file);
        }

        // Register in-memory mapping files (Jakarta Data ORM path)
        List<InMemoryMappingFile> inMemoryMappingFiles = copyInMemoryMappingFiles(conf.getInMemoryMappingFiles());
        List<String> inMemNames = processInMemoryMappingFiles(inMemoryMappingFiles);
        _mappingFileNames.addAll(inMemNames);

        _props = new Properties();

        // Auto table creation if configured
        if (conf.getCreateOrUpdateTables()) {
            _props.put(SCHEMA_GENERATION_DATABASE_ACTION, SCHEMA_GENERATION_CREATE_ACTION);
        }

        // Disable bytecode enhancement — Liberty's internal entities use static weaving.
        // ByteBuddy SPI discovery is suppressed via LibertyClassLoaderService; these
        // properties additionally ensure no enhancement is attempted at the field level.
        _props.put("hibernate.enhancer.enableLazyInitialization", "false");
        _props.put("hibernate.enhancer.enableDirtyTracking", "false");
        _props.put("hibernate.enhancer.enableAssociationManagement", "false");

        // Hibernate 8 generates property accessor classes into the entity's classloader by
        // default. Entity classloaders in Liberty (consumer bundles, applications) cannot see
        // org.hibernate.accessor.*, so use plain reflection instead.
        _props.put("hibernate.accessor.strategy", "reflection");

        // Wire Liberty JTA — use the composite loader so Hibernate's own packages are visible.
        // conf.getConsumerLoader() only sees consumer classes, not org.hibernate.* internals.
        configureJtaPlatform(_appLoader);

        // Consumer-supplied properties (may override any of the above)
        _props.putAll(conf.getProperties());
    }

    /**
     * Wires Hibernate's JTA platform.
     * If Hibernate 8.x ships WebSphereLibertyJtaPlatform (added in 5.2.13), use it by name.
     * Otherwise create a dynamic proxy using LibertyJtaPlatformJakarta.
     */
    private void configureJtaPlatform(ClassLoader loader) {
        // Check for Hibernate's built-in Liberty integration
        try {
            loader.loadClass(WEBSPHERE_LIBERTY_JTA_PLATFORM);
            // Found — tell Hibernate to use it by class name
            _props.put("hibernate.transaction.jta.platform", WEBSPHERE_LIBERTY_JTA_PLATFORM);
            return;
        } catch (ClassNotFoundException ignored) {
            // Built-in not available — fall through to proxy
        }

        // Create a dynamic proxy implementing Hibernate's JtaPlatform interface
        try {
            Class<?> jtaPlatformInterface = loader.loadClass(JTA_PLATFORM_INTERFACE);
            Object proxy = Proxy.newProxyInstance(
                loader,
                new Class<?>[] { jtaPlatformInterface },
                new LibertyJtaPlatformJakarta());
            _props.put("hibernate.transaction.jta.platform", proxy);
        } catch (ClassNotFoundException e) {
            // Hibernate not on classpath — will fail at EMF creation time with a clear error
            throw new RuntimeException("Cannot locate Hibernate JtaPlatform interface. "
                + "Ensure Hibernate 8.x is on the classpath.", e);
        }
    }

    /**
     * Copy constructor used by createCopyWithNewName() for schema generation.
     * EclipseLink had a bug where it ignored repeated schema-gen with same PU name.
     * Hibernate does not have this bug, but the copy is kept for safety and consistency.
     */
    private HibernatePUInfoImpl(HibernatePUInfoImpl source) {
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

    /**
     * Private copy of in-memory mapping files, assigning new IDs while sharing the byte arrays.
     */
    private List<InMemoryMappingFile> copyInMemoryMappingFiles(List<InMemoryMappingFile> copyIMMF) {
        List<InMemoryMappingFile> immf = new ArrayList<>();
        for (InMemoryMappingFile file : copyIMMF) {
            String name = file.getName();
            if (name.startsWith("mappingfile-")) {
                immf.add(new InMemoryMappingFile(file.getMappingFile()));
            } else {
                immf.add(new InMemoryMappingFile(file.getMappingFile(), name));
            }
        }
        return immf;
    }

    /**
     * Registers in-memory mapping files with the classloader and URL handler.
     * Returns the list of resource names for "mappingfile-" prefixed entries
     * (those are ORM XML files; non-prefixed entries are generated entity classes).
     */
    @Trivial
    private List<String> processInMemoryMappingFiles(List<InMemoryMappingFile> immfs) {
        List<String> res = new ArrayList<>();
        for (InMemoryMappingFile immf : immfs) {
            URL url = _appLoader.registerInMemoryResource(immf);
            _inMemHandler.register(url, immf);
            _inMemoryMappingFileURLs.add(url);
            String resourceName = immf.getName();
            if (resourceName.startsWith("mappingfile-")) {
                res.add(immf.getName());
            }
        }
        return res;
    }

    /**
     * Creates a copy of this PUInfoImpl with a new name.
     * Used by HibernateSchemaManager to drive schema generation.
     */
    PersistenceUnitInfo createCopyWithNewName() {
        return new HibernatePUInfoImpl(this);
    }

    /**
     * Cleanup resources. Deregisters in-memory URL mappings to prevent leaks.
     */
    public void close() {
        _inMemHandler.deregister(_inMemoryMappingFileURLs);
    }

    // ---- PersistenceUnitInfo implementation ----

    @Override
    public void addTransformer(ClassTransformer arg0) {
        // noop — static weaving only
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
        return List.of();
    }

    @Override
    @Trivial
    public DataSource getJtaDataSource() {
        return _jtaDataSource;
    }

    @Override
    @Trivial
    public List<String> getManagedClassNames() {
        return _classes != null ? _classes : List.of();
    }

    @Override
    @Trivial
    public List<String> getMappingFileNames() {
        return _mappingFileNames != null ? _mappingFileNames : List.of();
    }

    @Override
    @Trivial
    public ClassLoader getNewTempClassLoader() {
        // Return null — no temporary classloader; Hibernate will use the app classloader.
        // This is acceptable since Liberty uses static weaving, not load-time weaving.
        return null;
    }

    @Override
    @Trivial
    public DataSource getNonJtaDataSource() {
        return _nonJtaDataSource;
    }

    @Override
    @Trivial
    public String getPersistenceProviderClassName() {
        return HibernatePersistenceProvider.class.getName();
    }

    @Override
    @Trivial
    public List<String> getQualifierAnnotationNames() {
        return List.of();
    }

    @Override
    @Trivial
    public String getScopeAnnotationName() {
        return null;
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
        return "4.0";
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
     * New in JPA 4.0: returns all class names known to this persistence unit,
     * including managed classes from mapped superclasses and embeddables.
     * For Liberty's internal persistence service units the managed class list
     * is fixed at creation time, so this is the same as getManagedClassNames().
     */
    @Override
    @Trivial
    public List<String> getAllClassNames() {
        return getManagedClassNames();
    }

    /**
     * New in Jakarta Persistence 4.0. Liberty's internal persistence units do not use
     * package-info or module-info descriptors, so all four package/module lists are empty.
     */
    @Override
    @Trivial
    public List<String> getManagedPackageDescriptors() {
        return List.of();
    }

    @Override
    @Trivial
    public List<String> getManagedModuleDescriptors() {
        return List.of();
    }

    @Override
    @Trivial
    public List<String> getAllPackageDescriptors() {
        return List.of();
    }

    @Override
    @Trivial
    public List<String> getAllModuleDescriptors() {
        return List.of();
    }

    /**
     * New in JPA 4.0 / Jakarta Persistence 4.0.
     * Returns the default fetch type for to-one associations (@ManyToOne, @OneToOne).
     * FetchType.EAGER matches the JPA spec default.
     */
    @Override
    @Trivial
    public jakarta.persistence.FetchType getDefaultToOneFetchType() {
        return jakarta.persistence.FetchType.EAGER;
    }
}
