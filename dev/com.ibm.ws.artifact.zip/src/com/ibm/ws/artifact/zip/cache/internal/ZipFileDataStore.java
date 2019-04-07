/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.zip.cache.internal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Utility encapsulation of an ordered zip data collection.
 */
public class ZipFileDataStore {
    public ZipFileDataStore(String name) {
        this.name = name;

        // Table of zip data paths to the cell which has that data:
        //   cells.get(aCell.data.path) == aCell.

        this.cells = new HashMap<String, Cell>();

        // Doubly linked list, with an anchor which has null data:
        //   anchor.data == null
        //   aCell.next.prev == aCell; aCell.prev.next == aCell.
        //   the anchor is not in the cell table

        this.anchor = new Cell(null);
        this.anchor.next = this.anchor;
        this.anchor.prev = this.anchor;
    }

    //

    private final String name;

    /**
     * Answer the name of this data store.  The name is used
     * for debugging.
     *
     * @return The name of this data store.
     */
    @Trivial
    public String getName() {
        return name;
    }

    //

    /**
     * Iterator type for the zip file store.
     */
    private class CellIterator implements Iterator<ZipFileData> {
        /**
         * The cell of the data most previously returned by {@link #next()}.
         * Initially null, and null after a call to {@link #remove()}. 
         */
        private Cell prev;
        
        /**
         * The cell of the data to be answered by the next call to {@link #next()}.
         */
        private Cell next;

        @Trivial
        public CellIterator() {
            this.prev = null; // No calls yet to 'next()', nor after a call to 'remove()'
            this.next = anchor.next; // The cell for the next call to 'next()'.
        }

        @Override
        @Trivial
        public boolean hasNext() {
            return ( next != anchor ); // We are finished when we circle back to the anchor.
        }

        @Override
        @Trivial
        public ZipFileData next() {
            if ( next == anchor ) { // We are finished when we circle back to the anchor.
                throw new NoSuchElementException();
            }

            prev = next; // Need to remember this to support 'remove()'.
            next = prev.next;
            return prev.data;
        }

        @Trivial
        @Override
        public void remove() {
            if ( prev == null ) {
                throw new IllegalStateException("'next()' has not been called");
            }

            @SuppressWarnings("unused")
            ZipFileData prevData = ZipFileDataStore.this.remove(prev.data.path);
            prev = null;
        }
    }
    
    public static class Cell {
        public ZipFileData data;

        public Cell prev;
        public Cell next;

        @Trivial
        public Cell(ZipFileData data) {
            this.data = data;

            this.prev = null;
            this.next = null;
        }

        @Trivial
        public void excise() {
            if ( this.next == this ) {
                throw new IllegalArgumentException("Cannot excise the anchor");
            }

            this.next.prev = this.prev;
            this.prev.next = this.next;

            this.prev = null;
            this.next = null;
        }

        @Trivial
        public void putBetween(Cell prev, Cell next) {
            if ( this == prev ) {
                throw new IllegalArgumentException("Cannot put a cell after itself");
            } else if ( this == next ) {
                throw new IllegalArgumentException("Cannot put a cell before itself");
            }

            // Either:
            //
            // A -> A + B ==> A -> B -> A
            //   A.next == A; A.prev == A
            //     ==>
            //   A.next == B; A.prev == B, B.prev == A, B.next == A
            //
            // A -> C + B ==> A -> B -> C
            //    A.next == C; C.prev == A
            //      ==>
            //    A.next == B; C.prev == B; B.prev == A; B.next == C

            this.next = next;
            next.prev = this;

            this.prev = prev;
            prev.next = this;
        }
    }

    private final Map<String, Cell> cells;
    private final Cell anchor;

    //

    @Trivial
    public boolean isEmpty() {
        return ( cells.isEmpty() );
    }

    @Trivial
    public int size() {
        return cells.size();
    }

    @Trivial
    public boolean hasOne() {
        return ( cells.size() == 1 );
    }

    @Trivial
    public Cell getCell(String path) {
        return cells.get(path);
    }

    @Trivial
    public Cell getFirstCell() {
        Cell firstCell = anchor.next;
        if ( firstCell == anchor ) {
            return null;
        } else {
            return firstCell;
        }
    }

    @Trivial
    public Cell getLastCell() {
        Cell lastCell = anchor.prev;
        if ( lastCell == anchor ) {
            return null;
        } else {
            return lastCell;
        }
    }

    //

    @Trivial
    public ZipFileData get(String path) {
        Cell cell = cells.get(path);
        return ( (cell == null) ? null : cell.data );
    }

    @Trivial    
    public ZipFileData getFirst() {
        Cell firstCell = getFirstCell();
        return ( (firstCell == null) ? null : firstCell.data );
    }

    @Trivial
    public ZipFileData getLast() {
        Cell lastCell = getLastCell();
        return ( (lastCell == null) ? null : lastCell.data );
    }    

    //

    @Trivial
    public Iterator<ZipFileData> values() {
        return new CellIterator();
    }

    //

    @Trivial
    public ZipFileData remove(String path) {
        Cell cell = cells.remove(path);
        if ( cell == null ) {
            return null;
        }

        ZipFileData data = cell.data;
        cell.excise();
        return data;
    }

    /**
     * Add data as new first data.
     * 
     * Answer the previous first data.
     *
     * Throw an exception if the path of the new data is already stored.
     * 
     * @param newFirstData The new data to put as the first stored data.
     * 
     * @return The data which was previously the first stored data.  Null
     *     if the store was empty.
     */
    public ZipFileData addFirst(ZipFileData newFirstData) {
        String newFirstPath = newFirstData.path;

        Cell dupCell = cells.get(newFirstPath);
        if ( dupCell != null ) {
            throw new IllegalArgumentException("Path [ " + newFirstPath + " ] is already stored");
        }

        Cell oldFirstCell = anchor.next;

        Cell newFirstCell = new Cell(newFirstData);
        cells.put(newFirstPath, newFirstCell);

        newFirstCell.putBetween(anchor, oldFirstCell);

        return oldFirstCell.data;
    }

    /**
     * Add data as new last data.
     * 
     * Answer the previous last data.
     *
     * Throw an exception if the path of the new data is already stored.
     * 
     * @param newLastData The new data to put as the last stored data.
     * 
     * @return The data which was previously the last stored data.  Null
     *     if the store was empty.
     */
    public ZipFileData addLast(ZipFileData newLastData) {
        String newLastPath = newLastData.path;

        Cell dupCell = cells.get(newLastPath);
        if ( dupCell != null ) {
            throw new IllegalArgumentException("Path [ " + newLastPath + " ] is already stored");
        }

        Cell oldLastCell = anchor.prev;

        Cell newLastCell = new Cell(newLastData);
        cells.put(newLastPath, newLastCell);

        newLastCell.putBetween(oldLastCell, anchor);

        return oldLastCell.data;
    }
    
    /**
     * Add data as the last data of the store.  If the addition pushes a cell
     * out of the list, answer the data of the cell which was removed.
     * 
     * Throw an exception if the path of the new data is already stored.
     *
     * @param newLastData The new data to add as the last data of the store.
     * @param maximumSize The maximum size of the store.  '-1' to allow the
     *     store to grow indefinitely.
     * 
     * @return Data which was removed from the store.  Null if the store maximum
     *     size has not yet been reached.
     */
    public ZipFileData addLast(ZipFileData newLastData, int maximumSize) {
        String newLastPath = newLastData.path;

        Cell dupCell = cells.get(newLastPath);
        if ( dupCell != null ) {
            throw new IllegalArgumentException("Path [ " + newLastPath + " ] is already stored");
        }

        int size = size();

        if ( (maximumSize == -1) || (size < maximumSize) ) {
            @SuppressWarnings("unused")
            ZipFileData oldLastData = addLast(newLastData);
            return null;
        }

        Cell oldFirstCell = anchor.next;
        ZipFileData oldFirstData = oldFirstCell.data;
        String oldFirstPath = oldFirstData.path;

        if ( oldFirstCell != cells.remove(oldFirstPath) ) {
            throw new IllegalStateException("Bad cell alignment on path [ " + oldFirstPath + " ]");
        }

        oldFirstCell.data = newLastData;
        cells.put(newLastPath,  oldFirstCell);

        if ( size != 1 ) {
            oldFirstCell.excise();
            oldFirstCell.putBetween(anchor.prev, anchor);
        }

        return oldFirstData;
    }

    //

    public void display() {
        System.out.println("Store [ " + name + " ]");

        int cellNo = 0;
        Cell next = anchor;
        display(cellNo, next);

        while ( (next = next.next) != anchor ) {
            cellNo++;
            display(cellNo, next);
        }
    }

    public void display(int cellNo, Cell cell) {
        String thisCell = cellText(cellNo, cell);
        String prevCell = cellText(cellNo - 1, cell.prev);
        String nextCell = cellText(cellNo + 1, cell.next);

        System.out.println("  Cell " + thisCell + " Prev " + prevCell + " Next " + nextCell);
    }

    public void validate() {
        int cellNo = 0;
        Cell next = anchor;
        validate(0, next, NULL_DATA);

        while ( (next = next.next) != anchor ) {
            cellNo++;
            validate(cellNo, next, NON_NULL_DATA);
        }
    }

    private static final boolean NULL_DATA = true;
    private static final boolean NON_NULL_DATA = false;

    private String cellText(int cellNo, Cell cell) {
        if ( cell == null ) {
            return "[ " + name + " : " + cellNo + " ] [ *** NULL CELL *** ]";
        }

        ZipFileData data = cell.data;
        if ( data == null ) {
            return "[ " + name + " : " + cellNo + " ]";
        } else {
            return "[ " + name + " : " + cellNo + " ] [ " + data.path + " ]";
        }
    }

    public void validate(int cellNo, Cell cell, boolean nullData) {
        if ( nullData ) {
            if ( cell.data != null ) {
                throw new IllegalStateException("Non-null data " + cellText(cellNo, cell));
            }
        } else {
            if ( cell.data == null ) {
                throw new IllegalStateException("Null data " + cellText(cellNo, cell));
            }
        }

        if ( cell.next == null ) {
            throw new IllegalStateException("Null next " + cellText(cellNo, cell));
        } else if ( cell.prev == null ) {
            throw new IllegalStateException("Null prev " + cellText(cellNo, cell));
        }

        if ( cell.next.prev == null ) {
            throw new IllegalStateException("Null next.prev " + cellText(cellNo, cell) + " " + cellText(cellNo + 1, cell.next));
        } else if ( cell.next.prev != cell ) {
            throw new IllegalStateException("Non-returning next.prev " + cellText(cellNo, cell) + " " + cellText(cellNo + 1, cell.next) + " " + cellText(cellNo, cell.next.prev));            
        }

        if ( cell.prev.next == null ) {
            throw new IllegalStateException("Null prev.next " + cellText(cellNo, cell) + " " + cellText(cellNo - 1, cell.prev));
        } else if ( cell.prev.next != cell ) {
            throw new IllegalStateException("Non-returning prev.next " + cellText(cellNo, cell) + " " + cellText(cellNo + 1, cell.prev) + " " + cellText(cellNo, cell.prev.next));            
        }
    }
}
