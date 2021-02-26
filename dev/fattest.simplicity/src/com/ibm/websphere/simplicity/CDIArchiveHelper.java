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
package com.ibm.websphere.simplicity;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.container.ManifestContainer;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.BeansAsset.Mode;
import com.ibm.websphere.simplicity.BeansAsset.Version;

/**
 * In future it would be better to make use of the ShrinkWrap Descriptors lib instead of the custom BeansAsset
 * https://github.com/OpenLiberty/open-liberty/issues/16057
 */
public class CDIArchiveHelper {

    public static final String BEANS_XML = "beans.xml";

    /**
     * Create an empty WEB-INF/beans.xml file in a war
     *
     * @param webArchive The WAR to create the beans.xml in
     */
    public static WebArchive addEmptyBeansXML(WebArchive webArchive) {
        return addBeansXML(webArchive, EmptyAsset.INSTANCE);
    }

    /**
     * Create a CDI 1.1 WEB-INF/beans.xml file in a war
     *
     * @param webArchive The WAR to create the beans.xml in
     * @param mode       The bean-discovery-mode to use; NONE, ALL or ANNOTATED
     */
    public static WebArchive addBeansXML(WebArchive webArchive, Mode mode) {
        return addBeansXML(webArchive, mode, Version.CDI11);
    }

    /**
     * Create a WEB-INF/beans.xml file in a war
     *
     * @param webArchive The WAR to create the beans.xml in
     * @param mode       The bean-discovery-mode to use; NONE, ALL or ANNOTATED
     * @param version    The beans.xml version to use; CDI11 (Java EE) or CDI30 (Jakarta EE)
     */
    public static WebArchive addBeansXML(WebArchive webArchive, Mode mode, Version version) {
        BeansAsset beans = new BeansAsset(mode, version);
        return addBeansXML(webArchive, beans);
    }

    /**
     * Create a WEB-INF/beans.xml file in a war
     *
     * @param webArchive The WAR to create the beans.xml in
     * @param beans      an Asset which represents the beans.xml file. Recommended to be either a BeansAsset or an EmptyAsset instance.
     */
    public static WebArchive addBeansXML(WebArchive webArchive, Asset beans) {
        return webArchive.addAsWebInfResource(beans, BEANS_XML);
    }

    /**
     * Create an empty META-INF/beans.xml file in a jar
     *
     * @param archive The archive to create the beans.xml in
     */
    public static JavaArchive addEmptyBeansXML(JavaArchive archive) {
        return addBeansXML(archive, EmptyAsset.INSTANCE);
    }

    /**
     * Create a CDI 1.1 META-INF/beans.xml file in a jar
     *
     * @param archive The archive to create the beans.xml in
     * @param mode    The bean-discovery-mode to use; NONE, ALL or ANNOTATED
     */
    public static JavaArchive addBeansXML(JavaArchive archive, Mode mode) {
        return addBeansXML(archive, mode, Version.CDI11);
    }

    /**
     * Create a META-INF/beans.xml file in a jar
     *
     * @param archive The archive to create the beans.xml in
     * @param mode    The bean-discovery-mode to use; NONE, ALL or ANNOTATED
     * @param version The beans.xml version to use; JAVAEE (CDI 1.1) or JAKARTA (CDI 3.0)
     */
    public static JavaArchive addBeansXML(JavaArchive archive, Mode mode, Version version) {
        BeansAsset beans = new BeansAsset(mode);
        return addBeansXML(archive, beans);
    }

    /**
     * Create a META-INF/beans.xml file in an a jar
     *
     * @param archive The archive to create the beans.xml in
     * @param beans   an Asset which represents the beans.xml file. Recommended to be either a BeansAsset or an EmptyAsset instance.
     */
    public static JavaArchive addBeansXML(JavaArchive archive, Asset beans) {
        return archive.addAsManifestResource(beans, BEANS_XML);
    }

    /**
     * Create a META-INF/services/javax.enterprise.inject.spi.Extension file containing the name of the extension class
     *
     * @param archive The archive to create the CDI Extension Service file in
     */
    public static <T extends Archive<T>> T addCDIExtensionService(ManifestContainer<T> archive, Class<? extends javax.enterprise.inject.spi.Extension> extensionClass) {
        return archive.addAsServiceProvider(javax.enterprise.inject.spi.Extension.class, extensionClass);
    }

    /**
     * Create a META-INF/services/jakarta.enterprise.inject.spi.Extension file containing the name of the extension class
     *
     * @param archive The archive to create the CDI Extension Service file in
     */
    public static <T extends Archive<T>> T addJakartaCDIExtensionService(ManifestContainer<T> archive, Class<? extends jakarta.enterprise.inject.spi.Extension> extensionClass) {
        return archive.addAsServiceProvider(jakarta.enterprise.inject.spi.Extension.class, extensionClass);
    }
}
