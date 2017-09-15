/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.mfp.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.TraceComponent;

/**
 *  JsDestinationAddressImpl implements JsDestinationAddress and hence
 *  SIDestinationAddress.
 */
final class JsDestinationAddressImpl implements JsDestinationAddress, Serializable {

  private final static long serialVersionUID = 1L;

  private final static TraceComponent tc = SibTr.register(JsDestinationAddressImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  private final static String TEMPORARY_QUEUE_DESTINATION_PREFIX = "_Q";
  private final static String TEMPORARY_TOPIC_DESTINATION_PREFIX = "_T";


 
  /* Instance variables */
  private String    destinationName;
  private boolean   localOnly;
  private transient SIBUuid8 meId;         // Transient because not Serializable
  private String    busName;
  /** This field indicates that the DestinationAddress is from a mediation. */


  private transient boolean temporary = false;
  private transient boolean temporarySet = false;

  /* **************************************************************************/
  /* Constructors                                                             */
  /* **************************************************************************/

  /**
   *  Constructor for a new JsDestinationAddressImpl
   *  This constructor should only be called by the JsDestinationAddressFactoryImpl
   *  create methods. A JsDestinationAddressImpl should never be instantiated directly.
   *
   *  @param destinationName  The name of the SIBus Destination
   *  @param localOnly        Indicates that the Destination should be localized
   *                          to the local Messaging Engine.
   *  @param meId             The Id of the Message Engine where the destination is localized
   *  @param busName          The name of the Bus where the destination is localized
   *  @param fromMediation    True if this is going to be used by the Mediation framework.
   */
  JsDestinationAddressImpl(String destinationName, boolean localOnly, SIBUuid8 meId, String busName) {
    this(destinationName, localOnly, meId, busName, false);
  }

  /**
   *  Constructor for a new JsDestinationAddressImpl
   *  This constructor should only be called by the JsDestinationAddressFactoryImpl
   *  create methods. A JsDestinationAddressImpl should never be instantiated directly.
   *
   *  @param destinationName  The name of the SIBus Destination
   *  @param localOnly        Indicates that the Destination should be localized
   *                          to the local Messaging Engine.
   *  @param meId             The Id of the Message Engine where the destination is localized
   *  @param busName          The name of the Bus where the destination is localized
   *  @param fromMediation    True if this is going to be used by the Mediation framework.
   */
  JsDestinationAddressImpl(String destinationName, boolean localOnly, SIBUuid8 meId, String busName, boolean fromMediation) {
    this.destinationName = destinationName;
    this.localOnly = localOnly;
    this.meId = meId;
    this.busName = busName;
  }


  /* **************************************************************************/
  /* Get methods                                                              */
  /* **************************************************************************/

  /*
   *  Determine whether the SIDestinationAddress represents a Temporary or
   *  Permanent Destination.
   *
   *  Javadoc description supplied by SIDestinationAddress interface.
   */
  public final boolean isTemporary() {
    if (!temporarySet) {
      if (  (destinationName.startsWith(TEMPORARY_QUEUE_DESTINATION_PREFIX))
         || (destinationName.startsWith(TEMPORARY_TOPIC_DESTINATION_PREFIX))
         ) {
        temporary = true;
      }
      temporarySet = true;
    }
    return temporary;
  }


  /*
   *  Get the name of the Destination represented by this SIDestinationAddress.
   *
   *  Javadoc description supplied by SIDestinationAddress interface.
   */
  public final String getDestinationName() {
    return destinationName;
  }


  /*
   *  Determine whether the LocalOnly indicator is set in the SIDestinationAddress.
   *
   *  Javadoc description supplied by JsDestinationAddress interface.
   */
  public final boolean isLocalOnly() {
    return localOnly;
  }

 

  /*
   *  Get the Id of the Message Engine where the Destination is localized.
   *
   *  Javadoc description supplied by JsDestinationAddress interface.
   */
  public final SIBUuid8 getME() {
    return meId;
  }

  /*
   *  Get the Bus name for the Destination represented by this SIDestinationAddress.
   *
   *  Javadoc description supplied by SIDestinationAddress interface.
   */
  public String getBusName() {
    return busName;
  }


  /* **************************************************************************/
  /* Set methods                                                              */
  /* **************************************************************************/

  /*
   *  Set the Id of the Message Engine where the Destination is localized.
   *  This method should only be called by the Message Processor component.
   *
   *  Javadoc description supplied by JsDestinationAddress interface.
   */
  public final void setME(SIBUuid8 meId) {
    this.meId = meId;
  }

  /*
   *  Set the name of the Bus where the Destination is localized.
   *  This method should only be called by the Message Processor component.
   *
   *  Javadoc description supplied by JsDestinationAddress interface.
   */
  public void setBusName(String busName) {
    this.busName = busName;
  }


  /* **************************************************************************/
  /* Equals and hashCode methods                                              */
  /* **************************************************************************/

  /**
   *  Override of java.lang.Object.hashCode()
   *  For description, see java.lang.Object.hashCode()
   *
   *  @return int The HashCode of ths JsDestinationAddressImpl
   */
  public final int hashCode() {
    int hash = 0;
    if (destinationName != null) {
      hash = destinationName.hashCode();
    }
    if (busName != null) {
      hash = hash + busName.hashCode();
    }
    return hash;
  }


  /**
   *  Override of java.lang.Object.equals(Object)
   *  For description, see java.lang.Object.equals(Object)
   *
   *  @return boolean true if the Object given is an instance of JsDestinationAddressImpl
   *                  with attributes equivalent to this instance.
   *                  Note that the localOnly is not checked for equality as it
   *                  is not flowed in a message.
   */
  public final boolean equals(Object obj) {

    /* Do the easy checks first */
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (obj.getClass() != this.getClass()) {
      return false;
    }

    // probably a bit of an overkill as I expect String.equals checks the hashcode first
    if (obj.hashCode() != this.hashCode()) {
      return false;
    }

    /* Now start on the content */
    final JsDestinationAddressImpl other = (JsDestinationAddressImpl)obj;

    /* Check the Destination name - need to survive if either is null */
    /* String.equals(x) copes OK if x is null.                        */
    if (  (other.getDestinationName() == destinationName)
       || (  (destinationName != null)
          && (destinationName.equals(other.getDestinationName()))
          )
       ) {
      // carry on
    }
    else {
      return false;
    }

    /* Check the Bus name - need to survive if either is null         */
    /* String.equals(x) copes OK if x is null.                        */
    if (  (other.getBusName() == busName)
       || (  (busName != null)
          && (busName.equals(other.getBusName()))
          )
       ) {
      // carry on
    }
    else {
      return false;
    }

    /* Finally the dodgiest one.... which could also be null            */
    /* SIBUuid8.equals(x) copes OK if x is null - but do we trust it?   */
    if (meId == other.getME()) {
      return true;
    }
    if (  (meId != null)
       && (meId.equals(other.getME()))
       ) {
      return true;
    }
    else {
      return false;
    }

  }

  /* **************************************************************************/
  /* toString                                                                 */
  /* **************************************************************************/

  public String toString() {
    StringBuffer buff = new StringBuffer();
    buff.append("[ " + destinationName);
    buff.append(", " + localOnly);
    buff.append(", ");
    buff.append((meId == null) ? null : meId.toString());
    buff.append(", " + busName);
    buff.append(" ]");
    return buff.toString();
  }

  /* **************************************************************************/
  /* Serialization and de-serialization methods                               */
  /* **************************************************************************/

  /**
   * The writeObject method for serialization.
   * <p>
   * writeObject calls the defaultWriteObject to serialize the existing class,
   * however the meId variable is not serialized as it is transient (only
   * because SIBUuid8 is not Serializable) so must be written explicitly.
   *
   * @param out The ObjectOutputStream used for serialization.
   */
  private void writeObject(ObjectOutputStream out) throws IOException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "writeObject");

    /* Call the default writeObject */
    out.defaultWriteObject();

    /* Get the flattened version of the ME Id field */
    byte[] id = null;
    if (meId != null) {
      id = meId.toByteArray();
    }

    /* Write it out to the ObjectOutputStream */
    out.writeObject(id);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "writeObject");
  }

  /**
   * The readObject method for serialization.
   * <p>
   * readObject calls the defaultReadObject to de-serialize the existing class,
   * however the meId variable is not recovered automatically as it is transient.
   * The meId is read explicitly from the inputStream.
   *
   * @param in The ObjectInputStream used for serialization.
   */
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "readObject");

    /* Call the default readObject */
    in.defaultReadObject();

    /* Recover the flattened version of the message */
    byte[] id = (byte [])in.readObject();
    if (id != null) {
      meId = new SIBUuid8(id);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "readObject");
  }


}
