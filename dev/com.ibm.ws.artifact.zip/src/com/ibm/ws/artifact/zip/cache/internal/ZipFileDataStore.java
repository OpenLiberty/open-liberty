/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
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

        this.cells = new HashMap<String, Cell>();

        this.anchor = new Cell(null);
        this.anchor.next = this.anchor;
        this.anchor.prev = this.anchor;
    }

    //

    private final String name;

    @Trivial
    public String getName() {
        return name;
    }

    //

    private class CellIterator implements Iterator<ZipFileData> {
        private Cell prev;
        private Cell next;

        @Trivial
        public CellIterator() {
            this.prev = null;
            this.next = anchor.next;
        }

        @Override
        @Trivial
        public boolean hasNext() {
            return ( next != anchor );
        }

        @Override
        @Trivial
        public ZipFileData next() {
            if ( next == anchor ) {
                throw new NoSuchElementException();
            }

            ZipFileData nextData = next.data;
            prev = next;
            next = next.next;
            return nextData;
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
            this.next.prev = this.prev;
            this.prev.next = this.next;

            this.next = null;
            this.prev = null;
        }

        @Trivial
        public void putBetween(Cell prev, Cell next) {
            this.next = next;
            next.prev = this;
            
            this.prev = prev;
            prev.next = this;
        }
    }

    private final Map<String, Cell> cells;
    private Cell anchor;

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

    @Trivial
    public Iterator<ZipFileData> values() {
        return new CellIterator();
    }

    @Trivial
    public ZipFileData addAfter(ZipFileData beforeData, ZipFileData afterData) {
        String beforePath = beforeData.path;

        if ( afterData == beforeData ) {
            throw new IllegalArgumentException("Attempt to put [ " + beforePath + " ] after itself");
        }

        Cell beforeCell = cells.get(beforePath);
        if ( beforeCell == null ) {
            throw new NoSuchElementException("Path not stored [ " + beforePath + " ]");
        }

        return addAfter(beforeCell, afterData).data;
    }

    @Trivial
    private Cell addAfter(Cell beforeCell, ZipFileData afterData) {
        Cell oldAfterCell = beforeCell.next;

        String afterPath = afterData.path;
        Cell newAfterCell = cells.get(afterPath);
        if ( newAfterCell == null ) {
            newAfterCell = new Cell(afterData);
            newAfterCell.putBetween(beforeCell, oldAfterCell);
        } else if ( newAfterCell != oldAfterCell ) {
            newAfterCell.excise();
            newAfterCell.putBetween(beforeCell, oldAfterCell);
        }

        return oldAfterCell;
    }

    @Trivial
    public ZipFileData addBefore(ZipFileData afterData, ZipFileData beforeData) {
        String afterPath = afterData.path;

        if ( beforeData == afterData ) {
            throw new IllegalArgumentException("Attempt to put [ " + afterPath + " ] before itself");
        }

        Cell afterCell = cells.get(afterPath);
        if ( afterCell == null ) {
            throw new NoSuchElementException("Path not stored [ " + afterPath + " ]");
        }

        return addBefore(afterCell, beforeData).data;
    }

    @Trivial
    private Cell addBefore(Cell afterCell, ZipFileData beforeData) {
        Cell oldBeforeCell = afterCell.next;

        String beforePath = beforeData.path;
        Cell newBeforeCell = cells.get(beforePath);
        if ( newBeforeCell == null ) {
            newBeforeCell = new Cell(beforeData);
            newBeforeCell.putBetween(afterCell, oldBeforeCell);
        } else if ( newBeforeCell != oldBeforeCell ) {
            newBeforeCell.excise();
            newBeforeCell.putBetween(afterCell, oldBeforeCell);
        }

        return oldBeforeCell;
    }

    public ZipFileData addFirst(ZipFileData newFirstData) {
        Cell oldFirstCell = anchor.next;

        String path = newFirstData.path;
        Cell newFirstCell = cells.get(path);
        if ( newFirstCell == null ) {
            newFirstCell = new Cell(newFirstData);
            cells.put(path, newFirstCell);
            newFirstCell.putBetween(anchor, oldFirstCell);
        } else if ( newFirstCell.prev != anchor ) {
            newFirstCell.excise();
            newFirstCell.putBetween(anchor, oldFirstCell);
        } else {
            // Already first.
        }

        return oldFirstCell.data;
    }

    public ZipFileData addLast(ZipFileData newLastData, int maximumSize) {
        int size = size();
        if ( (maximumSize == -1) || (size < maximumSize) ) {
            @SuppressWarnings("unused")
            ZipFileData oldLastData = addLast(newLastData);
            return null;

        } else if ( size == 1 ) {
            Cell lastCell = anchor.prev;
            ZipFileData oldLastData = lastCell.data;
            lastCell.data = newLastData;
            return oldLastData;

        } else {
            Cell oldFirstCell = anchor.next;
            ZipFileData oldFirstData = oldFirstCell.data;
            
            oldFirstCell.excise();
            oldFirstCell.data = newLastData;
            oldFirstCell.putBetween(anchor.prev, anchor);

            return oldFirstData;
        }
    }

    public ZipFileData addLast(ZipFileData newLastData) {
        Cell oldLastCell = anchor.prev;

        String path = newLastData.path;
        Cell newLastCell = cells.get(path);
        if ( newLastCell == null ) {
            newLastCell = new Cell(newLastData);
            cells.put(path, newLastCell);
            newLastCell.putBetween(oldLastCell, anchor);
        } else if ( newLastCell.next != anchor ) {
            newLastCell.excise();
            newLastCell.putBetween(oldLastCell, anchor);
        } else {
            // Already last.
        }

        return oldLastCell.data;
    }

    /**
     * Add data keeping the expiration times in order.
     * 
     * While this is expected to be done only with data which has a
     * short expiration, this method handles new data with both short
     * and long expirations.
     * 
     * Walk the data from the beginning until finding the insertion point. 
     *
     * @param newData Data to add.
     * @param maximumSize The maximum allowed size of the data.
     * @param quickDelay The delay used for fast expirations.
     * @param slowDelay The delay used for slow expirations.
     * 
     * @return The first data, if the storage capacity has been exceeded.
     *     Otherwise, null.
     */
    public ZipFileData addFirst(
        ZipFileData newData,
        int maximumSize,
        long quickDelay, long slowDelay) {

        long nextExpireAt = newData.lastPendAt + (newData.expireQuickly ? quickDelay : slowDelay);

        // Stop upon reaching the anchor;
        // Or, stop on the first data that expires after the new data.

        // TODO: This is a bit slow!  We may want to use
        //       a more advanced data structure to speed up insertions.

        Cell nextCell = anchor;
        while ( ((nextCell = nextCell.next) != anchor) &&
                (nextExpireAt >= nextCell.data.expireAt(quickDelay, slowDelay)) ) {
            // NO-OP
        }

        if ( (maximumSize != -1) && (size() == maximumSize) ) {
            if ( nextCell.prev == anchor ) {
                // The new data would be first.  Don't bother adding it,
                // since it would be pushed out.

                return newData;

            } else {
                // Push out the first data; reuse and insert the old first cell.

                Cell firstCell = anchor.next;
                ZipFileData firstData = firstCell.data;

                firstCell.excise();
                firstCell.data = newData;
                firstCell.putBetween(nextCell.prev, nextCell);

                return firstData;
            }

        } else {
            // Create and insert a wholly new cell.

            Cell newCell = new Cell(newData);
            cells.put(newData.path, newCell);
            newCell.putBetween(nextCell.prev, nextCell);

            return null;
        }
    }

    @Trivial
    public ZipFileData remove(String path) {
        Cell cell = cells.remove(path);
        if ( cell == null ) {
            return null;
        }

        ZipFileData data = cell.data;
        cell.data = null;
        cell.excise();
        return data;
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
        if ( anchor == null ) {
            throw new IllegalStateException("Null anchor [ " + name + " ]");
        }

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
