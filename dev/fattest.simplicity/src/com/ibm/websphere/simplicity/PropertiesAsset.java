/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.jboss.shrinkwrap.api.asset.Asset;

/**
 * Shrinkwrap asset for a properties file
 * <p>
 * Example usage:
 *
 * <pre>
 * <code>
 * PropertiesAsset config = new PropertiesAsset()
 *              .addProperty("myConfigKey", "myConfigValue")
 *              .addProperty("myConfigKey2", "myConfigValue2");
 *
 * WebArchive war = ShrinkWrap.create(WebArchive.class)
 *              .addPackage(MyClass.class.getPackage())
 *              .addAsResource(config, "META-INF/microprofile-config.properties");
 * </code>
 * </pre>
 */
public class PropertiesAsset implements Asset {

    private final Properties props = new Properties();
    private final List<PropertiesAsset> included = new ArrayList<>();

    public PropertiesAsset addProperty(String key, String value) {
        props.put(key, value);
        return this;
    }

    public PropertiesAsset removeProperty(String key) {
        props.remove(key);
        return this;
    }

    public PropertiesAsset include(PropertiesAsset include) {
        included.add(include);
        return this;
    }

    @Override
    public InputStream openStream() {
        try {
            Properties finalProps = new Properties();
            for (PropertiesAsset include : included) {
                finalProps.putAll(include.props);
            }
            finalProps.putAll(props);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            finalProps.store(baos, null);
            return new ByteArrayInputStream(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Unable to create properties asset", e);
        }
    }

}
