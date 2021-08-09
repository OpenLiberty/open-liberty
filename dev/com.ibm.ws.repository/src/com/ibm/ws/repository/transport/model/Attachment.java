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

import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

import com.ibm.ws.repository.common.enums.AttachmentLinkType;
import com.ibm.ws.repository.common.enums.AttachmentType;

public class Attachment extends AbstractJSON {

    private String _id = null;
    private String name = null;
    private AttachmentType type = null;
    private String url = null;
    private String assetId = null;
    private String contentType = null;
    private Calendar uploadOn = null;
    private String gridFSId = null;
    private String content = null;
    private String authentication = null;
    private long size;
    private AttachmentInfo wlpInformation = null;
    private AttachmentLinkType linkType = null;
    private Locale locale;

    public Attachment() {
        wlpInformation = new AttachmentInfo();
    }

    public void setLinkType(AttachmentLinkType t) {
        linkType = t;
    }

    public AttachmentLinkType getLinkType() {
        return linkType;
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

    public AttachmentType getType() {
        return type;
    }

    public void setType(AttachmentType type) {
        this.type = type;
        if (AttachmentType.CONTENT == type) {
            setAuthentication("NONE");
        } else {
            setAuthentication(null);
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void generateId() {
        _id = UUID.randomUUID().toString();
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Calendar getUploadOn() {
        return uploadOn;
    }

    public void setUploadOn(Calendar uploadOn) {
        this.uploadOn = uploadOn;
    }

    public String getGridFSId() {
        return gridFSId;
    }

    public void setGridFSId(String gridFSId) {
        this.gridFSId = gridFSId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /**
     * @param authentification the authentification to set
     */
    public void setAuthentication(String authentication) {
        this.authentication = authentication;
    }

    /**
     * @return the authentification
     */
    public String getAuthentication() {
        return authentication;
    }

    public void setWlpInformation(AttachmentInfo attachInformation) {
        this.wlpInformation = attachInformation;
    }

    public AttachmentInfo getWlpInformation() {
        return this.wlpInformation;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((_id == null) ? 0 : _id.hashCode());
        result = prime * result + ((assetId == null) ? 0 : assetId.hashCode());
        result = prime * result + ((content == null) ? 0 : content.hashCode());
        result = prime * result
                 + ((contentType == null) ? 0 : contentType.hashCode());
        result = prime * result
                 + ((gridFSId == null) ? 0 : gridFSId.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (int) (size ^ (size >>> 32));
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result
                 + ((uploadOn == null) ? 0 : uploadOn.hashCode());
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        result = prime * result + ((wlpInformation == null) ? 0 : wlpInformation.hashCode());
        result = prime * result + ((locale == null) ? 0 : locale.hashCode());
        result = prime * result + ((linkType == null) ? 0 : linkType.hashCode());
        result = prime * result + ((authentication == null) ? 0 : authentication.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!equivalent(obj)) {
            return false;
        }
        // If the other object wasn't an attachment then we'd have
        // returned false from the equivalent method
        Attachment other = (Attachment) obj;

        // Now check the fields that are set by massive, and are not
        // used in the equivalent check
        if (_id == null) {
            if (other._id != null)
                return false;
        } else if (!_id.equals(other._id))
            return false;
        if (uploadOn == null) {
            if ((other.uploadOn) != null)
                return false;
        } else if (!uploadOn.equals(other.uploadOn))
            return false;
        if (gridFSId == null) {
            if (other.gridFSId != null)
                return false;
        } else if (!gridFSId.equals(other.gridFSId))
            return false;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        if (assetId == null) {
            if (other.assetId != null)
                return false;
        } else if (!assetId.equals(other.assetId))
            return false;
        if (contentType == null) {
            if (other.contentType != null)
                return false;
        } else if (!contentType.equals(other.contentType))
            return false;
        if (authentication == null) {
            if (other.authentication != null)
                return false;
        } else if (!authentication.equals(other.authentication))
            return false;
        if (wlpInformation == null) {
            if (other.wlpInformation != null)
                return false;
        } else if (!wlpInformation.equals(other.wlpInformation))
            return false;
        return true;
    }

    public boolean equivalent(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Attachment other = (Attachment) obj;
        if (content == null) {
            if (other.content != null)
                return false;
        } else if (!content.equals(other.content))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (size != other.size)
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        if (locale == null) {
            if (other.locale != null) {
                return false;
            }
        } else if (!locale.equals(other.locale)) {
            return false;
        }

        if (linkType == null) {
            if (other.linkType != null) {
                return false;
            }
        } else if (!linkType.equals(other.linkType))
            return false;

        // Since we copy attachments on upload for attachments stored in massive, checking the URL's
        // doesn't really make much sense for that case (when linkType == null) so only compare
        // URL's if one of the linkTypes is not null
        if ((null != linkType) || (null != other.linkType)) {
            if (url == null) {
                if (other.url != null) {
                    return false;
                }
            } else if (!url.equals(other.url)) {
                return false;
            }
        }

        if (wlpInformation == null) {
            if (other.wlpInformation != null)
                return false;
        } else if (!wlpInformation.equals(other.wlpInformation))
            return false;
        return true;
    }

}
