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
 * is described as fast because it uses hashcodes (and lengths) of strings as
 * keys into the HashMap for the next cells.
 * This allows for faster, but less complete, comparisons.
 * 
 * Taken from {@link com.ibm.ws.tcpchannel.internal.FilterCellFastStr}
 */
public class FilterCellFastStr {

    private FastSynchHashTable nextCell = null;
    // nextCell - Key: int, String Hashcode
    // Value: the next cell supporting this String in the chain

    private FilterCellFastStr wildcardCell = null;
    private int hashLength;

    /**
     * Find the next cell, for a given string hashcode, which may come after this
     * cell
     * in the address tree.
     * 
     * @param hashcode
     *            The next string hashcodein the URL Address that is to be tested
     *            for inclusion in the address tree
     * @return null if the hashcode does not have another cell connected to this
     *         cell in the tree. Otherwise return the next cell in the tree that
     *         is represented
     *         by the hashcode.
     */
    public FilterCellFastStr findNextCell(int hashcode) {
        if (nextCell == null) {
            return null;
        }

        return (FilterCellFastStr) nextCell.get(hashcode);
    }

    /**
     * Find the next cell, for a given string hashcode, which may come after this
     * cell
     * in the address tree.
     * 
     * @param hashcode
     *            The next string hashcodein the URL Address that is to be tested
     *            for inclusion in the address tree
     * @param length
     *            the length of the string. If the hashcode and length match an
     *            entry
     *            for the next cell, then a match has been found.
     * @return null if the the hashcode and length do not have describe another
     *         cell
     *         connected to this cell in the tree.
     *         Otherwise return the next cell in the tree that is represented
     *         by the hashcode.
     */
    public FilterCellFastStr findNextCellWithLength(int hashcode, int length) {
        if (nextCell == null) {
            return null;
        }

        FilterCellFastStr x = (FilterCellFastStr) nextCell.get(hashcode);

        if (x != null) {
            if (x.getHashLength() != length) {
                return null;
            }
        }

        return x;
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
    public FilterCellFastStr getWildcardCell() {
        return wildcardCell;
    }

    /**
     * Get the length of the string which is represented by this cell
     * 
     * @return the length of the string which is represented by this cell
     */
    public int getHashLength() {
        return hashLength;
    }

    /**
     * Add a new succeeding cell to this cell, based on a new valid address value
     * that
     * can follow this cell's value, into the address tree. This is done
     * by adding the new cell to the hashtable of next cells.
     * 
     * @param hashcode
     *            a valid value, that can follow this cell's value, in the address
     *            tree
     * @param length
     *            of the string which is represented by this cell
     * @return the new cell that was added.
     */
    public FilterCellFastStr addNewCell(int hashcode, int length) {
        // check for wildcard
        if ((hashcode != 0) || (length != 0)) {
            // not a wildcard
            if (nextCell == null) {
                // no next node on this tree, so add one
                nextCell = new FastSynchHashTable();
            }

            FilterCellFastStr newCell = new FilterCellFastStr();
            newCell.hashLength = length;
            nextCell.put(hashcode, newCell);

            return newCell;
        }
        // entry is a wildcard, create wildcode node if it doesn't exist
        if (wildcardCell == null) {
            wildcardCell = new FilterCellFastStr();
        }
        return wildcardCell;
    }

}
