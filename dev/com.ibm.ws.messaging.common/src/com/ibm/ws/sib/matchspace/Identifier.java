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

/** This interface represents one Identifier within a Selector expression */

public interface Identifier extends Selector 
{
  /**
   * Returns the accessor.
   * @return ValueAccessor
   */
  public ValueAccessor getAccessor(); 

  /**
   * Returns the name.
   * @return String
   */
  public String getName();

  /**
   * Returns the type.
   * @return int
   */
  public int getType();

  /**
   * Sets the accessor.
   * @param accessor The accessor to set
   */
  public void setAccessor(ValueAccessor accessor); 

  /**
   * Sets the name.
   * @param name The name to set
   */
  public void setName(String name);

  /**
   * Sets the full name of an Identifier. The full name is only relevant in the XPath Selector Domain and 
   * incorporates the full path for the attribute or element referenced.
   * 
   * A full name is necessary so that we are able to distinguish between common names at the same depth in an XML
   * document.
   * 
   * @param name
   */
  public void setFullName(String name);
  
  /**
   * Return the full name of the Identifier. The full name is only relevant in the XPath Selector Domain.
   * 
   * @return
   */
  public String getFullName();
  
  /**
   * Sets the type.
   * @param type The type to set
   */
  public void setType(int type); 

  /**
   * Returns the schemaId.
   * @return long
   */
  public long getSchemaId();
  
  /**
   * Returns the ordinalPosition.
   * @return int
   */
  public Object getOrdinalPosition();
  
  /**
   * Returns the caseOf.
   * @return Identifier
   */
  public Identifier getCaseOf(); 

  /**
   * Sets the caseOf.
   * @param caseOf The caseOf to set
   */
  public void setCaseOf(Identifier caseOf);
  
  /**
   * Sets the ordinalPosition.
   * @param ordinalPosition The ordinalPosition to set
   */
  public void setOrdinalPosition(Object ordinalPosition);  
  
  /**
   * Sets the selector domain.
   * @param domain The value to set
   */
  public void setSelectorDomain(int domain); 
  
  /**
   * Returns the selector domain.
   * @return int
   */
  public int getSelectorDomain(); 
  
  /**
   * Sets a compiled expression into the Identifier.
   * @param domain The value to set
   */
  public void setCompiledExpression(Object expression);   
  /**
   * Returns the compiled expression.
   * @return int
   */
  public Object getCompiledExpression();   
  
  /**
   * Returns the location step.
   * @return int
   */
  public int getStep();  
}
