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

import static com.ibm.ws.test.image.build.BuildProperties.IMAGES_PATH;
import static com.ibm.ws.test.image.util.FileUtils.TEST_OUTPUT_PATH_ABS;
import static com.ibm.ws.test.image.util.FileUtils.ensureNonexistence;

import java.lang.reflect.Constructor;

import com.ibm.ws.test.image.build.BuildImages;
import com.ibm.ws.test.image.util.FileUtils;

public class RepositorySingleton<RepoType extends Repository> {
    public static final String CLASS_NAME = RepositorySingleton.class.getSimpleName();

    public static void log(String className, String message) {
        System.out.println(className + ": " + message);
    }

    public void log(String message) {
        log(CLASS_NAME, message);
    }

    //

    public RepositorySingleton(
            BuildImages.ImageType imageType,
            String storageName,
            Class<? extends RepoType> implType) {

        this.imageType = imageType;
        this.storageName = storageName;
        this.implType = implType;

        this.failedRepo = false;
        this.repo = null;
    }

    private final BuildImages.ImageType imageType;
    private final String storageName;
    private final Class<? extends RepoType> implType;

    public BuildImages.ImageType getImageType() {
        return imageType;
    }
    
    public String getStorageName() {
        return storageName;
    }
    
    public Class<? extends RepoType> getImplementationType() {
        return implType;
    }
    
    private boolean failedRepo;
    private RepoType repo;

    public RepoType getRepository() throws Exception {
        if ( failedRepo ) {
            throw new Exception("Prior failure to setup [ " + imageType.getDescription() + " ]");
        }

        if ( repo == null ) {
            try {
                repo = createRepository();
                failedRepo = false;
            } catch ( Exception e ) {
                repo = null;
                failedRepo = true;
                throw e;
            }
        }

        return repo;
    }

    public RepoType createRepository() throws Exception {
        String repoPath = TEST_OUTPUT_PATH_ABS + "/" + getStorageName();
        Class<? extends RepoType> useImplType = getImplementationType();
        
        log("Setting up repository [ " + repoPath + " ]" +
            " of type [ " + useImplType.getName()  + " ]");

        BuildImages.ImageType useImageType = getImageType();
        log("Images [ " + IMAGES_PATH + " ]");
        log("Target image [ " + useImageType.getDescription() + " ]");
        String imagePath = useImageType.getImagePath();
        log("Located image [ " + imagePath + " ]");

        log("Extracting repository ...");
        FileUtils.logOutput();
        ensureNonexistence(repoPath);
        FileUtils.logOutput();
        useImageType.extract(repoPath);
        FileUtils.logOutput();
        log("Extracted repository");

        Constructor<? extends RepoType> repoCtor =
            implType.getDeclaredConstructor( new Class<?>[] { String.class, String.class } );
        return repoCtor.newInstance( new Object[] { imagePath, repoPath } );
    }
}
