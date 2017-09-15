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
package com.ibm.ws.sib.processor.matching;

import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.ws.sib.processor.MPSelectorEvaluator;
import com.ibm.wsspi.sib.core.SelectorDomain;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException;
import com.ibm.ws.sib.matchspace.Selector;

/**
 * This class conatains the implementation of the MPSelectorEvaluator from sib.processor.
 */
public class MPSelectorEvaluatorImpl implements MPSelectorEvaluator
{
  //trace
  private static final TraceComponent tc =
    SibTr.register(
      MPSelectorEvaluatorImpl.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
   
  private MessageProcessorMatching mpm = null;

  /**
   * Create a new MPSelectorEvaluator
   */
  public MPSelectorEvaluatorImpl()
  { 
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, "MPSelectorEvaluatorImpl");       
    
    mpm = new MessageProcessorMatching();
 
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "MPSelectorEvaluatorImpl", this);
  }
  
  /**
   * Method parseSelector
   * Used to parse the string representation of a selector into a MatchSpace selector tree.
   * @param selector
   */

  public Selector parseSelector(String selectorString,
                                SelectorDomain domain) throws SISelectorSyntaxException
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "parseSelector", new Object[] { selectorString, domain });
    
    Selector selectorTree = mpm.parseSelector(selectorString, domain);
       
    if (tc.isEntryEnabled()) SibTr.exit(tc, "parseSelector", selectorTree);
    return selectorTree;       
  }

  public Selector parseDiscriminator(String discriminator) throws SIDiscriminatorSyntaxException
  {
    if (tc.isEntryEnabled()) SibTr.entry(tc, "parseDiscriminator", discriminator);
    
    Selector discriminatorTree = mpm.parseDiscriminator(discriminator);
    
    if (tc.isEntryEnabled()) SibTr.exit(tc, "parseDiscriminator", discriminatorTree);

    return discriminatorTree;
  }
  
  /**
   * Method evaluateMessage
   * Used to evaluate a parsed selector expression against a message.
   * @param selectorTree
   * @param msg
   */

  public boolean evaluateMessage(
    Selector selectorTree,
    Selector discriminatorTree,
    SIBusMessage msg)
  { 
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "evaluateMessage", new Object[] { selectorTree, discriminatorTree, msg });

    boolean ret = mpm.evaluateMessage(selectorTree, discriminatorTree, msg );
          
    if (tc.isEntryEnabled()) SibTr.exit(tc, "evaluateMessage", new Boolean(ret));
    return ret;             
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.MPSelectorEvaluator#evaluateDiscriminator(java.lang.String, java.lang.String)
   */
  public boolean evaluateDiscriminator(String fullTopic, String wildcardTopic) throws SIDiscriminatorSyntaxException
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "evaluateDiscriminator", new Object[] { fullTopic, wildcardTopic });

    boolean ret = mpm.evaluateDiscriminator(fullTopic, wildcardTopic);
          
    if (tc.isEntryEnabled()) SibTr.exit(tc, "evaluateDiscriminator", new Boolean(ret));
    return ret;       
  }
}
