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

import java.util.ArrayList;
import java.util.List;

/**
 * Contains the address tree of multiple URL Addresses. This tree can then
 * be used to determine if a new address is contain in this tree. Therefore
 * this object is used to see in an Address is contained in a given list
 * of addresses. The tree is structured such that each substring between
 * periods (".") in a URL is a node of the tree. Subsequent nodes are valid
 * address paths from preceding nodes. A wildcard is allowed to be the first
 * substring in an address that is placed in the tree. For this reason, the
 * tree is built "backwards". The last substring of the URL is the first node
 * in an address path through the tree. This list is "slow" because the nodes
 * are based on the substrings and not the hashcodes of the substrings,
 * therefore
 * tree traversal is slower due to string compares.
 * 
 * Taken from {@link com.ibm.ws.tcpchannel.internal.FilterListSlowStr}
 */
public class FilterListSlowStr implements FilterListStr {

    static final String wildCard = "*";

    private FilterCellSlowStr firstCell = null;
    private boolean active = false;

    /**
     * Constructor.
     */
    public FilterListSlowStr() {
        this.firstCell = new FilterCellSlowStr();
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.FilterListStr#setActive(boolean)
     */
    public void setActive(boolean value) {
        this.active = value;
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.FilterListStr#getActive()
     */
    public boolean getActive() {
        return this.active;
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.FilterListStr#buildData(String[])
     */
    public boolean buildData(String[] data) {
        final int length = data.length;

        for (int i = 0; i < length; i++) {
            addAddressToList(data[i]);
        }

        return true;
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.FilterListStr#findInList(String)
     */
    public boolean findInList(String address) {
        return (findInList(convertToArray(address)));
    }

    /**
     * convert an address to a string array, where each level in the array
     * represent a substring between two periods (".") of the address.
     * The rightmost substring will be at index 0, and so on.
     * 
     * @param newAddress
     *            the address to convert
     * @return the String array representing the substrings of the address
     */
    private String[] convertToArray(String newAddress) {
        int start = 0;
        String sub;
        List<String> addressWords = new ArrayList<String>();

        String addr = newAddress;
        while (true) {
            // fill address from back to front
            start = addr.lastIndexOf('.');
            if (start != -1) {
                sub = addr.substring(start + 1);
                addressWords.add(sub);
            } else {
                addressWords.add(addr);
                break;
            }
            addr = addr.substring(0, start);
        }

        String[] sa = new String[addressWords.size()];
        sa = addressWords.toArray(sa);
        return (sa);
    }

    /**
     * Add a new address to the address tree.
     * 
     * @param newAddress
     *            to add
     */
    private void addAddressToList(String newAddress) {
        putInList(convertToArray(newAddress));
    }

    /**
     * Add and new address to the address tree, where the new address is formatted
     * as a string array. The 0th index of the array is the rightmost substring of
     * the string and so on. Each substring is the chars between two periods (".")
     * in
     * a URL address. The last index in the array may be a wildcard ("*").
     * 
     * @param address
     *            String array representation of the address, as described above
     */
    private void putInList(String[] address) {
        FilterCellSlowStr currentCell = firstCell;
        FilterCellSlowStr nextCell = null;

        for (int i = 0; i < address.length; i++) {
            if (address[i].equals(wildCard) != true) {
                nextCell = currentCell.findNextCell(address[i]);
            } else {
                // rightmost wildcard in string detected
                currentCell.addNewCell(wildCard);
                return;
            }

            if (nextCell == null) { // new address, complete the tree
                for (int j = i; j < address.length; j++) {
                    currentCell = currentCell.addNewCell(address[j]);
                    if (address[j].equals(wildCard)) {
                        return;
                    }
                }
                return;
            }
            currentCell = nextCell;
        }
    }

    /**
     * Determine if an address is in the address tree
     * 
     * @param address
     *            address to look for
     *            as a string array. The 0th index of the array is the rightmost
     *            substring of
     *            the string and so on. Each substring is the chars between two
     *            periods (".") in
     *            a URL address.
     * @return true if this address is found in the address tree, false if
     *         it is not.
     */
    private boolean findInList(String[] address) {
        return findInList(address, 0, firstCell, (address.length - 1));
    }

    /**
     * Determine, recursively, if an address is in the address tree.
     * 
     * @param address
     *            address to look for as a string array.
     *            The 0th index of the array is the rightmost substring of
     *            the string and so on. Each substring is the chars between
     *            two periods (".") in a URL address.
     * @param index
     *            the next index in the address array to match against the tree.
     * @param cell
     *            the current cell in the tree that we are matching against
     * @param endIndex
     *            the last index in the address array that we need to match
     * @return true if this address is found in the address tree, false if
     *         it is not.
     */
    private boolean findInList(String[] address, int index, FilterCellSlowStr cell, int endIndex) {

        if (cell.getWildcardCell() != null) {
            // a wildcard, match found
            return true;
        }
        // no wildcard so far, see if there is a still a path
        FilterCellSlowStr nextCell = cell.findNextCell(address[index]);
        if (nextCell != null) {
            // see if we are at the end of a valid path
            if (index == endIndex) {
                // this path found a match, unwind returning true
                return true;
            }
            // ok so far, recursively search this path
            return findInList(address, index + 1, nextCell, endIndex);
        }
        // this path did not find a match.
        return false;
    }

}
