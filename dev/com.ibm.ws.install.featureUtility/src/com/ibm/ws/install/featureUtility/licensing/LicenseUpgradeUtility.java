package com.ibm.ws.install.featureUtility.licensing;

import com.ibm.ws.install.internal.InstallUtils;
import com.ibm.ws.kernel.boot.cmdline.Utils;

import java.io.*;
import java.util.Properties;

public class LicenseUpgradeUtility {

    private String productId, productOwner, productVersion, productName, productReplaces, productInstallType, productEdition, productLicenseType;
    private String wlpDir;

    final String WEBSPHERE_PROPERTIES_FILE_DIR = "/lib/versions/";
    final String WEBSPHERE_PROPERTIES_FILE_NAME = "WebsphereApplicationServer.properties";

    public LicenseUpgradeUtility(LicenseUpgradeUtilityBuilder builder){
        this.productId = builder.productId;
        this.productOwner = builder.productOwner;
        this.productVersion = builder.productVersion;
        this.productName = builder.productName != null ? builder.productName : "Websphere Application Server";
        this.productReplaces = builder.productReplaces != null ? builder.productReplaces : "io.openliberty";
        this.productInstallType = builder.productInstallType;
        this.productEdition = builder.productEdition;
        this.productLicenseType = builder.productLicenseType;

        this.wlpDir = builder.wlpDir != null ? builder.wlpDir : Utils.getInstallDir().getAbsolutePath();

    }

    /**
     * Generates the WebsphereApplicationServer.properties file for license upgrade
     */
    public void generateWebspherePropertiesFile(){
        String propertiesDir = wlpDir + WEBSPHERE_PROPERTIES_FILE_DIR + WEBSPHERE_PROPERTIES_FILE_NAME;
        Properties prop = new Properties();
        InputStream input = null;

        try (OutputStream os = new FileOutputStream(propertiesDir)){
            input = new FileInputStream(propertiesDir);
            prop.load(input);
            prop.setProperty("com.ibm.websphere.productId", productId);
            prop.setProperty("com.ibm.websphere.productReplaces", productReplaces);
            prop.setProperty("com.ibm.websphere.productOwner", productOwner);
            prop.setProperty("com.ibm.websphere.productVersion", productVersion);
            prop.setProperty("com.ibm.websphere.productNane", productName);
            prop.setProperty("com.ibm.websphere.productInstallType", productInstallType);
            prop.setProperty("com.ibm.websphere.productEdition", productEdition);
            prop.setProperty("com.ibm.websphere.productLicense", productLicenseType);
            prop.store(os, null);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public static class LicenseUpgradeUtilityBuilder{
        String productId, productOwner, productVersion, productName, productReplaces, productInstallType, productEdition, productLicenseType;
        String wlpDir;

        public LicenseUpgradeUtilityBuilder setProductId(String productId){
            this.productId = productId;
            return this;
        }

        public LicenseUpgradeUtilityBuilder setProductOwner(String productOwner) {
            this.productOwner = productOwner;
            return this;
        }

        public LicenseUpgradeUtilityBuilder setProductVersion(String productVersion) {
            this.productVersion = productVersion;
            return this;
        }

        public LicenseUpgradeUtilityBuilder setProductName(String productName) {
            this.productName = productName;
            return this;
        }

        public LicenseUpgradeUtilityBuilder setProductReplaces(String productReplaces) {
            this.productReplaces = productReplaces;
            return this;
        }

        public LicenseUpgradeUtilityBuilder setProductInstallType(String productInstallType) {
            this.productInstallType = productInstallType;
            return this;
        }

        public LicenseUpgradeUtilityBuilder setProductEdition(String productEdition) {
            this.productEdition = productEdition;
            return this;
        }

        public LicenseUpgradeUtilityBuilder setProductLicenseType(String productLicenseType) {
            this.productLicenseType = productLicenseType;
            return this;
        }

        public LicenseUpgradeUtilityBuilder setWlpDir(String wlpDir){
            this.wlpDir = wlpDir;
            return this;
        }

        public LicenseUpgradeUtility build(){
            return new LicenseUpgradeUtility(this);
        }
    }


}
