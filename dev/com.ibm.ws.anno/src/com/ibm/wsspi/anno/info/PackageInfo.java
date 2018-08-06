/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corporation 2011, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.wsspi.anno.info;

/**
 * <p>Info object type representing a java package.</p>
 * 
 * <p>The name and qualified name of a package info object are the same.</p>
 */
public interface PackageInfo extends Info {
    /**
     * <p>Naming constant: The prefix for the <code>java</code> package,
     * (with a trailing ".").</p>
     */
    String JAVA_CLASS_PREFIX = "java.";

    /**
     * <p>Naming constant: The prefix for the <code>javax</code> package,
     * (with a trailing ".").</p>
     */
    String JAVAX_CLASS_PREFIX = "javax.";

    /**
     * <p>Naming constant: The prefix for the <code>javax.ejb</code> package,
     * (with a trailing ".").</p>
     */
    String JAVAX_EJB_CLASS_PREFIX = "javax.ejb.";

    /**
     * <p>Naming constant: The prefix for the <code>javax.servlet</code> package,
     * (with a trailing ".").</p>
     */
    String JAVAX_SERVLET_CLASS_PREFIX = "javax.servlet.";

    /**
     * <p>Tell if this package info was created to represent a package which
     * either could not be loaded, or which has no declared package information.</p>
     * 
     * @return True if this package object is artificial. Otherwise, false.
     */
    boolean getIsArtificial();

    /**
     * <p>Tell if this package info represents a failed load. This is a special
     * case for artificial packages, and represents the case when declared package
     * information is available, but could not be loaded.</p>
     * 
     * @return True if this package object is for a failed load. Otherwise, false.
     */
    boolean getForFailedLoad();
}
