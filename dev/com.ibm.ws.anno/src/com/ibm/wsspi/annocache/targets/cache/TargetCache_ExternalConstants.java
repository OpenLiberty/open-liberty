/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.annocache.targets.cache;

/**
 * <p>Constants used to name cache files.</p>
 *
 * <p>Folders are created in three tiers.  Folder names are generated using a
 * combination of prefix + encoded name.  For example, at the root level,
 * application folders names use the pattern "APP_" + encode(applicationName).</p>
 *
 * <p>Three folder tiers are provided, with a final, forth, tier of container details
 * beneath the third folder tier:</p>
 *

 * <p>Often, a name is immediately available for folders.  For example,
 * modules have relative URIs which are used as names, and nested
 * jars of a module have relative URIs which are used as names.  When
 * a name is not immediately available, a constant name is used according
 * to the particular case:</p>
 *
 * <p>TBD: Naming for root containers ("/" for EJB, CLIENT, and CONNECTOR).
 *
 * <p>Results are assigned specific names and are saved the same as
 * intermediate container results:  Seed, partial, excluded, and external
 * results use the names "seed", "partial", "excluded", and "external",
 * and do not use the "CON_" prefix.</p>
 *
 * <p>Specific names are used for specific generated data, in all cases
 * absent of a prefix.  The containers listing of a module uses the
 * name "containers".  The time stamp data of a container uses the
 * name "time.stamp".  The class reference data of a container uses the
 * name "class.refs".  The annotation targets data of a container uses
 * the name "anno.targets".  The annotation details data of a container
 * uses the name "anno.details".</p>
 *
 * <p>"resolved.refs" and "unresolved.refs" list class reference processing
 * results just prior to the tail external scan.</p>
 *
 * <p>Currently, no specific data is saved for applications.</p>
 */

public interface TargetCache_ExternalConstants {
    // Zero / root

    String CACHE_NAME_DEFAULT = "anno";

    // First / applications level:

    String APP_PREFIX = "A_";
    String APP_SUFFIX = "_A";

    // Second / module detail / containers level

    String MOD_PREFIX = "M_";
    String MOD_SUFFIX = "_M";

    String CON_PREFIX = "C_";
    String CON_SUFFIX = "_C";

    /** Named used for the module classes file. */
	String CLASSES_NAME = "classes";

    /** Name of the resolved class references file. */
    String RESOLVED_REFS_NAME = "resolved";
    /** Name of the unresolved class references file. */
    String UNRESOLVED_REFS_NAME = "unresolved";

    /** Name of the containers file. */
    String CONTAINERS_NAME = "containers";

    /** Name used when reading the root container. */
    String CANONICAL_ROOT_CONTAINER_NAME = "/";
    /** Un-encoded name for a root container data (with path "/"). */
    String ROOT_CONTAINER_NAME = "root";
    /** Un-encoded suggested module classes container name. */
    String CLASSES_CONTAINER_NAME = "classes";

    /** Un-encoded name for a the seed result data. */
    String SEED_RESULT_NAME = "seed";
    /** Un-encoded name for a the partial result data. */
    String PARTIAL_RESULT_NAME = "partial";
    /** Un-encoded name for a the excluded result data. */
    String EXCLUDED_RESULT_NAME = "excluded";
    /** The external results are not saved, but the name is included for completeness. */
    String EXTERNAL_RESULT_NAME = "external";

    // Third / container detail level

    /** Extension of time stamp files. */
    String TIMESTAMP_NAME = "stamp";
    /** The extension for jandex format cache data files. */
    String DATA_NAME_JANDEX = "jandex.idx";
    /** The extension for internal format cache data files. */
    String DATA_NAME_INTERNAL = "data";

    // Fourth / query log

    /** Name of the a queries log file. */
    String QUERIES_NAME = "queries";
}
