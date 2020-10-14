/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session.cache.fat.infinispan.container;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;

/**
 * Transform FAT resources to Jakarta ee9 equivalents
 */
public class ResourceTransformationHelper {

    public static final Class<?> c = ResourceTransformationHelper.class;

    /**
     * Transform FAT resources to Jakarta ee9 equivalents and place them in a folder
     */
    public static void transformResourcestoEE9(RemoteFile originalResourceDir, RemoteFile jakartaResourceDir, LibertyServer serverA, LibertyServer serverB) throws Exception {
        RemoteFile[] originalResources = originalResourceDir.list(true);
        RemoteFile[] jakartaResources = jakartaResourceDir.list(true);

        ServerConfiguration serverConfig;

        if (jakartaResources.length == 0) {
            Log.info(c, "transformResourcestoEE9", "Transforming resources");
            for (RemoteFile resource : originalResources) {
                Path resourceFile = Paths.get(originalResourceDir.getAbsolutePath(), resource.getName());
                Path newResourceFile = Paths.get(jakartaResourceDir.getAbsolutePath(),
                                                 resource.getName().substring(0, resource.getName().lastIndexOf(".")) + ".jakarta.jar");
                JakartaEE9Action.transformApp(resourceFile, newResourceFile);
            }
        }
        if (serverA != null) {
            serverConfig = serverA.getServerConfiguration();
            serverConfig.getLibraries().stream().filter((l) -> "InfinispanLib".equals(l.getId())).findFirst().ifPresent((l) -> {
                l.getFilesets().forEach((f) -> f.setDir("${shared.resource.dir}/infinispan-jakarta"));
            });
            serverA.updateServerConfiguration(serverConfig);
        }

        if (serverB != null) {
            serverConfig = serverB.getServerConfiguration();
            serverConfig.getLibraries().stream().filter((l) -> "InfinispanLib".equals(l.getId())).findFirst().ifPresent((l) -> {
                l.getFilesets().forEach((f) -> f.setDir("${shared.resource.dir}/infinispan-jakarta"));
            });
            serverB.updateServerConfiguration(serverConfig);
        }
    }

}
