package com.ibm.ws.install.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.InstallLicense;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.kernel.boot.cmdline.Utils;

import wlp.lib.extract.SelfExtract;
import wlp.lib.extract.SelfExtractUtils;

public class LicenseUpgradeUtility {

    private final String productId, productOwner, productVersion, productName, productReplaces, productInstallType, productEdition, productLicenseType;
    private final String wlpDir;
    private final List<String> features;
    private final boolean acceptLicense;
    Set<InstallLicense> featureLicenses = Collections.emptySet();
    static final private String UNSPECIFIED_LICENSE_TYPE = "UNSPECIFIED";

    final String WEBSPHERE_PROPERTIES_FILE_DIR = "/lib/versions/";
    final String WEBSPHERE_TAGS_DIR = WEBSPHERE_PROPERTIES_FILE_DIR + "tags/";
    final String WEBSPHERE_PROPERTIES_FILE_NAME = "WebsphereApplicationServer.properties";
    final String WEBSPHERE_LICENSE_FILE_DIR = "/lafiles/com.ibm.websphere.appserver/";
    final String OL_LICENSE_FILE_DIR = "/lafiles/io.openliberty/";
    final String OL_LICENSE = "LICENSE";
    final String OL_NOTICES = "NOTICES";
    final String OL_TAG_PREFIX = "openliberty";
    private String name;
    private final String programName;
    private static final String PROGRAM_NAME = "Program Name (Program Number):";
    private static final String LA_PREFIX = "LA_";
    private static final String LI_PREFIX = "LI_";

    public LicenseUpgradeUtility(LicenseUpgradeUtilityBuilder builder) {
        this.productId = builder.productId;
        this.productOwner = builder.productOwner;
        this.productVersion = builder.productVersion;
        this.productName = builder.productName != null ? builder.productName : "Websphere Application Server";
        this.productReplaces = builder.productReplaces != null ? builder.productReplaces : "io.openliberty";
        this.productInstallType = builder.productInstallType;
        this.productEdition = builder.productEdition;
        this.productLicenseType = builder.productLicenseType;
        this.name = null;
        this.programName = null;

        this.wlpDir = builder.wlpDir != null ? builder.wlpDir : Utils.getInstallDir().getAbsolutePath();
        this.features = builder.features;
        this.acceptLicense = builder.acceptLicense;
    }

    /**
     * Generates the WebsphereApplicationServer.properties file for license upgrade
     */
    public void generateWebspherePropertiesFile() {
        String propertiesDir = wlpDir + WEBSPHERE_PROPERTIES_FILE_DIR + WEBSPHERE_PROPERTIES_FILE_NAME;
        Properties prop = new Properties();
        InputStream input = null;

        try (OutputStream os = new FileOutputStream(propertiesDir)) {
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

    /**
     * @return
     * @throws IOException
     * @throws InstallException
     */
    public boolean handleLicenses(String featureList) throws InstallException {
        if (acceptLicense) {
            return true;
        }
        Locale def = Locale.getDefault();
        String laFilePath = getLicenseFilePath(def, true);
        String liFilePath = getLicenseFilePath(def, false);
        processLicense(liFilePath);
        if (laFilePath == null || liFilePath == null) {
            System.out.println("unable to find license files in folder: " + wlpDir + WEBSPHERE_LICENSE_FILE_DIR);
            //TODO throw an error
        }
        if (!!!handleLicenseAcceptance(laFilePath, liFilePath, featureList)) {
            return false;
        }
        return true;
    }

    /**
     * @param def
     * @param b
     * @return
     */
    private String getLicenseFilePath(Locale desiredLocale, boolean isAgreement) {
        String result = null;

        Locale desiredLanguage = new Locale(desiredLocale.getLanguage());

        File folder = new File(wlpDir + WEBSPHERE_LICENSE_FILE_DIR);
        File[] listOfFiles = folder.listFiles();
        String enFilePath = "";
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String filename = listOfFiles[i].getName();
                String filepath = wlpDir + WEBSPHERE_LICENSE_FILE_DIR + filename;
                if (isAgreement) {
                    if (filename.startsWith(LA_PREFIX)) {
                        String loca = filename.replace(LA_PREFIX, "");
                        Locale locla = new Locale(loca);
                        if (loca.contains("_")) {
                            String[] locSplit = loca.split("_");
                            locla = new Locale(locSplit[0], locSplit[1]);
                        }
                        if (locla.equals(desiredLocale)) {
                            return filepath;
                        } else if (locla.equals(desiredLanguage)) {
                            result = filepath;
                        } else if (locla.equals(Locale.ENGLISH)) {
                            enFilePath = filepath;
                        }
                    }
                } else {
                    if (filename.startsWith(LI_PREFIX)) {
                        String loci = filename.replace(LI_PREFIX, "");
                        Locale locli = new Locale(loci);
                        if (loci.contains("_")) {
                            String[] locSplit = loci.split("_");
                            locli = new Locale(locSplit[0], locSplit[1]);
                        }
                        if (locli.equals(desiredLocale)) {
                            return filepath;
                        } else if (locli.equals(desiredLanguage)) {
                            result = filepath;
                        } else if (locli.equals(Locale.ENGLISH)) {
                            enFilePath = filepath;
                        }
                    }
                }

            }
        }
        if (result == null && !enFilePath.isEmpty()) {
            return enFilePath;
        }
        return result;
    }

    private boolean handleLicenseAcceptance(String laFilePath, String liFilePath, String featureList) {
        //
        // Display license requirement
        //
        SelfExtract.wordWrappedOut(SelfExtract.format("licenseStatement", new Object[] { featureList, name }));
        System.out.println();
        if (!obtainLicenseAgreement(laFilePath, liFilePath)) {
            return false;
        }

        return true;
    }

    private boolean obtainLicenseAgreement(String laFilePath, String liFilePath) {
        // Prompt for word-wrapped display of license agreement & information
        boolean view;

        //SelfExtract.wordWrappedOut(SelfExtract.format("showAgreement", "--viewLicenseAgreement"));
        view = SelfExtract.getResponse(SelfExtract.format("promptAgreement"), "", "xX");
        if (view) {
            SelfExtract.wordWrappedOut(getStrFromFile(laFilePath));
            System.out.println();
        }

        //SelfExtract.wordWrappedOut(SelfExtract.format("showInformation", "--viewLicenseInfo"));
        view = SelfExtract.getResponse(SelfExtract.format("promptInfo"), "", "xX");
        if (view) {
            SelfExtract.wordWrappedOut(getStrFromFile(liFilePath));
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

    /**
     * @param laFilePath
     * @return
     */
    private String getStrFromFile(String filePath) {
        File file = new File(filePath);
        StringBuffer sb = new StringBuffer();
        BufferedReader reader = null;
        try {
            InputStream is = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(is, "UTF-16"));
            for (String line; (line = reader.readLine()) != null;) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            return "";
        } finally {
            SelfExtractUtils.tryToClose(reader);
        }
        return sb.toString();
    }

    public static class LicenseUpgradeUtilityBuilder {
        String productId, productOwner, productVersion, productName, productReplaces, productInstallType, productEdition, productLicenseType;
        String wlpDir;
        Boolean acceptLicense;
        List<String> features;

        public LicenseUpgradeUtilityBuilder setProductId(String productId) {
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

        public LicenseUpgradeUtilityBuilder setWlpDir(String wlpDir) {
            this.wlpDir = wlpDir;
            return this;
        }

        public LicenseUpgradeUtilityBuilder setFeatures(List<String> features) {
            this.features = features;
            return this;
        }

        public LicenseUpgradeUtilityBuilder setAcceptLicense(Boolean acceptLicense) {
            this.acceptLicense = acceptLicense;
            return this;
        }

        public LicenseUpgradeUtility build() {
            return new LicenseUpgradeUtility(this);
        }
    }

    private void processLicense(String filePath) throws InstallException {
        File file = new File(filePath);
        StringBuffer sb = new StringBuffer();
        BufferedReader reader = null;
        try {
            InputStream is = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(is, "UTF-16"));
            boolean isProgramName = false;
            int firstTenLinesCounter = 0;
            for (String line; (line = reader.readLine()) != null;) {
                if (firstTenLinesCounter < 6) {
                    if (line.contains("License Agreement")) {
                        name = line.replaceFirst("\\d\\.", "").trim();
                    }
                    firstTenLinesCounter += 1;
                }
            }
        } catch (IOException e) {
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_FAILED_TO_READ_LICENSE", filePath));
        } finally {
            SelfExtractUtils.tryToClose(reader);
        }
    }

    /**
     * @throws InstallException
     *
     */
    public void handleOLLicense() throws InstallException {
        File oldLicense = new File(wlpDir + File.separator + OL_LICENSE);
        File oldNotices = new File(wlpDir + File.separator + OL_NOTICES);
        File laDir = new File(wlpDir + OL_LICENSE_FILE_DIR);
        laDir.mkdirs();
        oldLicense.renameTo(new File(wlpDir + OL_LICENSE_FILE_DIR + OL_LICENSE));
        oldNotices.renameTo(new File(wlpDir + OL_LICENSE_FILE_DIR + OL_NOTICES));
        File tagsFolder = new File(wlpDir + WEBSPHERE_TAGS_DIR);
        File[] listOfFiles = tagsFolder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].getName().toLowerCase().contains(OL_TAG_PREFIX)) {
                listOfFiles[i].delete();
            }
        }
    }

}
