/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jcache.internal;

import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.serialization.DeserializationContext;
import com.ibm.ws.serialization.DeserializationObjectResolver;
import com.ibm.ws.serialization.SerializationService;
import com.ibm.wsspi.library.Library;

import io.openliberty.jcache.JCacheService;

@Component(immediate = true, configurationPolicy = REQUIRE, configurationPid = "io.openliberty.jcache", property = {
		"library.target=(id=unbound)", "service.vendor=IBM" })
public class JCacheServiceImpl implements JCacheService {

	/**
	 * Register the class to trace service.
	 */
	private final static TraceComponent tc = Tr.register(JCacheServiceImpl.class);

	private Library library;
	private Map<String, Object> configurationProperties;
	private CachingProvider cachingProvider = null;
	private CacheManager cacheManager = null;
	private Cache<Object, byte[]> cache = null;
	private String cacheName = null;
	private Properties vendorProperties = null;
	private URI configuredURI = null;
	private ClassLoader classLoader = null;
	private SerializationService serializationService = null;
	private String cachingProviderClass = null;

	private static final String BASE_PREFIX = "properties";
	private static final int BASE_PREFIX_LENGTH = BASE_PREFIX.length();
	private static final int TOTAL_PREFIX_LENGTH = BASE_PREFIX_LENGTH + 3; // 3 is the length of .0.

	private static final String KEY_NAME = "name";
	private static final String KEY_URI = "uri";
	private static final String KEY_CACHING_PROVIDER = "cachingProvider";

	@Activate
	public void activate(Map<String, Object> properties) throws Exception {
		configurationProperties = new HashMap<String, Object>(properties);

		/*
		 * Get the cache name.
		 */
		cacheName = (String) configurationProperties.get(KEY_NAME);
		cachingProviderClass = (String) configurationProperties.get(KEY_CACHING_PROVIDER);

		/*
		 * Get the URI.
		 */
		String uriValue = (String) configurationProperties.get(KEY_URI);
		if (uriValue != null)
			try {
				configuredURI = new URI(uriValue);
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException(Tr.formatMessage(tc, "INCORRECT_URI_SYNTAX", e), e);
			}
		else {
			configuredURI = null;
		}

		/*
		 * Get the configured vendor properties.
		 */
		vendorProperties = new Properties();
		for (Map.Entry<String, Object> entry : configurationProperties.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			// properties start with properties.0.
			if (key.length() > TOTAL_PREFIX_LENGTH && key.charAt(BASE_PREFIX_LENGTH) == '.'
					&& key.startsWith(BASE_PREFIX)) {
				key = key.substring(TOTAL_PREFIX_LENGTH);
				if (!key.equals("config.referenceType"))
					vendorProperties.setProperty(key, (String) value);
			}
		}

	}

	@Deactivate
	public void deactivate(int reason) {
		if (cachingProvider != null) {
			AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
				cachingProvider.close();
				return null;
			});

			cachingProvider = null;
			cacheManager = null;
			classLoader = null;
		}
	}

	@Modified
	public void modified(Map<String, Object> properties) {
		// TODO
	}

	@Reference(name = "library", cardinality = ReferenceCardinality.MANDATORY, policyOption = ReferencePolicyOption.GREEDY)
	public void setLibrary(Library library) {
		this.library = library;
	}

	public void unsetLibrary(Library library) {
		this.library = null;
	}

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	public void setSerializationService(SerializationService serializationService) {
		this.serializationService = serializationService;
	}

	public void unsetLibrary(SerializationService serializationService) {
		this.serializationService = null;
	}

	@Override
	public Cache<Object, byte[]> getCache() {

		return AccessController.doPrivileged((PrivilegedAction<Cache<Object, byte[]>>) () -> {
			if (cacheManager == null) {
				classLoader = new JCacheServiceClassLoader(library.getClassLoader());

				/*
				 * load JCache provider from configured library, which is either specified as a
				 * libraryRef or via a bell.
				 * 
				 * No doPriv due to limitations in OSGi and security manager. If running with
				 * SecurityManager, permissions will need to be granted explicitly.
				 */
				try {
					if (cachingProviderClass != null && !cachingProviderClass.isEmpty()) {
						cachingProvider = Caching.getCachingProvider(cachingProviderClass, classLoader);
					} else {
						cachingProvider = Caching.getCachingProvider(classLoader);
					}
					if (TraceComponent.isAnyTracingEnabled() && tc.isInfoEnabled()) {
						Tr.info(tc, "JCCHE0003I", cacheName, cachingProvider.getClass().getName());
					}
				} catch (CacheException e) {
					Tr.error(tc, "JCCHE0004E", e);
					throw e;
				}

				// TODO Much more configuration in CacheStoreService. Appears to be Infinispan
				// support.

				/*
				 * Catch incorrectly formatted httpSessionCache uri values from server.xml file
				 */
				try {
					cacheManager = cachingProvider.getCacheManager(configuredURI, classLoader, vendorProperties);
				} catch (NullPointerException e) {
					throw new IllegalArgumentException(Tr.formatMessage(tc, "INCORRECT_URI_SYNTAX", e), e);
				}

				/*
				 * For trace, check which caches are available.
				 */
				if (TraceComponent.isAnyTracingEnabled() && tc.isInfoEnabled()) {
					List<String> cacheNames = new ArrayList<String>();
					for (String name : cacheManager.getCacheNames()) {
						cacheNames.add(name);
					}
					Tr.debug(tc, "Found the following existing caches: ", cacheNames);
				}

				/*
				 * Get an existing cache. If it doesn't exist, create it.
				 */
				try {
					cache = cacheManager.getCache(cacheName, Object.class, byte[].class);
				} catch (Exception e) {
					// TODO classcastexception if hazelcast key / value types and they don't match
					// TODO Error here? Should we try to still create it?
					Tr.error(tc, "Error getting cache: " + e, e);
				}
				if (cache == null) {
					MutableConfiguration<Object, byte[]> config = new MutableConfiguration<Object, byte[]>();
					config.setTypes(Object.class, byte[].class);
					cache = cacheManager.createCache(cacheName, config);

					if (TraceComponent.isAnyTracingEnabled() && tc.isInfoEnabled()) {
						Tr.info(tc, "JCCHE0001I", cacheName);
					}
				} else {
					if (TraceComponent.isAnyTracingEnabled() && tc.isInfoEnabled()) {
						Tr.info(tc, "JCCHE0002I", cacheName);
					}
				}
			}

			return this.cache;
		});
	}

	@Override
	public Object deserialize(byte[] bytes) {
		if (bytes == null)
			return null;

		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
			// TODO Might need to use Library class loader.
			ObjectInputStream ois = serializationService.createObjectInputStream(bais,
					this.getClass().getClassLoader());
			return ois.readObject();
		} catch (ClassNotFoundException e) {
			Tr.error(tc, "Error serializing object.", e);
			return null;
		} catch (IOException e) {
			Tr.error(tc, "Error serializing object.", e);
			return null;
		}
	}

	@Override
	public byte[] serialize(Object o) {
		if (o == null)
			return null;

		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = serializationService.createObjectOutputStream(baos);
			oos.writeObject(o);
			return baos.toByteArray();
		} catch (IOException e) {
			Tr.error(tc, "Error serializing object.", e);
			return null;
		}
	}
}
