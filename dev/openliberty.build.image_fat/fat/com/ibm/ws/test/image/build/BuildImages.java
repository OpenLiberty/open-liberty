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

import com.ibm.ws.test.image.Timeouts;
import com.ibm.ws.test.image.util.FileUtils;

public class BuildImages {
    public static final String CLASS_NAME = BuildImages.class.getSimpleName();

    public static void log(String message) {
        System.out.println(CLASS_NAME + ": " + message);
    }

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
        OL_MAVEN_REPO("openliberty-MavenArtifact", null, null, ".zip"),

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
        OPEN_LIBERTY_MICRO9("openliberty", "microProfile5", null, null, ".zip");

        private ImageType(String ... parts) {
            this.parts = parts;
            this.rawImages = new RawImages( name(), parts );
        }

        private final String[] parts;

        public String[] getParts() {
            return parts;
        }

        private final RawImages rawImages;
        
        public RawImages getRawImages() {
            return rawImages;
        }

        public String getDescription() {
            return rawImages.getDescription();
        }

        public RawImage getRawImage() {
            return rawImages.getRawImage();
        }

        public RawImage ensureRawImage(String opName) throws Exception {
            return rawImages.ensureRawImage(opName);
        }

        public String getImagePath() {
            RawImage useRawImage = getRawImage();
            return ( (useRawImage == null) ? null : useRawImage.getImagePath() );
        }

        public File getImageFile() {
            RawImage useRawImage = getRawImage();
            return ( (useRawImage == null) ? null : useRawImage.getImageFile() );
        }

        public long getImageLength() {
            RawImage useRawImage = getRawImage();
            return ( (useRawImage == null) ? -1L : useRawImage.getImageLength() );
        }

        public void extract(String extractPath) throws Exception {
            RawImage useRawImage = ensureRawImage("extract");
            useRawImage.extract(extractPath);
        }

        public void installFromJar(String installationPath) throws Exception {
            RawImage useRawImage = ensureRawImage("install");
            useRawImage.installFromJar(installationPath, Timeouts.INSTALL_FROM_JAR_TIMEOUT_NS);
        }
    };
    
    //
    
    public static class RawImages {
        public RawImages(String name, String[] parts) {
            this.name = name;
            this.parts = parts;
            this.description = describe(parts);
            
            this.isSetRawImage = false;
            this.rawImage = null;
        }

        private final String name;
        
        public String getName() {
            return name;
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
        
        public String getImagesPath() {
            return BuildProperties.getImagesPath();
        }

        private List<String> imageNames;
        private boolean isSetImageNames;
        
        public List<String> getImageNames() {
            if ( !isSetImageNames ) {
                imageNames = BuildProperties.getImageNames( getParts() );
                isSetImageNames = true;
            }
            return imageNames;
        }

        public String getImageName() {
            String useDescription = getDescription();
            
            List<String> useImageNames = getImageNames();
            if ( (useImageNames == null) || useImageNames.isEmpty() ) {
                log("No images: [ " + useDescription + " ] under [ " + IMAGES_PATH + " ]");
                return null;
            }
            
            String imageName;
            if ( useImageNames.size() > 1 ) {
                for ( String imagePath : useImageNames ) {
                    log("Image [ " + imagePath + " ]");
                }
                log("Too many images: [ " + useDescription + " ] under [ " + IMAGES_PATH + " ]");

                imageName = useImageNames.get(0);
                log("Using first image [ " + imageName + " ] [ " + useDescription + " ] under [ " + IMAGES_PATH + " ]");                

            } else {
                imageName = useImageNames.get(0);
                log("Found image [ " + imageName + " ] [ " + useDescription + " ] under [ " + IMAGES_PATH + " ]");
            }

            return imageName;
        }
        
        private boolean isSetRawImage;
        private RawImage rawImage;

        public RawImage getRawImage() {
            if ( !isSetRawImage ) {
                RawImage useRawImage;
                String imageName = getImageName();
                if ( imageName != null ) {
                    String imagesPath = getImagesPath();
                    String imagePath = imagesPath + '/' + imageName;
                    useRawImage = new RawImage( name, imagePath );
                } else {
                    useRawImage = null;
                }

                rawImage = useRawImage;
                isSetRawImage = true;
            }

            return rawImage;
        }

        public RawImage ensureRawImage(String opName) throws Exception {
            RawImage useRawImage = getRawImage();
            if ( useRawImage == null ) {
                throw new Exception(
                    "Cannot " + opName +
                    " [ " + getName() + " : " + getDescription() + "]" +
                    ": Image is not available");
            }
            return useRawImage;
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
            // makeRunnable(extractPath);
        }

        public void installFromJar(String installationPath, long timeout) throws Exception {
            runJar( timeout, getImagePath(), "--acceptLicense", installationPath);
            // makeRunnable(installationPath);
        }
    }
}
