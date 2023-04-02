/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package test.jakarta.data.web;

/**
 * This is not an entity. One of the repository methods uses this class to return multiple values.
 */
public class ProductCount {
    public final long totalNames;
    public final long totalDescriptions;
    public final long totalPrices;

    public ProductCount(long numDistinctNames, long numDistinctDescriptions, long numDistinctPrices) {
        this.totalNames = numDistinctNames;
        this.totalDescriptions = numDistinctDescriptions;
        this.totalPrices = numDistinctPrices;
    }
}
