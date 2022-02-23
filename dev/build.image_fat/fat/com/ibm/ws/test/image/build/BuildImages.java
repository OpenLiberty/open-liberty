/*******************************************************************************
 * Copyright (c) 2016, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.test.image.build;

import static com.ibm.ws.test.image.build.BuildProperties.*;
import static com.ibm.ws.test.image.util.FileUtils.*;
import static com.ibm.ws.test.image.util.ProcessRunner.runJar;

import java.io.File;
import java.util.List;

import com.ibm.ws.test.image.util.FileUtils;

public class BuildImages {
    public static final String CLASS_NAME = BuildImages.class.getSimpleName();

    public static void log(String message) {
        System.out.println(CLASS_NAME + ": " + message);
    }

    //
    
    // From 22.002 build:

    // wlp-developers-ipla-all-22.0.0.1.jar Image for all installable archive
    // wlp-developers-runtime-22.0.0.1.jar Image for runtime installable archive
    //
    // wlp-repo-cl220220220105-0300.zip wlp Maven repo zip
    // wlp-repo-spec-cl220220220105-0300.zip wlp Spec maven repo zip
    //
    // wlp-zos-all-cl220220220105-0300.zip z/OS All Image
    // wlp-zos-core-cl220220220105-0300.zip Base image for IM installs on z/OS
    // wlp-zos-kernel-cl220220220105-0300.zip Base kernel image for IM installs on z/OS
    // wlp-embeddable-zos-cl220220220105-0300.zip z/OS Embedder Image
    //
    // wlp-cl220220220105-0300.zip wlp zipped image (a convenience but not our actual product deliverable)
    // wlp-tradelite-cl220220220105-0300.zip wlp-tradelite zipped image
    //
    // zautoFVT.zip Liberty autoFVT image
    // liberty-image-for-bvt.zip Liberty image for asynchronous BVT execution

    // Open liberty:
    //
    // dev/build.image/build/libs/distributions/
    //
    // openliberty-22.0.0.1-202202171441.zip
    // openliberty-22.0.0.2-beta-202202171441.zip
    //
    // openliberty-all-22.0.0.1-202202171441.zip
    // openliberty-dev-22.0.0.1-202202171441.zip
    //
    // openliberty-kernel-22.0.0.1-202202171441.zip        
    //
    // openliberty-javaee8-22.0.0.1-202202171441.zip
    // openliberty-webProfile8-22.0.0.1-202202171441.zip
    // openliberty-microProfile4-22.0.0.1-202202171441.zip
    //
    // openliberty-jakartaee9-22.0.0.1-202202171441.zip        
    // openliberty-webProfile9-22.0.0.1-202202171441.zip
    // openliberty-microProfile5-22.0.0.1-202202171441.zip
    
    public static enum ImageType {
        // WS-CD
        //        KERNEL_ZIP("wlp-kernel", ".zip"),
        //
        //        CORE_LIC("wlp-core-license", ".jar"),
        //        CORE_ALL("wlp-core-all", ".jar"),
        //
        //        BASE_LIC("wlp-base-license", ".jar"),
        //        BASE_ALL("wlp-base-all", ".jar"),
        //
        //        ND_LIC("wlp-nd-license", ".jar"),
        //        ND_ALL("wlp-nd-all", ".jar"),
        //
        //        DEVELOPERS_IPLA_ALL("wlp-developers-ipla-all", ".jar"),
        //
        MAVEN_REPO("mavenArtifact", null, null, ".zip"),
        FEATURE_REPO("wlp-featureRepo", null, null, ".zip"),

        // openliberty-22.0.0.1-202202171441.zip
        // openliberty-22.0.0.2-beta-202202171441.zip
        // openliberty-all-22.0.0.1-202202171441.zip
        // openliberty-dev-22.0.0.1-202202171441.zip
        OPEN_LIBERTY("openliberty", null, null, ".zip"),
        OPEN_LIBERTY_BETA("openliberty", null, "beta", null, ".zip"),
        OPEN_LIBERTY_ALL("openliberty", "all", null, null, ".zip"),
        OPEN_LIBERTY_DEV("openliberty", "dev", null, null, ".zip"),
        
        // openliberty-kernel-22.0.0.1-202202171441.zip
        OPEN_LIBERTY_KERNEL("openliberty", "kernel", null, null, ".zip"),

        // openliberty-javaee8-22.0.0.1-202202171441.zip
        // openliberty-webProfile8-22.0.0.1-202202171441.zip
        // openliberty-microProfile4-22.0.0.1-202202171441.zip        
        OPEN_LIBERTY_JAVAEE8("openliberty", "javaee8", null, null, ".zip"),
        OPEN_LIBERTY_WEB8("openliberty", "webProfile8", null, null, ".zip"),
        OPEN_LIBERTY_MICRO8("openliberty", "microProfile4", null, null, ".zip"),

        // openliberty-jakartaee9-22.0.0.1-202202171441.zip        
        // openliberty-webProfile9-22.0.0.1-202202171441.zip
        // openliberty-microProfile5-22.0.0.1-202202171441.zip
        OPEN_LIBERTY_JAKARTAEE9("openliberty", "jakartaee9", null, null, ".zip"),
        OPEN_LIBERTY_WEB9("openliberty", "webProfile9", null, null, ".zip"),
        OPEN_LIBERTY_MICRO9("openliberty", "microProfile5", null, null, ".zip"),

        OL_MAVEN_REPO("openliberty-MavenArtifact", null, null, ".zip");

        public static void ensureImageFiles() throws Exception {
            for ( ImageType imageType : ImageType.values() ) {
                imageType.ensureImageFile();
            }
        }

        private ImageType(String ... parts) {
            this.parts = parts;
            this.description = FileUtils.describe(parts);

            String imagePath = (new RawImages(parts)).getImagePath();
            this.rawImage = new RawImage( name(), imagePath );
        }

        private final String[] parts;

        public String[] getParts() {
            return parts;
        }

        private final String description;

        public String getDescription() {
            return description;
        }

        //

        private final RawImage rawImage;

        public RawImage getRawImage() {
            return rawImage;
        }

        public String getImagePath() {
            return getRawImage().getImagePath();
        }

        public File getImageFile() {
            return getRawImage().getImageFile();
        }

        public void ensureImageFile() throws Exception {
            File imageFile = getImageFile();
            if ( !imageFile.exists() ) {
                throw new Exception("Image [ " + imageFile.getAbsolutePath() + " ] does not exist");
            }
        }

        public long getImageLength() {
            return getRawImage().getImageLength();
        }

        public void extract(String extractPath) throws Exception {
            getRawImage().extract(extractPath);
        }
        
        public void installFromJar(String installationPath) throws Exception {
            getRawImage().installFromJar(installationPath);
        }
    };
    
    //
    
    public static class RawImages {
        public RawImages(String[] parts) {
            this.parts = parts;
            this.description = describe(parts);
        }

        private final String[] parts;
        private final String description;

        public String[] getParts() {
            return parts;
        }
        
        public String getDescription() {
            return description;
        }

        //
        
        private List<String> imagePaths;
        private boolean isSetImagePaths;
        
        public List<String> getImagePaths() {
            if ( !isSetImagePaths ) {
                imagePaths = BuildProperties.getImages( getParts() );
                isSetImagePaths = true;
            }
            return imagePaths;
        }
        
        public String getImagePath() {
            String useDescription = getDescription();
            
            List<String> useImagePaths = getImagePaths();
            
            if ( (useImagePaths == null) || useImagePaths.isEmpty() ) {
                log("No images: [ " + useDescription + " ] under [ " + IMAGES_PATH + " ]");
                return null;

            } else if ( useImagePaths.size() > 1 ) {
                for ( String imagePath : useImagePaths ) {
                    log("Image [ " + imagePath + " ]");
                }
                log("Too many images: [ " + useDescription + " ] under [ " + IMAGES_PATH + " ]");
                return useImagePaths.get(0);

            } else {
                String image = useImagePaths.get(0);
                log("Found image [ " + image + " ] [ " + useDescription + " ] under [ " + IMAGES_PATH + " ]");
                return image;
            }
        }
    }

    //

    public static class RawImage {
        public RawImage(String name, String imagePath) {
            this.name = name;
            this.imagePath = imagePath;
        }

        private final String name;
        
        public String getName() {
            return name;
        }

        private final String imagePath;

        public String getImagePath() {
            return imagePath;
        }

        private File imageFile;
        private long imageLength;

        public void setImageFile() {
            if ( imageFile == null ) {
                imageFile = new File(imagePath);
                imageLength = imageFile.length();
            }
        }
        
        public File getImageFile() {
            setImageFile();
            return imageFile;
        }

        public long getImageLength() {
            setImageFile();
            return imageLength;
        }

        public void makeRunnable(String extractPath) throws Exception {
            setPermissions(extractPath + "/wlp/bin", "+x");
        }

        public void extract(String extractPath) throws Exception {
            FileUtils.extract( getImagePath(), extractPath );
            makeRunnable(extractPath);
        }

        public void installFromJar(String installationPath) throws Exception {
            runJar( getImagePath(), "--acceptLicense", installationPath);
            makeRunnable(installationPath);
        }
    }
}
