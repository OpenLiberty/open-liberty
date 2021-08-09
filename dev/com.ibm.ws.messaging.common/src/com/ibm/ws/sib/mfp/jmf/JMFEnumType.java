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
 * A representation of enumerated types.  For use in tools and API mapping layers, the
 * JMFEnumType can record its associated enumerators as Strings, but most of JMF ignores
 * these and they are not propagated in schema propagations.  Rather, a JMFEnumType is
 * always encoded as a non-negative integer less than #enumerators.  The number of
 * enumerators (rather than their specific names) is propagated and is used to decide
 * whether two JMFEnumTypes are the same at runtime.
 */

public interface JMFEnumType extends JMFFieldDef {

  /** 
   * Provide the enumerators in the order of their assigned codes or null if the
   * enumerators are not locally known (never set, or not propagated).
   */
  public String[] getEnumerators();

  /** 
   * Set the enumerators in the order of their assigned codes 
   */
  public void setEnumerators(String[] val);

  /** 
   * Get the enumerator count.  This is always available, even if the enumerator 
   * strings are not
   */
  public int getEnumeratorCount();

  /** 
   * Set the enumerator count without setting the enumerators explicitly.  Sets
   * enumerators to null as a side-effect.
   */
  public void setEnumeratorCount(int count);
}
