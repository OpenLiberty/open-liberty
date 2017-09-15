/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.translator.visitor.validator;

public class ValidateTagFileResult extends ValidateResult {
    protected String displayName = "";
    protected String bodyContent = "";
    protected String dynamicAttributes = "";
    protected String smallIcon = "";
    protected String largeIcon = "";
    protected String description = "";
    protected String example = "";
    
    public ValidateTagFileResult(String jspVisitorId) {
        super(jspVisitorId);
    }
    /**
     * Returns the bodyContent.
     * @return String
     */
    public String getBodyContent() {
        return bodyContent;
    }

    /**
     * Returns the description.
     * @return String
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the displayName.
     * @return String
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the dynamicAttributes.
     * @return String
     */
    public String getDynamicAttributes() {
        return dynamicAttributes;
    }

    /**
     * Returns the example.
     * @return String
     */
    public String getExample() {
        return example;
    }

    /**
     * Returns the largeIcon.
     * @return String
     */
    public String getLargeIcon() {
        return largeIcon;
    }

    /**
     * Returns the smallIcon.
     * @return String
     */
    public String getSmallIcon() {
        return smallIcon;
    }

    /**
     * Sets the bodyContent.
     * @param bodyContent The bodyContent to set
     */
    public void setBodyContent(String bodyContent) {
        this.bodyContent = bodyContent;
    }

    /**
     * Sets the description.
     * @param description The description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sets the displayName.
     * @param displayName The displayName to set
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Sets the dynamicAttributes.
     * @param dynamicAttributes The dynamicAttributes to set
     */
    public void setDynamicAttributes(String dynamicAttributes) {
        this.dynamicAttributes = dynamicAttributes;
    }

    /**
     * Sets the example.
     * @param example The example to set
     */
    public void setExample(String example) {
        this.example = example;
    }

    /**
     * Sets the largeIcon.
     * @param largeIcon The largeIcon to set
     */
    public void setLargeIcon(String largeIcon) {
        this.largeIcon = largeIcon;
    }

    /**
     * Sets the smallIcon.
     * @param smallIcon The smallIcon to set
     */
    public void setSmallIcon(String smallIcon) {
        this.smallIcon = smallIcon;
    }

}
