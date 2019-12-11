package com.ibm.ws.install.featureUtility;

import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.InstallKernel;
import com.ibm.ws.install.InstallKernelFactory;
import com.ibm.ws.install.InstallKernelInteractive;
import com.ibm.ws.install.internal.InstallKernelImpl;
import com.ibm.ws.install.internal.InstallLogUtils;
import com.ibm.ws.install.internal.InstallUtils;
import com.ibm.ws.kernel.boot.cmdline.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class FeatureBundle {

    private String openLibertyVersion;
    private List<String> featureNames;
    private Set<String> featureJsons;
    private List<File> esaFiles;
    private final static String OPEN_LIBERTY_PRODUCT_ID = "io.openliberty";

    public FeatureBundle() throws IOException, InstallException {
        featureNames = new ArrayList<>();
        featureJsons = new HashSet<String>();
        esaFiles = new ArrayList<>();
        openLibertyVersion = getLibertyVersion();
    }

    /**
     * Parse a list of arguments and identify between feature names and esa files
     * @param args
     */
    public void parseArgs(List<String> args) throws InstallException {
        for(String arg  : args){
            // handle esa files
            if(arg.endsWith(".esa")){
                addEsaFile(new File(arg));
            } else { // handle feature name
                addFeatureName(arg);
            }
        }
    }

    public void addFeatureName(String feature) throws InstallException {
        String[] mavenCoords = feature.split(":");
        String groupId, artifactId, version, packaging;

        switch(mavenCoords.length){
            case 1: // artifactId
                groupId = "io.openliberty.features";
                artifactId = mavenCoords[0];
                version = openLibertyVersion;
                packaging = "esa";
                break;
            case 2: // groupId:artifactId
                groupId = mavenCoords[0];
                artifactId = mavenCoords[1];
                version = openLibertyVersion;
                packaging = "esa";
                break;
            case 3: // groupId:artifactId:version
                groupId = mavenCoords[0];
                artifactId = mavenCoords[1];
                version = mavenCoords[2];
                packaging = "esa";
                break;
            case 4: // groupId:artifactId:version:packaging
                groupId = mavenCoords[0];
                artifactId = mavenCoords[1];
                version = mavenCoords[2];
                packaging = mavenCoords[3];
                break;
            default:
                throw new InstallException(InstallLogUtils.Messages.INSTALL_KERNEL_MESSAGES.getMessage("ERROR_MAVEN_COORDINATE_INVALID", feature));
        }
        verifyMavenCoordinate(feature, groupId, artifactId, version, packaging);
        featureNames.add(artifactId);
        featureJsons.add(groupId);
    }


    public void addEsaFile(File esaFile) throws InstallException {
        String featureName = InstallUtils.getFeatureName(esaFile);
        esaFiles.add(esaFile);
        addFeatureName(featureName);
    }

    public void addEsaFiles(List<File> esaFiles) throws InstallException {
        for(File esaFile : esaFiles){
            addEsaFile(esaFile);
        }
    }

    public List<File> getEsaFiles(){
        return esaFiles;
    }

    public List<String> getFeatureNames(){
        return featureNames;
    }

    public Set<String> getFeatureJsons(){
        return featureJsons;
    }

    private void verifyMavenCoordinate(String feature, String groupId, String artifactId, String version, String packaging) throws InstallException {
        // check for any empty parameters
        if(groupId.isEmpty() || artifactId.isEmpty() || version.isEmpty() || packaging.isEmpty()){
            throw new InstallException(InstallLogUtils.Messages.INSTALL_KERNEL_MESSAGES.getMessage("ERROR_MAVEN_COORDINATE_INVALID", feature));
        }

        if (!version.equals(openLibertyVersion)) {
            throw new InstallException(
                    InstallLogUtils.Messages.INSTALL_KERNEL_MESSAGES.getMessage("ERROR_MAVEN_COORDINATE_WRONG_VERSION", feature, openLibertyVersion));
        }
        if(!"esa".equals(packaging)){
            throw new InstallException(InstallLogUtils.Messages.INSTALL_KERNEL_MESSAGES.getMessage("ERROR_MAVEN_COORDINATE_WRONG_PACKAGING", feature));
        }
        // block closed liberty features
        if("com.ibm.websphere.appserver.features".equals(groupId)){
            throw new InstallException(InstallLogUtils.Messages.INSTALL_KERNEL_MESSAGES.getMessage("ERROR_FAILED_TO_RESOLVE_FEATURES_FOR_OPEN_LIBERTY", feature));
        }

    }


    /**
     * Get the open liberty runtime version.
     * TODO move this to  a Utils class?
     * @throws IOException
     * @throws InstallException
     *
     */
    private String getLibertyVersion() throws IOException, InstallException {
        File propertiesFile = new File(Utils.getInstallDir(), "lib/versions/openliberty.properties");
        String openLibertyVersion = null;
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(propertiesFile)) {
            properties.load(input);
            String productId = properties.getProperty("com.ibm.websphere.productId");
            String productVersion = properties.getProperty("com.ibm.websphere.productVersion");

            if (productId.equals(OPEN_LIBERTY_PRODUCT_ID)) {
                openLibertyVersion = productVersion;
            }

        }

        if (openLibertyVersion == null) {
            // openliberty.properties file is missing or invalidly formatted
            throw new InstallException(InstallLogUtils.Messages.INSTALL_KERNEL_MESSAGES.getMessage("ERROR_COULD_NOT_DETERMINE_RUNTIME_PROPERTIES_FILE", propertiesFile.getAbsolutePath()));

        }
        return openLibertyVersion;
    }

}
