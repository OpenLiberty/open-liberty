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

import java.net.URL;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.container.ManifestContainer;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.beansxml.BeansAsset;
import com.ibm.websphere.simplicity.beansxml.BeansAsset.CDIVersion;
import com.ibm.websphere.simplicity.beansxml.BeansAsset.DiscoveryMode;

/**
 * In future it would be better to make use of the ShrinkWrap Descriptors lib instead of the custom BeansAsset
 * https://github.com/OpenLiberty/open-liberty/issues/16057
 */
public class CDIArchiveHelper {

    public static final String BEANS_XML = "beans.xml";
    public static final String JAVAX_EXTENSION = javax.enterprise.inject.spi.Extension.class.getName();

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
    public static WebArchive addBeansXML(WebArchive webArchive, DiscoveryMode mode) {
        return addBeansXML(webArchive, mode, CDIVersion.CDI11);
    }

    /**
     * Create a WEB-INF/beans.xml file in a war
     *
     * @param webArchive The WAR to create the beans.xml in
     * @param mode       The bean-discovery-mode to use; NONE, ALL or ANNOTATED
     * @param version    The beans.xml version to use; CDI11 (Java EE) or CDI30 (Jakarta EE)
     */
    public static WebArchive addBeansXML(WebArchive webArchive, DiscoveryMode mode, CDIVersion version) {
        BeansAsset beans = BeansAsset.getBeansAsset(mode, version);
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
     * Create a WEB-INF/beans.xml file in a war
     *
     * @param webArchive       The WAR to create the beans.xml in
     * @param beansXMLResource The Resource URL of the beans.xml file to add.
     */
    public static WebArchive addBeansXML(WebArchive webArchive, URL beansXMLResource) {
        return webArchive.addAsWebInfResource(beansXMLResource, BEANS_XML);
    }

    /**
     * Create a WEB-INF/beans.xml file in a war
     *
     * @param webArchive  The WAR to create the beans.xml in
     * @param owningClass The class to which the source beans.xml belongs. Must be in the same package.
     */
    public static WebArchive addBeansXML(WebArchive webArchive, Class<?> owningClass) {
        return addBeansXML(webArchive, owningClass.getPackage());
    }

    /**
     * Create a WEB-INF/beans.xml file in a war
     *
     * @param webArchive The WAR to create the beans.xml in
     * @param srcPackage The package where the source beans.xml file can be found
     */
    public static WebArchive addBeansXML(WebArchive webArchive, Package srcPackage) {
        return webArchive.addAsWebInfResource(srcPackage, BEANS_XML, BEANS_XML);
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
    public static JavaArchive addBeansXML(JavaArchive archive, DiscoveryMode mode) {
        return addBeansXML(archive, mode, CDIVersion.CDI11);
    }

    /**
     * Create a META-INF/beans.xml file in a jar
     *
     * @param archive The archive to create the beans.xml in
     * @param mode    The bean-discovery-mode to use; NONE, ALL or ANNOTATED
     * @param version The beans.xml version to use
     */
    public static JavaArchive addBeansXML(JavaArchive archive, DiscoveryMode mode, CDIVersion version) {
        BeansAsset beans = BeansAsset.getBeansAsset(mode, version);
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
     * Create a WEB-INF/beans.xml file in a war
     *
     * @param archive          The JAR to create the beans.xml in
     * @param beansXMLResource The Resource URL of the beans.xml file to add.
     */
    public static JavaArchive addBeansXML(JavaArchive archive, URL beansXMLResource) {
        return archive.addAsManifestResource(beansXMLResource, BEANS_XML);
    }

    /**
     * Create a WEB-INF/beans.xml file in a war
     *
     * @param archive     The JAR to create the beans.xml in
     * @param owningClass The class to which the source beans.xml belongs. Must be in the same package.
     */
    public static JavaArchive addBeansXML(JavaArchive archive, Class<?> owningClass) {
        return addBeansXML(archive, owningClass.getPackage());
    }

    /**
     * Create a META-INF/beans.xml file in an a jar
     *
     * @param archive    The archive to create the beans.xml in
     * @param srcPackage The package where the source beans.xml file can be found
     */
    public static JavaArchive addBeansXML(JavaArchive archive, Package srcPackage) {
        return archive.addAsManifestResource(srcPackage, BEANS_XML, BEANS_XML);
    }

    /**
     * Add a META-INF/services/javax.enterprise.inject.spi.Extension file to an archive
     *
     * @param archive     The JAR to create the javax.enterprise.inject.spi.Extension in
     * @param owningClass The class to which the source javax.enterprise.inject.spi.Extension belongs. Must be in the same package.
     */
    public static <T extends Archive<T>> T addCDIExtensionFile(ManifestContainer<T> archive, Class<?> owningClass) {
        return addCDIExtensionFile(archive, owningClass.getPackage());
    }

    /**
     * Add a META-INF/services/javax.enterprise.inject.spi.Extension file to an archive
     *
     * @param archive    The archive to create the javax.enterprise.inject.spi.Extension in
     * @param srcPackage The package where the source javax.enterprise.inject.spi.Extension file can be found
     */
    public static <T extends Archive<T>> T addCDIExtensionFile(ManifestContainer<T> archive, Package srcPackage) {
        return archive.addAsManifestResource(srcPackage, JAVAX_EXTENSION, "services/" + JAVAX_EXTENSION);
    }

    /**
     * Create a META-INF/services/javax.enterprise.inject.spi.Extension file containing the name of the extension class
     *
     * @param archive The archive to create the CDI Extension Service file in
     */
    @SafeVarargs
    public static <T extends Archive<T>> T addCDIExtensionService(ManifestContainer<T> archive, Class<? extends javax.enterprise.inject.spi.Extension>... extensionClass) {
        return archive.addAsServiceProvider(javax.enterprise.inject.spi.Extension.class, extensionClass);
    }

    /**
     * Create a META-INF/services/jakarta.enterprise.inject.spi.Extension file containing the name of the extension class
     *
     * @param archive The archive to create the CDI Extension Service file in
     */
    @SafeVarargs
    public static <T extends Archive<T>> T addJakartaCDIExtensionService(ManifestContainer<T> archive, Class<? extends jakarta.enterprise.inject.spi.Extension>... extensionClass) {
        return archive.addAsServiceProvider(jakarta.enterprise.inject.spi.Extension.class, extensionClass);
    }
}
