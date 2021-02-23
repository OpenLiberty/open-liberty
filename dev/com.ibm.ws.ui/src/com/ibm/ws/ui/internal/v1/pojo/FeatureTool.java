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
package com.ibm.ws.ui.internal.v1.pojo;

import org.owasp.esapi.ESAPI;

import com.ibm.websphere.jsonsupport.JSON;
import com.ibm.websphere.jsonsupport.JSONMarshallException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.TraceOptions;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ui.internal.RequestNLS;
import com.ibm.ws.ui.internal.TraceConstants;
import com.ibm.ws.ui.internal.v1.ITool;
import com.ibm.ws.ui.internal.v1.utils.Utils;
import com.ibm.ws.ui.internal.validation.InvalidToolException;

/**
 * Class design requirements:
 * 1. JSONable POJO. We leverage Jackson to serialize this object into JSON format.
 * 2. Container of system tool configuration data.
 */
@TraceOptions(messageBundle = TraceConstants.VALIDAITON_STRING_BUNDLE)
public class FeatureTool extends Bookmark {
    private static transient final TraceComponent tc = Tr.register(FeatureTool.class);

    /**
     * The feature name which provides the Tool.
     * <b>This is serialized by Jackson</b>
     */
    private String featureName = null;

    /**
     * The version of the feature which provides the Tool.
     * <b>This is serialized by Jackson</b>
     */
    private String featureVersion = null;

    /**
     * The short name of the feature which provides the Tool.
     * <b>This is serialized by Jackson</b>
     */
    private String featureShortName = null;

    /**
     * Default FeatureTool constructor.
     * Zero-argument constructor used by Jackson.
     * Should only be invoked directly in unit test.
     */
    FeatureTool() {
        // No initialization of internal data structures as Jackson will set them via setters.
        objectType = TYPE_FEATURE_TOOL;
    }

    /**
     * Constructs the FeatureTool's id.
     * 
     * @param featureName See {@link #featureName}.
     * @param featureVersion See {@link #featureVersion}.
     * @return The FeatureTool's id.
     */
    private static String createId(final String featureName, final String featureVersion) {
        // TODO: Eventually, we'll want to use the featureShortName as the ID whenever possible.
        // This can not be done yet because if we change the ID existing toolboxes break. This needs
        // to be coded around and this work is not yet done.
        if (featureName != null && !featureName.isEmpty() && featureVersion != null && !featureVersion.isEmpty()) {
            return Utils.urlEncode(featureName + "-" + featureVersion);
        } else {
            return null;
        }
    }

    /**
     * Construct a tool representation.
     * 
     * @param featureName See {@link #featureName}.
     * @param featureVersion See {@link #featureVersion}.
     * @param name See {@link #name}.
     * @param url See {@link #url}.
     * @param icon See {@link #icon}.
     * @param description See {@link #description}.
     */
    public FeatureTool(final String featureName, final String featureVersion, final String featureShortName,
                       final String name, final String url, final String icon, final String description) {
        this(createId(featureName, featureVersion), ITool.TYPE_FEATURE_TOOL, featureName, featureVersion, featureShortName, name, url, icon, description);
    }

    /**
     * Construct a FeatureTool representation.
     * 
     * @param id the Tool's ID
     * @param type Set the Tool's type.
     * @param featureName See {@link #featureName}.
     * @param featureVersion See {@link #featureVersion}.
     * @param name See {@link #name}.
     * @param url See {@link #url}.
     * @param icon See {@link #icon}.
     * @param description See {@link #description}.
     */
    public FeatureTool(final String id, final String type, final String featureName, final String featureVersion,
                       final String featureShortName, final String name, final String url, final String icon, final String description) {
        super(id, type, name, url, icon, description);
        objectType = TYPE_FEATURE_TOOL;
        this.featureName = featureName;
        this.featureVersion = featureVersion;
        this.featureShortName = featureShortName;
    }

    /**
     * Setter used by Jackson when deserializing.
     * Intentionally private visibility.
     */
    @SuppressWarnings("unused")
    private void setFeatureName(final String featureName) {
        this.featureName = featureName;
    }

    /**
     * Retrieve the feature that maps to this tool.
     * This can be null if the tool wasn't installed via a feature.
     * 
     * @return the feature name that corresponds to the this tool
     */
    public String getFeatureName() {
        return featureName;
    }

    /**
     * Setter used by Jackson when deserializing.
     * Intentionally private visibility.
     */
    @SuppressWarnings("unused")
    private void setFeatureVersion(final String featureVersion) {
        this.featureVersion = featureVersion;
    }

    /**
     * Retrieve the tool's feature version.
     * 
     * @return the Tool's feature version
     */
    public String getFeatureVersion() {
        return featureVersion;
    }

    /**
     * Setter used by Jackson when deserializing.
     * Intentionally private visibility.
     */
    @SuppressWarnings("unused")
    private void setFeatureShortName(final String featureShortName) {
        this.featureShortName = featureShortName;
    }

    /**
     * Retrieve the tool's feature short name.
     * 
     * @return the Tool's feature short name
     */
    public String getFeatureShortName() {
        return featureShortName;
    }

    /**
     * Determines which fields are null and/or blank.
     * 
     * @return String comma-separated list of field names that are not valid
     */
    @Trivial
    private String listNullFields() {
        StringBuilder sb = new StringBuilder();
        if (featureName == null || featureName.isEmpty()) {
            addTo(sb, "featureName");
        }
        if (featureVersion == null || featureVersion.isEmpty()) {
            addTo(sb, "featureVersion");
        }
        return sb.toString();
    }

    /**
     * Validates which fields contain any malicious data.
     * <p>
     * This does not check version as version is already checked for syntax
     * of x.y.z.
     * 
     * @return String comma-separated list of field names that are not valid
     */
    private String listXSSFields() {
        StringBuilder sb = new StringBuilder();
        if (!containsNoXSS(featureName)) {
            addTo(sb, "featureName");
        }
        if (!containsNoXSS(featureVersion)) {
            addTo(sb, "featureVersion");
        }
        return sb.toString();
    }

    /**
     * Check to see if the String is a valid version for the tool.
     * 
     * @param value The String to check
     * @return {@code true} if the String is a valid version.
     */
    private boolean isValidVersion(final String value) {
        return ESAPI.validator().isValidInput("VERSION",
                                              value,
                                              "UIToolVersion",
                                              MAX_LENGTH,
                                              false);
    }

    /** {@inheritDoc} */
    @Override
    public void validateSelf() throws InvalidToolException {
        super.validateSelf(false, false);

        // check required fields for this object
        final String badRQDfields = listNullFields();
        if (badRQDfields.length() != 0) {
            throw new InvalidToolException(RequestNLS.formatMessage(tc, "RQD_FIELDS_MISSING", badRQDfields));
        }

        // check for XSS and other malicious data
        final String badXSSfields = listXSSFields();
        if (badXSSfields.length() != 0) {
            throw new InvalidToolException(RequestNLS.formatMessage(tc, "XSS_DETECTED", badXSSfields));
        }

        // check version syntax: x, x.y, and x.y.z
        // split on .
        // verify at most 3 parts and each part numeric
        if (!isValidVersion(featureVersion)) {
            throw new InvalidToolException(RequestNLS.formatMessage(tc, "VERSION_NOT_VALID", featureVersion));
        }

        // A FeatureTool's id must match its featureName-featureVersion
        final String expectedId = createId(featureName, featureVersion);
        if (!id.equals(expectedId)) {
            throw new InvalidToolException(RequestNLS.formatMessage(tc, "ID_NOT_VALID", id, expectedId));
        }

        // Features are not required to have an IBM-ShortName, so we do not validate its value
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof FeatureTool) {
            final FeatureTool that = (FeatureTool) o;

            boolean sameFields = true;
            sameFields &= (this.featureName == that.featureName) || (this.featureName != null && this.featureName.equals(that.featureName));
            sameFields &= (this.featureVersion == that.featureVersion) || (this.featureVersion != null && this.featureVersion.equals(that.featureVersion));
            sameFields &= (this.featureShortName == that.featureShortName) || (this.featureShortName != null && this.featureShortName.equals(that.featureShortName));

            return super.equals(that) && sameFields;
        } else {
            return false;
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Gets this Object's JSON using the given ObjectMapper.
     * 
     * @param mapper
     */
    @Trivial
    String getMyJson(final JSON jsonService) {
        try {
            return jsonService.stringify(this);
        } catch (JSONMarshallException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Unexpected JSONMarshallException during Tool.toString()", super.toString(), e);
            }
        }
        return null;
    }

    @Trivial
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("FeatureTool {\"id\":");
        sb.append(getJSONString(id));
        sb.append(",\"type\":");
        sb.append(getJSONString(type));
        sb.append(",\"featureName\":");
        sb.append(getJSONString(featureName));
        sb.append(",\"featureVersion\":");
        sb.append(getJSONString(featureVersion));
        sb.append(",\"featureShortName\":");
        sb.append(getJSONString(featureShortName));
        sb.append(",\"name\":");
        sb.append(getJSONString(name));
        sb.append(",\"url\":");
        sb.append(getJSONString(url));
        sb.append(",\"icon\":");
        sb.append(getJSONString(icon));
        sb.append(",\"description\":");
        sb.append(getJSONString(description));
        sb.append("}");
        return sb.toString();
    }

}
