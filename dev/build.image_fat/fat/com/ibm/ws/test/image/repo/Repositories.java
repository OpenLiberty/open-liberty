/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.test.image.repo;

import com.ibm.ws.test.image.build.BuildImages;
import com.ibm.ws.test.image.build.BuildImages.ImageType;

public class Repositories {
    
    private static RepositorySingleton<FeatureRepository> featureRepo =
            new RepositorySingleton<FeatureRepository>(
                    BuildImages.ImageType.FEATURE_REPO,
                    "featureRepo",
                    FeatureRepository.class);

    public FeatureRepository getFeatureRepository() throws Exception {
        return featureRepo.getRepository();
    }

    private static RepositorySingleton<MavenRepository> mavenRepo =
        new RepositorySingleton<MavenRepository>(
                BuildImages.ImageType.MAVEN_REPO,
                "mavenRepo",
                MavenRepository.class);

    public MavenRepository getMavenRepository() throws Exception {
        return mavenRepo.getRepository();
    }

    private static RepositorySingleton<MavenRepository> olMavenRepo =
            new RepositorySingleton<MavenRepository>(
                    BuildImages.ImageType.OL_MAVEN_REPO,
                    "olMavenRepo",
                    MavenRepository.class);

    public MavenRepository getOpenLibertyMavenRepository() throws Exception {
        return olMavenRepo.getRepository();    
    }
}
