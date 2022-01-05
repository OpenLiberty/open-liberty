/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.image.test.topo;

import static com.ibm.ws.image.test.util.FileUtils.FLATTEN;
import static com.ibm.ws.image.test.util.FileUtils.LOCAL_TMP_PATH_ABS;
import static com.ibm.ws.image.test.util.FileUtils.ensureNonexistence;
import static com.ibm.ws.image.test.util.FileUtils.extract;
import static com.ibm.ws.image.test.util.FileUtils.load;
import static com.ibm.ws.image.test.util.FileUtils.normalize;
import static com.ibm.ws.image.test.util.FileUtils.setPermissions;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ibm.ws.image.test.util.FileUtils;

@SuppressWarnings("null")
public class ServerImages {
    public static final String CLASS_NAME = ServerImages.class.getSimpleName();

    public static void log(String message) {
        System.out.println(message);
    }

    //

    public static final String IMAGES_PROPERTY_NAME = "image.output.upload.dir";
    public static final String IMAGES_DEFAULT = "../build.image/output/upload/externals/installables";

    public static final String IMAGES_PATH;
    public static final String[] IMAGE_PATHS;

    static {
        String useImagesPath = System.getProperty(IMAGES_PROPERTY_NAME, IMAGES_DEFAULT);
        File useImagesDir = new File(useImagesPath);
        useImagesPath = normalize( useImagesDir.getAbsolutePath() );

        if ( !useImagesDir.exists() ) {
            fail("Image directory [ " + useImagesPath + " ] does not exist");
        } else if ( !useImagesDir.isDirectory() ) {
            fail("Image directory [ " + useImagesPath + " ] is not a directory");
        } else {
            log("Image directory [ " + useImagesPath + " ]");
        }
        
        String[] useImagePaths = useImagesDir.list( ServerImages::isImage );
        if ( useImagePaths == null ) {
            fail("Image directory [ " + useImagesPath + " ] could not be accessed");
        } else if ( useImagePaths.length == 0 ) {
            fail("Image directory [ " + useImagesPath + " ] is empty");
        }

        if ( File.separatorChar == '\\' ) {
            normalize(useImagePaths);
        }

        log("Images:");
        for ( String imagePath : useImagePaths ) {
            log("[ " + imagePath + " ]");
        }

        IMAGES_PATH = useImagesPath;
        IMAGE_PATHS = useImagePaths;
    }

    // There are too many of these to have an explicit list.
    public static boolean isImage(File parent, String name) {
        return ( name.endsWith(".jar") || name.endsWith(".zip") );
    }

    public static final String IGNORE_PREFIX = null;
    public static final String IGNORE_SUFFIX = null;
    
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
    
    //

    public static RawImages getRawImages(String prefix, String suffix) {
        return new RawImages(prefix, suffix);
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

        public ServerInstallation install(String extractPath) throws IOException {
            return new ServerInstallation( extract(extractPath) );
        }
    }

    //

    public static RawImage getRawImage(String prefix, String suffix) {
        return new RawImage( getImage(prefix, suffix) );
    }

    public static ServerInstallation install(String imagePath, String extractPath) throws IOException {
        return new RawImage(imagePath).install(extractPath);
    }

    public static ServerInstallation install(String prefix, String suffix, String extractPath)
        throws IOException {
        return getRawImage(prefix, suffix).install(extractPath);
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
    
    public static enum ImageType {
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
            RawImages rawImages = new RawImages(prefix, suffix);
            this.rawImage = new RawImage( rawImages.getImagePath() );
        }

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

        public String extract(String extractPath) throws IOException {
            return getRawImage().extract(extractPath);
        }
    };
    
    //
    
    public static final String GA_FEATURES_PATH = "../prereq.published/build/gaFeatures.txt";
    public static final String GA_FEATURES_PATH_ABS;
    public static final List<String> GA_FEATURE_NAMES;

    // From WS-CD-Open 22.002:
    //
    // betaFeatures-wasliberty.txt
    // betaFeatures.txt
    // gaFeatures-wasliberty.txt
    // gaFeatures.txt

    static {
        File gaFeaturesFile = new File(GA_FEATURES_PATH);
        String gaFeaturesAbsPath = gaFeaturesFile.getAbsolutePath();

        if ( !gaFeaturesFile.exists() ) {
            fail("GA features file [ " + gaFeaturesAbsPath + " ] does not exist");
        }

        List<String> gaFeatures;
        try {
            gaFeatures = load(gaFeaturesFile);
        } catch ( IOException e ) {
            fail("Failed to load GA features [ " + gaFeaturesAbsPath + " ]: " + e.getMessage());
            throw new RuntimeException(e);
        }

        GA_FEATURES_PATH_ABS = gaFeaturesAbsPath;
        GA_FEATURE_NAMES = gaFeatures;
    }
    
    //

    private static boolean failedRepoPath;
    private static String repoPath;

    public static String setupRepository() throws IOException {
        if ( failedRepoPath ) {
            fail("Prior failure to process repo-*.zip");
        } else if ( repoPath != null ) {
            return repoPath;
        }

        try { 
            log("Locating repository (repo-*.zip) in [ " + IMAGES_PATH + " ]");            

            List<String> images = getImages("repo.", ".zip");
        
            if ( (images != null) && !images.isEmpty() ) {        
                Iterator<String> useImages = images.iterator();
                while ( useImages.hasNext() ) {
                    String image = useImages.next();
                    int nameLoc = image.lastIndexOf('/');
                    String name = ( (nameLoc == -1) ? image : image.substring(nameLoc + 1) );
                    if ( name.contains("beta") || name.contains("json") ) {
                        log("Ignoring [ beta ] or [ json ] image: [ " + image + " ]");
                        useImages.remove();
                    }
                }
            }
        
            String image;
            if ( (images == null) || images.isEmpty() ) {
                fail("Failed to find repo-*.zip in [ " + IMAGES_PATH + " ]");
                image = null;
            } else if ( images.size() > 1 ) {
                fail("Too many repo-*.zip in [ " + IMAGES_PATH + " ]");
                image = null;            
            } else {
                image = images.get(0);
            }
            log("Located repository [ " + image + " ]");

            String useRepoPath = LOCAL_TMP_PATH_ABS + "/repo";
            ensureNonexistence(useRepoPath);
            extract(image, useRepoPath, ".esa", FLATTEN);

            repoPath = useRepoPath;

        } catch ( Throwable th ) {
            failedRepoPath = true;
            throw th;
        }

        return repoPath;
    }

    private static boolean failedFeatureRepoPath;
    private static String featureRepoPath;

    public static String getFeatureRepositoryPath() {
        return featureRepoPath;
    }

    public static String setupFeatureRepo() throws IOException {
        if ( failedFeatureRepoPath ) {
            fail("Prior failure to process wlp-featureRepo-*.zip");
        } else if ( featureRepoPath != null ) {
            return featureRepoPath;
        }

        try {
            log("Locating feature repository  [ wlp-featureRepo-*.zip ] in [ " + IMAGES_PATH + " ]");
            String image = getImage("wlp-featureRepo-", ".zip");
            log("Located feature repository [ " + image + " ]");

            String useFeatureRepoPath = LOCAL_TMP_PATH_ABS + "/featureRepo";
            ensureNonexistence(useFeatureRepoPath);
            extract(image, useFeatureRepoPath);

            featureRepoPath = useFeatureRepoPath;

        } catch ( Throwable th ) {
            failedFeatureRepoPath = true;
            throw th;
        }

        return featureRepoPath;
    }
    
    //
    
    public static final String INSTALL_MAP_PREFIX = "com.ibm.ws.install.map";
    public static final String INSTALL_MAP_SUFFIX = ".jar";

    public static boolean isInstallMap(File parent, String name) {
        return ( name.startsWith(INSTALL_MAP_PREFIX) && name.endsWith(INSTALL_MAP_SUFFIX) );        
    }
    
    public static String getMapVersion(String name) {
        String version = FileUtils.removeEnds(name, INSTALL_MAP_PREFIX, INSTALL_MAP_SUFFIX);
        return ( version.isEmpty() ? null : version );
    }

    private static Map<String, File> installMapJars;

    private static File getInstallMapJar(String installationPath) {
        if ( installMapJars == null ) {
            installMapJars = new HashMap<>();
        }
        return installMapJars.computeIfAbsent( installationPath, ServerImages::locateInstallMapJar );
    }

    private static File locateInstallMapJar(String installationPath) {
        String libPath = installationPath + "/lib";
        File[] installMapJars = (new File(libPath)).listFiles( ServerImages::isInstallMap );
        if ( installMapJars == null ) {
            fail("Failed to list library folder [ " + libPath + " ]");
            return null;
        } else if ( installMapJars.length == 0 ) {
            fail("Library folder has no install map jars [ " + libPath + " ]");
            return null;            
        }

        File latestJar = null;
        String latestVersion = null;

        for ( File nextJar : installMapJars ) {
            String nextName = nextJar.getName();
            String nextVersion = getMapVersion(nextName);

            if ( nextVersion == null ) {
                latestJar = nextJar;
                latestVersion = null;
                break; // The null version is highest.
            }

            if ( latestJar == null ) {
                latestJar = nextJar;
                latestVersion = nextVersion;
                continue;
            }
            
            // 'latestVersion' cannot be null here.

            if ( nextVersion.compareTo(latestVersion) > 0 ) {
                latestJar = nextJar;
                latestVersion = nextVersion;
            }
        }

        log("Install map jar [ " + latestJar.getPath() + " ] [ " + latestVersion + " ]");
        return latestJar;
    }    

    public static final String INSTALL_MAP_CLASS_NAME = "com.ibm.ws.install.map.InstallMap";

    public static Map<String, Object> getInstallMap(String installationPath) throws Exception {
        File installMapJar = getInstallMapJar(installationPath);

        Map<String, Object> installMap = AccessController.doPrivileged(
            new PrivilegedExceptionAction<Map<String, Object>>() {
                @SuppressWarnings({ "unchecked", "resource" })
                @Override
                public Map<String, Object> run() throws Exception {
                    URL installMapURL = installMapJar.toURI().toURL();
                    URL[] installMapURLs = new URL[] { installMapURL };
                    ClassLoader installMapLoader = new URLClassLoader(installMapURLs, null);

                    Class<Map<String, Object>> installMapClass = (Class<Map<String, Object>>)
                        installMapLoader.loadClass(INSTALL_MAP_CLASS_NAME);

                    return installMapClass.newInstance();
                }
            }
        );

        log("install.kernel.init.code [ " + installMap.get("install.kernel.init.code") + " ]");
        log("install.kernel.init.error.message [ " + installMap.get("install.kernel.init.error.message") + " ]");

        String installMapSubpath =
            installMapJar.getParentFile().getName() + '/' + installMapJar.getName();

        installMap.put("runtime.install.dir", installationPath);
        installMap.put("install.map.jar", installMapSubpath);

        String installMapTmpPath = installationPath + "/usr/tmp";
        installMap.put( "target.user.directory", new File(installMapTmpPath) );

        return installMap;
    }

    public static List<String> resolveFeatures(
        String installationPath,
        List<File> availableFeatures, List<String> features) throws Exception {

        log("Resolving features");
        for ( String feature : features ) {
            log("  [ " + feature + " ]");
        }
        log("Available features [ " + availableFeatures.size() + " ]");

        Map<String, Object> installMap = getInstallMap(installationPath);
        installMap.put("install.local.esa", true);
        installMap.put("single.json.file", availableFeatures);
        installMap.put("license.accept", true);

        if ( installMap.put("features.to.resolve", features) != null ) {
            String errorMessage = (String) installMap.get("action.error.message");
            log("Feature resolution failure [ action.error.message ] [ " + errorMessage + " ]");
            fail(errorMessage);
            return null;
        }

        @SuppressWarnings("unchecked")
        Collection<String> resolvedFeatures = (Collection<String>)
            installMap.get("action.result");

        List<String> useResolvedFeatures = new ArrayList<String>();;
        log("Resolved feature: ");
        for ( String featureResolvant : resolvedFeatures ) {
            int firstColon = featureResolvant.indexOf(":");
            int secondColon = featureResolvant.indexOf(":", firstColon + 1);
            String feature = featureResolvant.substring(firstColon + 1, secondColon);
            if ( !feature.startsWith("com.ibm.") ) {
                feature = "com.ibm.websphere.appserver." + feature;
            }
            useResolvedFeatures.add(feature);
            log("  [ " + feature + " ]");
        }

        Collections.sort(useResolvedFeatures);

        return useResolvedFeatures;
    }

    //

    public static File[] listMavenFeatures(String path) {
        return (new File(path)).listFiles( ServerImages::isMavenFeature );
    }

    public static boolean isMavenFeature(File parent, String name) {
        return ( name.startsWith("features-") && name.endsWith(".json") );
    }
    
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
        String mavenExtractPath = LOCAL_TMP_PATH_ABS + "/mavenArtifact";
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
        String olMavenExtractPath = LOCAL_TMP_PATH_ABS + "/ol-mavenArtifact";
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
