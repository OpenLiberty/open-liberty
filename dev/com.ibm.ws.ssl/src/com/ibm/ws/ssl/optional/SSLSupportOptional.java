/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package com.ibm.ws.ssl.optional;

import com.ibm.wsspi.ssl.SSLSupport;

/**
 * Marker interface for optionally configured SSL subsystem
 */
public interface SSLSupportOptional extends SSLSupport {

    public static final String KEYSTORE_IDS = "keystoreIds";

    public static final String REPERTOIRE_IDS = "repertoireIds";

    public static final String REPERTOIRE_PIDS = "repertoirePIDs";

}
