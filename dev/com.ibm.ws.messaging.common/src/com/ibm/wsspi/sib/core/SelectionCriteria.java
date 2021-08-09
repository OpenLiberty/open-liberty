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
package com.ibm.wsspi.sib.core;

  /**
   A SelectionCriteria object can be used when creating a ConsumerSession or
   BrowserSession, to support durable subscription creation or when using the 
   receive methods of SICoreConnection, to indicate that messages are to be 
   selected according a message properties selector expression and/or a 
   discriminator string. 
   <p> 
   A SelectionCriteria object is created by calling 
   SelectionCriteriaFactory.createMessageSelector. (Note that only SelectionCriteria 
   objects returned from SelectionCriteriaFactory.createMessageSelector may be used; 
   the Core SPI user cannot provide their own SelectionCriteria implementation.)  
   <p>
   Property naming is not identical between the JMS API and the SIMessage API, 
   briefly:
   <ul>
   <li> JMS supports property names of JMSxxxxx or xxxxxx (where xxxxxx are any 
   user property)
   <li>Core SPI supports names of SI_xxxxx, JMSxxxxx or user.xxxxxx
   </ul>
   The SelectionCriteria encapsulates the string selector expression and a 
   SelectorDomain attribute to indicate in which messaging context the selector
   was provided. The SelectorDomain class is also provided on the Core SPI. In
   addition the SelectionCriteria encapsulates a discriminator string. 
  <p>
  This class has no security implications.
   */
  public interface SelectionCriteria 
  {

  	/**
  	 * Returns the discriminator used in matching. The Javadoc overview document 
  	 * for this package describes the syntax and interpretation of discriminators
  	 * in the Core SPI.
  	 *   
  	 * @return the discriminator
  	 */
	public String getDiscriminator();
	
	/**
	 * Setsthe discriminator used in matching. The Javadoc overview document 
	 * for this package describes the syntax and interpretation of discriminators
	 * in the Core SPI.
	 *   
	 * @param discriminator
	 */
	public void setDiscriminator(String discriminator);
	
	/**
	 * Returns the selector string used in matching. The selector is interpreted
	 * according to the value of the SelectorDomain property. Selectors are 
	 * described in the Javadoc overview document for this package.
	 *   
	 * @return the selector string
	 */
	public String getSelectorString();
	
	/**
	 * Sets the selector string used in matching. The selector is interpreted
	 * according to the value of the SelectorDomain property. Selectors are 
	 * described in the Javadoc overview document for this package.
	 *   
	 * @param selectorString
	 */
	public void setSelectorString(String selectorString);
	
	/**
	 * Returns the selector domain that will be used to interpret the selector
	 * string. Selectors are described in the Javadoc overview document for this 
	 * package.
	 *   
	 * @return the selector domain
	 */
	public SelectorDomain getSelectorDomain();
	
	/**
	 * Sets the selector domain that will be used to interpret the selector
	 * string. Selectors are described in the Javadoc overview document for this 
	 * package.
	 *   
	 * @param selectorDomain
	 */
	public void setSelectorDomain(SelectorDomain selectorDomain);
	
  }
