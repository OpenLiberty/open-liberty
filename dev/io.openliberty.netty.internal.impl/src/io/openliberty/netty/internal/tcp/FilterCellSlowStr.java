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

import java.util.HashMap;
import java.util.Map;

/**
 * A Cell (or node) of an address tree. The tree defines a set of
 * string URL addresses. Each cell represents a valid string for one of the
 * positions
 * in the address. A cell string is defined as the characters between two
 * periods
 * in an address. Each cell contains a hash table of valid cells (strings)
 * which may come after, in the next position of the address, this cell.
 * Each cell can also represent a wildcard. A wildcard cell contains
 * the next cells which may come after, in the next position of the
 * address, a wildcard in this position. The tree is build backwards, that is
 * the rightmost strings in an address are at the root of the address tree.
 * Wildcards may only appear once in an address that is represented in this
 * tree, and no characters may precede the wildcard. Finally, this object
 * is described as slow because it uses strings as keys into the HashMap for the
 * next cells. This makes for slower, but more complete, comparisons.
 * 
 * Taken from {@link com.ibm.ws.tcpchannel.internal.FilterCellSlowStr}
 */
public class FilterCellSlowStr {

    private Map<String, FilterCellSlowStr> nextCell = null;

    private FilterCellSlowStr wildcardCell = null;

    /**
     * Find the next cell, for a given string, which may come after this cell
     * in the address tree.
     * 
     * @param nextValue
     *            The next string the URL Address that is to be tested
     *            for inclusion in the address tree
     * @return null if the nextValue does not have another cell connected to this
     *         cell in the tree. Otherwise return the next cell in the tree that
     *         is represented
     *         by nextValue.
     */
    public FilterCellSlowStr findNextCell(String nextValue) {
        if (nextCell == null) {
            return null;
        }
        return nextCell.get(nextValue);
    }

    /**
     * Get the cell that comes after this cell, if a wildcard is a valid entry
     * for this cell in the address tree. Othrewise return null if there is
     * no wildcard entry for this cell. The wildcard cell should be the last
     * cell in this tree branch, since not strings are allowed to precede
     * a wildcard for addresses in the address tree.
     * 
     * @return the next cell that comes after the wildcard entry for this cell, if
     *         there is no wildcard entry for this cell then return null.
     */
    public FilterCellSlowStr getWildcardCell() {
        return this.wildcardCell;
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
    public FilterCellSlowStr addNewCell(String newValue) {
        if (!newValue.equals(FilterListSlowStr.wildCard)) {
            if (nextCell == null) {
                nextCell = new HashMap<String, FilterCellSlowStr>();
            }

            FilterCellSlowStr newCell = new FilterCellSlowStr();
            nextCell.put(newValue, newCell);
            return newCell;
        }
        if (wildcardCell == null) {
            wildcardCell = new FilterCellSlowStr();
        }
        return wildcardCell;
    }

}
