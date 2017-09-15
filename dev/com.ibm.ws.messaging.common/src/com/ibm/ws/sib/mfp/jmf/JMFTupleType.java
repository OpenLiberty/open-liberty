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

package com.ibm.ws.sib.mfp.jmf;

/** 
 * A representation of all tuple-like types in a Jetstream schema.  These correspond to
 * EClasses in Ecore or to XSDComplexType or XSDModelGroup (all or sequence) in XSD.
 *
 * <p>A JMFTupleType is not a JMFFieldDef.  The members of the tuple have individual
 * accessors and must be referred to individually.  However, the FeatureName of a
 * JMFTupleType does become part of the pathNames used to resolve those accessors.
 */

public interface JMFTupleType extends JMFType {

  /** 
   * Get the number of fields in the tuple 
   */
  public int getFieldCount();

  /** 
   * Get the field at a particular position (the initial position is position 0) 
   */
  public JMFType getField(int position);

  /** 
   * Add a field to this tuple (a tuple need not have any fields).  
   */
  public void addField(JMFType newField);
}
