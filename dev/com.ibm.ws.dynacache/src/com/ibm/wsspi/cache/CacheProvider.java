/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.cache;

/**
 * <p> Dynacache is the default cache provider for the WebSphere Application Server.
 * This interface is used to provide an alternate cache provider for a cache instance,
 * and provides the methods necessary to properly interact with the WebSpehre Application Server.
 * 
 * <p>The same cache provider must be configured for a cache instance across all the members of a cluster in
 * a ND environment. Each cache instance must be configured with a cache provider. If a provider is not
 * explicitly specified using the techniques described below, the default Dynacache cache provider is used.
 * 
 * <p>There are two ways of packaging a cache provider to make it available to the
 * WebSphere Application Server:
 * 
 * <p> @since WAS 7.0.0.0
 * <b>OSGI Bundle</b> <br>
 * A bundle is a software module packaged as a JAR. The bundle adheres to the JAR specification,
 * but also contains additional metadata as defined by the OSGi specification.
 * Among other things, a bundle's manifest contains lists of packages to be exported from the bundle
 * and imported into the bundle. For further details, refer to http://www.osgi.org.
 * <p>
 * The cache provider bundle should define an extension for the com.ibm.wsspi.extension.cache-provider
 * extension point. The plugin.xml of the cache provider bundle must contain a stanza similar to:<br>
 * <code>&lt;extension id="cache-provider" point="com.ibm.wsspi.extension.cache-provider"&gt;<br>
 * &lt;cache-provider name="ObjectGrid" class="com.companyx.CacheProviderImpl" /&gt;
 * <br>&lt;/extension&gt;</code><br>
 * The specified class must implement this interface.
 * <p>
 * At startup, the <code>com.ibm.wsspi.extension.cache-provider</code> extension point will be processed, and
 * <code>org.eclipse.core.runtime.IConfigurationElement.createExecutableExtension(String propertyName)</code>
 * will be used to create an instance of the cache provider.
 * <b>NOTE:</b> The name attribute of the cache-provider extension point should match the name of the
 * cache provider configured via the admin-console or via wsadmin scripting.
 * 
 * <p> @since WAS 6.1.0.27
 * <b>Jar File</b><br>
 * An alternative means of configuring the {@link CacheProvider} is via the WAS ExtensionClassloader.
 * The cache provider implementation and its dependencies must be packaged as a jar file placed in
 * the $WAS_INSTALL_ROOT\lib directory. The implementation of the CacheProvider will be loaded
 * reflectively using the cache provider name and its implementation class name. The cache provider name
 * and the fully qualified implementation class to be used for a cache instance can be specified using
 * the admin console, cacheinstances.properties file or {@link DistributedObjectCacheFactory} Custom property/cache-instance property name:
 * com.ibm.ws.cache.CacheConfig.cacheProviderName
 * Custom property/cache-instance property value: com.ibm.ws.objectgrid.dynacache.CacheProviderImpl
 * 
 * <p>
 * After obtaining an instance of the CacheProvider via a bundle or jar, {@link CacheProvider#createCache(CacheConfig)} will be called to instantiate and obtain a reference to
 * {@link CoreCache}, which implements the core functionality of the cache.
 * <p>
 * A cache provider will also need to implement/extend the classes {@link CacheProvider}, {@link CacheEntry}, {@link CoreCache}, {@link CacheStatistics}, and
 * {@link CacheFeatureSupport} to create a complete solution.
 * 
 * @ibm-spi
 */
public interface CacheProvider {

    /**
     * Returns the CoreCache object which is created by cache provider according to
     * the cache config settings. If the CoreCache has already
     * been created it returns the previously created instance of the {@link CoreCache}.
     * 
     * @param cacheConfig The configuration of the cache instance
     * 
     * @return The object that implements the CoreCache interface
     *         return null if there were any errors or exceptions when creating the cache.
     *         If null is returned Dynacache will use the default Dynacache provider for creating the
     *         cache instance.
     */
    public CoreCache createCache(CacheConfig cacheConfig);

    /**
     * Returns the name of the cache provider.
     * <p><b>NOTE:</b>The name returned by this method should match the name configured
     * in the cache-provider plugin extension and the cache provider name attribute
     * in the Dynacache configuration specified in the server.xml
     * 
     * @return The name of the Cache Provider
     */
    public String getName();

    /**
     * Provides the Dynacache features supported by the {@link CacheProvider}.
     * The features supported determines the methods of {@link CoreCache} that will be called.
     * 
     * @return The {@link CacheFeatureSupport} object.
     */
    public CacheFeatureSupport getCacheFeatureSupport();

    /**
     * This method is called before any caches are created.
     * All initialization of the cache provider internal implementation
     * must occur here. Throw relevant exceptions if there are any errors during {@link CacheProvider} startup.
     * Dynacache cannot recover from these fatal errors and will let cache creation fail if errors or exceptions are thrown during
     * cache provider startup.
     */
    public void start();

    /**
     * This method is called when the cache provder is no longer being used. All
     * resource cleanup must occur in this method.
     * Throw relevant exceptions if there are any errors when stopping the {@link CacheProvider}.
     */
    public void stop();
}