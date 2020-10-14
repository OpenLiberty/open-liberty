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

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.util.LinkedList;

/**
 * A list of Optimized Local Adapter registrations.
 */
public class OLARGEList implements Serializable {                /* @F003691C*/

	/**
	 * Serialization key
	 */
	private static final long serialVersionUID = -5672644324301733044L;

  /**
   * Fields that we are serializing
   */
  private static ObjectStreamField[] serialPersistentFields =    /* @F003691A*/
    new ObjectStreamField[]
    {
      new ObjectStreamField("_version", Integer.TYPE),
      new ObjectStreamField("_serverRGE", OLARGEInformation.class),
      new ObjectStreamField("_serverName", String.class),
      new ObjectStreamField("_registrations", LinkedList.class)
    };
	
  /**
   * First version of this class
   */
  private static final int VERSION_1 = 1;                        /* @F003691A*/

  /**
   * Current version of this class
   */
  private static final int CUR_VERSION = VERSION_1;              /* @F003691A*/

  /**
   * Version of this serializable object
   */
  private int _version = CUR_VERSION;                            /* @F003691A*/

	/** 
   * The server registartion that this list pertains to 
   */
	private OLARGEInformation _serverRGE = null;                   /* @F003691C*/
	
	/** 
   * The server name of the server RGE 
   */
	private String _serverName = null;

	/**
   * The list of registrations
   */
  private LinkedList<OLARGEInformation> _registrations = null;

  /**
   * Constructor
   */
  public OLARGEList(LinkedList<OLARGEInformation> registrations,
                    OLARGEInformation serverRegistration,
                    String serverName)                           /* @F003691A*/
  {
    _registrations = registrations;
    _serverName = serverName;
    _serverRGE = serverRegistration;
  }

	/**
   * Obtains the registration that represents the WebSphere Application Server
   * which owns the registations returned in this list.
	 * @return the server's registration object, or null if unavailable
	 */
	public OLARGEInformation get_serverRGE()                       /* @F003691C*/
  {
		return _serverRGE;
	}

	/**
   * Returns the name of the server which owns the registrations returned in
   * this list.
	 * @return the server name
	 */
	public String get_serverName()                                 /* @F003691C*/
  {
		return _serverName;
	}

	/**
   * Obtains the list of registrations
   * @return The list of registrations
	 */
  public LinkedList<OLARGEInformation> get_registrations()       /* @F003691A*/
  {
    return _registrations;
  }

  /**
   * Provides the hash code for this object
   * @return The hash code
   */
  public int hashCode()                                          /* @F003691A*/
  {
    return _registrations.hashCode();
  }

  /**
   * Equality function
   * @return true if equal, false if not
   */
  public boolean equals(Object that)                             /* @F003691A*/
  {
    boolean result = false;

    if (that == this)
    {
      result = true;
    }
    else if ((that != null) && (that instanceof OLARGEList))
    {
      OLARGEList thatList = (OLARGEList)that;

      if ((this._version == thatList._version) &&
          (this._serverName.equals(thatList._serverName)) &&
          (this._serverRGE.equals(thatList._serverRGE)) &&
          (this._registrations.equals(thatList._registrations)))
      {
        result = true;
      }
    }

    return result;
  }

  /**
   * Serialization support for writing this object.
   */
  private void writeObject(ObjectOutputStream s)                 /* @F003691A*/
    throws java.io.IOException
  {
    ObjectOutputStream.PutField putField = s.putFields();
    putField.put("_version", _version);
    putField.put("_serverRGE", _serverRGE);
    putField.put("_serverName", _serverName);
    putField.put("_registrations", _registrations);
    s.writeFields();
	}

	/**
   * Serialization support for reading this object.
	 */
  private void readObject(ObjectInputStream s)                   /* @F003691A*/
    throws java.io.IOException,
           java.lang.ClassNotFoundException
  {
    ObjectInputStream.GetField getField = s.readFields();

    /*----------------------------------------------------------------------*/
    /* Version number must be specified.                                    */
    /*----------------------------------------------------------------------*/
    if (getField.defaulted("_version"))
    {
      throw new java.io.IOException("The OLARGEList class cannot be deserialized, version number is not specified");
    }

    _version = getField.get("_version", CUR_VERSION);

    /*----------------------------------------------------------------------*/
    /* Read the rest of the variables.                                      */
    /*----------------------------------------------------------------------*/
    if (getField.defaulted("_registrations"))
    {
      throw new java.io.IOException("The OLARGEList class cannot be deserialized, the registration list is not specified");
    }

    _registrations = 
      (LinkedList<OLARGEInformation>)getField.get("_registrations", null);

    _serverName = (String)getField.get("_serverName", new String("UNKNOWN"));
    _serverRGE = (OLARGEInformation)getField.get("_serverRGE", null);
	}
}
