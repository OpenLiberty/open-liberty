/*******************************************************************************
 * Copyright (c) 2018,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.tests.anno.caching.util;

public interface AnnoCacheLocations {

    // Constants from:
    //   open-liberty/dev/com.ibm.ws.anno/src/
    //     com/ibm/wsspi/anno/targets/cache/TargetCache_ExternalConstants.java

    String CACHE_NAME = "anno";

    String APP_PREFIX = "A_";
    String APP_SUFFIX = "_A";

    String MOD_PREFIX = "M_";
    String MOD_SUFFIX = "_M";

    String CON_PREFIX = "C_";
    String CON_SUFFIX = "_C";

    String RESOLVED_REFS_NAME = "resolved";
    String UNRESOLVED_REFS_NAME = "unresolved";
    String CONTAINERS_NAME = "containers";

    String SEED_RESULT_NAME = "seed";
    String PARTIAL_RESULT_NAME = "partial";
    String EXCLUDED_RESULT_NAME = "excluded";
    String EXTERNAL_RESULT_NAME = "external";

    String TIMESTAMP_NAME = "stamp";
    String CLASS_REFS_NAME = "classes";
    String ANNO_TARGETS_NAME = "targets";

    String QUERIES_NAME = "queries";
}
