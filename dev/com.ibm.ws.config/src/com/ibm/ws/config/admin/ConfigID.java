/*******************************************************************************
 * Copyright (c) 2009, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.config.admin;

import java.io.Serializable;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.config.admin.internal.ConfigAdminConstants;

//@formatter:off
@Trivial
public class ConfigID implements Serializable {
    private static final long serialVersionUID = 1L;

    //

    /**
     * Context free configuration ID: This ID has only a factory PID.  No
     * parent and no child attribute are specified.
     *
     * @param factoryPid The factory PID of the configuration.
     */
    @Trivial
    public ConfigID(String factoryPid) {
        this(null, factoryPid, null, null);
    }

    /**
     * Context free configuration ID: This ID has a factory PID and a PID.
     * No parent and no child attribute are specified.
     *
     * @param factoryPid The factory PID of the configuration.
     * @param pid The PID of the configuration.
     */
    @Trivial
    public ConfigID(String factoryPid, String pid) {
        this(null, factoryPid, pid, null);
    }

    /**
     * A configuration ID for an element which is a child of a specified
     * configuration, identified by the configuration ID of that parent
     * configuration.
     *
     * @param parentId The ID of the parent configuration.
     * @param factoryPid The factory PID of the configuration.
     * @param pid The PID of the configuration.
     */
    @Trivial
    public ConfigID(ConfigID parentId, String factoryPid, String pid) {
        this(parentId, factoryPid, pid, null);
    }

    /**
     * Fully parameterized initializer.
     *
     * @param parentId The ID of the parent configuration.
     * @param factoryPid The factory PID of the configuration.
     * @param pid The PID of the configuration.
     * @param childAttribute The attribute of the parent configuration
     *     which reaches the configuration of this ID.  Only set when
     *     a parent ID is provided.
     */
    public ConfigID(ConfigID parentId, String factoryPid, String pid, String childAttribute) {
        this.factoryPid = factoryPid;
        this.pid = pid;

        this.parentId = parentId;
        this.childAttribute = childAttribute;

        this.hashCode = computeHash();
        this.baseString = computeBase();
    }

    private final int hashCode;

    @Override
    @Trivial
    public int hashCode() {
        return hashCode;
    }

    private static final int PRIME = 31;

    @Trivial
    private int computeHash() {
        int hash = 1;
        hash = PRIME * hash + ((pid == null) ? 0 : pid.hashCode());
        hash = PRIME * hash + ((factoryPid == null) ? 0 : factoryPid.hashCode());
        hash = PRIME * hash + ((parentId == null) ? 0 : parentId.hashCode());
        hash = PRIME * hash + ((childAttribute == null) ? 0 : childAttribute.hashCode());
        return hash;
    }

    @Override
    @Trivial
    public boolean equals(Object obj) {
        if ( obj == null ) {
            return false;
        } else if ( this == obj ) {
            return true;
        } else if ( !(obj instanceof ConfigID) ) {
            return false;
        }

        ConfigID other = (ConfigID) obj;

        if ( !eq(pid, other.pid)) {
            return false;
        } else if ( !eq(factoryPid, other.factoryPid) ) {
            return false;
        } else if ( !eq(parentId, other.parentId) ) {
            return false;
        } else if ( !eq(childAttribute, other.childAttribute) ) {
            return false;
        } else {
            return true;
        }
    }

    @Trivial
    private static final <T> boolean eq(T t0, T t1) {
        if ( t0 == null ) {
            if ( t1 != null ) {
                return false;
            } else {
                return true;
            }
        } else { // t0 != null
            if ( t1 == null ) {
                return false;
            } else {
                return t0.equals(t1);
            }
        }
    }

    /**
     * Generate a print string for the configuration ID.
     *
     * The print string is not just for debugging: The value
     * is stored into configurations as property
     * {@link ConfigAdminConstants#CFG_CONFIG_INSTANCE_ID}.  See
     * {@link com.ibm.ws.config.admin.internal.ExtendedConfigurationImpl#getFullId()}.
     *
     * The print string has the format:
     *
     * <code>
     *   baseString :: factoryPid
     *              :: factoryPid[pid]
     *              :: factoryPid(childAttribute)[pid]
     *              :: factoryPid(childAttribute)
     *
     *   printString :: baseString
     *               :: parentId.printString/baseString
     * </code>
     *
     * For example, omitting child attributes, a configuration having
     * two parents will have a full ID print string of:
     *
     * <code>
     *   grand_fPid[grand_pid]/parent_fPid[parent_pid]/child_fPid[child_pid]
     * </code>
     *
     * @return The print string of this ID.  Also, the property value used
     *     for this ID.
     */
    @Override
    @Trivial
    public String toString() {
        StringBuilder builder = new StringBuilder();
        appendTo(builder);
        return builder.toString();
    }

    @Trivial
    protected void appendTo(StringBuilder builder) {
        if ( parentId != null ) {
            parentId.appendTo(builder);
            builder.append('/');
        }
        builder.append( getBase() );
    }

    private final String baseString;

    @Trivial
    public String getBase() {
        return baseString;
    }

    /**
     * Compute the base print string of this configuration ID.
     *
     * @return The base print string of this configuration ID.
     */
    @Trivial
    private String computeBase() {
        StringBuilder builder = new StringBuilder();

        builder.append(factoryPid);

        if ( childAttribute != null ) {
            builder.append('(').append(childAttribute).append(')');
        }

        if ( pid != null ) {
            builder.append('[').append(pid).append(']');
        }

        return builder.toString();
    }

    //

    private final String factoryPid;
    private final String pid;

    @Trivial
    public String getPid() {
        return factoryPid;
    }

    @Trivial
    public String getId() {
        return pid;
    }

    private final ConfigID parentId;
    private final String childAttribute;

    @Trivial
    public ConfigID getParent() {
        return parentId;
    }

    @Trivial
    public String getChildAttribute() {
        return childAttribute;
    }

    //

    // element :: factoryPid
    //         :: factoryPid[pid]
    //         :: factoryPid(childAttribute)[pid]
    //         :: factoryPid(childAttribute)
    //
    // property :: (element '/')* element

    public static ConfigID fromProperty(String property) {
        ConfigID parentId = null;
        int nextStart = 0;
        int nextEnd;
        while ( (nextEnd = property.indexOf('/', nextStart)) != -1 ) {
            parentId = parseId(parentId, property, nextStart, nextEnd);
            nextStart = nextEnd + 1;
        }
        ConfigID id = parseId(parentId, property, nextStart, property.length());
        return id;
    }

    // factoryPid(childAttribute)[pid]
    private static ConfigID parseId(ConfigID parentId,
                                    String property, int start, int end) {

        int cStart = -1;
        int cEnd = -1;

        int pStart = -1;
        int pEnd = -1;

        for ( int cNo = start; cNo < end; cNo++ ) {
            char c = property.charAt(cNo);
            if ( c == '(' ) {
                cStart = cNo;
            } else if ( c == ')' ) {
                cEnd = cNo;
            } else if ( c == '[' ) {
                pStart = cNo;
            } else if ( c == ']' ) {
                pEnd = cNo;
            }
        }

        int fEnd;
        if ( cStart == -1 ) {
            if ( pStart == -1 ) {
                fEnd = end;
            } else {
                fEnd = pStart;
            }
        } else {
            fEnd = cStart;
        }
        String factoryPid = property.substring(start, fEnd);

        String childAttribute;
        if ( (cStart != -1) && (cEnd != -1) ) {
            childAttribute = property.substring(cStart + 1, cEnd);
        } else {
            childAttribute = null;
        }

        String pid;
        if ( (pStart != -1) && (pEnd != -1) ) {
            pid = property.substring(pStart + 1, pEnd);
        } else {
            pid = null;
        }

        return new ConfigID(parentId, factoryPid, pid, childAttribute);
    }
}
//@formatter:on