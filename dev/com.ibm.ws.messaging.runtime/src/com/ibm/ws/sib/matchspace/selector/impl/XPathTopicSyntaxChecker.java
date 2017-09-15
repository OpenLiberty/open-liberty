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

public class XPathTopicSyntaxChecker implements TopicSyntaxChecker
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
	    boolean acceptSeparator = true;
	    // becomes false when hash seen, stays false thereafter
	    boolean acceptDot = false;
	    // becomes false when hash seen, stays false thereafter    
	    boolean acceptStar = true;
	    // becomes false on non-separator, true on separator
	    boolean acceptOrdinary = true;
	    // becomes false on wildcard, true on separator
	    boolean prevSeparator = false;
	    // becomes true on separator    
	    boolean hasWild = false;
	    for (int i = 0; i < chars.length; i++)
	    {
	      char cand = chars[i];
	      if (cand == MatchSpace.SUBTOPIC_MATCHONE_CHAR)
	      {
	        if (acceptStar)
	        {
	          hasWild = true;
	          acceptStar = acceptOrdinary = prevSeparator = acceptDot = false;
	          acceptSeparator = true;
	        }
	        else
	          throw new InvalidTopicSyntaxException(
	            NLS.format(
	              "INVALID_TOPIC_ERROR_CWSIH0001",
	               new Object[] { topic, new Integer(i+1) }));
	      }
	      else if (cand == MatchSpace.SUBTOPIC_STOP_CHAR)
	      {
	        if (acceptDot)
	        {
	          if(prevSeparator)
	          {
	            acceptOrdinary = false;
	          }
	          else
	          {
	            acceptOrdinary = true;
	          }
	          acceptStar = prevSeparator = acceptDot = false;
	          acceptSeparator = true;
	        }
	        else
	          throw new InvalidTopicSyntaxException(
	            NLS.format(
	              "INVALID_TOPIC_ERROR_CWSIH0002",
	               new Object[] { topic, new Integer(i+1) }));
	      }
	      else if (cand == MatchSpace.SUBTOPIC_SEPARATOR_CHAR)
	      {
	        if (acceptSeparator)
	        {
	          // A separator is disallowed at the end of a publication
	          if(i == chars.length - 1)
	          {
	            throw new InvalidTopicSyntaxException(
	                NLS.format(
	                  "INVALID_TOPIC_ERROR_CWSIH0003",
	                  new Object[] { topic, new Integer(i+1) }));           
	          }
	          
	          if(prevSeparator)
	          {
	            // We've found a '//' 
	            acceptSeparator = false;
	            hasWild = true;
	            acceptDot = true;
	          }
	          else // First separator
	          {
	            acceptStar = acceptOrdinary = prevSeparator = true;
	            acceptDot = true;
	          }
	        }
	        else
	          // Three separators together
	          throw new InvalidTopicSyntaxException(
	            NLS.format(
	              "INVALID_TOPIC_ERROR_CWSIH0003",
	              new Object[] { topic, new Integer(i+1) }));
	      }
	      else if( cand == ':')
	      {
	        // :'s are disallowed in topic name parts
	        throw new InvalidTopicSyntaxException(
	           NLS.format(
	             "TEMPORARY_CWSIH9999",
	             new Object[] { " ':' characters are not allowed in topics. A ':' was found at character " + (i+1) }));
	      }      
	      else // Ordinary character
	      {
	        if (!acceptOrdinary)
	           throw new InvalidTopicSyntaxException(
	             NLS.format(
	               "INVALID_TOPIC_ERROR_CWSIH0004",
	               new Object[] { topic, new Integer(i+1) }));
	        else
	        {
	          acceptStar = prevSeparator = false;
	          acceptSeparator = acceptDot = true;
	        }
	      }
	    }
	    return hasWild;
	  }
	  
	  /**
	   * null topic check
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
	   * contain wildcard characters, the MatchOne and MatchMany
	   * 
	   * @param topic  The topic to be checked
	   *
	   * @throws InvalidTopicSyntaxException if topic is syntactically invalid
	   */
	  public void checkEventTopicSyntax(String topic)
	  throws InvalidTopicSyntaxException
	  {
	    checkTopicNotNull(topic);
	        
	    char[] chars = topic.toCharArray();

	    // becomes false when dot seen, stays false thereafter
	    boolean acceptDot = false;
	    // becomes false when hash seen, stays false thereafter    
	    boolean acceptOrdinary = true;
	    // becomes false on wildcard, true on separator
	    boolean prevSeparator = false;
	    // becomes true on separator    

	    for (int i = 0; i < chars.length; i++)
	    {
	      char cand = chars[i];
	      if (cand == MatchSpace.SUBTOPIC_MATCHONE_CHAR)
	      {
	        throw new InvalidTopicSyntaxException(
	          NLS.format(
	            "INVALID_TOPIC_ERROR_CWSIH0006",
	            new Object[] { topic }));
	      }
	      else if (cand == MatchSpace.SUBTOPIC_STOP_CHAR)
	      {
	        if (acceptDot)
	        {
	          prevSeparator = acceptDot = false;
	          acceptOrdinary = true;
	        }
	        else
	          throw new InvalidTopicSyntaxException(
	            NLS.format(
	              "INVALID_TOPIC_ERROR_CWSIH0006",
	              new Object[] { topic }));
	      }
	      else if (cand == MatchSpace.SUBTOPIC_SEPARATOR_CHAR)
	      {
	          // A separator is disallowed at the end of a publication
	          if(i == chars.length - 1)
	          {
	            throw new InvalidTopicSyntaxException(
	                NLS.format(
	                  "INVALID_TOPIC_ERROR_CWSIH0006",
	                  new Object[] { topic }));           
	          }
	          if(prevSeparator)
	          {
	            // We've found a '//' this is disallowed in a publication
	            // discriminator string
	            throw new InvalidTopicSyntaxException(
	              NLS.format(
	                "INVALID_TOPIC_ERROR_CWSIH0006",
	                new Object[] { topic }));
	          }
	          
	          // First separator          
	          acceptOrdinary = prevSeparator = true;
	          acceptDot = false;
	          
	      }
	      else if( cand == ':')
	      {
	        // :'s are disallowed in topic name parts
	        throw new InvalidTopicSyntaxException(
	          NLS.format(
	            "INVALID_TOPIC_ERROR_CWSIH0006",
	            new Object[] { topic }));
	      }      
	      else // Ordinary character
	      {
	        if (!acceptOrdinary)
	          throw new InvalidTopicSyntaxException(
	            NLS.format(
	              "INVALID_TOPIC_ERROR_CWSIH0006",
	              new Object[] { topic }));
	        
	        prevSeparator = false;
	        acceptDot = true;
	      }
	    }          
	  }

}
