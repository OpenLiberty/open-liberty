/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wlp.repository.esa;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.internal.subsystem.SubsystemFeatureDefinitionImpl;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;

/**
 * This {@link Task} will create the description HTML that can be used for an ESA. The generation is also accessible via the
 * {@link #generateDescriptionHtml(ProvisioningFeatureDefinition)} method. Both {@link #setFeatureManifestFile(File)} and {@link #setDescriptionHtmlOutputFile(File)} must be set to
 * a non <code>null</code> value.
 */
public class EsaDescriptionHtmlGenerator extends Task {

    private boolean generateKnowledgeCentreLinks = true;
    private File featureManifestFile;
    private File descriptionHtmlOutputFile;
    private String licenseType;

    private final static String FP_KC_URL_PATH_SEGMENT = "SSAW57_liberty/com.ibm.websphere.wlp.nd.multiplatform.doc";
    private final static String BETA_KC_URL_PATH_SEGMENT = "was_beta_liberty/com.ibm.websphere.liberty.autogen.beta.doc";
    private final static String ZOS_KC_URL_PATH_SEGMENT = "SS7K4U_liberty/com.ibm.websphere.liberty.autogen.zos.doc";
    private final static String SOFTWARE_COMPATIBITY_URL = "http://www-969.ibm.com/software/reports/compatibility/clarity-reports/report/html/softwareReqsForProductByComponent?deliverableId=1337870535828&duComponent=Server_118F7A80A6C511E2A4CA497F0925FE1B";

    /*
     * (non-Javadoc)
     *
     * @see org.apache.tools.ant.Task#execute()
     */
    @Override
    public void execute() throws BuildException {
        try {
            ProvisioningFeatureDefinition feature = new SubsystemFeatureDefinitionImpl(null, this.featureManifestFile);
            String description = generateDescriptionHtml(feature, this.generateKnowledgeCentreLinks, this.licenseType);
            Files.write(this.descriptionHtmlOutputFile.toPath(), description.getBytes());
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Creates the HTML to describe an ESA.
     *
     * @param feature The feature to generate the description for
     * @param generateKnowledgeCentreLinks <code>true</code> if the description should include links to the knowledge centre for this feature.
     * @param licenseType to generate usage restriction message for ILAN feature pages.
     * @return
     */
    public String generateDescriptionHtml(ProvisioningFeatureDefinition feature, boolean generateKnowledgeCentreLinks, String licenseType) {
        final String shortName = feature.getHeader("IBM-ShortName");
        final String installName = shortName != null ? shortName : feature.getSymbolicName();
        final String shortDescription = feature.getHeader("Subsystem-Description", Locale.ENGLISH);
        final String appliesTo = feature.getHeader("IBM-AppliesTo");
        StringBuilder result = new StringBuilder();
        if (shortDescription != null && !shortDescription.isEmpty()) {
            result.append("<div id=\"ibm-wasdev-feature-desc-content\">").append(System.lineSeparator());
            result.append(shortDescription).append(System.lineSeparator());
            result.append("</div>").append(System.lineSeparator());
        }
        if (licenseType != null && licenseType.equals("ILAN")) {
            result.append("<div id=\"ibm-wasdev-feature-usage-message\">").append(System.lineSeparator());
            result.append("<p><b>Usage restriction:</b> Use of this feature is provided as-is and is not covered by any IBM support contract.").append(System.lineSeparator());
            result.append("</div>").append(System.lineSeparator());
        }
        result.append("<h2 id=\"ibm-wasdev-feature-instructions-title\">Command Line Install</h2>").append(System.lineSeparator());
        result.append("<div id=\"ibm-wasdev-feature-instructions-content\">").append(System.lineSeparator());
        result.append("To install the feature from the command line, type:<br/>");
        result.append("<code>bin/installUtility install ");
        result.append(installName);
        result.append("</code> <br/>").append(System.lineSeparator());
        if (!isBeta(appliesTo)) {
            result.append("If you are installing into 8.5.5.5 or earlier and the feature supports that version, use the featureManager command, for example:<br/>");
            result.append("<code>bin/featureManager install ");
            result.append(installName);
            result.append(" --when-file-exists=ignore</code>").append(System.lineSeparator());
        }
        result.append("</div>").append(System.lineSeparator());
        if (shortName != null && !shortName.isEmpty() && feature.getVisibility() == Visibility.PUBLIC) {
            result.append(" <h2 id=\"ibm-wasdev-feature-notes-title\">Config Instructions</h2> ").append(System.lineSeparator());
            result.append("<div id=\"ibm-wasdev-feature-notes-content\">").append(System.lineSeparator());
            result.append("To use the feature at runtime add the following to your server.xml file<br/>").append(System.lineSeparator());
            result.append("<code>&lt;featureManager&gt;</br>").append(System.lineSeparator());
            result.append("&nbsp;&nbsp;&lt;feature&gt;");
            // user features don't have a knowledge center link but do need the feature name prefixed with "usr:"
            if (!generateKnowledgeCentreLinks) {
                result.append("usr:");
            }
            result.append(shortName);
            result.append("&lt;/feature&gt;</br>").append(System.lineSeparator());
            result.append("&lt;/featureManager&gt;</br></code> </div>").append(System.lineSeparator());

            if (generateKnowledgeCentreLinks) {
                String urlPathSegment = "";
                if (isBeta(appliesTo)) {
                    urlPathSegment = BETA_KC_URL_PATH_SEGMENT;
                } else if (isZOS(appliesTo)) {
                    urlPathSegment = ZOS_KC_URL_PATH_SEGMENT;
                } else {
                    urlPathSegment = FP_KC_URL_PATH_SEGMENT;
                }
                // Additional info block
                result.append(" <h2 id=\"ibm-wasdev-feature-notes-title\">Additional Information</h2> ").append(System.lineSeparator());
                result.append(" <div id=\"ibm-wasdev-feature-additional-information\">").append(System.lineSeparator());
                result.append("  <ul>").append(System.lineSeparator());

                result.append("   <li>For the complete feature documentation, see the <a href=\"http://www.ibm.com/support/knowledgecenter/");
                result.append(urlPathSegment);
                result.append("/ae/rwlp_feature_");
                result.append(shortName);
                result.append(".html");
                result.append("\">" + shortName + "</a>");
                result.append(" feature description in IBM Knowledge Center.</li>").append(System.lineSeparator());

                result.append("   <li>For more detailed installation instructions, including using Installation Manager and WebSphere Developer Tools, see ");
                result.append("<a href=\"http://www.ibm.com/support/knowledgecenter/");
                result.append(urlPathSegment);
                result.append("/com.ibm.websphere.wlp.nd.multiplatform.doc/ae/twlp_inst_assets.html\">Installing Liberty Repository assets</a>");
                result.append(" in IBM Knowledge Center.</li>").append(System.lineSeparator());

                result.append("   <li>For offline installation from a directory-based repository using 8.5.5.8 or higher, see ");
                result.append("<a href=\"https://www.ibm.com/support/knowledgecenter/");
                result.append(urlPathSegment);
                result.append("/com.ibm.websphere.wlp.nd.multiplatform.doc/ae/t_install_assets_installUtility.html\">Installing assets using the installUtility command</a>");
                result.append(" in IBM Knowledge Center.</li>").append(System.lineSeparator());
                
                result.append("   <li>For system requirements, see <a href=\"");
                result.append(SOFTWARE_COMPATIBITY_URL);
                result.append("\">WebSphere Application Server detailed system requirements</a>. ");
                result.append("Select the appropriate release, and click <b>WebSphere Application Server (all editions)</b>. ");
                result.append("Select the edition, and choose the offering you want in Requirements by component.</li>").append(System.lineSeparator());

                result.append("  </ul>").append(System.lineSeparator());
                result.append("</div>").append(System.lineSeparator());
            }
        }
        return result.toString();
    }

    /**
     * @param generateKnowledgeCentreLinks the generateKnowledgeCentreLinks to set
     */
    public void setGenerateKnowledgeCentreLinks(boolean generateKnowledgeCentreLinks) {
        this.generateKnowledgeCentreLinks = generateKnowledgeCentreLinks;
    }

    /**
     * @param featureManifestFile the featureManifestFile to set
     */
    public void setFeatureManifestFile(File featureManifestFile) {
        this.featureManifestFile = featureManifestFile;
    }

    /**
     * @param descriptionHtmlOutputFile the descriptionHtmlOutputFile to set
     */
    public void setDescriptionHtmlOutputFile(File descriptionHtmlOutputFile) {
        this.descriptionHtmlOutputFile = descriptionHtmlOutputFile;
    }

    /**
     * @param licenseType the licenseType to set
     */
    public void setLicenseType(String licenseType) {
        this.licenseType = licenseType;
    }

    public static boolean isBeta(String appliesTo) {
        final String BETA_REGEX = "[2-9][0-9][0-9][0-9][.].*";

        // Use the appliesTo string to determine whether a feature is a Beta or a regular feature.
        // Beta features are of the format:
        // "com.ibm.websphere.appserver; productVersion=2014.8.0.0; productInstallType=Archive",
        //
        // Update: features build using FeatureBnd look like this
        // "\"com.ibm.websphere.appserver;productEdition=EARLY_ACCESS;productVersion=\"2015.4.0.0\""
        // the changed regex supports the version quoted or unquoted.
        if (appliesTo == null) {
            return false;
        } else {
            String regex = ".*productVersion=\"?" + BETA_REGEX;
            boolean matches = appliesTo.matches(regex);
            return matches;
        }
    }

    public static boolean isZOS(String appliesTo) {
        // Use the appliesTo to determine if the feature is z/OS feature or not.
        // z/OS feature are of the format: com.ibm.websphere.appserver;productEdition=zOS;productVersion="17.0.0.1"
        if (appliesTo == null) {
            return false;
        } else {
            String regex = ".*productEdition=\"?zOS.*";
            boolean matches = appliesTo.matches(regex);
            return matches;
        }
    }

}
