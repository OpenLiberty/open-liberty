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
 * A representation of all repeated types in a Jetstream schema.  These correspond to
 * EStructuralFeature nodes with isMany()==true in Ecore or to XSDParticle nodes with
 * maxOccurs>1 in XSD.
 *
 * <p>JMF records minOccurs and maxOccurs information in a JMFRepeatedType but does not
 * actually use that information (currently) to enforce any limits at runtime nor does it
 * exploit it for optimization purposes.  Those are future objectives.  The JSParser
 * notation does not (currently) provide any way to specify this information but
 * translations from Ecore or XSD preserve the information.
 *
 * <p>A JMFRepeatedType is not a JMFFieldDef. It simply causes the JMFFieldDefs under it
 * to repeat.  The FeatureName of a JMFRepeatedType never becomes part of any pathName and
 * is used (if specified at all) solely for documentation.
 */

public interface JMFRepeatedType extends JMFType {

  /** 
   * Gets the minimum occurance count 
   */
  public int getMinOccurs();

  /** 
   * Gets the maximum occurance count (-1 indicates "unbounded") 
   */
  public int getMaxOccurs();

  /** 
   * Get the JMFType that is repeated by this JMFRepeatedType.  This may be any JMFType,
   * including a JMFTupleType, JMFRepeatedType, or JMFVariantType: these can be used to
   * express complex content models.
   */
  public JMFType getItemType();

  /** 
   * Set the bounds (default is {0, unbounded}).  Use maxOccurs=-1 to indicate "unbounded."
   */
  public void setBounds(int minOccurs, int maxOccurs);

  /** 
   * Set the ItemType 
   */
  public void setItemType(JMFType item);
}
