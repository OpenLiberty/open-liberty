/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
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
package com.ibm.fvtra;

import java.util.concurrent.ConcurrentHashMap;

import jakarta.jms.Destination;

public class DummyME {

    // List of all the destinations created. Need to add to this list when any new destination is created.
    static ConcurrentHashMap<String, Destination> destinations = new ConcurrentHashMap<String, Destination>();

}
