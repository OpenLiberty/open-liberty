/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.internal.filter;

import java.nio.charset.StandardCharsets;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Contains the address tree of multiple URL Addresses. This tree can then
 * be used to determine if a new address is contain in this tree. Therefore
 * this object is used to see in an Address is contained in a given list
 * of addresses. The tree is structured such that each substring between
 * periods (".") in a URL is a node of the tree. Subsequent nodes are valid
 * address paths from preceding nodes. A wildcard is allowed to be the first
 * substring in an address that is placed in the tree. For this reason, the
 * tree is built "backwards". The last substring of the URL is the first node
 * in an address path through the tree. This list is "fast" because the nodes
 * are based on the hashcodes of the substrings, therefore
 * tree traversal is faster than if doing string compares.
 *
 * from from com.ibm.ws.tcpchannel.internal.FilterListFastStr
 */
public class FilterListFastStr implements FilterListStr {

    private static final TraceComponent tc = Tr.register(FilterListFastStr.class);

    private final byte PERIOD_VALUE = 46;
    private final byte WILDCARD_VALUE = 42;
    private FilterCellFastStr firstCell = null;
    private boolean active = false;

    /**
     * Constructor.
     */
    public FilterListFastStr() {
        this.firstCell = new FilterCellFastStr();
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.FilterListStr#setActive(boolean)
     */
    @Override
    public void setActive(boolean value) {
        this.active = value;
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.FilterListStr#getActive()
     */
    @Override
    public boolean getActive() {
        return this.active;
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.FilterListStr#buildData(String[])
     */
    @Override
    public boolean buildData(String[] data) {
        final int length = data.length;

        for (int i = 0; i < length; i++) {
            if (!addAddressToList(data[i])) {
                return false;
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "host added to list of trusted hosts: " + data[i]);
                }
            }
        }

        return true;
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.FilterListStr#findInList(String)
     */
    @Override
    public boolean findInList(String address) {
        return (findInList(convertToEntries(address)));
    }

    /**
     * Add a new address to the address tree
     *
     * @param newAddress
     *            address to add
     */
    private boolean addAddressToList(String newAddress) {
        return putInList(convertToEntries(newAddress));
    }

    /**
     * Determine if an address represented by an Entry object is in the address
     * tree
     *
     * @param oEntry
     *            Entry object for the address to look for
     * @return true if this address is found in the address tree, false if
     *         it is not.
     */
    private boolean findInList(Entry oEntry) {
        return findInList(oEntry.getHashcodes(), oEntry.getLengths(), firstCell, oEntry.getCurrentSize() - 1);
    }

    /**
     * Determine, recursively, if an string is in the address tree. The string is
     * to be represented by an array of hashcodes where the 0th index in the array
     * is the hashcode of the rightmost substring of the address. A substring is
     * the characters between two periods (".") in an address. The string is
     * also represented by an array of lengths, where the 0th index in the array
     * is the length of the rightmost substring. If hashcodes an length match each
     * for each entry in the arrays, with a path through the address tree, then
     * the address is found in the tree.
     *
     * @param hashcodes
     *            - array of hashcodes of the substrings of the address to find
     * @param lengths
     *            - array of lengths of the substrings of the address to find
     * @param cell
     *            - the current cell that is being traversed in the address tree
     * @param index
     *            - the next index into the arrays that is to be matched against the
     *            tree
     * @return true if this address is found in the address tree, false if
     *         it is not.
     */
    private boolean findInList(int[] hashcodes, int[] lengths, FilterCellFastStr cell, int index) {

        if (cell.getWildcardCell() != null) {
            // a wildcard, match found
            return true;
        }
        // no wildcard so far, see if there is a still a path
        FilterCellFastStr nextCell = cell.findNextCellWithLength(hashcodes[index], lengths[index]);
        if (nextCell != null) {
            // see if we are at the end of a valid path
            if (index == 0) {
                // this path found a match, unwind returning true
                return true;
            }
            // ok so far, recursively search this path
            return findInList(hashcodes, lengths, nextCell, index - 1);
        }
        // this path did not find a match.
        return false;
    }

    /**
     * Add and new address to the address tree, where the new address is
     * represented
     * by an Entry object.
     *
     * @param entry
     * @return boolean
     */
    private boolean putInList(Entry entry) {
        FilterCellFastStr currentCell = firstCell;
        FilterCellFastStr nextCell = null;

        int[] hashcodes = entry.getHashcodes();
        int[] lengths = entry.getLengths();

        // work from back to front
        int lastIndex = entry.getCurrentSize() - 1;

        for (int i = lastIndex; i >= 0; i--) {
            // test for wildcard
            if ((hashcodes[i] == 0) && (lengths[i] == 0)) {
                currentCell.addNewCell(hashcodes[i], lengths[i]);
                return true;
            }
            // check in nextCell has different lengths, if so, then
            // we can't use this "fast" method
            nextCell = currentCell.findNextCell(hashcodes[i]);
            if (nextCell != null) {
                if (nextCell.getHashLength() != lengths[i]) {
                    return false;
                }
            }

            if (nextCell == null) { // new address, complete the tree
                for (int j = i; j >= 0; j--) {
                    currentCell = currentCell.addNewCell(hashcodes[j], lengths[j]);
                    if ((hashcodes[j] == 0) && (lengths[j] == 0)) {
                        // nothing can come before a wildcard, so return
                        return true;
                    }
                }
                return true;
            }
            currentCell = nextCell;
        }
        return true;
    }

    /**
     * Convert a single URL address to an Entry object. The entry object will
     * contain the hashcode array and length array of the substrings of this
     * address
     *
     * @param newAddress
     *            address to convert
     * @return the Entry object created from this address.
     */
    private Entry convertToEntries(String newAddress) {
        byte[] ba = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "convertToEntries");
        }

        ba = newAddress.getBytes(StandardCharsets.ISO_8859_1);
        int baLength = ba.length;
        int hashValue = 0;
        int hashLength = 0;
        int e31 = 1;

        Entry oEntry = new Entry();

        for (int i = 0; i < baLength; i++) {
            if (ba[i] == WILDCARD_VALUE) {
                boolean valid = true;

                // make sure it is the first entry, followed by a ".", or nothing
                if (i != 0) {
                    valid = false;
                }
                if (baLength >= 2) {
                    if (ba[1] != PERIOD_VALUE) {
                        valid = false;
                    }
                }

                if (valid) {
                    // if isolated, then store it, and continue to next word.
                    // Store as wildcard entry
                    oEntry.addEntry(0, 0);
                    // jump over next period to avoid processing this char again
                    i = 1;
                    // go back to start of for loop
                    continue;
                }
            }

            if (ba[i] != PERIOD_VALUE) {
                // continue calculating hashcode for this entry
                hashValue += e31 * ba[i];
                e31 = e31 * 31;
                hashLength++;
            }
            if ((ba[i] == PERIOD_VALUE) || (i == baLength - 1)) {
                // end of a "word", need to add if length is non-zero
                if (hashLength > 0) {
                    oEntry.addEntry(hashValue, hashLength);
                }
                // prepare to calculate next entry
                hashLength = 0;
                e31 = 1;
                hashValue = 0;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "convertToEntries");
        }

        return oEntry;
    }

    /**
     * An inner class which represents an address using hashcodes and lengths of
     * the substrings contained in the address.
     */
    private static class Entry {

        // the maximum number of substrings allowed before new arrays must
        // be created to constain the address
        private final int incrementalSize = 100;

        private int[] hashcodes1 = new int[incrementalSize];
        private int[] lengths1 = new int[incrementalSize];
        private int[] hashcodes2;
        private int[] lengths2;
        private int index = 0;

        // denotes which hashcode and length array is currently being used, 1 or 2
        private int iSwitch = 1;

        // the current number of substrings that represent this address
        private int currentSize;

        /**
         * constructor.
         */
        public Entry() {
            this.currentSize = this.incrementalSize;
        }

        /**
         * get the current number of substrings that represent this entry
         *
         * @return the current number of substrings that represent this entry
         */
        public int getCurrentSize() {
            return this.index;
        }

        /**
         * get the array of hashcodes for the substrings that represent this entry
         *
         * @return the array of hashcodes for the substrings that represent this
         *         entry
         */
        public int[] getHashcodes() {
            if (iSwitch == 1) {
                return this.hashcodes1;
            }
            return this.hashcodes2;
        }

        /**
         * get the array of lengths for the substrings that represent this entry
         *
         * @return the array of lengths for the substrings that represent this entry
         */
        public int[] getLengths() {
            if (iSwitch == 1) {
                return this.lengths1;
            }
            return this.lengths2;
        }

        /**
         * Add a new hashcode and length to the arrays of hashcodes and lengths for
         * the substrings that represent this entry
         *
         * @param hashcode
         *            hashcode to add
         * @param length
         *            length to add
         */
        public void addEntry(int hashcode, int length) {
            if (iSwitch == 1) {
                if (index < currentSize) {
                    hashcodes1[index] = hashcode;
                    lengths1[index] = length;
                    index++;
                } else {
                    // increase array size
                    currentSize += incrementalSize;
                    hashcodes2 = new int[currentSize];
                    lengths2 = new int[currentSize];
                    System.arraycopy(hashcodes1, 0, hashcodes2, 0, hashcodes1.length);
                    System.arraycopy(lengths1, 0, lengths2, 0, lengths1.length);
                    iSwitch = 2;
                    hashcodes2[index] = hashcode;
                    lengths2[index] = length;
                    index++;
                }
            } else {
                if (index < currentSize) {
                    hashcodes2[index] = hashcode;
                    lengths2[index] = length;
                    index++;
                } else {
                    // increase array size
                    currentSize += incrementalSize;
                    hashcodes1 = new int[currentSize];
                    lengths1 = new int[currentSize];
                    System.arraycopy(hashcodes2, 0, hashcodes1, 0, hashcodes2.length);
                    System.arraycopy(lengths2, 0, lengths1, 0, lengths2.length);
                    iSwitch = 1;
                    hashcodes1[index] = hashcode;
                    lengths1[index] = length;
                    index++;
                }
            }
        }

    }

}
