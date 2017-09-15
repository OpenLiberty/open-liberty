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
package com.ibm.ws.sib.processor;

import com.ibm.wsspi.sib.core.SelectorDomain;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException;
import com.ibm.ws.sib.matchspace.Selector;

  /**
   A SelectorEvaluator is used to parse selector expressions and evaluate them against
   an SIBusMessage.
   */
  public interface MPSelectorEvaluator 
  {

    /**
     * Method parseSelector
     * Used to parse the string representation of a selector into a MatchSpace selector tree.
     * @param selectorString
     * @param domain
     */

    public Selector parseSelector(String selectorString,
                                  SelectorDomain domain)
      throws SISelectorSyntaxException;
      
    /**
     * Method parseDiscriminator
     * Used to parse the string representation of a discriminator into a MatchSpace selector tree.
     * @param discriminator
     */      
    public Selector parseDiscriminator(String discriminator)
      throws SIDiscriminatorSyntaxException;
            
    /**
     * Method evaluateMessage
     * Used to evaluate a parsed selector expression against a message.
     * @param selectorTree
     * @param msg
     */

    public boolean evaluateMessage(
      Selector selectorTree,
      Selector discriminatorTree,
      SIBusMessage msg);
      
    /**
     * Method evaluateDiscriminator
     * 
     * Used to determine whether a supplied fully qualified discriminator matches
     * a supplied wildcarded discriminator expression.
     * 
     * @param fullTopic
     * @param wildcardTopic
     * @return
     * @throws SIDiscriminatorSyntaxException
     */
    public boolean evaluateDiscriminator(
      String fullTopic,
      String wildcardTopic)
    throws SIDiscriminatorSyntaxException;          
  }
