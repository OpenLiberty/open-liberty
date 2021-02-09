/*
 * Copyright (c) 1998, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

// Contributors:
//     tware - added to allow pluggage archive factory
package org.eclipse.persistence.config;

/**
 * This class provides the list of System properties that are recognized by EclipseLink.
 * @author tware
 *
 */
public class SystemProperties {

    /**
     * Configures the factory class we use to produce instances of org.eclispe.persistence.jpa.Archive
     * These instances are used to examine persistence units and the files within them and are used for discovery
     * of classes in the persistence unit
     * Allows user-provided ArchiveFactory and Archive
     */
    public static final String ARCHIVE_FACTORY = "eclipselink.archive.factory";

    /**
     * This property is used to debug weaving issues.   When it is set, weaved classes will be
     * output to the given path as they are weaved
     */
    public static final String WEAVING_OUTPUT_PATH = "eclipselink.weaving.output.path";

    /**
     * This property is used in conjunction with WEAVING_OUTPUT_PATH.  By default, existing classes
     * on the path provided to WEAVING_OUTPUT_PATH will not be overridden.  If this is set to true, they will be
     */
    public static final String WEAVING_SHOULD_OVERWRITE = "eclipselink.weaving.overwrite.existing";

    /**
     * This property can be used to tell EclipseLink to process classes in the ASM Default manner.  The fix for bug
     * 370975 changes EclipseLink's weaving support to use ASM itself to examine class hierarchies.  Setting this flag to
     * true will cause us to use the default reflection mechanism again.  This flag provides a means to workaround any issues encountered with
     * the ASM-based weaving introspection
     */
    public static final String WEAVING_REFLECTIVE_INTROSPECTION = "eclipselink.weaving.reflective-introspection";

    /**
     * This property is used in conjunction with
     * org.eclipse.persistence.sessions.IdentityMapAccessor.printIdentityMapLocks().
     * Setting this property will cause EclipseLink to record the stack trace of
     * the lock acquisition and print it along with the identity map locks. This
     * should only be set if the thread that owns a lock is not 'stuck' but
     * still owns the lock when a normal printIdentityMapLocks is done.
     *
     * This can also be set in code statically through ConcurrencyManager.setShouldTrackStack(true)
     */
    public static final String RECORD_STACK_ON_LOCK = "eclipselink.cache.record-stack-on-lock";

    /**
     * This property can be set to disable processing of X-Many relationship
     * attributes for Query By Example objects. In previous versions of
     * EclipseLink these attributes would have been ignored but as of this
     * release they will be processed into the expression.
     */
    public static final String DO_NOT_PROCESS_XTOMANY_FOR_QBE = "eclipselink.query.query-by-example.ignore-xtomany";

    /**
     * This property can be set to <code>false</code> to enable UPDATE call to set
     * foreign key value in the target row in unidirectional 1-Many mapping
     * with not nullable FK. In previous versions of EclipseLink this was
     * the default behaviour.
     * Allowed values are: true/false.
     */
    public static final String ONETOMANY_DEFER_INSERTS = "eclipselink.mapping.onetomany.defer-inserts";

    /**
     * This system property can be set to override target server platform set by the Java EE container
     * with the one either set in persistence.xml or auto detected.
     */
    public static final String ENFORCE_TARGET_SERVER = "eclipselink.target-server.enforce";
    
    /**
     * This system property can be set the specific time zone used by ConversionManager to convert 
     * LocalDateTime, OffsetDateTime, and OffsetTime types.
     */
    public static final String CONVERSION_USE_TIMEZONE = "org.eclipse.persistence.conversion.useTimeZone";
    
    /**
     * This system property can be set to restore ConversionManager behavior with converting 
     * LocalDateTime, OffsetDateTime, and OffsetTime types back to using the JVM's default time zone instead
     * of UTC.  This restores behavior prior to fixing Bug 538296.  This property is ignored if the
     * System Property CONVERSION_USE_TIMEZONE has been set.
     */
    public static final String CONVERSION_USE_DEFAULT_TIMEZONE = "org.eclipse.persistence.conversion.useDefaultTimeZoneForJavaTime";
}
