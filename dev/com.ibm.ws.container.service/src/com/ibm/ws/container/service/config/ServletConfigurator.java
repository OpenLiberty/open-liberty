/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.config;

import java.util.Map;
import java.util.Set;

import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.wsspi.adaptable.module.Container;

/**
 * Type for server configuration.
 */
public interface ServletConfigurator {

    /**
     * Source of a configuration item: From a web.xml, web-fragment.xml or from annotations.
     */
    public static enum ConfigSource {
        WEB_XML, WEB_FRAGMENT, ANNOTATION;
    }

    /**
     * Generic configuration item. Encapsulates a value, a source, and provides
     * a specific value comparison implementation.
     * 
     * @param <I> The type of value held by the configuration item.
     */
    public interface ConfigItem<I> {

        /**
         * Answer the value of the configuration item. Null
         * may be returned.
         * 
         * @return The value of the configuration item.
         */
        public I getValue();

        /**
         * Answer a value of a specific type.
         * 
         * @param cls The type of the value which is to be obtained.
         * 
         * @return A value of the specified type.
         */
        public <T> T getValue(Class<T> cls);

        /**
         * Compare the value of this configuration item with a
         * specified value. The specified value may be null.
         * 
         * @param otherValue The other value to compare against.
         * 
         * @return True if the values are equal. False if the
         *         values are unequal.
         */
        public boolean compareValue(I otherValue);

        /**
         * Tell the source of the configuration item (web, web fragment,
         * or annotation). When the source of the item is a fragment,
         * the library URI is set to the URI of the library. When the
         * source of the item is web.xml, the library URI is fixed to
         * "WEB-INF/web.xml".
         * 
         * See {@link #getLibraryURI()}.
         * 
         * @return The source of the configuration item.
         */
        public ConfigSource getSource();

        /**
         * Tell the URI of the library of the configuration item. When
         * the source of the item is web.xml, answer the fixed value
         * "WEB-INF/web.xml".
         * 
         * @return The URI of the library of the configuration.
         */
        public String getLibraryURI();
    }

    public interface MergeComparator<T> {
        public boolean compare(T o1, T o2);
    }

    public Container getModuleContainer();

    public Object getFromModuleCache(Class<?> owner);

    public void addToModuleCache(Class<?> owner, Object data);

    public int getServletVersion();

    public boolean isMetadataComplete();

    public ConfigSource getConfigSource();

    public String getLibraryURI();

    boolean getMetadataCompleted();

    public WebAnnotations getWebAnnotations();

    public <T> Map<String, ConfigItem<T>> getConfigItemMap(String key);

    public long generateUniqueId();

    public <T> Set<T> getContextSet(String key);

    public <T> ConfigItem<T> createConfigItem(T value);

    public <T> ConfigItem<T> createConfigItem(T value, MergeComparator<T> comparator);

    public <T> void validateDuplicateConfiguration(String parentElementName,
                                                   String elementName, T newValue,
                                                   ConfigItem<T> currentConfigItem);

    public <T> void validateDuplicateKeyValueConfiguration(String parentElementName,
                                                           String keyElementName, String keyElementValue,
                                                           String valueElementName, T newValue,
                                                           ConfigItem<T> currentConfigItem);

    public void validateDuplicateDefaultErrorPageConfiguration(String newLocationValue,
                                                               ConfigItem<String> currentLocationItem);

    public void addErrorMessage(String errorMessage);
}
