/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
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
package com.ibm.ws.feature.tests.util;

public interface PlatformConstants {

    String MICROPROFILE_DESCENDING = "MicroProfile-7.0, MicroProfile-6.1, MicroProfile-6.0, MicroProfile-5.0, MicroProfile-4.1, " +
                                     "MicroProfile-4.0, MicroProfile-3.3, MicroProfile-3.2, MicroProfile-3.0, MicroProfile-2.2, MicroProfile-2.1, " +
                                     "MicroProfile-2.0, MicroProfile-1.4, MicroProfile-1.3, MicroProfile-1.2, MicroProfile-1.0";

    String MICROPROFILE_ASCENDING = "MicroProfile-1.0, MicroProfile-1.2, MicroProfile-1.3, MicroProfile-1.4, " +
                                    "MicroProfile-2.0, MicroProfile-2.1, MicroProfile-2.2, MicroProfile-3.0, MicroProfile-3.2, MicroProfile-3.3, " +
                                    "MicroProfile-4.0, MicroProfile-4.1, MicroProfile-5.0, MicroProfile-6.0, MicroProfile-6.1, MicroProfile-7.0";

    String JAKARTA_DESCENDING = "jakartaee-11.0, jakartaee-10.0, jakartaee-9.1, jakartaee-8.0, javaee-8.0, javaee-7.0, javaee-6.0";

    String JAKARTA_ASCENDING = "javaee-6.0, javaee-7.0, javaee-8.0, jakartaee-8.0, jakartaee-9.1, jakartaee-10.0, jakartaee-11.0";

}