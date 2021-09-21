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

/**
 * Contains the address tree of multiple IPv4 and IPv6 Addresses. This tree can
 * then
 * be used to determine if a new address is contain in this tree. Therefore
 * this object is used to see in a IPv4 or IPv6 Address in contained in a given
 * list
 * of IPv4 and IPv6 addresses. The compatibility between IPv4 and IPv6 within
 * this tree is maintained by first converting all IPv4 addresses to IPv6 before
 * processing the address.
 * 
 * Taken from {@link com.ibm.ws.tcpchannel.internal.FilterList}
 */
public class FilterList {

    // the number of places in an IPv6 address
    private static final int IP_ADDR_NUMBERS = 8;

    protected boolean active = false;

    // The root of the tree, and therefore access to the entire tree
    FilterCell firstCell = null;

    /**
     * Constructor.
     */
    public FilterList() {
        this.firstCell = new FilterCell();
    }

    protected void setActive(boolean value) {
        this.active = value;
    }

    protected boolean getActive() {
        return this.active;
    }

    /**
     * Build the address tree from a string of data which contains valid
     * IPv4 and/or IPv6 addresses. The string array should contain all the
     * addresses.
     * 
     * @param data
     *            list of IPv4 and/or IPv6 address which are
     *            to be used to create a new address tree.
     */
    protected void buildData(String[] data, boolean validateOnly) {

        if (data == null) {
            return;
        }

        int length = data.length;

        for (int i = 0; i < length; i++) {
            addAddressToList(data[i], validateOnly);
        }

    }

    /**
     * Add one IPv4 or IPv6 address to the tree. The address is passed in as
     * a string and converted to an integer array by this routine. Another method
     * is then called to put it into the tree
     * 
     * @param newAddress
     *            address to add
     */
    private void addAddressToList(String newAddress, boolean validateOnly) {
        int start = 0;
        char delimiter = '.';
        String sub;
        int radix = 10;

        // Address initially set to all zeroes
        int addressToAdd[] = new int[IP_ADDR_NUMBERS];
        for (int i = 0; i < IP_ADDR_NUMBERS; i++) {
            addressToAdd[i] = 0;
        }

        int slot = IP_ADDR_NUMBERS - 1;
        // assume the address is IP4, but change to IP6 if there is a colon
        if (newAddress.indexOf('.') == -1) {
            // if no ".", then assume IPv6 Address
            delimiter = ':';
            radix = 16;
        }

        String addr = newAddress;
        while (true) {
            // fill address from back to front
            start = addr.lastIndexOf(delimiter);
            if (start != -1) {
                sub = addr.substring(start + 1);

                if (sub.trim().equals("*")) {
                    addressToAdd[slot] = -1; // 0xFFFFFFFF is the wildcard.
                } else {
                    addressToAdd[slot] = Integer.parseInt(sub, radix);
                }
            } else {
                if (addr.trim().equals("*")) {
                    addressToAdd[slot] = -1; // 0xFFFFFFFF is the wildcard.
                } else {
                    addressToAdd[slot] = Integer.parseInt(addr, radix);
                }
                break;
            }
            slot = slot - 1;
            addr = addr.substring(0, start);
        }

        if (!validateOnly) {
            putInList(addressToAdd);
        }
    }

    /**
     * Add one address to the tree. The address is passed in as
     * an (n-level) integer array. n is currently set at 8, since that
     * is the number of numbers in an IPv6 address. The format of the array
     * is that the rightmost number of the address goes into the n-1 position
     * in the array, the next right most number into the n-2 position, and so on.
     * Positions in the array which have not be set, are then set to 0.
     * This means an IPv4 address of 1.2.3.4 should be
     * put into the array as a[0] - a[7] as: 0, 0, 0, 0, 1, 2, 3, 4, in
     * IPv6 format this would be 0:0:0:0:1:2:3:4
     * 
     * @param address
     *            int array of the address to add. The format of this
     *            address is described above.
     */
    private void putInList(int[] address) {
        FilterCell currentCell = firstCell;
        FilterCell nextCell = null;

        for (int i = 0; i < address.length; i++) {
            if (address[i] != -1) {
                nextCell = currentCell.findNextCell(address[i]);
            } else {
                nextCell = currentCell.getWildcardCell();
            }

            if (nextCell == null) { // new address, complete the tree
                for (int j = i; j < address.length; j++) {
                    currentCell = currentCell.addNewCell(address[j]);
                }
                return;
            }

            currentCell = nextCell;
        }
    }

    /**
     * Determine if an address, represented by a byte array, is in the address
     * tree
     * 
     * @param address
     *            byte array representing the address, leftmost number
     *            in the address should start at array offset 0.
     * @return true if this address is found in the address tree, false if
     *         it is not.
     */
    public boolean findInList(byte[] address) {
        int len = address.length;
        int a[] = new int[len];

        for (int i = 0; i < len; i++) {
            // convert possible negative byte value to positive int
            a[i] = address[i] & 0x00FF;
        }
        return findInList(a);

    }

    /**
     * Determine if an IPv6 address, represented by a byte array, is in the
     * address tree
     * 
     * @param address
     *            byte array representing the address, leftmost number
     *            in the address should start at array offset 0. Two bytes form 1
     *            number
     *            of the address, bytes assumed to be in network order
     * @return true if this address is found in the address tree, false if
     *         it is not.
     */
    public boolean findInList6(byte[] address) {
        int len = address.length;
        int a[] = new int[len / 2];

        // IPv6, need to combine every two bytes to the ints
        int j = 0;
        int highOrder = 0;
        int lowOrder = 0;
        for (int i = 0; i < len; i += 2) {
            // convert possible negative byte value to positive int
            highOrder = address[i] & 0x00FF;
            lowOrder = address[i + 1] & 0x00FF;
            a[j] = highOrder * 256 + lowOrder;
            j++;
        }

        return findInList(a);
    }

    /**
     * Determine if an address, represented by an integer array, is in the address
     * tree
     * 
     * @param address
     *            integer array representing the address, leftmost number
     *            in the address should start at array offset 0.
     * @return true if this address is found in the address tree, false if
     *         it is not.
     */
    public boolean findInList(int[] address) {
        int len = address.length;

        if (len < IP_ADDR_NUMBERS) {
            int j = IP_ADDR_NUMBERS - 1;

            // for performace, hard code the size here
            int a[] = { 0, 0, 0, 0, 0, 0, 0, 0 };
            // int a[] = new int[IP_ADDR_NUMBERS];
            // for (int i = 0; i < IP_ADDR_NUMBERS; i++)
            // {
            // a[i] = 0;
            // }

            for (int i = len; i > 0; i--, j--) {
                a[j] = address[i - 1];
            }
            return findInList(a, 0, firstCell, 7);
        }

        return findInList(address, 0, firstCell, (address.length - 1));
    }

    /**
     * Determine, recursively, if an address, represented by an integer array, is
     * in the address tree
     * 
     * @param address
     *            integer array representing the address, leftmost number
     *            in the address should start at array offset 0. IPv4 address should
     *            be
     *            padded LEFT with zeroes.
     * @param index
     *            the next index in the address array of the number to match against
     *            the tree.
     * @param cell
     *            the current cell in the tree that we are matching against
     * @param endIndex
     *            the last index in the address array that we need to match
     * @return true if this address is found in the address tree, false if
     *         it is not.
     */
    private boolean findInList(int[] address, int index, FilterCell cell, int endIndex) {
        if (cell.getWildcardCell() != null) {
            // first look at wildcard slot
            if (index == endIndex) {
                // at the end, so we found a match, unwind returning true
                return true;
            }

            // go to next level of this tree path
            FilterCell newcell = cell.getWildcardCell();

            // recursively search this path
            if (findInList(address, index + 1, newcell, endIndex)) {
                return true;
            }
            // the wildcard path didn't work, so see if there is a non-wildcard path
            FilterCell nextCell = cell.findNextCell(address[index]);
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
        // no wildcard here, try the non-wildcard path
        FilterCell nextCell = cell.findNextCell(address[index]);
        if (nextCell != null) {
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
