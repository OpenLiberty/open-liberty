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

import javax.resource.cci.InteractionSpec;

import java.io.ObjectInputStream;                                /* @F003705A*/
import java.io.ObjectOutputStream;                               /* @F003705A*/
import java.io.ObjectStreamField;                                /* @F003705A*/

/**
 * This class defines the methods necessary to interact with a service
 * being hosted by an external address space or CICS region.
 *
 * @ibm-api
 */
public class InteractionSpecImpl implements InteractionSpec 
{
	private static final long serialVersionUID = 6558841348299793668L;

  /**
   * First version of this class
   */
  private static final int VERSION_1 = 1;                        /* @F003705A*/

  /**
   * Second version of this class
   */
  private static final int VERSION_2 = 2;                        /* @F013381A*/

  /**
   * Current object version.  Only needs to be changed if something drastic
   * alters the object structure, since small additions/deletions can be
   * handled by the Java serialization mechanism.
   */
  private static final int CURRENT_VERSION = VERSION_2;          /* @F013381C*/

  /**
   * Fields that we are serializing
   */
  private static final ObjectStreamField[] serialPersistentFields =
    new ObjectStreamField[]                                      /* @F003705A*/
    {
      new ObjectStreamField("_version", Integer.TYPE),
      new ObjectStreamField("_serviceName", String.class)
    };

  /**
   * Version number
   */
  private int _version = CURRENT_VERSION;                        /* @F003705A*/

	/**
	 * Service name
	 */
	private String _serviceName = null;
	
	/**
	 * Sets the service name to connect to.
   * @param serviceName The name of the service to connect to.
	 */
	public void setServiceName(String serviceName)
	{
		_serviceName = serviceName;
	}
	
	/**
	 * Gets the service name to connect to.
   * @return The name of the service to connect to.
	 */
	public String getServiceName()
	{
		return _serviceName;
	}

  /**
   * Hash code for equality tests
   */
  public int hashCode()                                          /* @F003705A*/
  {
    return ((_serviceName != null) ? _serviceName.hashCode() : 0);
  }

  /**
   * Equality check
   */
  public boolean equals(Object thatObject)                       /* @F003705A*/
  {
    boolean result = false;

    if (thatObject == this)
    {
      result = true;
    }
    else if ((thatObject != null) && 
             (thatObject instanceof InteractionSpecImpl))
    {
      InteractionSpecImpl that = (InteractionSpecImpl)thatObject;

      result = ((this._version == that._version) &&
                (compareObjects(this._serviceName, that._serviceName)));
    }

    return result;
  }

  /**
   * Compares two objects, either of which could be null
   */
  private boolean compareObjects(Object a, Object b)             /* @F003705A*/
  {
    boolean result = false;

    if (a == b) result = true;
    else if ((a != null) && (b != null))
    {
      result = a.equals(b);
    }

    return result;
  }

  /**
   * Serialization support to write an object.  Note that this is implemented
   * so that we can support object versioning in the future, even though there
   * is only one object version now.
   */
  private void writeObject(ObjectOutputStream s)                 /* @F003705A*/
    throws java.io.IOException
  {
    ObjectOutputStream.PutField putField = s.putFields();
    putField.put("_version", _version);
    putField.put("_serviceName", _serviceName);
    s.writeFields();
  }

  /**
   * Serialization support to read an object.  Note that this is implemented
   * so that we can support object versioning in the future, even though there
   * is only one object version now.
   */
  private void readObject(ObjectInputStream s)                   /* @F003705A*/
    throws java.io.IOException, 
           java.lang.ClassNotFoundException
  {
    ObjectInputStream.GetField getField = s.readFields();
    
    if (getField.defaulted("_version"))
    {
      throw new java.io.InvalidObjectException(
        "No version number in serialized object");
    }
    
    _version = getField.get("_version", (int)CURRENT_VERSION);

    if (_version > CURRENT_VERSION)
    {
      throw new java.io.InvalidObjectException(
        "Version number in serialized object is not supported (" + _version + 
        " > " + CURRENT_VERSION + ")");
    }

    _serviceName = (String)getField.get("_serviceName", null);
  }
}
