/*******************************************************************************
 * Copyright (c) 1997, 2002 IBM Corporation and others.
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

package com.ibm.websphere.pmi.server;

//import com.ibm.wsspi.pmi.stat.SPITimeStatistic;

import com.ibm.wsspi.pmi.stat.SPIStatistic;

public interface SpdStatExternalValue {
    public SPIStatistic getStatValue();

    public void updateStatistic();
}
