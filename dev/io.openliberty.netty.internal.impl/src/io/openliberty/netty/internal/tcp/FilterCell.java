/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.netty.internal.tcp;

import com.ibm.ws.bytebuffer.internal.FastSynchHashTable;

/**
 * A Cell (or node) of an address tree. The tree defines a set of
 * IPv6 addresses. Each cell is a valid numerial for one of the positions
 * in the address. Each cell contains a hash table of valid cells (numerials)
 * which may come after, in the next position of the IPv6 address, this cell.
 * Each cell can also represent a wildcard. A wildcard cell contains
 * the next cells which may come after, in the next position of the IPv6
 * address, a wildcard in this position.
 * 
 * Taken from {@link com.ibm.ws.tcpchannel.internal.FilterCell}
 */
public class FilterCell {
    private FastSynchHashTable nextCell = null;
    // nextCell - Key: int, the IP6 number
    // Value: the next cell supporting this number/address chain

    private FilterCell wildcardCell = null;

    /**
     * Find the next cell, for a given number, which may come after this cell
     * in the address tree.
     * 
     * @param nextValue
     *            The next value in the IPv6 Address that is to be tested
     *            for inclusion in the address tree
     * @return null if the the nextValue does not have another cell connected to
     *         this
     *         cell in the tree. Otherwise return the next cell in the tree that
     *         is represented
     *         by the nextValue.
     */
    public FilterCell findNextCell(int nextValue) {
        if (nextCell == null) {
            return null;
        }
        return (FilterCell) (nextCell.get(nextValue));
    }

    /**
     * Get the cell that comes after this cell, if a wildcard is a valid entry
     * for this cell in the address tree. Othrewise return null if there is
     * no wildcard entry for this cell.
     * 
     * @return the next cell that comes after the wildcard entry for this cell, if
     *         there is no wildcard entry for this cell then return null.
     */
    public FilterCell getWildcardCell() {
        return wildcardCell;
    }

    /**
     * Add a new succeeding cell to this cell, based on a new valid address value
     * that
     * can follow this cell's value, into the address tree. This is done
     * by adding the new cell to the hashtable of next cells.
     * 
     * @param newValue
     *            a valid value, that can follow this cell's value, in the address
     *            tree
     * @return the new cell that was added.
     */
    public FilterCell addNewCell(int newValue) {
        if (newValue != -1) {
            if (nextCell == null) {
                nextCell = new FastSynchHashTable();
            }

            FilterCell newCell = new FilterCell();
            nextCell.put(newValue, newCell);

            return newCell;
        }
        if (wildcardCell == null) {
            wildcardCell = new FilterCell();
        }
        return wildcardCell;
    }

}
