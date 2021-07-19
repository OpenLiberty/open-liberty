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

/**
 * Object representing a OLA connector.
 *
 */
public class OLAConnectionHandle implements Serializable {       /* @F003691C*/
  
  /**
   * Serialization support.
   */
  private static ObjectStreamField[] serialPersistentFields =    /* @F003691A*/
    new ObjectStreamField[]                                      /* @F003691A*/
    {                                                            /* @F003691A*/
      new ObjectStreamField("_ahdleye", String.class),           /* @F003691A*/
      new ObjectStreamField("_ahdlflg_active", Boolean.TYPE),    /* @F003691A*/
      new ObjectStreamField("_address", Long.TYPE),              /* @F003691A*/
      new ObjectStreamField("_lscb", Integer.TYPE),              /* @F003691A*/
      new ObjectStreamField("_state", Long.TYPE),                /* @F003691A*/
      new ObjectStreamField("_valid", Boolean.TYPE)              /* @F003691A*/
    };                                                           /* @F003691A*/
	
	/** Eye catcher 'BBOAHDL' */
	private String _ahdleye;

	/** Flag indicating if the connection is active */
	private boolean _ahdlflg_active;
	
	/** 8 byte handle ID (address) */
	private long _address;
	
	/** 4 byte address of the LSCB control block */
	private int _lscb;                                             /* @F003691C*/
	
	/** 8 byte state of the connection handle */
	private long _state;
	
  /** Valid bit */
	private boolean _valid = false;
	
	/**
   * Constructor
	 */
  public OLAConnectionHandle(String eyeCatcher,
                             boolean active,
                             long address,
                             int lscbAddress,
                             long state,
                             boolean valid)                      /* @F003691A*/
  {
    _ahdleye = eyeCatcher;
    _ahdlflg_active = active;
    _address = address;
    _lscb = lscbAddress;
    _state = state;
    _valid = valid;
	}

	/**
	 * @return the _ahdleye
	 */
	public String get_ahdleye() {
		return _ahdleye;
	}

	/**
	 * @return the _ahdlflg_active
	 */
	public boolean is_ahdlflg_active() {
		return _ahdlflg_active;
	}

	/**
	 * @return the _address
	 */
	public long get_address() {
		return _address;
	}

	/**
	 * @return the _lscb
	 */
	public long get_lscb() {                                       /* @F003691C*/
		return _lscb;                                                /* @F003691C*/
	}

	/**
   * Gets the state of the connection handle
	 */
	public long get_state() {
		return _state;
	}


	/**
	 * Returns the string corresponding to the state 
	 * of the connection handle.
	 */
	public String get_stateString()
	{
		String stateString;
		if (_state == 0x00)
			stateString = "Pooled";
		else if (_state == 0x01)
			stateString = "Ready";
		else if (_state == 0x02)
			stateString = "ReqSent";
		else if (_state == 0x03)
			stateString = "RspRcvd";
		else if (_state == 0x04)
			stateString = "ReqRcvd";
		else if (_state == 0x05)
			stateString = "DataRcvd";
		else if (_state == 0x06)
			stateString = "Err";
		else
			stateString = "Unknown";
		
		return stateString;
	}

	public boolean is_valid() {
		return _valid;
	}


  public boolean equals(Object thatObject)                       /* @F003691A*/
  {
    boolean result = false;

    if (this == thatObject)
    {
      result = true;
    }
    else if ((thatObject != null) && 
             (thatObject instanceof OLAConnectionHandle))
    {
      OLAConnectionHandle that = (OLAConnectionHandle)thatObject;

      if ((this._ahdleye.equals(that._ahdleye)) &&
          (this._ahdlflg_active == that._ahdlflg_active) &&
          (this._address == that._address) &&
          (this._lscb == that._lscb) &&
          (this._state == that._state) &&
          (this._valid == that._valid))
      {
        result = true;
      }
    }

    return result;
	}


  /**
   * Serialization support to write an object
   */
  private void writeObject(ObjectOutputStream s)                 /* @F003691A*/
    throws java.io.IOException
  {
    ObjectOutputStream.PutField putField = s.putFields();
    putField.put("_ahdleye", _ahdleye);
    putField.put("_ahdlflg_active", _ahdlflg_active);
    putField.put("_address", _address);
    putField.put("_lscb", _lscb);
    putField.put("_state", _state);
    putField.put("_valid", _valid);
    s.writeFields();
	}

  /**
   * Serialization support to read an object
   */
  private void readObject(ObjectInputStream s)                   /* @F003691A*/
    throws java.io.IOException,
           java.lang.ClassNotFoundException
  {
    ObjectInputStream.GetField getField = s.readFields();
    _ahdleye = (String)getField.get("_ahdleye", new String("UNKNOWN"));
    _ahdlflg_active = getField.get("_ahdlflg_active", false);
    _address = getField.get("_address", (long)-1);
    _lscb = getField.get("_lscb", (int)-1);
    _state = getField.get("_state", (long)-1);
    _valid = getField.get("_valid", false);
  }

}
