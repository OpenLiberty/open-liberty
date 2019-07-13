/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.tu;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ibm.ws.sip.container.util.SipUtil;

/**
 * A set of DialogUserKey elements.
 * This set is implemented as either an int or a HashSet.
 * The int is all that is needed if all elements in the set have just
 * a method and no "secondary key".
 * If there is one or more element with a secondary key in the set,
 * then implementation switches to the HashSet.
 * 
 * @author ran
 */
public class DialogUsageKeySet implements Externalizable, Cloneable
{
	/** Serialization UID (do not change) */
    private static final long serialVersionUID = 5788271534706037540L;
    
	/**
	 * the bit set of all methods in this set.
	 * this member is used as long as there are no secondary-key elements.
	 */
	private int m_bitSet;

	/**
	 * a HashSet of the elements. this HashSet is instantiated only when
	 * inserting the first element that contains a secondary-key.
	 */
	private Set<DialogUsageKey> m_set;

	/**
	 * constructor
	 */
	public DialogUsageKeySet() {
		m_bitSet = 0;
		m_set = null;
	}

	/**
	 * adds a new element to this set
	 * @param key the new element to add
	 */
	public void add(DialogUsageKey key) {
		if (m_set == null) {
			// efficient mode
			String method = key.getMethod();
			String secondary = key.getSecondaryKey();
			if (secondary == null) {
				// remain in efficient mode
				int bit = SipUtil.getDialogRelatedMethodId(method);
				m_bitSet |= (1 << bit);
			}
			else {
				switchMode();
				m_set.add(key);
			}
		}
		else {
			m_set.add(key);
		}
	}

	/**
	 * called when adding a key with "secondaryKey" the first time,
	 * to switch from efficient (bit set) to HashSet
	 */
	private void switchMode() {
		//Synchronized to prevent another thread (from the application) modifying it during replication.  
		m_set = Collections.synchronizedSet(new HashSet<DialogUsageKey>());
		int bitSet = m_bitSet;
		int i = 0;
		while (bitSet != 0) {
			int bit = bitSet & 1;
			if (bit == 1) {
				String method = SipUtil.getDialogRelatedMethod(i);
				DialogUsageKey key = DialogUsageKey.instance(method, null);
				m_set.add(key);
			}
			bitSet >>= 1;
			i++;
		}
	}

	/**
	 * removes the given element from the set
	 * @param key the element to remove
	 */
	public void remove(DialogUsageKey key) {
		if (m_set == null) {
			// efficient mode
			String method = key.getMethod();
			int bit = SipUtil.getDialogRelatedMethodId(method);
			m_bitSet &= ~(1 << bit);
		}
		else {
			m_set.remove(key);
		}
	}

	/**
	 * removes all elements from this set
	 */
	public void clear() {
		if (m_set == null) {
			// efficient mode
			m_bitSet = 0;
		}
		else {
			m_set.clear();
		}
	}

	/**
	 * @return true if this set is empty
	 */
	public boolean isEmpty() {
		if (m_set == null) {
			// efficient mode
			return m_bitSet == 0;
		}
		else {
			return m_set.isEmpty();
		}
	}

	/**
	 * @see java.lang.Object#clone()
	 */
	public Object clone() {
		DialogUsageKeySet cloned = new DialogUsageKeySet();
		cloned.m_bitSet = m_bitSet;

		if (m_set == null) {
			cloned.m_set = null;
		} else {
			cloned.m_set = Collections.synchronizedSet(new HashSet<DialogUsageKey>());
			cloned.m_set.addAll(m_set);
		}

		return cloned;
	}

	/**
	 * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
	 */
	public void readExternal(ObjectInput in) throws IOException,
		ClassNotFoundException
	{
		m_bitSet = in.readInt();
		if (in.readBoolean()) {
			m_set = (Set<DialogUsageKey>)in.readObject();
		}
		else {
			m_set = null;
		}
	}

	/**
	 * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(m_bitSet);
		if (m_set == null) {
			out.writeBoolean(false);
		}
		else {
			out.writeBoolean(true);
			out.writeObject(m_set);
		}
	}
}
