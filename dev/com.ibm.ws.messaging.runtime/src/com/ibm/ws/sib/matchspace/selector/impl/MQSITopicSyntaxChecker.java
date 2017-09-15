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

package com.ibm.ws.sib.matchspace.selector.impl;

import com.ibm.ws.sib.matchspace.MatchSpace;
import com.ibm.ws.sib.matchspace.InvalidTopicSyntaxException;
import com.ibm.ws.sib.matchspace.tools.TopicSyntaxChecker;
import com.ibm.ws.sib.matchspace.utils.NLS;

public class MQSITopicSyntaxChecker implements TopicSyntaxChecker
{
  	
  /** checkTopicSyntax: Rules out syntactically inappropriate wildcard usages and
   * determines if there are any wildcards
   * @param topic the topic to check
   * @return true if topic contains wildcards 
   * @throws InvalidTopicSyntaxException if topic is syntactically invalid
   */
  public boolean checkTopicSyntax(String topic)
    throws InvalidTopicSyntaxException
  {
    checkTopicNotNull(topic);
		    
    char[] chars = topic.toCharArray();
    boolean acceptHash = true;
    // becomes false when hash seen, stays false thereafter
    boolean acceptWild = true;
    // becomes false on non-separator, true on separator
    boolean acceptOrdinary = true;
    // becomes false on wildcard, true on separator
    boolean hasWild = false;

    for (int i = 0; i < chars.length; i++)
    {
      char cand = chars[i];
      if (cand == MatchSpace.SUBTOPIC_MQSI_MATCHONE_CHAR)
        if (acceptWild)
        {
          hasWild = true;
          acceptWild = acceptOrdinary = false;
        }
        else
          throw new InvalidTopicSyntaxException(
            NLS.format(
              "INVALID_MQSI_TOPIC_ERROR_CWSIH0008",
              new Object[] { topic, new Integer(i+1) }));      
      else
        if (cand == MatchSpace.SUBTOPIC_MQSI_MATCHMANY_CHAR)
          if (acceptWild && acceptHash)
          {
            hasWild = true;
            acceptWild = acceptHash = acceptOrdinary = false;
          }
          else
            throw new InvalidTopicSyntaxException(
              NLS.format(
                "INVALID_MQSI_TOPIC_ERROR_CWSIH0009",
                new Object[] { topic }));
        else
          if (cand == MatchSpace.SUBTOPIC_SEPARATOR_CHAR)
            if (acceptWild && i > 0) // Allow an initial separator
              // Two separators together
              throw new InvalidTopicSyntaxException(
                NLS.format(
                  "INVALID_MQSI_TOPIC_ERROR_CWSIH0010",
                  new Object[] { topic }));
            else
            {
              acceptWild = true;
              acceptOrdinary = true;
            }
            else
              if (!acceptOrdinary)
                throw new InvalidTopicSyntaxException(
                  NLS.format(
                    "INVALID_MQSI_TOPIC_ERROR_CWSIH0011",
                    new Object[] { topic }));
              else
                acceptWild = false;
    }
    return hasWild;
  }
	  
  /**
   *   null topic check
   * 
   * @param topic  The topic to be checked.
   * 
   * @throws InvalidTopicSyntaxException if the topic is null
   */
  private final void checkTopicNotNull(String topic) 
    throws InvalidTopicSyntaxException
  {
    if (topic == null)
      throw new InvalidTopicSyntaxException(
        NLS.format(
          "INVALID_TOPIC_ERROR_CWSIH0005",
          new Object[] { topic }));
  }
	  
  /**Checks the topic for any wildcards as a Event topic can not
   *  contain wildcard characters, the MatchOne and MatchMany
   *
   * @param topic  The topic to be checked
   *
   * @throws InvalidTopicSyntaxException if topic is syntactically invalid
   */
  public void checkEventTopicSyntax(String topic)
    throws InvalidTopicSyntaxException
  {
    checkTopicNotNull(topic);
		    
    if (topic.indexOf(MatchSpace.SUBTOPIC_MQSI_MATCHONE_CHAR) > -1 || 
      topic.indexOf(MatchSpace.SUBTOPIC_MQSI_MATCHMANY_CHAR) > -1)
      throw new InvalidTopicSyntaxException(
         NLS.format(
           "INVALID_TOPIC_ERROR_CWSIH0006",
           new Object[] { topic }));
  }
}
