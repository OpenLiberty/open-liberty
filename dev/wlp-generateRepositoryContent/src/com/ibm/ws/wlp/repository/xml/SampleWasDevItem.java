/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wlp.repository.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

/**
 * This is an XML element that can provide information about a single sample to generate part of the WAS dev page.
 */
public class SampleWasDevItem {

    private String postTitle;
    private String postDateGMT;
    private String postExcerpt;
    private String postContent;
    private String postName;
    private String postType;
    private String postStatus;
    private List<String> customFields;
    private List<String> categories;

    /**
     * @return the categories
     */
    public List<String> getCategories() {
        if (this.categories == null)
            this.categories = new ArrayList<String>();
        return this.categories;
    }

    public List<String> getCustomFields() {
        if (this.customFields == null)
            this.customFields = new ArrayList<String>();
        return this.customFields;
    }

    /**
     * @return the postTitle
     */
    public String getPostTitle() {
        return postTitle;
    }

    /**
     * @return the postDateGMT
     */
    public String getPostDateGMT() {
        return postDateGMT;
    }

    /**
     * @return the postExcerpt
     */
    public String getPostExcerpt() {
        return postExcerpt;
    }

    /**
     * @return the postContent
     */
    public String getPostContent() {
        return postContent;
    }

    /**
     * @return the postName
     */
    public String getPostName() {
        return postName;
    }

    /**
     * @return the postType
     */
    public String getPostType() {
        return postType;
    }

    /**
     * @return the postStatus
     */
    public String getPostStatus() {
        return postStatus;
    }

    /**
     * @param postTitle the postTitle to set
     */
    @XmlElement(name = "post_title")
    public void setPostTitle(String postTitle) {
        this.postTitle = postTitle;
    }

    /**
     * @param postDateGMT the postDateGMT to set
     */
    @XmlElement(name = "post_date_gmt")
    public void setPostDateGMT(String postDateGMT) {
        this.postDateGMT = postDateGMT;
    }

    /**
     * @param postExcerpt the postExcerpt to set
     */
    @XmlElement(name = "post_excerpt")
    public void setPostExcerpt(String postExcerpt) {
        this.postExcerpt = postExcerpt;
    }

    /**
     * @param postContent the postContent to set
     */
    @XmlElement(name = "post_content")
    public void setPostContent(String postContent) {
        this.postContent = postContent;
    }

    /**
     * @param postName the postName to set
     */
    @XmlElement(name = "post_name")
    public void setPostName(String postName) {
        this.postName = postName;
    }

    /**
     * @param postType the postType to set
     */
    @XmlElement(name = "post_type")
    public void setPostType(String postType) {
        this.postType = postType;
    }

    /**
     * @param customFields the customFields to set
     */
    @XmlElement(name = "asset_download_url")
    @XmlElementWrapper(name = "custom_fields")
    public void setCustomFields(List<String> customFields) {
        this.customFields = customFields;
    }

    /**
     * @param categories the categories to set
     */
    @XmlElement(name = "category")
    @XmlElementWrapper
    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    /**
     * @param postStatus the postStatus to set
     */
    public void setPostStatus(String postStatus) {
        this.postStatus = postStatus;
    }

}
