/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.ola;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import javax.resource.cci.IndexedRecord;

/**
 * Implementation of the IndexedRecord interface used by the Optimized
 * Local Adapter (OLA).  This implementation should be obtained through the 
 * RecordFactory provided by the cci.
 *
 * @ibm-api
 */
public class IndexedRecordImpl implements IndexedRecord {

	//TODO: Need to add support for dynamically returning RAD / RD/z tooling type as
	//      described in the SDD
	
	/**
	 * The serial version (for Java serialization APIs)
	 */
	private static final long serialVersionUID = 1607302106806298841L;

	/**
	 * Record bytes class name
	 */
	private static final String RECORD_BYTES_CLASSNAME = "com.ibm.etools.marshall.RecordBytes";
	
	/**
	 * A place to store the individual data elements
	 */
	private ArrayList<byte[]> records = new ArrayList<byte[]>();
	
	/**
	 * The name of this record
	 */
	private String recordName;
	
	/**
	 * A description of the record
	 */
	private String description;
	
	/**
	 * Public, general purpose constructor
	 */
	public IndexedRecordImpl()
	{
	}
	
	/**
	 * Private, copy/clone constructor
	 */
	@SuppressWarnings("unchecked")
	private IndexedRecordImpl(IndexedRecordImpl base)
	{
		this.recordName = base.getRecordName();
		this.description = base.getRecordShortDescription();
		
		ArrayList<byte[]> list = base.records;
		this.records = (ArrayList<byte[]>)(list.clone());
	}
	
	/**
	 * Gets the record name (created by the client)
   * @return The name of the record
	 */
	public String getRecordName() 
	{
		return recordName;
	}

	/**
	 * Gets the description
   * @return The description
	 */
	public String getRecordShortDescription() 
	{
		return description;
	}

	/**
	 * Sets the record name
   * @param arg0 The name of the record
	 */
	public void setRecordName(String arg0) 
	{
		recordName = arg0;
	}

	/**
	 * Sets the description
   * @param arg0 The description of the record
	 */
	public void setRecordShortDescription(String arg0) 
	{
		description = arg0;
	}

    /**
     * Tells us whether this Record is an instance of the RecordBytes interface (meaning it was
     * generated from RAD).
     * 
     * Since we require applications in Liberty to package the RecordBytes interface and supporting
     * code (marshall.jar) inside their application, we can't just do an instanceof check here to
     * see if we have an instance of RecordBytes.  The RecordBytes instance that we are checking
     * against will have been loaded by a different classloader, and the check will fail.
     * 
     * Instead, we must use the classloader which loaded/created the Record class, or just query
     * the class for the interfaces it implements and see if the string name of the RecordBytes
     * interface is one of those.
     */
    private boolean isRecordBytes(Object o) {
    	try {
    		Class<?> recordClass = o.getClass();
    		ClassLoader cl = recordClass.getClassLoader();
    		Class<?> recordBytesClass = cl.loadClass(RECORD_BYTES_CLASSNAME);
    		return recordBytesClass.isAssignableFrom(recordClass);
    	} catch (Throwable t) {
    		/* FFDC */
    	}

    	return false;
    }

    /**
	 * Adds an element to the record.  The element can be a byte[] or can be
	 * a Record instance created by the RAD or RD/z tooling.  The element is added
	 * at the end of the list.
   * @param o The record or byte[] to add.
   * @return true if the record was added successfully
	 */
	public boolean add(Object o)
	{
		if (o == null)
		{
			throw new NullPointerException();
		}
		
		if (o instanceof byte[])
		{
			records.add((byte[])o);
		}
		else if (isRecordBytes(o))
		{
			try {
        		Class<?> c = o.getClass();
        		Method m = c.getMethod("getBytes");
        		records.add((byte[]) m.invoke(o, (Object[])null));
			} catch (Throwable t) {
				throw new IllegalArgumentException("Unable to store RecordBytes into IndexedRecord", t);
			}
		}
		else
		{
			throw new IllegalArgumentException("Invalid type");
		}

		return true;
	}

	/**
	 * Adds an element to the record.  The element can be a byte[] or can be
	 * a Record instance created by the RAD or RD/z tooling.  The element is added
	 * at the specified index, as per the java.util.List contract.
   * @param index The index at which to add the object
   * @param o The byte[] or Record to insert
	 */
	public void add(int index, Object o) 
	{
		if (o == null)
		{
			throw new NullPointerException();
		}
		
		if (o instanceof byte[])
		{
			records.add(index, (byte[])o);
		}
		else if (isRecordBytes(o))
		{
			try {
        		Class<?> c = o.getClass();
        		Method m = c.getMethod("getBytes");
        		records.add(index, (byte[]) m.invoke(o, (Object[])null));
			} catch (Throwable t) {
				throw new IllegalArgumentException("Unable to store RecordBytes into IndexedRecord", t);
			}
		}
		else
		{
			throw new IllegalArgumentException("Invalid type");
		}
	}

	/**
	 * Adds all of the elements of the collection to the record.
   * @param c The collection of objects to add.
   * @return true if the collection was added successfully
	 */
	public boolean addAll(Collection c) 
	{
		LinkedList<byte[]> tempList = new LinkedList<byte[]>();
		
		if (c == null) throw new NullPointerException();
		
		Iterator i = c.iterator();
		
		while (i.hasNext())
		{
			Object o = i.next();
			
			if (o == null) throw new NullPointerException();
			
			if (o instanceof byte[])
			{
				tempList.add((byte[])o);
			}
			else if (isRecordBytes(o))
			{
				try {
	        		Class<?> cl = o.getClass();
	        		Method m = cl.getMethod("getBytes");
	        		tempList.add((byte[]) m.invoke(o, (Object[])null));
				} catch (Throwable t) {
					throw new IllegalArgumentException("Unable to store RecordBytes into IndexedRecord", t);
				}
			}
			else
			{
				throw new IllegalArgumentException("Invalid type contained within the collection");
			}
		}
		
		return records.addAll(tempList);
	}

	/**
	 * Adds all the elements of the collection to the record at the specified 
   * index.
   * @param index The index at which to add the collection
   * @param c The collection of objects to add
   * @return true if the add was successful
	 */
	public boolean addAll(int index, Collection c) {
		LinkedList<byte[]> tempList = new LinkedList<byte[]>();
		
		if (c == null) throw new NullPointerException();
		
		Iterator i = c.iterator();
		
		while (i.hasNext())
		{
			Object o = i.next();
			
			if (o == null) throw new NullPointerException();
			
			if (o instanceof byte[])
			{
				tempList.add((byte[])o);
			}
			else if (isRecordBytes(o))
			{
				try {
	        		Class<?> cl = o.getClass();
	        		Method m = cl.getMethod("getBytes");
	        		tempList.add((byte[]) m.invoke(o, (Object[])null));
				} catch (Throwable t) {
					throw new IllegalArgumentException("Unable to store RecordBytes into IndexedRecord", t);
				}
			}
			else
			{
				throw new IllegalArgumentException("Invalid type contained within the collection");
			}
		}
		
		return records.addAll(index, tempList);
	}

	/**
	 * Clears the record of all data.
	 */
	public void clear() 
	{
		records.clear();
	}

	/**
	 * Searches the record for the specified entry.  The binary data (either the
	 * byte[] or the data within the Record objects) are used to compare the
	 * entries, and thus this is an expensive operation.  Null entries are not
	 * allowed and will never be matched.
   * @param o The object for which to check
   * @return true if the object was found
	 */
	public boolean contains(Object o) 
	{
		return (indexOf(o) != -1);
	}

	/**
	 * Searches the record for the specified entrys.  The binary data (either the
	 * byte[] or the data within the Record objects) are used to compare the
	 * entries, and thus this is an expensive operation.  Null entries are not
	 * allowed and will never be matched.
   * @param c The collection of objects to search for
   * @return true if all the objects were found
	 */
	public boolean containsAll(Collection c) 
	{
		if (c == null) throw new NullPointerException();
		
		boolean found = true;
		
		Iterator i = c.iterator();
		while ((i.hasNext()) && (found))
		{
			Object o = i.next();
			found = contains(o);
		}

		return found;
	}

	/**
	 * Retrieves an object from the record at the specified index.
   * @param index The index at which to get the object
   * @return The object
	 */
	public Object get(int index) 
	{
		return records.get(index);
	}

	/**
	 * Gets the index of a particular entry.  The byte[] representation of the
	 * object is used to compare for equality.
   * @param o The object to search for
   * @return The index of the object, or -1 if not found.
	 */
	public int indexOf(Object o) 
	{
		byte[] data = null;

		int index = -1;
		
		if (o != null)
		{
			if (o instanceof byte[])
			{
				data = (byte[])o;
			}
			else if (isRecordBytes(o))
			{
				try {
	        		Class<?> c = o.getClass();
	        		Method m = c.getMethod("getBytes");
	        		data = (byte[]) m.invoke(o, (Object[])null);
				} catch (Throwable t) {
					throw new IllegalArgumentException("Unable to store RecordBytes into IndexedRecord", t);
				}
			}
			
			if (data != null)
			{
				ListIterator<byte[]> i = records.listIterator();
				while (i.hasNext() && (index == -1))
				{
					int curIndex = i.nextIndex();
					byte[] curData = i.next();
					if ((curData.length == data.length) &&
						(Arrays.equals(curData, data)))
					{
						index = curIndex;
					}
				}
			}
		}

		return index;
	}

	/**
	 * Checks to see if the record is empty
   * @return true if the record is empty
	 */
	public boolean isEmpty() 
	{
		return records.isEmpty();
	}

	/**
	 * Gets an iterator over the records
   * @return An iterator over the records within this indexed record
	 */
	public Iterator iterator() 
	{
		return records.iterator();
	}


	/**
	 * Gets the last index of a particular entry.  The byte[] representation of the
	 * object is used to compare for equality.
   * @param o The object to search for
   * @return The index of the object, or -1 if not found.
	 */
	public int lastIndexOf(Object o) {
		byte[] data = null;

		int index = -1;
		
		if (o != null)
		{
			if (o instanceof byte[])
			{
				data = (byte[])o;
			}
			else if (isRecordBytes(o))
			{
				try {
	        		Class<?> c = o.getClass();
	        		Method m = c.getMethod("getBytes");
	        		data = (byte[]) m.invoke(o, (Object[])null);
				} catch (Throwable t) {
					throw new IllegalArgumentException("Unable to store RecordBytes into IndexedRecord", t);
				}
			}
			
			if (data != null)
			{
				for (int i = records.size() - 1; ((i >= 0) && (index == -1)); i--)
				{
					byte[] curData = records.get(i);
					if ((curData.length == data.length) &&
						(Arrays.equals(curData, data)))
					{
						index = i;
					}
						
				}
			}
		}

		return index;
	}

	/**
	 * Gets a list iterator over the records.  The records returned in the iterator
	 * are in their raw serialized form (byte[]).
   * @return A ListIterator over the records within this indexed record.
	 */
	public ListIterator listIterator() 
	{
		return records.listIterator();
	}

	/**
	 * Gets a list iterator starting at the specified index.  The records returned
	 * in the iterator are in their raw serialized form (byte[]).
   * @return A ListIterator over the records within this indexed record.
	 */
	public ListIterator listIterator(int index) 
	{
		return records.listIterator(index);
	}

	/**
	 * Removes an object from the list.  The raw byte[] version of the object is
	 * used to find the correct element.
   * @param o The object to remove
   * @return true if the object was found and removed.
	 */
	public boolean remove(Object o) 
	{
		boolean removed = false;
		
		int index = indexOf(o);
		
		if (index != -1) 
		{
			records.remove(index);
			removed = true;
		}

		return removed;
	}

	/**
	 * Removes a specified element from the list, based on index.  If removed,
	 * the object returned represents the raw serialized form (byte[]) of the
	 * object.
   * @param index The index that should be removed
   * @return The object instance that was removed.
	 */
	public Object remove(int index) 
	{
		return records.remove(index);
	}

	/**
	 * Removes all members of the specified collection.  The raw serialized (byte[])
	 * version of the elements are used to perform the comparison.
   * @param c The collection of objects to remove.
   * @return true if all the object in the collection were removed.
	 */
	public boolean removeAll(Collection c) 
	{
		boolean removedSomething = false;
		
		if (c == null) throw new NullPointerException();
		
		Iterator i = c.iterator();
		while (i.hasNext())
		{
			Object o = i.next();
			if (remove(o) == true) removedSomething = true;
		}

		return removedSomething;
	}

	/**
	 * This operation is not supported.
   * @throws java.lang.UnsupportedOperationException
	 */
	public boolean retainAll(Collection c) 
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Sets an element at a given index.  The object at that element (if any) is
	 * returned.  The byte[] representation of the object is returned, regardless of
	 * what type of item was used to set it initially.
   * @param index The index at which to set the object
   * @param o The object to set
   * @return The object which was previously at the given index.  The object
   *         is returned in byte[] form.
	 */
	public Object set(int index, Object o) 
	{
		byte[] data = null;
		
		if (o == null)
		{
			throw new NullPointerException();
		}
		
		if (o instanceof byte[])
		{
			data = (byte[])o;
		}
		else if (isRecordBytes(o))
		{
			try {
        		Class<?> c = o.getClass();
        		Method m = c.getMethod("getBytes");
        		data = (byte[]) m.invoke(o, (Object[])null);
			} catch (Throwable t) {
				throw new IllegalArgumentException("Unable to store RecordBytes into IndexedRecord", t);
			}
		}
		else
		{
			throw new IllegalArgumentException("Invalid type");
		}		

		return records.set(index, data);
	}

	/**
	 * Returns the size of the record (number of elements).
   * @return The number of elements in this indexed record
	 */
	public int size()
	{
		return records.size();
	}

	/**
	 * Returns a portion of the record, as a list.  The list elements represent the byte[]
	 * versions of the data.
   * @param fromIndex The index from which to start
   * @param toIndex The index at which to end
   * @return A List spanning the two indexes.
	 */
	public List subList(int fromIndex, int toIndex) 
	{
		return records.subList(fromIndex, toIndex);
	}

	/**
	 * Creates an array of byte[] objects representing the elements of this record.
   * @return The objects in this indexed record, as an array of Objects.
	 */
	public Object[] toArray() 
	{
		return records.toArray();
	}

	/**
	 * Returns an array of byte[] objects.  The input array must be of type byte[][]. 
   * @param a An array of byte[] objects to be filled in.
   * @return The array of objects in byte[] form.
	 */
	@SuppressWarnings("unchecked")
	public Object[] toArray(Object[] a) 
	{
		return records.toArray(a);
	}

	/**
	 * Makes a copy of the list.
   * @return A copy of this indexed record.
	 */
	public Object clone() throws java.lang.CloneNotSupportedException
	{
		return new IndexedRecordImpl(this);
	}
}
