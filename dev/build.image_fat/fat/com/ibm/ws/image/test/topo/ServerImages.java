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
package com.ibm.ws.image.test.topo;

import static com.ibm.ws.image.test.topo.BuildProperties.IMAGES_PATH;
import static com.ibm.ws.image.test.topo.BuildProperties.IMAGE_PATHS;
import static com.ibm.ws.image.test.util.FileUtils.TEST_OUTPUT_PATH_ABS;
import static com.ibm.ws.image.test.util.FileUtils.extract;
import static com.ibm.ws.image.test.util.FileUtils.setPermissions;
import static com.ibm.ws.image.test.util.ProcessRunner.runJar;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.ws.image.test.util.FileUtils;

public class ServerImages {
    public static final String CLASS_NAME = ServerImages.class.getSimpleName();

    public static void log(String message) {
        System.out.println(message);
    }

    //

    public static final String IGNORE_PREFIX = null;
    public static final String IGNORE_SUFFIX = null;
    
    public static String describeFileSelector(String prefix, String suffix) {
        boolean noPrefix = ( (prefix == null) || prefix.isEmpty() );
        boolean noSuffix = ( (suffix == null) || suffix.isEmpty() );
        
        if ( noPrefix ) {
            if ( noSuffix ) {
                return "*";
            } else {
                return "*" + suffix;
            }
        } else {
            if ( noSuffix ) {
                return prefix + "*";
            } else {
                return prefix + "*" + suffix;
            }
        }
    }
    
    public static List<String> getImages(String prefix, String suffix) {
        if ( IMAGE_PATHS == null ) {
            return null;
        }
        List<String> images = null;
        for ( String imagePath : IMAGE_PATHS ) {
            int slashLoc = imagePath.lastIndexOf(File.separatorChar);
            String imageName = ( (slashLoc == -1) ? imagePath : imagePath.substring(slashLoc + 1) );
            if ( (prefix != null) && !imageName.startsWith(prefix) ) {
                continue;
            }
            if ( (suffix != null) && !imageName.endsWith(suffix) ) {
                continue;
            }
            if ( images == null ) {
                images = new ArrayList<>(1);
            }
            images.add(imagePath);
        }
        return images;
    }

    //
    
    public static class RawImages {
        public RawImages(String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }

        private final String prefix;
        
        public String getPrefix() {
            return prefix;
        }
        
        private final String suffix;

        public String getSuffix() {
            return suffix;
        }
        
        //
        
        private List<String> imagePaths;
        private boolean isSetImagePaths;
        
        public List<String> getImagePaths() {
            if ( !isSetImagePaths ) {
                imagePaths = ServerImages.getImages( getPrefix(), getSuffix() );
                isSetImagePaths = true;
            }
            return imagePaths;
        }
        
        public String getImagePath() {
            List<String> useImagePaths = getImagePaths();
            
            if ( (useImagePaths == null) || useImagePaths.isEmpty() ) {
                fail("No images: [ " + prefix + '*' + suffix + " ] under [ " + IMAGES_PATH + " ]");
                return null;

            } else if ( useImagePaths.size() > 1 ) {
                for ( String imagePath : useImagePaths ) {
                    log("Image [ " + imagePath + " ]");
                }
                fail("Too many images: [ " + prefix + '*' + suffix + " ] under [ " + IMAGES_PATH + " ]");
                return null;

            } else {
                String image = useImagePaths.get(0);
                log("Found image [ " + image + " ] [ " + prefix + '*' + suffix + " ] under [ " + IMAGES_PATH + " ]");
                return image;
            }
        }
    }

    public static RawImages getRawImages(String prefix, String suffix) {
        return new RawImages(prefix, suffix);
    }

    public static RawImages getRawImages(String prefix) {
        return getRawImages(prefix, IGNORE_SUFFIX);
    }

    public static String getImage(String prefix, String suffix) {
        return getRawImages(prefix, suffix).getImagePath();
    }

    //

    public static class RawImage {
        public RawImage(String imagePath) {
            this.imagePath = imagePath;
        }

        private final String imagePath;

        public String getImagePath() {
            return imagePath;
        }

        public String ensureImagePath() {
            String useImagePath = getImagePath();
            if ( useImagePath == null ) {
                fail("Image [ " + this + " ] was not found"); 
            } else {
                log("Image [ " + this + " ] at [ " + useImagePath + " ]");
            }
            return useImagePath;
        }

        private boolean isSetImageFile;
        private File imageFile;
        private long imageLength;

        public File getImageFile() {
            if ( !isSetImageFile ) {
                String imagePath = ensureImagePath();
                if ( imagePath != null ) {
                    imageFile = new File(imagePath);
                    imageLength = imageFile.length();
                }
                isSetImageFile = true;
            }
            return imageFile;
        }

        public long getImageLength() {
            getImageFile();
            return imageLength;
        }

        public File ensureImageFile() {
            ensureImagePath();
            return getImageFile();
        }

        public String extract(String extractPath) throws IOException {
            FileUtils.extract( ensureImagePath(), extractPath );

            String binPath = extractPath + "/wlp/bin";
            setPermissions(binPath, "+x");

            return extractPath;
        }

        // Use in open-liberty
        public ServerInstallation install(String extractPath) throws IOException {
            return new ServerInstallation( extract(extractPath) );
        }

        // Use in WS-CD
        public ServerInstallation installFromJar(String installationPath) throws Exception {
            runJar( ensureImagePath(), "--acceptLicense", installationPath);
            return new ServerInstallation(installationPath);
        }
    }

    //

    public static RawImage getRawImage(String prefix) {
        return getRawImage(prefix, IGNORE_SUFFIX);
    }

    public static RawImage getRawImage(String prefix, String suffix) {
        return new RawImage( getImage(prefix, suffix) );
    }

    public static ServerInstallation install(String imagePath, String extractPath) throws Exception {
        return new RawImage(imagePath).install(extractPath);
    }

    public static ServerInstallation install(String prefix, String suffix, String extractPath) throws Exception {
        return getRawImage(prefix, suffix).install(extractPath);
    }    

    public static ServerInstallation installFromJar(String prefix, String installationPath) throws Exception {
        return getRawImage(prefix).installFromJar(installationPath);
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
    
    public static enum ImageType { // WS-CD
        KERNEL_ZIP("wlp-kernel", ".zip"),

        CORE_LIC("wlp-core-license", ".jar"),
        CORE_ALL("wlp-core-all", ".jar"),

        BASE_LIC("wlp-base-license", ".jar"),
        BASE_ALL("wlp-base-all", ".jar"),

        ND_LIC("wlp-nd-license", ".jar"),
        ND_ALL("wlp-nd-all", ".jar"),

        DEVELOPERS_IPLA_ALL("wlp-developers-ipla-all", ".jar"),

        FEATURE_REPO("wlp-featureRepo", ".zip");

        public static void ensureImageFiles() {
            for ( ImageType imageType : ImageType.values() ) {
                imageType.ensureImageFile();
            }
        }

        private ImageType(String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
            this.description = describeFileSelector(prefix, suffix);

            RawImages rawImages = new RawImages(prefix, suffix);
            this.rawImage = new RawImage( rawImages.getImagePath() );
        }

        private final String prefix;

        public String getPrefix() {
            return prefix;
        }

        private final String suffix;

        public String getSuffix() {
            return suffix;
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

        public String ensureImagePath() {
            return getRawImage().ensureImagePath();
        }

        public File getImageFile() {
            return getRawImage().getImageFile();
        }

        public long getImageLength() {
            return getRawImage().getImageLength();
        }

        public File ensureImageFile() {
            return getRawImage().ensureImageFile();
        }

        public FeatureRepository extract(String extractPath) throws Exception {
            return new FeatureRepository( getRawImage().extract(extractPath) );
        }
    };

    //

    public static File[] listMavenFeatures(String path) {
        return (new File(path)).listFiles( ServerImages::isMavenFeature );
    }

    public static boolean isMavenFeature(File parent, String name) {
        return ( name.startsWith("features-") && name.endsWith(".json") );
    }
    
    // WS-CD version of maven archive tests
    
    public static boolean IS_SET_MAVEN_FEATURES;
    public static String MAVEN_ARCHIVE_PATH;
    public static String MAVEN_EXTRACT_PATH;
    public static List<File> MAVEN_FEATURES;
    public static List<String> MAVEN_FEATURE_NAMES;

    public static void setMavenFeatures() throws IOException {
        if ( IS_SET_MAVEN_FEATURES ) {
            return;
        } else {
            IS_SET_MAVEN_FEATURES = true;
        }

        String mavenArchivePath = getImage("mavenArtifact", ".zip");
        log("Maven archive [ " + mavenArchivePath + " ]");
        String mavenExtractPath = TEST_OUTPUT_PATH_ABS + "/mavenArtifact";
        log("Maven extract [ " + mavenExtractPath + " ]");
        extract(mavenArchivePath, mavenExtractPath);

        String mavenFeaturesPath = mavenExtractPath + "/com/ibm/websphere/appserver/features/features";
        log("Maven features [ " + mavenFeaturesPath + " ]");
        File[] mavenFeatures = ServerImages.listMavenFeatures(mavenFeaturesPath);
        log("Maven features count [ " + mavenFeatures.length + " ]");
        
        MAVEN_ARCHIVE_PATH = mavenArchivePath;
        MAVEN_EXTRACT_PATH = mavenExtractPath;

        List<File> mavenFeaturesList = new ArrayList<>( mavenFeatures.length );
        List<String> mavenFeatureNames = new ArrayList<>( mavenFeatures.length );
        for ( File feature : mavenFeatures ) {
            mavenFeaturesList.add(feature);
            mavenFeatureNames.add( feature.getName() );
        }

        MAVEN_FEATURES = mavenFeaturesList;
        MAVEN_FEATURE_NAMES = mavenFeatureNames;
    }

    // open-liberty version of maven archive tests
    
    public static boolean IS_SET_OL_MAVEN_FEATURES;
    public static String OL_MAVEN_ARCHIVE_PATH;
    public static String OL_MAVEN_EXTRACT_PATH;
    public static List<File> OL_MAVEN_FEATURES;
    public static List<String> OL_MAVEN_FEATURE_NAMES;

    public static void setOLMavenFeatures() throws IOException {
        if ( IS_SET_OL_MAVEN_FEATURES ) {
            return;
        } else {
            IS_SET_OL_MAVEN_FEATURES = true;
        }

        String olMavenArchivePath = getImage("openliberty-MavenArtifact", ".zip");
        log("Open liberty Maven archive [ " + olMavenArchivePath + " ]");
        String olMavenExtractPath = TEST_OUTPUT_PATH_ABS + "/ol-mavenArtifact";
        log("Open liberty Maven extract [ " + olMavenExtractPath + " ]");
        extract(olMavenArchivePath, olMavenExtractPath);

        String olMavenFeaturesPath = olMavenExtractPath + "/io/openliberty/features/features";
        log("Open liberty Maven features [ " + olMavenFeaturesPath + " ]");
        File[] olMavenFeatures = ServerImages.listMavenFeatures(olMavenFeaturesPath); 
        log("Open liberty Maven features count [ " + olMavenFeatures.length + " ]");        

        OL_MAVEN_ARCHIVE_PATH = olMavenArchivePath;
        OL_MAVEN_EXTRACT_PATH = olMavenExtractPath;

        List<File> olMavenFeaturesList = new ArrayList<>( olMavenFeatures.length );
        List<String> olMavenFeatureNames = new ArrayList<>( olMavenFeatures.length );
        for ( File feature : olMavenFeatures ) {
            olMavenFeaturesList.add(feature);
            olMavenFeatureNames.add( feature.getName() );
        }

        OL_MAVEN_FEATURES = olMavenFeaturesList;
        OL_MAVEN_FEATURE_NAMES = olMavenFeatureNames;
    }
}
