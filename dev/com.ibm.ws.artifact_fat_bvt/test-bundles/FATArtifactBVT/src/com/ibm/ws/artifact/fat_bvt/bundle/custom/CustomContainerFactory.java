/*******************************************************************************
 * Copyright (c) 2015,2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.fat_bvt.bundle.custom;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;

import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.factory.contributor.ArtifactContainerFactoryContributor;

/**
 * A Really really simple example of a custom container implementation.
 * <p>
 * Creates a container from a property file.. each key becomes an entry, each keys value becoming the entries data.
 * <p>
 * This is made MUCH simpler because, it does not implement <br>
 * <ul>
 * <li>Notifiers</li>
 * <li>Nested entries</li>
 * <li>Nested containers</li>
 * <li>Opening properties files as nested containers</li>
 * <li>Any physicalpath/url references</li>
 * <li>fastmode</li>
 * </ul>
 * Other than that, it'll open .custom files as containers. Data in .custom to be as .properties.
 * <p>
 * <pre>
 * Service-Component: \
 *     com.ibm.ws.artifact.fat_bvt.bundle.custom.container; \
 *     implementation:=com.ibm.ws.artifact.fat_bvt.bundle.CustomContainerFactory; \
 *     provide:=com.ibm.wsspi.artifact.factory.contributor.ArtifactContainerFactoryContributor; \
 *     configuration-policy:=ignore; \
 *     properties:="service.vendor=IBM,category=CUSTOM,handlesType=java.io.File,handlesEntries=.custom"
 * </pre>
 */
public class CustomContainerFactory implements ArtifactContainerFactoryContributor {
    private static boolean isFile(final File target) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return Boolean.valueOf( target.isFile() );
            }
        }).booleanValue();
    }

    private static Properties load(File propertiesFile) {
        Properties properties = new Properties();

        InputStream is = null;
        try {
            is = new FileInputStream(propertiesFile); // throws FileNotFoundException
            properties.load(is); // throws IOException

        } catch ( Exception e ) {
            return null;

        } finally {
            if ( is != null ) {
                try {
                    is.close(); // throws IOException
                } catch ( IOException e ) {
                    return null;
                }
            }
        }

        return properties;
    }

    //

    private boolean isCustomContainer(String name) {
        return name.matches("(?i:(.*)\\.(CUSTOM))");
    }

    /**
     * Attempt to create a custom container.
     * 
     * The container data must be a file, which must exist, which must
     * have the custom file extension, and must load as a properties file. 
     */
    @Override
    public ArtifactContainer createContainer(File cacheDir, Object containerData) {
        if ( !(containerData instanceof File) ) {
            return null;
        }

        File fileContainerData = (File) containerData;

        if ( !isFile(fileContainerData) ) {
            return null;
        }

        if ( !isCustomContainer( fileContainerData.getName() ) ) {
            return null;
        }

        Properties properties = load(fileContainerData);
        if ( properties == null ) {
            return null;
        }

        return new CustomContainer(properties);
    }

    /**
     * Attempt to create a custom nested container.
     * 
     * This implementation always answers null.
     */
    @Override
    public ArtifactContainer createContainer(
        File cacheDir,
        ArtifactContainer parentContainer,
        ArtifactEntry enclosingEntry,
        Object containerData) {

        return null;
    }
}
