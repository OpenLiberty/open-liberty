/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal.v1.pojo;

import java.util.Locale;

import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Validator;

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
 * Represents a Bookmark.
 * <p>
 * Example JSON: {id:"id",type:"type",name:"name",url:"http://",icon:"http://"}
 * Example JSON: {id:"id",type:"type",name:"name",url:"http://",icon:"http://",description:"..."}
 */
@TraceOptions(messageBundle = TraceConstants.VALIDAITON_STRING_BUNDLE)
public class Bookmark extends ToolEntry {
    private static transient final TraceComponent tc = Tr.register(Bookmark.class);

    static transient final String INVALID_CHARACTERS = ".*[~&:;\\\\/?{}<>\\[\\]].*";

    /**
     * The Object's supported type. Override to support polymorphism.
     */
    transient String objectType = TYPE_BOOKMARK;

    /**
     * The bookmark's name.
     * <b>This is serialized by Jackson</b>
     */
    String name;

    /**
     * The bookmark's address.
     * <b>This is serialized by Jackson</b>
     */
    String url;

    /**
     * The bookmark's icon address.
     * <b>This is serialized by Jackson</b>
     */
    String icon;

    /**
     * The bookmark's description.
     * <b>This is serialized by Jackson</b>
     */
    String description;

    /**
     * Default Bookmark constructor.
     * Zero-argument constructor used by Jackson.
     * Should only be invoked directly in unit test.
     * 
     * @param initialzeDefaults
     */
    @Trivial
    Bookmark() {
        // No initialization of internal data structures as Jackson will set them via setters.
    }

    /**
     * Construct a Bookmark. ID defaults to the name, and type is set to {@value ITool#TYPE_BOOKMARK}.
     * 
     * @param name See {@link #name}.
     * @param url See {@link #url}.
     * @param icon See {@link #icon}.
     */
    @Trivial
    public Bookmark(final String name, final String url, final String icon) {
        this(Utils.urlEncode(name), TYPE_BOOKMARK, name, url, icon, null);
    }

    /**
     * Construct a Bookmark. ID defaults to the name, and type is set to {@value ITool#TYPE_BOOKMARK}.
     * 
     * @param name See {@link #name}.
     * @param url See {@link #url}.
     * @param icon See {@link #icon}.
     * @param description See {@link #description}.
     */
    @Trivial
    public Bookmark(final String name, final String url, final String icon, final String description) {
        this(Utils.urlEncode(name), TYPE_BOOKMARK, name, url, icon, description);
    }

    /**
     * Construct a Bookmark or an subclass. Should only be invoked directly in unit test.
     * 
     * @param id The Tool ID, must be URL encoded. Must be the Tool's name.
     * @param type Set the Tool's type.
     * @param name See {@link #name}.
     * @param url See {@link #url}.
     * @param icon See {@link #icon}.
     * @param description See {@link #description}.
     */
    @Trivial
    Bookmark(final String id, final String type, final String name, final String url, final String icon, final String description) {
        super(id, type);
        this.name = name;
        this.url = url;
        this.icon = icon;
        this.description = description;
    }

    /**
     * Setter used by Jackson when deserializing.
     * Intentionally private visibility.
     */
    @Trivial
    private void setName(final String name) {
        this.name = name;
    }

    /** {@inheritDoc} */
    @Trivial
    public String getName() {
        return name;
    }

    /**
     * Setter used by Jackson when deserializing.
     * Intentionally private visibility.
     */
    @Trivial
    private void setURL(final String url) {
        this.url = url;
    }

    /** {@inheritDoc} */
    @Trivial
    public String getURL() {
        return url;
    }

    /**
     * Setter used by Jackson when deserializing.
     * Intentionally private visibility.
     */
    @Trivial
    private void setIcon(final String icon) {
        this.icon = icon;
    }

    /** {@inheritDoc} */
    @Trivial
    public String getIcon() {
        return icon;
    }

    /**
     * Setter used by Jackson when deserializing.
     * Intentionally private visibility.
     */
    @Trivial
    private void setDescription(final String description) {
        this.description = description;
    }

    /** {@inheritDoc} */
    @Trivial
    public String getDescription() {
        return description;
    }

    /**
     * Determines which fields are null and/or blank.
     * 
     * @return String comma-separated list of field names that are not valid
     */
    @Trivial
    private String listNullFields(final boolean descriptionOptional) {
        final StringBuilder sb = new StringBuilder();
        if (name == null || name.isEmpty()) {
            addTo(sb, "name");
        }
        if (url == null || url.isEmpty()) {
            addTo(sb, "url");
        }
        if (icon == null || icon.isEmpty()) {
            addTo(sb, "icon");
        }
        if (!descriptionOptional) {
            if (description == null || description.isEmpty()) {
                addTo(sb, "description");
            }
        }
        return sb.toString();
    }

    /**
     * Check to see if the String is a valid URL for the tool.
     * 
     * @param value The String to check
     * @return {@code true} if the String is a valid URL.
     */
    private boolean isValidURL(final String value) {
        final String lowerURL = value.toLowerCase();
        final Validator v = ESAPI.validator();
        // If the string contains :/ assume that it's an absolute URL.
        if (lowerURL.contains(":/")) {
            // Only supporting HTTP and HTTPS protocols.
            return v.isValidInput("URL",
                                  lowerURL,
                                  "UIToolAbsoluteURLRule",
                                  MAX_LENGTH,
                                  false);
        }
        final String[] relativeURLRules = { "UIToolRelativeURLRule1",
                                           "UIToolRelativeURLRule2",
                                           "UIToolRelativeURLRule3",
                                           "UIToolRelativeURLRule4" };
        // Must not be equal to "..", start with "../", end with "/.." or contain "/../".
        for (int i = 0; i < relativeURLRules.length; ++i) {
            if (!v.isValidInput("URL",
                                lowerURL,
                                relativeURLRules[i],
                                MAX_LENGTH,
                                true)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Simple check to see if the String contains no XSS.
     * 
     * @param value The String to check
     * @return {@code true} if the String contains no XSS characters.
     */
    boolean containsNoXSS(final String value) {
        if (value != null) {
            return ESAPI.validator().isValidInput("FIELD",
                                                  value.toLowerCase(Locale.ENGLISH),
                                                  "NoXSSContent",
                                                  MAX_LENGTH,
                                                  true);
        } else {
            return true;
        }
    }

    /**
     * Check to see if the String contains valid characters
     * 
     * @param value The String to check
     * @return {@code true} if the String contains valid characters.
     */
    boolean containsValidCharacters(final String value) {
        return ESAPI.validator().isValidInput("FIELD",
                                              value,
                                              "UIToolValidCharacters",
                                              MAX_LENGTH,
                                              true);
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
        final StringBuilder sb = new StringBuilder();
        if (!containsNoXSS(name)) {
            addTo(sb, "name");
        }
        if (!containsNoXSS(url)) {
            addTo(sb, "url");
        }
        if (!containsNoXSS(icon)) {
            addTo(sb, "icon");
        }
        if (!containsNoXSS(description)) {
            addTo(sb, "description");
        }
        return sb.toString();
    }

    /** {@inheritDoc} */
    @Override
    public void validateSelf() throws InvalidToolException {
        validateSelf(true, true);
    }

    /** {@inheritDoc} */
    void validateSelf(final boolean shouldIdCheck, final boolean descriptionOptional) throws InvalidToolException {
        super.validateSelf();

        // Not the expected type, the wrong Object is in use.
        if (!objectType.equals(type)) {
            throw new InvalidToolException(RequestNLS.formatMessage(tc, "TYPE_NOT_CORRECT", objectType, type));
        }

        // check required fields for this object
        final String badRQDfields = listNullFields(descriptionOptional);
        if (badRQDfields.length() != 0) {
            throw new InvalidToolException(RequestNLS.formatMessage(tc, "RQD_FIELDS_MISSING", badRQDfields));
        }

        // check for XSS and other malicious data
        final String badXSSfields = listXSSFields();
        if (badXSSfields.length() != 0) {
            throw new InvalidToolException(RequestNLS.formatMessage(tc, "XSS_DETECTED", badXSSfields));
        }

        // check URL field syntax (url and icon fields)
        if (!isValidURL(url)) {
            throw new InvalidToolException(RequestNLS.formatMessage(tc, "URL_NOT_VALID", url));
        }

        // Check to see if we have an icon set. If not, then configure the default icon.
        if (!isValidURL(icon)) {
            throw new InvalidToolException(RequestNLS.formatMessage(tc, "ICON_NOT_VALID", icon));
        }

        // A URL's id must match its name
        if (shouldIdCheck && !id.equals(Utils.urlEncode(name))) {
            throw new InvalidToolException(RequestNLS.formatMessage(tc, "ID_NOT_VALID", id, name));
        }

        // name does not contain ~ & : ; / \ ? {} < > []
        if (!containsValidCharacters(name)) {
            throw new InvalidToolException(RequestNLS.formatMessage(tc, "NAME_NOT_VALID", name, INVALID_CHARACTERS));
        }

        if (description != null) {
            // description does not contain ~ & : ; / \ ? {} < > []
            if (!containsValidCharacters(description)) {
                throw new InvalidToolException(RequestNLS.formatMessage(tc, "DESCRIPTION_NOT_VALID", description, INVALID_CHARACTERS));
            }
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof Bookmark) {
            final Bookmark that = (Bookmark) o;

            boolean sameFields = true;
            sameFields &= (this.name == that.name) || (this.name != null && this.name.equals(that.name));
            sameFields &= (this.url == that.url) || (this.url != null && this.url.equals(that.url));
            sameFields &= (this.icon == that.icon) || (this.icon != null && this.icon.equals(that.icon));
            sameFields &= (this.description == that.description) || (this.description != null && this.description.equals(that.description));

            return super.equals(o) && sameFields;
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

    @Trivial
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Bookmark {\"id\":");
        sb.append(getJSONString(id));
        sb.append(",\"type\":");
        sb.append(getJSONString(type));
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
