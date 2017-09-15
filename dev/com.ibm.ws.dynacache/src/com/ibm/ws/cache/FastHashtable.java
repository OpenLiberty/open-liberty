/*******************************************************************************
 * Copyright (c) 1997, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.NoSuchElementException;

public class FastHashtable extends Dictionary {

	// ///////////////////////////////////////////////////////////////////////
	//
	// Construction
	//

	public FastHashtable(int expectedEntries) {
		// Tr.entry(tc, "<init>");

		buckets = new Bucket[expectedEntries];
		for (int i = 0; i < expectedEntries; ++i) {
			buckets[i] = new Bucket();
		}

		// Tr.exit(tc, "<init");
	}

	// ///////////////////////////////////////////////////////////////////////
	//
	// Attributes
	//

	public int size() {
		return size;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	// ///////////////////////////////////////////////////////////////////////
	//
	// Operations
	//

	public Object getLock(Object key) {
		return getBucketForKey(key);
	}

	public boolean contains(Object key) {
		// if (tc.isEntryEnabled())
		// Tr.entry(tc, "contains", key);

		final Bucket bucket = getBucketForKey(key);

		synchronized (bucket) {

			Element element = bucket.findByKey(key);

			// if (tc.isEntryEnabled())
			// Tr.exit(tc, "contains", new Boolean(element != null));

			return element != null;

		}
	}

	public Object get(Object key, boolean incCount) {
		// if (tc.isEntryEnabled())
		// Tr.entry(tc, "get", key);

		final Bucket bucket = getBucketForKey(key);

		synchronized (bucket) {

			final Element element = bucket.findByKey(key);
			// if (tc.isEntryEnabled())
			// Tr.exit(tc, "get", element);
			// return element != null ? element.object : null;
			if (element != null) {
				CacheEntry ce = (CacheEntry) element.object;
				if ((null != ce) && incCount)
					ce.incRefCount();
				return ce;
			} else
				return null;

		}
	}

	public Object get(Object key) {
		// if (tc.isEntryEnabled())
		// Tr.entry(tc, "get", key);

		final Bucket bucket = getBucketForKey(key);

		synchronized (bucket) {

			final Element element = bucket.findByKey(key);
			// if (tc.isEntryEnabled())
			// Tr.exit(tc, "get", element);
			return element != null ? element.object : null;

		}
	}

	public Object remove(Object key) {
		// if (tc.isEntryEnabled())
		// Tr.entry(tc, "remove", key);

		final Bucket bucket = getBucketForKey(key);
		Element element = null;

		synchronized (bucket) {
			element = bucket.removeByKey(key);
		}

		// if (tc.isEntryEnabled())
		// Tr.exit(tc, "remove", element);

		if (element != null) {
			synchronized (this) {
				--size;
			}
			return element.object;
		}

		return null;

	}

	public Object put(Object key, Object object) {
		// if (tc.isEntryEnabled())
		// Tr.entry(tc, "put", new Object[]{key, object});

		// synchronized (this) {
		// ++size;
		// }

		final Bucket bucket = getBucketForKey(key);

		synchronized (bucket) {
			final Element e = bucket.replaceByKey(key, object);

			if (e == null) {
				synchronized (this) {
					++size;
				}
			}

			// if (tc.isEntryEnabled())
			// Tr.exit(tc, "put", e);
			return (e != null ? e.object : null);
		}
	}

	public void add(Object key, Object object) {
		// if (tc.isEntryEnabled())
		// Tr.entry(tc, "add", new Object[]{key, object});

		synchronized (this) {
			++size;
		}

		final Bucket bucket = getBucketForKey(key);

		synchronized (bucket) {
			bucket.addByKey(key, object);
			// if (tc.isEntryEnabled())
			// Tr.exit(tc, "add");
		}
	}

	public final Enumeration elements() {
		return new ObjectEnumerator();
	}

	public final Enumeration keys() {
		return new KeyEnumerator();
	}

	public synchronized void clear() {
		size = 0;

		buckets = new Bucket[buckets.length];
		for (int i = 0; i < buckets.length; ++i) {
			buckets[i] = new Bucket();
		}
	}

	// ////////////////////////////////////////////////////////////////////////
	//
	// Implementation
	//

	// Returns the bucket which the specified key hashes to
	protected final Bucket getBucketForKey(Object key) {
		return buckets[(key.hashCode() & 0x7FFFFFFF) % buckets.length];
	}

	// ////////////////////////////////////////////////////////////////////////
	//
	// Data
	//

	protected Bucket[] buckets;
	protected int size = 0;

	// private static final TraceComponent tc =
	// Tr.register(com.ibm.ejs.util.FastHashtable.class);

	// ////////////////////////////////////////////////////////////////////////

	class ElementEnumerator implements Enumeration {

		//
		// Enumeration interface
		//

		// Determine if there are more elements remaining in the enumeration;
		// returns true if more elements remain, false otherwise
		public boolean hasMoreElements() {
			// If there are more elements in the bucket we're currently
			// enumerating or if we can find another non-empty bucket, the
			// the enumeration has more elements

			if (bucketContents != null && bucketContents.hasMoreElements()) {
				return true;
			} else {
				return findNextBucket();
			}
		}

		// Get the next element in the enumeration.
		public Object nextElement() {
			// If we don't have a current bucket, or we've exhausted the
			// elements in the current bucket, try to find another non-empty
			// bucket

			if (bucketContents == null || !bucketContents.hasMoreElements()) {

				if (!findNextBucket()) {
					// No more non-empty buckets, no more elements
					throw new NoSuchElementException();
				}
			}

			// Try to return the next element from the current bucket;
			// since there may be concurrent access to the bucket, we
			// my fail "unexpectedly"; if we do, try to find another
			// non-empty bucket

			do {

				try {
					return bucketContents.nextElement();
				} catch (NoSuchElementException e) {
					com.ibm.ws.ffdc.FFDCFilter.processException(e,
							"com.ibm.ejs.util.FastHashtable.nextElement",
							"267", this);
				}

			} while (findNextBucket());

			// Nothing left!

			throw new NoSuchElementException();
		}

		// Finds the next non-empty bucket of the cache's hash table; returns
		// true if such a bucket it found, false otherwise
		private boolean findNextBucket() {
			bucketContents = null;

			while (bucketIndex < buckets.length) {
				Bucket bucket = buckets[bucketIndex++];
				synchronized (bucket) {
					if (bucket.size() > 0) {
						bucketContents = bucket.elements();
						return true;
					}
				}
			}

			return false;
		}

		// The array index of the hash table bucket we're currenly
		// enumerating
		private int bucketIndex = 0;

		// The contents of the current bucket
		private Enumeration bucketContents;

	}

	class ObjectEnumerator extends ElementEnumerator {
		public Object nextElement() {
			return ((Element) super.nextElement()).object;
		}
	}

	class KeyEnumerator extends ElementEnumerator {
		public Object nextElement() {
			return ((Element) super.nextElement()).key;
		}
	}

}
