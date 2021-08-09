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

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.ibm.ws.repository.common.enums.LicenseType;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.common.enums.State;
import com.ibm.ws.repository.transport.client.JSONIgnore;

public class Asset extends AbstractJSON {

    private String _id = null;
    @JSONIncludeForFile
    private String name = null;
    @JSONIncludeForFile
    private String description = null;
    @JSONIncludeForFile
    private String shortDescription = null;
    @JSONIncludeForFile
    private ResourceType type = null;
    private Calendar createdOn = null;
    private User createdBy = null;
    private Calendar lastUpdatedOn = null;
    private List<Attachment> attachments = null;
    @JSONIncludeForFile
    private Provider provider = null;
    private Featured featured = null;
    private State state = null;
    private Feedback feedback = null;
    private AssetInformation information = null;
    @JSONIncludeForFile
    private LicenseType licenseType = null;
    private String marketplaceId = null;
    private String marketplaceName = null;
    private String inMyStore = null;
    private Privacy privacy;
    @JSONIncludeForFile
    private String version = null;
    @JSONIncludeForFile
    private WlpInformation wlpInformation;
    private Reviewed reviewed;
    @JSONIncludeForFile
    private String licenseId;

    public enum Privacy {
        PUBLIC, PRIVATE
    }

    public enum Featured {
        YES("yes"), NO("no");
        private final String featured;

        private Featured(String featured) {
            this.featured = featured;
        }

        public String getValue() {
            return featured;
        }

        public static Featured forValue(String value) {
            for (Featured featured : Featured.values()) {
                if (featured.getValue().equals(value)) {
                    return featured;
                }
            }
            return null;
        }
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public ResourceType getType() {
        return type;
    }

    public void setType(ResourceType type) {
        this.type = type;
    }

    public Calendar getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Calendar createdOn) {
        this.createdOn = createdOn;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public Calendar getLastUpdatedOn() {
        return lastUpdatedOn;
    }

    public void setLastUpdatedOn(Calendar lastUpdatedOn) {
        this.lastUpdatedOn = lastUpdatedOn;
    }

    @JSONIgnore
    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void addAttachement(Attachment attachment) {
        // associate the attachment with this asset
        if (attachments == null) {
            attachments = new ArrayList<Attachment>();
        }
        attachments.add(attachment);
    }

    public void setAttachments(List<Attachment> attachments) {
        // blow away old attachments if they exist
        this.attachments = null;

        if (attachments != null) {
            // Make sure the IDs are set correctly
            for (Attachment attachment : attachments) {
                addAttachement(attachment);
            }
        }
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public Asset.Featured getFeatured() {
        return featured;
    }

    public void setFeatured(Asset.Featured featured) {
        this.featured = featured;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Feedback getFeedback() {
        return feedback;
    }

    public void setFeedback(Feedback feedback) {
        this.feedback = feedback;
    }

    public AssetInformation getInformation() {
        return information;
    }

    public void setInformation(AssetInformation information) {
        this.information = information;
    }

    public void setLicenseType(LicenseType lt) {
        this.licenseType = lt;
    }

    public LicenseType getLicenseType() {
        return licenseType;
    }

    public void setLicenseId(String s) {
        this.licenseId = s;
    }

    public String getLicenseId() {
        return licenseId;
    }

    public String getMarketplaceId() {
        return marketplaceId;
    }

    public void setMarketplaceId(String marketplaceId) {
        this.marketplaceId = marketplaceId;
    }

    public String getMarketplaceName() {
        return marketplaceName;
    }

    public void setMarketplaceName(String marketplaceName) {
        this.marketplaceName = marketplaceName;
    }

    public String getInMyStore() {
        return inMyStore;
    }

    public void setInMyStore(String inMyStore) {
        this.inMyStore = inMyStore;
    }

    public Privacy getPrivacy() {
        return privacy;
    }

    public void setPrivacy(Privacy privacy) {
        this.privacy = privacy;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setWlpInformation(WlpInformation wlpInformation) {
        this.wlpInformation = wlpInformation;
    }

    public WlpInformation getWlpInformation() {
        return this.wlpInformation;
    }

    public Reviewed getReviewed() {
        return reviewed;
    }

    public void setReviewed(Reviewed reviewed) {
        this.reviewed = reviewed;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((licenseType == null) ? 0 : licenseType.hashCode());
        result = prime * result + ((provider == null) ? 0 : provider.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        result = prime * result + ((wlpInformation == null) ? 0 : wlpInformation.hashCode());

        // name is not immutable for features but is useful for other types
        // (providesFeature from WlpInfo is immutable for features)
        if ((type != null) && (type != ResourceType.FEATURE)) {
            result = prime * result + ((name == null) ? 0 : name.hashCode());
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!equivalent(obj)) {
            return false;
        }
        // If the other object wasn't an asset then we'd have
        // returned false from the equivalent method
        Asset other = (Asset) obj;

        // Now check the fields that are set by massive, and are not
        // used in the equivalent check
        if (_id == null) {
            if (other._id != null)
                return false;
        } else if (!_id.equals(other._id))
            return false;

        if (createdBy == null) {
            if (other.createdBy != null)
                return false;
        } else if (!createdBy.equals(other.createdBy))
            return false;

        if (createdOn == null) {
            if (other.createdOn != null)
                return false;
        } else if (!createdOn.equals(other.createdOn))
            return false;

        if (lastUpdatedOn == null) {
            if (other.lastUpdatedOn != null)
                return false;
        } else if (!lastUpdatedOn.equals(other.lastUpdatedOn))
            return false;

        if (marketplaceId == null) {
            if (other.marketplaceId != null)
                return false;
        } else if (!marketplaceId.equals(other.marketplaceId))
            return false;

        if (marketplaceName == null) {
            if (other.marketplaceName != null)
                return false;
        } else if (!marketplaceName.equals(other.marketplaceName))
            return false;

        if (inMyStore == null) {
            if (other.inMyStore != null)
                return false;
        } else if (!inMyStore.equals(other.inMyStore))
            return false;

        if (reviewed == null) {
            if (other.reviewed != null)
                return false;
        } else if (!reviewed.equals(other.reviewed))
            return false;

        if (state == null) {
            if (other.state != null)
                return false;
        } else if (!state.equals(other.state))
            return false;

        if (featured == null) {
            if (other.featured != null)
                return false;
        } else if (!featured.equals(other.featured))
            return false;

        if (attachments == null) {
            if (other.attachments != null)
                return false;
        } else if (!attachments.equals(other.attachments))
            return false;

        return true;
    }

    public boolean equivalentWithoutAttachments(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Asset other = (Asset) obj;

        if (description == null) {
            if (other.description != null)
                return false;
        } else if (!description.equals(other.description))
            return false;

        if (shortDescription == null) {
            if (other.shortDescription != null)
                return false;
        } else if (!shortDescription.equals(other.shortDescription))
            return false;

        if (feedback == null) {
            if (other.feedback != null)
                return false;
        } else if (!feedback.equals(other.feedback))
            return false;

        if (information == null) {
            if (other.information != null)
                return false;
        } else if (!information.equals(other.information))
            return false;

        if (licenseType == null) {
            if (other.licenseType != null)
                return false;
        } else if (!licenseType.equals(other.licenseType))
            return false;

        if (licenseId == null) {
            if (other.licenseId != null) {
                return false;
            }
        } else if (!licenseId.equals(other.licenseId)) {
            return false;
        }

        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;

        if (privacy == null) {
            if (other.privacy != null)
                return false;
        } else if (!privacy.equals(other.privacy))
            return false;

        if (provider == null) {
            if (other.provider != null)
                return false;
        } else if (!provider.equals(other.provider))
            return false;

        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;

        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;

        if (wlpInformation == null) {
            if (other.wlpInformation != null)
                return false;
        } else if (!wlpInformation.equivalent(other.wlpInformation))
            return false;

        return true;
    }

    public boolean equivalent(Object obj) {
        if (!equivalentWithoutAttachments(obj)) {
            return false;
        }
        // If the other object wasn't an asset then we'd have
        // returned false from the equivalent method
        Asset other = (Asset) obj;

        // Need to call equivalent on attachments
        if (attachments == null) {
            if (other.attachments != null)
                return false;
        } else {
            // Attachments is not null

            // Check if other attachments are null
            if (other.attachments == null) {
                return false;
            } else {

                if (other.attachments.size() != attachments.size()) {
                    return false;
                }

                // Check attachment contents
                outer: for (Attachment at : attachments) {
                    // try and find equivalent attachment in other asset
                    for (Attachment otherAt : other.attachments) {
                        if (at.equivalent(otherAt)) {
                            // Found a mactch, move to next attachment
                            continue outer;
                        }
                    }
                    // Didn't find a match
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Creates a <b>shallow</b> copy of the Asset that can contains the information needed for a file based
     * repository to use. Used when the Asset is read from massive and is pushed to a file based repository (to
     * contain only the relevant fields)
     *
     * @return
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public Asset createMinimalAssetForJSON() throws IllegalArgumentException, IllegalAccessException {
        Asset ass = new Asset();
        for (Field f : Asset.class.getDeclaredFields()) {
            if (f.isAnnotationPresent(JSONIncludeForFile.class)) {
                f.set(ass, f.get(this));
            }
        }
        return ass;
    }

    public void dumpMinimalAsset(OutputStream stream) throws IllegalArgumentException, IllegalAccessException {
        createMinimalAssetForJSON().dump(stream);
    }

}
