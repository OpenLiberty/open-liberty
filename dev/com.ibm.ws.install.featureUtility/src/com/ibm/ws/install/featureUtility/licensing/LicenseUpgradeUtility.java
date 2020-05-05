package com.ibm.ws.install.featureUtility.licensing;

import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.InstallKernelFactory;
import com.ibm.ws.install.InstallKernelInteractive;
import com.ibm.ws.install.InstallLicense;
import com.ibm.ws.install.internal.InstallUtils;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.productinfo.DuplicateProductInfoException;
import com.ibm.ws.kernel.productinfo.ProductInfoParseException;
import com.ibm.ws.kernel.productinfo.ProductInfoReplaceException;
import wlp.lib.extract.SelfExtract;

import java.io.*;
import java.util.*;

public class LicenseUpgradeUtility {

    private String productId, productOwner, productVersion, productName, productReplaces, productInstallType, productEdition, productLicenseType;
    private String wlpDir;
    private List<String> features;
    private boolean acceptLicense;
    Set<InstallLicense> featureLicenses = Collections.emptySet();
    static final private String UNSPECIFIED_LICENSE_TYPE = "UNSPECIFIED";


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
        this.features = builder.features;
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

    public boolean handleLicenses() {
        if(acceptLicense) {
            return true;
        }

        InstallKernelInteractive installKernel = InstallKernelFactory.getInteractiveInstance();
        try {
            installKernel.resolve(this.features, false);
            featureLicenses = installKernel.getFeatureLicense(Locale.getDefault());
            Set<InstallLicense> licenseToAccept = InstallUtils.getLicenseToAccept(featureLicenses);
            if (!acceptLicense) {
                if (!licenseToAccept.isEmpty()) {
                    showFeaturesForLicenseAcceptance(licenseToAccept);
                }
                for (InstallLicense license : licenseToAccept) {
                    if (!!!handleLicenseAcceptance(license)) {
                        return false;
                    }
                }
            }
        } catch (ProductInfoParseException e) {
            System.out.println(e.getMessage());
            return false;
        } catch (DuplicateProductInfoException e) {
            System.out.println(e.getMessage());
            return false;
        } catch (ProductInfoReplaceException e) {
            System.out.println(e.getMessage());
            return false;
        } catch (InstallException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean handleLicenseAcceptance(InstallLicense licenseToAccept) {
        //
        // Display license requirement
        //
        SelfExtract.wordWrappedOut(SelfExtract.format("licenseStatement", new Object[] { licenseToAccept.getProgramName(), licenseToAccept.getName() }));
        System.out.println();

        if (!obtainLicenseAgreement(licenseToAccept)) {
            return false;
        }

        return true;
    }

    private boolean obtainLicenseAgreement(InstallLicense license) {
        // Prompt for word-wrapped display of license agreement & information
        boolean view;

        if (featureLicenses.size() > 1) {
            SelfExtract.wordWrappedOut(SelfExtract.format("showAgreement", "--viewLicenseAgreement"));
            view = SelfExtract.getResponse(SelfExtract.format("promptAgreement"), "", "xX");
            if (view) {
                System.out.println(license.getAgreement());
                System.out.println();
            }

            SelfExtract.wordWrappedOut(SelfExtract.format("showInformation", "--viewLicenseInfo"));
            view = SelfExtract.getResponse(SelfExtract.format("promptInfo"), "", "xX");
            if (view) {
                System.out.println(license.getInformation());
                System.out.println();
            }

        } else {
            System.out.println();
            System.out.println(license.getAgreement());
            System.out.println();
        }

        System.out.println();
        SelfExtract.wordWrappedOut(SelfExtract.format("licenseOptionDescription"));
        System.out.println();

        boolean accept = SelfExtract.getResponse(SelfExtract.format("licensePrompt", new Object[] { "[1]", "[2]" }),
                "1", "2");
        System.out.println();
        return accept;
    }

    private void showFeaturesForLicenseAcceptance(Set<InstallLicense> licenseToAccept) {
        Set<String> featuresToAccept = new HashSet<String>();
        StringBuffer buf = new StringBuffer();
        for (InstallLicense license : licenseToAccept) {
            String featureToMatch = getFeaturesForLicense(license);
            if (!featureToMatch.isEmpty()) {
                featuresToAccept.add(featureToMatch);
                buf.append(featureToMatch);
            }
        }
        String featuresToAcceptMsg = buf.toString().trim();
//        logger.info("Accept license for: " + featuresToAcceptMsg);
    }

    private String getFeaturesForLicense(InstallLicense license) {
        StringBuffer buf = new StringBuffer();
        for (String feature : license.getFeatures()) {
            for (String featureToMatch : features) {
                if (feature.contains(featureToMatch)) {
                    buf.append(featureToMatch + " ");
                }
            }
        }
        String matchedFeatures = buf.toString();
        return matchedFeatures;
    }



    public static class LicenseUpgradeUtilityBuilder{
        String productId, productOwner, productVersion, productName, productReplaces, productInstallType, productEdition, productLicenseType;
        String wlpDir;
        List<String> features;

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

        public LicenseUpgradeUtilityBuilder setFeatures(List<String> features){
            this.features = features;
            return  this;
        }

        public LicenseUpgradeUtility build(){
            return new LicenseUpgradeUtility(this);
        }
    }


}
