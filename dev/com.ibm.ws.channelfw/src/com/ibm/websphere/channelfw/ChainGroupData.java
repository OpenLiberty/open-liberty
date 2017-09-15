/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.channelfw;

import java.io.Serializable;

/**
 * ChainGroupData provides information specifically about a logical group of Transport Chains
 * and the attributes of that grouping.
 * 
 * @ibm-api
 */
public interface ChainGroupData extends Serializable {

    /**
     * Get the name of this chain group.
     * 
     * @return String
     */
    String getName();

    /**
     * Get the list of chains in this group.
     * 
     * @return ChainData[]
     */
    ChainData[] getChains();

    /**
     * Returns whether or not the input chain is included in this group.
     * 
     * @param chainName Name of a Transport Chain.
     * @return boolean Whether or not the input chain is included in this group.
     */
    boolean containsChain(String chainName);
}
