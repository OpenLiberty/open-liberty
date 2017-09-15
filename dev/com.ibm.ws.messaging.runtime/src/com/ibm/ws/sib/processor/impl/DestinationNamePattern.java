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
package com.ibm.ws.sib.processor.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.matchspace.Selector;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.matching.MessageProcessorMatching;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;

public class DestinationNamePattern
{
  private static final TraceComponent tc =
    SibTr.register(DestinationNamePattern.class, SIMPConstants.MP_TRACE_GROUP, SIMPConstants.RESOURCE_BUNDLE);
  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
  
  private String destinationNamePatternString;
  private Selector parsedPattern;
  private String wildcardStem = null;
  private boolean patternIsWildcarded = false;
  
  /** Back reference to the messageprocessor */
  private MessageProcessorMatching mpm;
  /**
   * Constructor
   * 
   * @param destinationNamePatternString
   * @param connection
   */
  protected DestinationNamePattern(String destinationNamePatternString,
                                   MessageProcessorMatching messageProcessorMatching)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "DestinationNamePattern",
        new Object[] { destinationNamePatternString, messageProcessorMatching });
    
    // Trim the pattern
    destinationNamePatternString = destinationNamePatternString.trim();
    this.destinationNamePatternString = destinationNamePatternString;

    this.mpm = messageProcessorMatching;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "DestinationNamePattern", this);
  }
  
  /**
   * If a pattern is wildcarded we csn parse it up front.
   * 
   * @throws SIDiscriminatorSyntaxException 
   */
  public void prepare() throws SIDiscriminatorSyntaxException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "prepare");

    // The user has specified a pattern to match against, check whether it is
    // wildcarded
    patternIsWildcarded = mpm.isWildCarded(destinationNamePatternString);
 
    // If the pattern is wildcarded then we can do some work up front.
    if(patternIsWildcarded)
    {
      // Retrieve the non-wildcarded stem
      wildcardStem = mpm.retrieveNonWildcardStem(destinationNamePatternString);
        
      // Validate and parse the pattern up front.
      parsedPattern = mpm.parseDiscriminator(destinationNamePatternString);
    } 

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "prepare");
  }
  
  /**
   * Match a string against the pattern represented by this object.
   */
  public boolean match(String destinationName)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "match", destinationName);

    boolean matches = false;
    if(!patternIsWildcarded)
    {
      // Match the name directly through String equality
      matches = destinationName.equals(destinationNamePatternString);
    }
    else
    {
      // We need to match a wildcarded expression
      if(destinationName.startsWith(wildcardStem))
      {
        //A candidate for full matching as the stem is the same              
        try
        {
          matches = mpm.evaluateDiscriminator(destinationName, 
                                              destinationNamePatternString,
                                              parsedPattern);
        } 
        // Any discriminator exceptions should have been caught at the prepare()
        // stage, so ffdc, etc.
        catch (SIDiscriminatorSyntaxException e)
        {
          // FFDC
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.DestinationNamePattern.match",
            "1:141:1.2",
            this);

          SibTr.exception(tc, e);
          
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "match", "SIErrorException");
          
          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.DestinationNamePattern",
              "1:152:1.2",
              e });
          
          throw new SIErrorException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0002",
              new Object[] {
                "com.ibm.ws.sib.processor.impl.DestinationNamePattern",
                "1:160:1.2",
                e },
              null),
            e);
        }   
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "match", Boolean.valueOf(matches));
    return matches;
  }  
}
