/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wsspi.pmi.stat;

import java.util.*;

import com.ibm.websphere.pmi.stat.WSStats;

/**
 * WebSphere interface to instrument Stats. Typically, the methods in this interface are not called from the application.
 * 
 * @ibm-spi
 */

public interface SPIStats extends WSStats {
    /** Set the Stats name */
    public void setName(String name);

    /** Set the stats type */
    public void setStatsType(String modName);

    /**
     * Set the stats monitoring level
     * 
     * @deprecated No replacement
     * */
    public void setLevel(int level);

    /** Set statistics for this Stats */
    public void setStatistics(ArrayList dataMembers);

    /** Set child stats */
    public void setSubStats(ArrayList subCollections);
}
