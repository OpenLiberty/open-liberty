/*******************************************************************************
 * Copyright (c) 2016, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.context_fat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;

public class CustomToken implements com.ibm.wsspi.security.token.Token, Serializable {

	private static final long serialVersionUID = -8127607411518090383L;
	private byte[] tokenBytes = null;
	private java.util.Hashtable hashtable = new java.util.Hashtable();
	private static long expire_period_in_millis = 2 * 60 * 60 * 1000; // 2 hours in millis, by default
	private long counter = 0;
	private String principalName;

	/**
	 * The constructor that is used to create initial token instance
	 */
	public CustomToken(String principal)
	{
		this.principalName = principal;
		// set the token version
		addAttribute("version", "1");
		// set the token expiration
		addAttribute("expiration", new Long(System.currentTimeMillis() + expire_period_in_millis).toString());
	}

	/**
	 * The constructor that is used to deserialize the token bytes received
	 * during a propagation login.
	 */
	public CustomToken(byte[] token_bytes)
	{
		try
		{
			hashtable = (java.util.Hashtable) deserialize(token_bytes);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.wsspi.security.token.Token#getName()
	 */
	@Override
	public String getName() {
		return this.getClass().getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.wsspi.security.token.Token#isValid()
	 */
	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.wsspi.security.token.Token#getExpiration()
	 */
	@Override
	public long getExpiration() {
		// get the expiration value from the hashtable
		String[] expiration = getAttributes("expiration");

		if (expiration != null && expiration[0] != null)
		{
			// expiration is the first element (should only be one)
			System.out.println("getExpiration: returning " + expiration[0]);
			return new Long(expiration[0]).longValue();
		}

		System.out.println("getExpiration: returning 0");
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.wsspi.security.token.Token#isForwardable()
	 */
	@Override
	public boolean isForwardable() {
		// TODO Auto-generated method stub
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.wsspi.security.token.Token#getPrincipal()
	 */
	@Override
	public String getPrincipal() {
		// TODO Auto-generated method stub
		return principalName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.wsspi.security.token.Token#getBytes()
	 */
	@Override
	public byte[] getBytes() {
		/*
		 * if (hashtable != null)
		 * {
		 * try
		 * {
		 * // Do this if the object is set to read-only during login commit
		 * // because this guarantees that no new data is set.
		 * //if (isReadOnly() && tokenBytes == null)
		 * tokenBytes = serializeObject(hashtable);
		 * return tokenBytes;
		 * } catch (Exception e)
		 * {
		 * e.printStackTrace();
		 * return null;
		 * }
		 * }
		 * 
		 * System.out.println("getBytes: returning null");
		 * return null;
		 */
		try {
			tokenBytes = serializeObject(this);
			System.out.println("EFK serialized custom token");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
			// http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
			e.printStackTrace();
		}
		return tokenBytes;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.wsspi.security.token.Token#getVersion()
	 */
	@Override
	public short getVersion() {
		String[] version = getAttributes("version");

		if (version != null && version[0] != null)
			return new Short(version[0]).shortValue();

		System.out.println("getVersion: returning default of 1");
		return 1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.wsspi.security.token.Token#getUniqueID()
	 */
	@Override
	public String getUniqueID() {
		return String.valueOf(counter);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.wsspi.security.token.Token#setReadOnly()
	 */
	@Override
	public void setReadOnly() {
		addAttribute("readonly", "true");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.wsspi.security.token.Token#getAttributes(java.lang.String)
	 */
	@Override
	public String[] getAttributes(String key) {
		ArrayList array = (ArrayList) hashtable.get(key);
		if (array != null && array.size() > 0)
		{
			return (String[]) array.toArray(new String[0]);
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.wsspi.security.token.Token#addAttribute(java.lang.String, java.lang.String)
	 */
	@Override
	/**
	 * Sets the attribute name and value pair. Returns the previous values set 
	 * for the key, or returns null if the value is not previously set.
	 * @param String key
	 * @param String value
	 * @returns String[];
	 */
	public String[] addAttribute(String key, String value)
	{
		// Gets the current value for the key
		ArrayList array = (ArrayList) hashtable.get(key);

		if (!isReadOnly())
		{
			// Increments the counter to change the uniqueID
			counter++;

			// Copies the ArrayList to a String[] as it currently exists
			String[] old_array = null;
			if (array != null && array.size() > 0)
				old_array = (String[]) array.toArray(new String[0]);

			// Allocates a new ArrayList if one was not found
			if (array == null)
				array = new ArrayList();

			// Adds the String to the current array list
			array.add(value);

			// Adds the current ArrayList to the Hashtable
			hashtable.put(key, array);

			// Returns the old array
			return old_array;
		}

		return (String[]) array.toArray(new String[0]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.wsspi.security.token.Token#getAttributeNames()
	 */
	@Override
	public Enumeration getAttributeNames() {
		return hashtable.keys();
	}

	@Override
	public Object clone() {
		CustomToken deep_clone = new CustomToken(principalName);
		java.util.Enumeration keys = getAttributeNames();
		while (keys.hasMoreElements())
		{
			String key = (String) keys.nextElement();
			String[] list = getAttributes(key);
			for (int i = 0; i < list.length; i++) {
				deep_clone.addAttribute(key, list[i]);
			}
		}
		return deep_clone;
	}

	/**
	 * Utility method to serialize an object
	 * 
	 * @param object the object to serialize
	 * @return the serialized object
	 * @throws IOException
	 */
	private byte[] serializeObject(Object object) throws IOException {

		ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
		ObjectOutputStream objectOS = new ObjectOutputStream(byteArrayOS);
		byte[] bytes = null;

		try
		{
			objectOS.writeObject(object);
			bytes = byteArrayOS.toByteArray();
		} finally
		{
			objectOS.close();
			byteArrayOS.close();
		}
		return bytes;
	}

	/**
	 * Called internally to see if the token is readonly
	 */
	private boolean isReadOnly()
	{
		String[] readonly = getAttributes("readonly");

		if (readonly != null && readonly[0] != null) {
			return new Boolean(readonly[0]).booleanValue();
		}

		System.out.println("isReadOnly: returning default of false");
		return false;
	}
	
	/**
	 * Deserialize an object
	 * 
	 * @param serializedObject the byte array to deserialize
	 * @return the deserialized object
	 * @throws IOException if there was a problem creating or writing to the input stream
	 * @throws ClassNotFoundException when class of serialized object can't be found
	 */
	private Object deserialize(byte[] serializedObject) throws IOException, ClassNotFoundException {
		Object deserializedObject = null;
		ByteArrayInputStream byteArrayIS = new ByteArrayInputStream(serializedObject);
		ObjectInputStream objectIS = new ObjectInputStream(byteArrayIS);
		try {
			deserializedObject = objectIS.readObject();
		} finally {
			objectIS.close();
			byteArrayIS.close();
		}
		return deserializedObject;
	}
}