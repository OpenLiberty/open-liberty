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
package com.ibm.ws.logging.hpel.impl;

import java.util.ArrayList;

import com.ibm.ws.logging.hpel.LogRecordSerializer;
import com.ibm.ws.logging.hpel.SerializationObject;

/**
 * Pool of reusable {@link SerializationObject} instances.
 */
public abstract class SerializationObjectPool {
	private static final int INITIAL_NUM_OF_OBJECTS = 20;
	private static final int MAXIMUM_NUM_OF_OBJECTS = 25;

	private ArrayList<SerializationObject> ivListOfObjects = new ArrayList<SerializationObject>(INITIAL_NUM_OF_OBJECTS);

	/**
	 * create <code>SerializationObjectPool</code> using the specified formatter.
	 * @see LogRecordSerializer
	 */
	public SerializationObjectPool() {
		// the intialization will be done here
		for (int i = 0; i < INITIAL_NUM_OF_OBJECTS; ++i) {
			ivListOfObjects.add(createNewObject());
		}
	}

	/**
	 * Gets the next avaialable serialization object.
	 * 
	 * @return ISerializationObject instance to do the conversation with.
	 */
	public SerializationObject getSerializationObject() {
		SerializationObject object;
		synchronized (ivListOfObjects) {
			if (ivListOfObjects.isEmpty()) {
				object = createNewObject();
			} else {
				object = ivListOfObjects.remove(ivListOfObjects.size() - 1);
			}
		}
		return object;
	}

	/**
	 * Returns a serialization object back into the pool.
	 * 
	 * @param object an instance previously allocated with {@link #getSerializationObject()}.
	 */
	public void returnSerializationObject(SerializationObject object) {
		synchronized (ivListOfObjects) {
			if (ivListOfObjects.size() < MAXIMUM_NUM_OF_OBJECTS) {
				ivListOfObjects.add(object);
			}
		}
	}

	/**
	 * Creates new object for the pool. It is called whenever pool runs
	 * out of the objects.
	 * 
	 * @return the instance of the SerializationObject.
	 */
	public abstract SerializationObject createNewObject();
}
