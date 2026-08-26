/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.internal.cache;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.IndexWriter;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.microprofile.openapi20.internal.utils.LoggingUtils;
import io.openliberty.microprofile.openapi20.internal.utils.MessageConstants;
import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.runtime.OpenApiProcessor;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.Format;
import io.smallrye.openapi.runtime.io.OpenApiSerializer;

/**
 * Responsible for collecting and writing an OpenAPI cache entry
 * <p>
 * The cache key comprises of:
 * <ul>
 * <li>the application name</li>
 * <li>the files scanned for annotations</li>
 * <li>the OpenAPI configuration</li>
 * </ul>
 * The cached value is the OpenAPI model constructed from the application and optionally a Jandex index of the scanned classes.
 * <p>
 * To cache a model:
 * <ul>
 * <li>Create a CacheEntry with {@link #createNew(String, Path)}</li>
 * <li>Add the model, the {@link OpenApiConfig} and all files used to generate the model</li>
 * <li>Optionally add a Jandex index of the scanned classes</li>
 * <li>Call {@link #write()}</li>
 * </ul>
 * <p>
 * To find a model from the cache for a given application:
 * <ul>
 * <li>Read the entry from the cache with {@link #read(String, Path)}</li>
 * <li>Create a new CacheEntry to represent the current application. Add the current {@link OpenApiConfig} and all files which would be used, but don't add a model.</li>
 * <li>Call {@link #modelUpToDate(CacheEntry)} to check whether the cached model can be used in the current environment.</li>
 * <li>Call {@link #indexUpToDate(CacheEntry)} to check whether the cached Jandex index can be used in the current environment.</li>
 * </ul>
 */
public class CacheEntry {

    private static final TraceComponent tc = Tr.register(CacheEntry.class);

    private static final String CACHE_DIR = "cache";
    private static final String MODEL_FILE = "model";
    private static final String CONFIG_FILE = "config";
    private static final String FILES_LIST_FILE = "files";
    private static final String INDEX_FILE = "index";

    private String appName;
    private final Path cacheDir;
    private final ConfigSerializer configSerializer;
    private Index index;
    private OpenAPI model;
    private OpenApiConfig config;
    private Properties configProperties;
    private List<String> fileEntries = new ArrayList<>();

    /**
     * Create a new empty cache entry
     * <p>
     * This entry can then be populated with {@link #setModel(OpenAPI)}, {@link #setConfig(OpenApiConfig)} and {@link #addDependentFile(Path)} and stored with {@link #write()}.
     *
     * @param applicationName the name of the application
     * @param baseDir the cache directory
     * @param configSerializer the config serializer
     * @return the new CacheEntry
     */
    public static CacheEntry createNew(String applicationName, Path baseDir, ConfigSerializer configSerializer) {
        return new CacheEntry(applicationName, baseDir, configSerializer);
    }

    /**
     * Load a cache entry for an application, if it exists.
     * <p>
     * This entry can then be compared with an entry for the current application using {@link #isUpToDateWith(CacheEntry)} and if it is up to date, the cached model can be
     * retrieved with {@link #getModel()}.
     *
     * @param applicationName the application name
     * @param baseDir the cache directory
     * @param configSerializer the config serializer
     * @return the loaded cache entry, or {@code null} if a valid cache entry does not exist for this application name
     */
    public static CacheEntry read(String applicationName, Path baseDir, ConfigSerializer configSerializer) {
        CacheEntry cacheEntry = new CacheEntry(applicationName, baseDir, configSerializer);
        if (!Files.isDirectory(cacheEntry.cacheDir)) {
            return null;
        }

        try {
            if (!cacheEntry.read()) {
                return null;
            }
        } catch (IOException e) {
            Tr.warning(tc, MessageConstants.OPENAPI_CACHE_READ_ERROR_CWWKO1688W, applicationName, e.toString());
            return null;
        }

        return cacheEntry;
    }

    private CacheEntry(String applicationName, Path baseDir, ConfigSerializer configSerializer) {
        try {
            this.appName = URLEncoder.encode(applicationName, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            // Won't happen
            throw new RuntimeException("Unable to use UTF-8 encoding", e);
        }
        this.cacheDir = baseDir.resolve(CACHE_DIR).resolve(this.appName);
        this.configSerializer = configSerializer;
    }

    /**
     * Add a file which was used to generate the OpenAPI model
     * <p>
     * This method must be called for every <i>on-disk</i> file used to generate the OpenAPI model.
     * <p>
     * Where files within archives are used to generate the model (e.g. library jars within a war file, or a war file within an ear file), only the enclosing archive needs to added
     * using this method.
     *
     * @param file the file
     * @throws IOException if the file's size or last modified time can't be read
     */
    public void addDependentFile(Path file) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(file.toAbsolutePath().toString());
        sb.append(",");
        sb.append(Files.size(file));
        sb.append(",");
        sb.append(Files.getLastModifiedTime(file).toMillis());
        fileEntries.add(sb.toString());
    }

    /**
     * Set the config for the cache key
     *
     * @param config the config
     */
    public void setConfig(OpenApiConfig config) {
        this.config = config;
        this.configProperties = null;
    }

    /**
     * Set the model to be cached
     *
     * @param model the model
     */
    public void setModel(OpenAPI model) {
        this.model = model;
    }

    /**
     * Get the model stored in this CacheEntry
     *
     * @return the model
     */
    public OpenAPI getModel() {
        return model;
    }

    /**
     * Get the Jandex index stored in this CacheEntry.
     * <p>
     * In some cases, a cache entry may not have an index if the user has disabled class scanning but in these cases {@link #indexUpToDate(CacheEntry)} will return {@code false}.
     *
     * @return the Jandex index, never {@code null} unless {@link #indexUpToDate(CacheEntry)} is {@code false}
     */
    public Index getIndex() {
        return index;
    }

    /**
     * Set the Jandex index to be cached.
     * <p>
     * In some cases, a cache entry may not have an index if the user has disabled class scanning.
     *
     * @param index the Jandex index, may be {@code null} to not store an index in the cache entry
     */
    public void setIndex(Index index) {
        this.index = index;
    }

    /**
     * Check whether the model from this cache entry can be used, based on the current state of the application and config.
     * <p>
     * This method may only be called on a cache entry read from disk with {@link #read(String, Path)}.
     *
     * @param current a CacheEntry representing the current state. It must have the config set and all dependent files added before this method is called.
     * @return {@code true} if this cache entry is up to date, {@code false} otherwise
     */
    @Trivial // method has sufficient tracing
    public boolean modelUpToDate(CacheEntry current) {
        if (model == null || configProperties == null) {
            throw new IllegalStateException("isUpToDateWith called on CacheEntry not read from disk");
        }
        Objects.requireNonNull(current, "cache entry for current state must be given");

        // verify the app name
        if (!Objects.equals(appName, current.appName)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Cache out of date because app name is not the same?!", appName, current.appName);
            }
            return false;
        }

        // Check if reader or filter configured
        // If there is user code that is run to build the model it could read additional config so we can't cache the model
        if (current.config.modelReader() != null || current.config.filter() != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Cache not usable because a filter or reader is registered");
            }
            return false;
        }

        // compare config
        // Note: old cached model is used here to provide a list of paths and operations
        // which is needed to get the full list of config properties for the current environment
        Properties currentConfigProperties = current.getConfigProperties(model);
        if (!Objects.equals(configProperties, currentConfigProperties)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Cache out of date because config is not the same", configProperties, currentConfigProperties);
            }
            return false;
        }

        // compare scanned files
        if (!Objects.equals(fileEntries, current.fileEntries)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Cache out of date because files have changed", fileEntries, current.fileEntries);
            }
            return false;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Cache is up to date");
        }

        return true;
    }

    /**
     * Check whether the Jandex index from this cache entry can be used, based on the current state of the application and config. The requirements for using the index are lower
     * than the requirements for using the model and building the index is usually the slowest part of building the model.
     * <p>
     * This method may only be called on a cache entry read from disk with {@link #read(String, Path)}.
     *
     * @param current a CacheEntry representing the current state. It must have the config set and all dependent files added before this method is called.
     * @return {@code true} if this cache entry is up to date, {@code false} otherwise
     */
    @Trivial // method has sufficient tracing
    public boolean indexUpToDate(CacheEntry current) {
        if (model == null || configProperties == null) {
            throw new IllegalStateException("isUpToDateWith called on CacheEntry not read from disk");
        }
        Objects.requireNonNull(current, "cache entry for current state must be given");

        // check we have an index cached
        if (index == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Index out of date because no index in cache");
            }
            return false;
        }

        // verify the app name
        if (!Objects.equals(appName, current.appName)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Index out of date because app name is not the same?!", appName, current.appName);
            }
            return false;
        }

        // compare scanning config
        Properties currentScanningProperties = current.getScanningConfig();
        Properties ourScanningProperties = getScanningConfig();
        if (!Objects.equals(ourScanningProperties, currentScanningProperties)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Index out of date because indexing config is not the same", ourScanningProperties, currentScanningProperties);
            }
            return false;
        }

        // compare scanned files
        if (!Objects.equals(fileEntries, current.fileEntries)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Index out of date because files have changed", fileEntries, current.fileEntries);
            }
            return false;
        }

        return true;
    }

    private Properties getConfigProperties(OpenAPI model) {
        if (configProperties != null) {
            return configProperties;
        } else if (config != null) {
            return configSerializer.serializeConfig(config, model);
        } else {
            return null;
        }
    }

    private Properties getScanningConfig() {
        if (configProperties != null) {
            Set<String> scanningKeys = configSerializer.getIndexingConfigKeys();
            Properties result = new Properties();
            for (Entry<Object, Object> entry : configProperties.entrySet()) {
                if (scanningKeys.contains(entry.getKey())) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
            return result;
        } else if (config != null) {
            return configSerializer.getIndexingConfig(config);
        } else {
            return null;
        }
    }

    public void write() throws IOException {
        // check that everything required for a cache entry has been provided
        if (appName == null || model == null || (configProperties == null && config == null) || fileEntries.isEmpty()) {
            if (LoggingUtils.isDebugEnabled(tc)) {
                if (appName == null)
                    Tr.debug(this, tc, "Not writing cache entry, AppName is null");
                if (model == null)
                    Tr.debug(this, tc, "Not writing cache entry, model is null");
                if (configProperties == null && config == null)
                    Tr.debug(this, tc, "Not writing cache entry, config is null");
                if (fileEntries.isEmpty())
                    Tr.debug(this, tc, "Not writing cache entry, fileEntries is empty");
            }
            return;
        }

        if (LoggingUtils.isDebugEnabled(tc)) {
            Tr.debug(this, tc, "Writing cache entry");
        }

        try {
            // delete cache directory if present
            if (Files.exists(cacheDir)) {
                if (Files.isDirectory(cacheDir)) {
                    Files.walkFileTree(cacheDir, RECURSIVE_DELETER);
                } else {
                    throw new IOException("Non-directory found in cache location: " + cacheDir.toAbsolutePath());
                }
            }

            // create cache directory
            Files.createDirectories(cacheDir);

            // write list of dependent file data
            writeFileList(cacheDir.resolve(FILES_LIST_FILE));

            // serialize config
            writeConfig(cacheDir.resolve(CONFIG_FILE));

            // serialize model
            writeModel(cacheDir.resolve(MODEL_FILE));

            if (index != null) {
                writeIndex(cacheDir.resolve(INDEX_FILE));
            }

            if (LoggingUtils.isDebugEnabled(tc)) {
                Tr.debug(this, tc, "Cache entry written");
            }
        } catch (Exception e) {
            // warn upon unexpected failure
            Tr.warning(tc, MessageConstants.OPENAPI_CACHE_WRITE_ERROR_CWWKO1689W, appName, e.toString());
        }
    }

    private boolean read() throws IOException {
        Path modelFile = cacheDir.resolve(MODEL_FILE);
        Path configFile = cacheDir.resolve(CONFIG_FILE);
        Path filesListFile = cacheDir.resolve(FILES_LIST_FILE);
        Path indexFile = cacheDir.resolve(INDEX_FILE);

        if (!Files.exists(modelFile) || !Files.exists(configFile) || !Files.exists(filesListFile)) {
            return false;
        }

        readModel(modelFile);
        readConfig(configFile);
        readFileList(filesListFile);

        if (Files.exists(indexFile)) {
            readIndex(indexFile);
        }

        return true;
    }

    private void readModel(Path input) throws IOException {
        try (InputStream is = new FileInputStream(input.toFile())) {
            OpenApiStaticFile openApiFile = new OpenApiStaticFile(is, Format.JSON);
            model = OpenApiProcessor.modelFromStaticFile(openApiFile);
        }
    }

    private void writeModel(Path output) throws IOException {
        String jsonModel = OpenApiSerializer.serialize(model, Format.JSON);
        Files.write(output, jsonModel.getBytes(StandardCharsets.UTF_8));
    }

    private void readConfig(Path input) throws IOException {
        Properties result = new Properties();
        try (InputStream is = Files.newInputStream(input)) {
            result.load(is);
        }
        configProperties = result;
    }

    private void writeConfig(Path output) throws IOException {
        try (OutputStream os = Files.newOutputStream(output)) {
            getConfigProperties(model).store(os, "");
        }
    }

    private void readFileList(Path input) throws IOException {
        fileEntries = Files.readAllLines(input, StandardCharsets.UTF_8);
    }

    private void writeFileList(Path output) throws IOException {
        Files.write(output, fileEntries, StandardCharsets.UTF_8);
    }

    private void readIndex(Path input) throws IOException {
        try (InputStream in = Files.newInputStream(input)) {
            IndexReader reader = new IndexReader(in);
            index = reader.read();
        }
    }

    private void writeIndex(Path output) throws IOException {
        try (OutputStream out = Files.newOutputStream(output)) {
            IndexWriter writer = new IndexWriter(out);
            writer.write(index);
        }
    }

    private static final FileVisitor<Path> RECURSIVE_DELETER = new SimpleFileVisitor<Path>() {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }

    };

}
