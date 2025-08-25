/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
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

package com.ibm.helloworldra;

import jakarta.resource.cci.IndexedRecord;

public interface HelloWorldIndexedRecord extends IndexedRecord {

    public static final String INPUT = "input";
    public static final String OUTPUT = "output";
    public static final int MESSAGE_FIELD = 0;
}
