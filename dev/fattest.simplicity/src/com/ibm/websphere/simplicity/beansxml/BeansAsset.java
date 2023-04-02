/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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
package com.ibm.websphere.simplicity.beansxml;

import org.jboss.shrinkwrap.api.asset.ClassLoaderAsset;
import org.jboss.shrinkwrap.impl.base.asset.AssetUtil;

/**
 * Shrinkwrap asset for a beans.xml file
 * <p>
 * Example usage, either using CDIArchiveHelper...
 *
 * <pre>
 * <code>
 * WebArchive war = ShrinkWrap.create(WebArchive.class)
 *              .addPackage(MyClass.class.getPackage());
 *
 * CDIArchiveHelper.addBeansXML(war, DiscoveryMode.ALL);
 * </code>
 * </pre>
 *
 * ...or directly...
 *
 * <pre>
 * <code>
 * BeansAsset beans = BeansAsset.getBeansAsset(DiscoveryMode.ALL);
 *
 * WebArchive war = ShrinkWrap.create(WebArchive.class)
 *              .addPackage(MyClass.class.getPackage())
 *              .addAsWebInfResource(beans, "beans.xml");
 * </code>
 * </pre>
 *
 * In future it would be better to make use of the ShrinkWrap Descriptors lib instead
 * https://github.com/OpenLiberty/open-liberty/issues/16057
 */
public class BeansAsset extends ClassLoaderAsset {

    /**
     * All the possible values of bean-discovery-mode
     */
    public static enum DiscoveryMode {
        NONE, ALL, ANNOTATED, DEFAULT
    };

    /**
     * All the supported CDI versions
     */
    public static enum CDIVersion {
        CDI10, CDI11, CDI20, CDI30, CDI40
    };

    //A static array containing cached BeansAsset instances for all combinations of DiscoveryMode and CDIVersion
    private static final BeansAsset[][] ASSETS = new BeansAsset[DiscoveryMode.values().length][CDIVersion.values().length];
    static {
        for (DiscoveryMode mode : DiscoveryMode.values()) {
            for (CDIVersion version : CDIVersion.values()) {
                if ((version == CDIVersion.CDI10 && mode == DiscoveryMode.DEFAULT) ||
                    (version != CDIVersion.CDI10)) {
                    ASSETS[mode.ordinal()][version.ordinal()] = new BeansAsset(mode, version);
                }
            }
        }
    }

    /**
     * Get a simple CDI 1.1 beans.xml asset
     *
     * @param  mode The bean-discovery-mode to use
     * @return      A BeansAsset
     */
    public static BeansAsset getBeansAsset(DiscoveryMode mode) {
        return getBeansAsset(mode, CDIVersion.CDI11);
    }

    /**
     * Get a simple beans.xml asset
     *
     * @param  mode    The bean-discovery-mode to use
     * @param  version the CDI version to use
     * @return         A BeansAsset
     */
    public static BeansAsset getBeansAsset(DiscoveryMode mode, CDIVersion version) {

        if (version == CDIVersion.CDI10 && mode != DiscoveryMode.DEFAULT) {
            //When using a CDI (implementation) prior to 4.0, a CDI 1.0 beans.xml would have been treated as discovery mode ALL
            //When using a CDI (implementation) 4.0 or higher, a CDI 1.0 beans.xml will now default to discovery mode ANNOTATED
            throw new IllegalArgumentException("Only DiscoveryMode.DEFAULT is supported with CDI 1.0");
        }

        return ASSETS[mode.ordinal()][version.ordinal()];
    }

    /**
     * Get the filename of the beans.xml for the given mode and version
     *
     * @param  mode
     * @param  version
     * @return         the name of the beans.xml file
     */
    private static String getFileName(DiscoveryMode mode, CDIVersion version) {
        String beans;
        if (version == CDIVersion.CDI10) {
            beans = "beans10_";
            if (mode != DiscoveryMode.DEFAULT) {
                throw new IllegalArgumentException("Only DiscoveryMode.DEFAULT is supported with CDI 1.0");
            }
        } else if (version == CDIVersion.CDI11) {
            beans = "beans11_";
        } else if (version == CDIVersion.CDI20) {
            beans = "beans20_";
        } else if (version == CDIVersion.CDI30) {
            beans = "beans30_";
        } else if (version == CDIVersion.CDI40) {
            beans = "beans40_";
        } else {
            throw new IllegalArgumentException("Unknown CDI Version: " + version);
        }

        if (mode == DiscoveryMode.ALL) {
            beans = beans + "all.xml";
        } else if (mode == DiscoveryMode.ANNOTATED) {
            beans = beans + "annotated.xml";
        } else if (mode == DiscoveryMode.NONE) {
            beans = beans + "none.xml";
        } else if (mode == DiscoveryMode.DEFAULT) {
            if (version == CDIVersion.CDI10) {
                beans = beans + "default.xml"; //CDI 1.0 is an implied equivalent of ALL
            } else {
                beans = beans + "annotated.xml"; //CDI 1.1 onwards, ANNOTATED is the default
            }
        } else {
            throw new IllegalArgumentException("Unknown CDI Discovery Mode: " + mode);
        }
        return beans;
    }

    /**
     * Private constructor
     */
    private BeansAsset(DiscoveryMode mode, CDIVersion version) {
        super(AssetUtil.getClassLoaderResourceName(BeansAsset.class.getPackage(), getFileName(mode, version)));
    }

}
