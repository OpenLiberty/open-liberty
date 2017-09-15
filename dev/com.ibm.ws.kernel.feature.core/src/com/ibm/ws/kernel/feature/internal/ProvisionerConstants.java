/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal;

public interface ProvisionerConstants {
    /**
     * Strings for trace and nls messages (for those classes w/in the bundle that
     * use Tr)
     */
    String NLS_PROPS = "com.ibm.ws.kernel.feature.internal.resources.ProvisionerMessages",
                    TR_GROUP = "featureManager";

    /** Location of feature files */
    String LIB_FEATURE_PATH = "lib/features/";

    /** Liberty Server start levels: 0 (stopped) has special meaning w/ OSGi */
    int LEVEL_FEATURE_PREPARE = 7,
                    // The next 3 levels all support an early and late level. So changes to these numbers should
                    // ensure there is at least 2 empty gaps between them.
                    LEVEL_FEATURE_SERVICES = 9,
                    LEVEL_FEATURE_CONTAINERS = 12,
                    LEVEL_FEATURE_APPLICATION = 18,
                    LEVEL_ACTIVE = 20;

    int PHASE_INCREMENT = 1;

    String PHASE_APPLICATION = "APPLICATION";
    String PHASE_APPLICATION_LATE = PHASE_APPLICATION + "_LATE";
    String PHASE_APPLICATION_EARLY = PHASE_APPLICATION + "_EARLY";
    String PHASE_SERVICE = "SERVICE";
    String PHASE_SERVICE_LATE = PHASE_SERVICE + "_LATE";
    String PHASE_SERVICE_EARLY = PHASE_SERVICE + "_EARLY";
    String PHASE_CONTAINER = "CONTAINER";
    String PHASE_CONTAINER_LATE = PHASE_CONTAINER + "_LATE";
    String PHASE_CONTAINER_EARLY = PHASE_CONTAINER + "_EARLY";
}
