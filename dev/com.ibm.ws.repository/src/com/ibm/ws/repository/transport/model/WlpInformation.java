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

package com.ibm.ws.repository.transport.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;

import com.ibm.ws.repository.common.enums.DisplayPolicy;
import com.ibm.ws.repository.common.enums.DownloadPolicy;
import com.ibm.ws.repository.common.enums.InstallPolicy;
import com.ibm.ws.repository.common.enums.ResourceTypeLabel;
import com.ibm.ws.repository.common.enums.Visibility;
import com.ibm.ws.repository.transport.client.HasBreakingChanges;
import com.ibm.ws.repository.transport.client.VersionableContent;
import com.ibm.ws.repository.transport.exceptions.BadVersionException;

/**
 * This class holds custom attributes for Liberty.
 */
public class WlpInformation extends AbstractJSON implements VersionableContent, HasBreakingChanges {

    public final static float MIN_VERSION = 1.0f;
    public final static float MAX_VERSION = 3.0f; // up to but not including

    private ResourceTypeLabel typeLabel;
    private String productVersion;
    private String productInstallType;
    private String productId;
    private String productEdition;
    private Collection<String> provideFeature;
    private Collection<String> requireFeature;
    private Collection<String> provideFix;
    private Collection<String> requireFix;
    private String appliesTo;
    private Collection<AppliesToFilterInfo> appliesToFilterInfo;
    private Visibility visibility;
    private String shortName;
    private String lowerCaseShortName;
    private Date date;
    private String scriptLanguage;
    private String provisionCapability;
    private InstallPolicy installPolicy;
    private DownloadPolicy downloadPolicy;
    private DisplayPolicy displayPolicy;
    private DisplayPolicy webDisplayPolicy;
    private String wlpInformationVersion;
    private long mainAttachmentSize;
    private Collection<Link> links;
    private String vanityRelativeURL;
    private String featuredWeight;
    private Collection<String> supersededBy;
    private Collection<String> supersededByOptional;
    private JavaSEVersionRequirements javaSEVersionRequirements;
    private String mainAttachmentSHA256;
    private String genericRequirements;
    private String packagedJava;
    private Collection<RequireFeatureWithTolerates> requireFeatureWithTolerates;
    private String mavenCoordinates;
    private String singleton;
    private String ibmInstallTo;

    public String getFeaturedWeight() {
        return featuredWeight;
    }

    public void setFeaturedWeight(String featuredWeight) {
        this.featuredWeight = featuredWeight;
    }

    /**
     * @return the links
     */
    public Collection<Link> getLinks() {
        return links;
    }

    /**
     * @param links the links to set
     */
    public void setLinks(Collection<Link> links) {
        this.links = links;
    }

    public Collection<String> getRequireFeature() {
        return requireFeature;
    }

    public void setRequireFeature(Collection<String> requireFeature) {
        this.requireFeature = requireFeature;
    }

    public void addRequireFeature(String requireFeature) {
        if (this.requireFeature == null) {
            this.requireFeature = new HashSet<String>();
        }
        this.requireFeature.add(requireFeature);
    }

    public Collection<RequireFeatureWithTolerates> getRequireFeatureWithTolerates() {
        return requireFeatureWithTolerates;
    }

    public void setRequireFeatureWithTolerates(Collection<RequireFeatureWithTolerates> requiredFeaturesWithTolerates) {
        this.requireFeatureWithTolerates = requiredFeaturesWithTolerates;
    }

    public void addRequireFeatureWithTolerates(RequireFeatureWithTolerates feature) {
        if (this.requireFeatureWithTolerates == null) {
            this.requireFeatureWithTolerates = new HashSet<RequireFeatureWithTolerates>();
        }
        this.requireFeatureWithTolerates.add(feature);
    }

    public ResourceTypeLabel getTypeLabel() {
        return typeLabel;
    }

    public void setTypeLabel(ResourceTypeLabel typeLabel) {
        this.typeLabel = typeLabel;
    }

    public String getProductVersion() {
        return productVersion;
    }

    public void setProductVersion(String productVersion) {
        this.productVersion = productVersion;
    }

    public String getProductInstallType() {
        return productInstallType;
    }

    public void setProductInstallType(String productInstallType) {
        this.productInstallType = productInstallType;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductEdition() {
        return productEdition;
    }

    public void setProductEdition(String productEdition) {
        this.productEdition = productEdition;
    }

    public Collection<String> getProvideFeature() {
        return provideFeature;
    }

    public void setProvideFeature(Collection<String> provideFeature) {
        this.provideFeature = provideFeature;
    }

    public void addProvideFeature(String provideFeature) {
        if (this.provideFeature == null) {
            this.provideFeature = new HashSet<String>();
        }
        this.provideFeature.add(provideFeature);
    }

    public Collection<String> getProvideFix() {
        return provideFix;
    }

    public void setProvideFix(Collection<String> provideFix) {
        this.provideFix = provideFix;
    }

    public void addProvideFix(String provideFix) {
        if (this.provideFix == null) {
            this.provideFix = new HashSet<String>();
        }
        this.provideFix.add(provideFix);
    }

    public Collection<String> getRequireFix() {
        return requireFix;
    }

    public void setRequireFix(Collection<String> requireFix) {
        this.requireFix = requireFix;
    }

    public void addRequireFix(String requireFix) {
        if (this.requireFix == null) {
            this.requireFix = new HashSet<String>();
        }
        this.requireFix.add(requireFix);
    }

    public String getVanityRelativeURL() {
        return vanityRelativeURL;
    }

    public void setVanityRelativeURL(String vanityRelativeURL) {
        this.vanityRelativeURL = vanityRelativeURL;
    }

    public String getAppliesTo() {
        return appliesTo;
    }

    public void setAppliesTo(String appliesTo) {
        this.appliesTo = appliesTo;
    }

    public Collection<AppliesToFilterInfo> getAppliesToFilterInfo() {
        return appliesToFilterInfo;
    }

    public void setAppliesToFilterInfo(
                                       Collection<AppliesToFilterInfo> appliesToFilterInfo) {
        this.appliesToFilterInfo = appliesToFilterInfo;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public String getShortName() {
        return shortName;
    }

    /**
     * Sets the short name to the supplied value and also sets a lower case version obtainable via the {@link #getLowerCaseShortName()}.
     *
     * @param shortName
     */
    public void setShortName(String shortName) {
        this.shortName = shortName;
        String lowerCaseShortName = shortName == null ? null : shortName.toLowerCase();
        this.setLowerCaseShortName(lowerCaseShortName);
    }

    public String getLowerCaseShortName() {
        return lowerCaseShortName;
    }

    private void setLowerCaseShortName(String lowerCaseShortName) {
        this.lowerCaseShortName = lowerCaseShortName;
    }

    public Date getDate() {
        if (this.date == null) {
            return null;
        } else {
            // Findbugs doesn't like you returning dates as it exposes internals
            // so wrap in a new object
            return new Date(this.date.getTime());
        }
    }

    public void setDate(Date date) {
        if (date == null) {
            this.date = null;
        } else {
            // Findbugs doesn't like you returning dates as it exposes internals
            // so wrap in a new object
            this.date = new Date(date.getTime());
        }
    }

    public void setScriptLanguage(String scriptLang) {
        this.scriptLanguage = scriptLang;
    }

    public String getScriptLanguage() {
        return scriptLanguage;
    }

    public void setDownloadPolicy(DownloadPolicy downloadPolicy) {
        this.downloadPolicy = downloadPolicy;
    }

    public DownloadPolicy getDownloadPolicy() {
        return downloadPolicy;
    }

    public String getProvisionCapability() {
        return provisionCapability;
    }

    public void setProvisionCapability(String ibmProvisionCapability) {
        this.provisionCapability = ibmProvisionCapability;
    }

    public InstallPolicy getInstallPolicy() {
        return installPolicy;
    }

    public void setDisplayPolicy(DisplayPolicy displayPolicy) {
        this.displayPolicy = displayPolicy;
    }

    public DisplayPolicy getDisplayPolicy() {
        return displayPolicy;
    }

    public void setWebDisplayPolicy(DisplayPolicy webDisplayPolicy) {
        this.webDisplayPolicy = webDisplayPolicy;
    }

    public DisplayPolicy getWebDisplayPolicy() {
        return webDisplayPolicy;
    }

    public void setInstallPolicy(InstallPolicy installPolicy) {
        this.installPolicy = installPolicy;
    }

    public String getWlpInformationVersion() {
        return this.wlpInformationVersion;
    }

    public void setWlpInformationVersion(String WlpInformationVersion) {
        this.wlpInformationVersion = WlpInformationVersion;
    }

    public long getMainAttachmentSize() {
        return mainAttachmentSize;
    }

    public void setMainAttachmentSize(long mainAttachmentSize) {
        this.mainAttachmentSize = mainAttachmentSize;
    }

    public String getMainAttachmentSHA256() {
        return mainAttachmentSHA256;
    }

    public void setMainAttachmentSHA256(String pMainAttachmentSHA256) {
        this.mainAttachmentSHA256 = pMainAttachmentSHA256;
    }

    public void addSupersededBy(String supersededByFeature) {
        if (this.supersededBy == null) {
            this.supersededBy = new HashSet<String>();
        }

        this.supersededBy.add(supersededByFeature);
    }

    public void addSupersededByOptional(String supersededByFeature) {
        if (this.supersededByOptional == null) {
            this.supersededByOptional = new HashSet<String>();
        }

        this.supersededByOptional.add(supersededByFeature);
    }

    public Collection<String> getSupersededBy() {
        return this.supersededBy;
    }

    public void setSupersededBy(Collection<String> supersededBy) {
        this.supersededBy = supersededBy;
    }

    public Collection<String> getSupersededByOptional() {
        return this.supersededByOptional;
    }

    public void setSupersededByOptional(Collection<String> supersededByOptional) {
        this.supersededByOptional = supersededByOptional;
    }

    public JavaSEVersionRequirements getJavaSEVersionRequirements() {
        return this.javaSEVersionRequirements;
    }

    public void setJavaSEVersionRequirements(JavaSEVersionRequirements javaSEVersionRequirements) {
        this.javaSEVersionRequirements = javaSEVersionRequirements;
    }

    public String getGenericRequirements() {
        return genericRequirements;
    }

    public void setGenericRequirements(String genericRequirements) {
        this.genericRequirements = genericRequirements;
    }

    public String getPackagedJava() {
        return packagedJava;
    }

    public void setPackagedJava(String packagedJava) {
        this.packagedJava = packagedJava;
    }

    public String getMavenCoordinates() {
        return mavenCoordinates;
    }

    public void setMavenCoordinates(String mavenCoordinates) {
        this.mavenCoordinates = mavenCoordinates;
    }

    public String getSingleton() {
        return singleton;
    }

    public void setSingleton(String singleton) {
        this.singleton = singleton;
    }

    public String getIbmInstallTo() {
        return ibmInstallTo;
    }

    public void setIbmInstallTo(String ibmInstallTo) {
        this.ibmInstallTo = ibmInstallTo;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((provideFeature == null) ? 0 : provideFeature.hashCode());
        result = prime * result + ((provideFix == null) ? 0 : provideFix.hashCode());
        result = prime * result + ((productId == null) ? 0 : productId.hashCode());
        result = prime * result + ((supersededBy == null) ? 0 : supersededBy.hashCode());
        result = prime * result + ((supersededByOptional == null) ? 0 : supersededByOptional.hashCode());

        return result;
    }

    /**
     * Override default equals method
     */
    @Override
    public boolean equals(Object obj) {
        if (!equivalent(obj)) {
            return false;
        }

        // If the other object wasn't a WlpInformation then we'd have
        // returned false from the equivalent method
        WlpInformation other = (WlpInformation) obj;

        // Now check the fields that are set by massive, and are not
        // used in the equivalent check
        if (appliesToFilterInfo == null) {
            if (other.appliesToFilterInfo != null)
                return false;
        } else if (!appliesToFilterInfo.equals(other.appliesToFilterInfo))
            return false;

        return true;
    }

    /**
     * Define what must be equals for us to consider a WlpInformation to be essentially the same.
     * This is defined with package visibility as this should only be called when comparing the
     * parent asset.
     */
    boolean equivalent(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        WlpInformation other = (WlpInformation) obj;

        if (appliesTo == null) {
            if (other.appliesTo != null)
                return false;
        } else if (!appliesTo.equals(other.appliesTo))
            return false;

        if (date == null) {
            if (other.date != null)
                return false;
        } else if (!date.equals(other.date))
            return false;

        if (links == null) {
            if (other.links != null)
                return false;
        } else if (!links.equals(other.links))
            return false;

        if (shortName == null) {
            if (other.shortName != null)
                return false;
        } else if (!shortName.equals(other.shortName))
            return false;

        if (lowerCaseShortName == null) {
            if (other.lowerCaseShortName != null)
                return false;
        } else if (!lowerCaseShortName.equals(other.lowerCaseShortName))
            return false;

        if (productEdition == null) {
            if (other.productEdition != null)
                return false;
        } else if (!productEdition.equals(other.productEdition))
            return false;

        if (productId == null) {
            if (other.productId != null)
                return false;
        } else if (!productId.equals(other.productId))
            return false;

        if (productInstallType == null) {
            if (other.productInstallType != null)
                return false;
        } else if (!productInstallType.equals(other.productInstallType))
            return false;

        if (productVersion == null) {
            if (other.productVersion != null)
                return false;
        } else if (!productVersion.equals(other.productVersion))
            return false;

        if (provideFeature == null) {
            if (other.provideFeature != null)
                return false;
        } else {
            if (other.provideFeature != null) {
                if (other.provideFeature.size() != provideFeature.size()) {
                    return false;
                }
                for (String s : other.provideFeature) {
                    if (!provideFeature.contains(s)) {
                        return false;
                    }
                }
            } else {
                return false;
            }
        }

        if (links == null) {
            if (other.links != null)
                return false;
        } else if (!links.equals(other.links))
            return false;

        if (provideFix == null) {
            if (other.provideFix != null)
                return false;
        } else if (!provideFix.equals(other.provideFix))
            return false;

        if (requireFeature == null) {
            if (other.requireFeature != null)
                return false;
        } else {
            if (other.requireFeature != null) {
                if (other.requireFeature.size() != requireFeature.size()) {
                    return false;
                }
                for (String s : other.requireFeature) {
                    if (!requireFeature.contains(s)) {
                        return false;
                    }
                }
            } else {
                return false;
            }
        }

        if (requireFix == null) {
            if (other.requireFix != null)
                return false;
        } else {
            if (other.requireFix != null) {
                if (other.requireFix.size() != requireFix.size()) {
                    return false;
                }
                for (String s : other.requireFix) {
                    if (!requireFix.contains(s)) {
                        return false;
                    }
                }
            } else {
                return false;
            }
        }

        if (typeLabel == null) {
            if (other.typeLabel != null)
                return false;
        } else if (!typeLabel.equals(other.typeLabel))
            return false;

        if (visibility == null) {
            if (other.visibility != null)
                return false;
        } else if (!visibility.equals(other.visibility))
            return false;

        if (scriptLanguage == null) {
            if (other.scriptLanguage != null)
                return false;
        } else if (!scriptLanguage.equals(other.scriptLanguage))
            return false;

        if (downloadPolicy == null) {
            if (other.downloadPolicy != null)
                return false;
        } else if (!downloadPolicy.equals(other.downloadPolicy))
            return false;

        if (provisionCapability == null) {
            if (other.provisionCapability != null)
                return false;
        } else if (!provisionCapability.equals(other.provisionCapability))
            return false;

        if (installPolicy == null) {
            if (other.installPolicy != null)
                return false;
        } else if (!installPolicy.equals(other.installPolicy))
            return false;

        if (displayPolicy == null) {
            if (other.displayPolicy != null)
                return false;
        } else if (!displayPolicy.equals(other.displayPolicy))
            return false;

        if (webDisplayPolicy == null) {
            if (other.webDisplayPolicy != null)
                return false;
        } else if (!webDisplayPolicy.equals(other.webDisplayPolicy))
            return false;

        if (vanityRelativeURL == null) {
            if (other.vanityRelativeURL != null)
                return false;
        } else if (!vanityRelativeURL.equals(other.vanityRelativeURL))
            return false;

        if (featuredWeight == null) {
            if (other.featuredWeight != null)
                return false;
        } else if (!featuredWeight.equals(other.featuredWeight))
            return false;

        if (wlpInformationVersion == null) {
            if (other.wlpInformationVersion != null)
                return false;
        } else if (!wlpInformationVersion.equals(other.wlpInformationVersion))
            return false;

        if (mainAttachmentSize != other.mainAttachmentSize)
            return false;

        if (supersededBy == null) {
            if (other.supersededBy != null) {
                return false;
            }
        } else if (!supersededBy.equals(other.supersededBy)) {
            return false;
        }

        if (supersededByOptional == null) {
            if (other.supersededByOptional != null) {
                return false;
            }
        } else if (!supersededByOptional.equals(other.supersededByOptional)) {
            return false;
        }

        if (javaSEVersionRequirements == null) {
            if (other.javaSEVersionRequirements != null) {
                return false;
            }
        } else if (!javaSEVersionRequirements.equals(other.javaSEVersionRequirements)) {
            return false;
        }

        if (mainAttachmentSHA256 == null) {
            if (other.mainAttachmentSHA256 != null) {
                return false;
            }
        } else if (!mainAttachmentSHA256.equals(other.mainAttachmentSHA256)) {
            return false;
        }

        if (genericRequirements == null) {
            if (other.genericRequirements != null) {
                return false;
            }
        } else if (!genericRequirements.equals(other.genericRequirements)) {
            return false;
        }

        if (packagedJava == null) {
            if (other.packagedJava != null) {
                return false;
            }
        } else if (!packagedJava.equals(other.packagedJava)) {
            return false;
        }

        if (requireFeatureWithTolerates == null) {
            if (other.requireFeatureWithTolerates != null) {
                return false;
            }
        } else if (!requireFeatureWithTolerates.equals(other.requireFeatureWithTolerates)) {
            return false;
        }

        if (mavenCoordinates == null) {
            if (other.mavenCoordinates != null) {
                return false;
            }
        } else if (!mavenCoordinates.equals(other.mavenCoordinates)) {
            return false;
        }

        if (singleton == null) {
            if (other.singleton != null)
                return false;
        } else if (!singleton.equals(other.singleton))
            return false;

        if (ibmInstallTo == null) {
            if (other.ibmInstallTo != null)
                return false;
        } else if (!ibmInstallTo.equals(other.ibmInstallTo))
            return false;

        return true;
    }

    @Override
    public void validate(String version) throws IllegalStateException, BadVersionException {
        float v = Float.valueOf(version);
        if (v < MIN_VERSION || v >= MAX_VERSION) {
            throw new BadVersionException(Float.toString(MIN_VERSION), Float.toString(MAX_VERSION), version);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String nameOfVersionAttribute() {
        return "wlpInformationVersion";
    }

    /** {@inheritDoc} */
    @Override
    public Collection<String> attributesThatCauseBreakingChanges() {
        // INSTALL was only introduced at 8.5.5.4 so if it's set to that won't work on earlier clients so force it into a second object
        if (Visibility.INSTALL.equals(this.visibility)) {
            return Collections.singleton("visibility");
        }
        return Collections.emptySet();
    }

}
