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
package com.ibm.ws.sib.matchspace;

/** Interface to be implemented by callers of the Evaluator in order to handle the
 * evaluation of Identifiers and the cacheing of partial results.
 **/

public interface MatchSpaceKey {

  /** Evaluate an Identifier
   *
   * @param id the Identifier to evaluate
   *
   * @param ignoreType if true, the identifier value should be returned if it exists,
   * regardless of the expected type recorded in id argument.  If false, the actual type
   * of value that is returned must conform to the expected type recorded in the
   * identifier (Selector.STRING, BOOLEAN, NUMERIC, or UNKNOWN).  If the value of the
   * identifier does not conform, null should be returned (as if the identifier has no
   * value).
   *
   * @param contextValue for use in XPath processing. Allows the provision of a context 
   * against which an XPath expression can be evaluated. 
   * 
   * @param returnList for use in XPath processing. Specifies whether the caller expects
   * the return to be an ArrayList of nodes or a Boolean. 
   * 
   * @return the value of the Identifier, which should be of type String, BooleanValue,
   * Integer, Long, Float, or Double, or null if the value does not exist or would not be
   * type-correct as controlled by the ignoreType argument.
   *
   * @exception BadMessageFormatMatchingException when the method is unable to determine a
   * value because the message (or other object) from which the value must be extracted is
   * corrupted or ill-formed.
   **/

  public Object getIdentifierValue(Identifier id, 
                                   boolean ignoreType,
                                   Object contextValue,
                                   boolean returnList)
    throws BadMessageFormatMatchingException;

 /** 
  * Older version of the above method, retained for compatibility.
  *
  * @param id the Identifier to evaluate
  *
  * @param ignoreType if true, the identifier value should be returned if it exists,
  * regardless of the expected type recorded in id argument.  If false, the actual type
  * of value that is returned must conform to the expected type recorded in the
  * identifier (Selector.STRING, BOOLEAN, NUMERIC, or UNKNOWN).  If the value of the
  * identifier does not conform, null should be returned (as if the identifier has no
  * value).
  *
  * @return the value of the Identifier, which should be of type String, BooleanValue,
  * Integer, Long, Float, or Double, or null if the value does not exist or would not be
  * type-correct as controlled by the ignoreType argument.
  *
  * @exception BadMessageFormatMatchingException when the method is unable to determine a
  * value because the message (or other object) from which the value must be extracted is
  * corrupted or ill-formed.
  **/

 public Object getIdentifierValue(Identifier id, 
                                  boolean ignoreType)
   throws BadMessageFormatMatchingException;
 
  /**
   * Provided for use in XPath support where the MatchSpace calls MFP
   * in order to retrieve the top most Node in a DOM tree.
   * 
   * @return a root object for use as a contextValue in getIdentifierValue() calls.
   * 
   * @exception BadMessageFormatMatchingException when the method is unable to determine a
   * value because the message (or other object) from which the value must be extracted is
   * corrupted or ill-formed.
   */
  public Object getRootContext()
    throws BadMessageFormatMatchingException;
  
  /** A vacuous MatchSpaceKey that can be used to evaluate identifier-less subtrees for
   * optimization purposes.
   **/

  public MatchSpaceKey DUMMY = new MatchSpaceKey() 
  {
      public Object getIdentifierValue(Identifier id, 
                                       boolean ignoreType,
                                       Object contextValue, 
                                       boolean returnList) 
        { return null; }

      public Object getRootContext() 
      {
        return null;
      }

      public Object getIdentifierValue(Identifier id, boolean ignoreType) 
        throws BadMessageFormatMatchingException 
        {  return null; }
  };


  
}
