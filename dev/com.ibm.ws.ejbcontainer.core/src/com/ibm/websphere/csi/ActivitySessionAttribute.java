/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 *  <code>ActivitySessionAttribute</code> defines legal values for the 
 *  session attributes passed to <code>UOWControlImpl</code>. <p>
 */

package com.ibm.websphere.csi;

public class ActivitySessionAttribute {

    public static final int NOT_SUPPORTED = 0;
    public static final int BEAN_MANAGED = 1;
    public static final int REQUIRED = 2;
    public static final int SUPPORTS = 3;
    public static final int REQUIRES_NEW = 4;
    public static final int MANDATORY = 5;
    public static final int NEVER = 6;
    public static final int UNKNOWN = 7;

    public static final ActivitySessionAttribute AS_NOT_SUPPORTED =
                    new ActivitySessionAttribute(NOT_SUPPORTED, "AS_NOT_SUPPORTED");

    public static final ActivitySessionAttribute AS_BEAN_MANAGED =
                    new ActivitySessionAttribute(BEAN_MANAGED, "AS_BEAN_MANAGED");

    public static final ActivitySessionAttribute AS_REQUIRED =
                    new ActivitySessionAttribute(REQUIRED, "AS_REQUIRED");

    public static final ActivitySessionAttribute AS_SUPPORTS =
                    new ActivitySessionAttribute(SUPPORTS, "AS_SUPPORTS");

    public static final ActivitySessionAttribute AS_REQUIRES_NEW =
                    new ActivitySessionAttribute(REQUIRES_NEW, "AS_REQUIRES_NEW");

    public static final ActivitySessionAttribute AS_MANDATORY =
                    new ActivitySessionAttribute(MANDATORY, "AS_MANDATORY");

    public static final ActivitySessionAttribute AS_NEVER =
                    new ActivitySessionAttribute(NEVER, "AS_NEVER");

    // --------------------------------------------------------------        
    // AS_UNKNOWN is used to indicate that the application has not
    // specified any activity session policies (e.g. a plain Java EE
    // application which does not use any WebSphere programming model
    // extensions).   
    // --------------------------------------------------------------

    public static final ActivitySessionAttribute AS_UNKNOWN =
                    new ActivitySessionAttribute(UNKNOWN, "AS_UNKNOWN");

    /**
     * Unique value for each legal <code>ActivitySessionAttribute</code> for
     * fast lookups.
     */

    private int value;
    private String name;
    private static final int numAttrs = 8;

    /**
     * Construct new <code>ActivitySessionAttribute</code> instance with
     * the given unique value. <p>
     */

    private ActivitySessionAttribute(int value, String s) {

        this.value = value;
        this.name = s;
    }

    /**
     * The getValue method returns unique value for this <code>ActivitySessionAttribute</code>. <p>
     */

    public int getValue() {

        return value;
    }

    /**
     * The toString method returns a string representation of this
     * <code>ActivitySessionAttribute</code>. <p>
     */

    public String toString() {

        return name;
    }

    /**
     * The getNumAttrs method returns number of possible <code>ActivitySessionAttribute</code>. <p>
     */

    public static int getNumAttrs() {

        return numAttrs;
    }

} // ActivitySessionAttribute

