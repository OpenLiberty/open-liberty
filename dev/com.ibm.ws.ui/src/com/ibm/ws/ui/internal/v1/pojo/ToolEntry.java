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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.TraceOptions;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ui.internal.RequestNLS;
import com.ibm.ws.ui.internal.TraceConstants;
import com.ibm.ws.ui.internal.v1.ITool;
import com.ibm.ws.ui.internal.validation.InvalidToolException;

/**
 * Represents a basic tool entry (which consists of an ID and a type).
 * <p>
 * Example JSON: {id:"id",type:"type"}
 */
@TraceOptions(messageBundle = TraceConstants.VALIDAITON_STRING_BUNDLE)
public class ToolEntry implements ITool {
    private static transient final TraceComponent tc = Tr.register(ToolEntry.class);
    // TODO: ESAPI requires that max length be specified. What is a reasonable value for max length?
    static final int MAX_LENGTH = Integer.MAX_VALUE;

    /**
     * The Tool's ID. Must be URL encoded.
     * <b>This is serialized by Jackson</b>
     */
    String id;

    /**
     * The ToolEntry type. One of 3 values:
     * <ul>
     * <li>{@link ITool#TYPE_FEATURE_TOOL}</li>
     * <li>{@link ITool#TYPE_BOOKMARK}</li>
     * </ul>
     * <b>This is serialized by Jackson</b>
     */
    String type;

    /**
     * Default ToolEntry constructor.
     * Zero-argument constructor used by Jackson.
     * Should only be invoked directly in unit test.
     * 
     * @param initialzeDefaults
     */
    @Trivial
    ToolEntry() {
        // No initialization of internal data structures as Jackson will set them via setters.
    }

    /**
     * Construct a ToolEntry.
     * 
     * @param id See {@link #id}.
     * @param type See {@link #type}.
     */
    @Trivial
    public ToolEntry(final String id, final String type) {
        this.id = id;
        this.type = type;
    }

    /**
     * Setter used by Jackson when deserializing.
     * Intentionally private visibility.
     */
    @Trivial
    private void setId(String id) {
        this.id = id;
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public String getId() {
        return id;
    }

    /**
     * Setter used by Jackson when deserializing.
     * Intentionally private visibility.
     */
    @Trivial
    private void setType(final String type) {
        this.type = type;
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public String getType() {
        return type;
    }

    /**
     * Adds a 'thing' String to the StringBuilder.
     * 
     * @param sb
     * @param thing
     */
    @Trivial
    void addTo(final StringBuilder sb, final String thing) {
        if (sb.length() != 0) {
            sb.append(", ");
        }
        sb.append(thing);
    }

    /**
     * Determines which fields are null and/or blank.
     * 
     * @return String comma-separated list of field names that are not valid
     */
    @Trivial
    private String listNullFields() {
        final StringBuilder sb = new StringBuilder();
        if (id == null || id.trim().isEmpty()) {
            addTo(sb, "id");
        }
        if (type == null || type.trim().isEmpty()) {
            addTo(sb, "type");
        }
        return sb.toString();
    }

    /**
     * Determines if the type is recognized.
     * 
     * @param type The type to check
     * @return {@code true} if the type is a recognized, supported type, {@code false} otherwise.
     */
    @Trivial
    private boolean isRecognizedType(final String type) {
        return TYPE_FEATURE_TOOL.equals(type) || TYPE_BOOKMARK.equals(type);
    }

    /** {@inheritDoc} */
    @Override
    public void validateSelf() throws InvalidToolException {
        // check required fields
        final String badRQDfields = listNullFields();
        if (badRQDfields.length() != 0) {
            throw new InvalidToolException(RequestNLS.formatMessage(tc, "RQD_FIELDS_MISSING", badRQDfields));
        }
        if (!isRecognizedType(type)) {
            throw new InvalidToolException(RequestNLS.formatMessage(tc, "TYPE_NOT_VALID", type));
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof ToolEntry) {
            final ToolEntry that = (ToolEntry) o;

            boolean sameFields = true;
            sameFields &= (this.id == that.id) || (getId() != null && getId().equals(that.id));
            sameFields &= (this.type == that.type) || (this.type != null && this.type.equals(that.type));
            return sameFields;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc} <p>
     * A tool with no ID is not valid.
     * Therefore we don't really need to worry about the hash code.
     */
    @Trivial
    @Override
    public int hashCode() {
        return (id == null) ? 0 : id.hashCode();
    }

    /**
     * Returns the appropriate String representation of the String.
     * Notably, if the str is null, then "null" (not quoted) is returned,
     * otherwise the String is quoted.
     * 
     * @param str
     * @return
     */
    @Trivial
    String getJSONString(final String str) {
        if (str == null) {
            return "null";
        } else {
            return "\"" + str + "\"";
        }
    }

    /**
     * Returns the JSON representation of the ToolEntry.
     */
    @Trivial
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ToolEntry {\"id\":");
        sb.append(getJSONString(id));
        sb.append(",\"type\":");
        sb.append(getJSONString(type));
        sb.append("}");
        return sb.toString();
    }

}
