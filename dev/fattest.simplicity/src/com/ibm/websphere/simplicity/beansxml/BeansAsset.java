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
 * war = CDIArchiveHelper.addBeansXML(war, Mode.ALL);
 * </code>
 * </pre>
 *
 * ...or directly...
 *
 * <pre>
 * <code>
 * BeansAsset beans = new BeansAsset(Mode.ALL);
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

    public static enum DiscoveryMode {
        NONE, ALL, ANNOTATED
    };

    public static enum CDIVersion {
        CDI11, CDI20, CDI30
    };

    public BeansAsset(DiscoveryMode mode) {
        this(mode, CDIVersion.CDI11);
    }

    public BeansAsset(DiscoveryMode mode, CDIVersion version) {
        super(AssetUtil.getClassLoaderResourceName(BeansAsset.class.getPackage(), getFileName(mode, version)));
    }

    /**
     * @param  mode
     * @param  version
     * @return         the name of the beans.xml file
     */
    private static String getFileName(DiscoveryMode mode, CDIVersion version) {
        String beans;
        if (version == CDIVersion.CDI11) {
            beans = "beans11_";
        } else if (version == CDIVersion.CDI20) {
            beans = "beans20_";
        } else if (version == CDIVersion.CDI30) {
            beans = "beans30_";
        } else {
            throw new RuntimeException("Unknown CDI Version: " + version);
        }
        if (mode == DiscoveryMode.ALL) {
            beans = beans + "all.xml";
        } else if (mode == DiscoveryMode.ANNOTATED) {
            beans = beans + "annotated.xml";
        } else if (mode == DiscoveryMode.NONE) {
            beans = beans + "none.xml";
        } else {
            throw new RuntimeException("Unknown CDI Discovery Mode: " + mode);
        }
        return beans;
    }

}
