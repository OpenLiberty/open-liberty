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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;

/**
 * This class manages the set of MonitoredConsumers (subscriptions) that have been registered
 * with the system. 
 * 
 * The class was provided to support WSN Registered publishers.
 */
public class SubscriptionRegistrar
{
  private static final TraceComponent tc =
    SibTr.register(
      SubscriptionRegistrar.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
    
  // Categories of subscriptions
  private Map _exactNonSelectorSubs = new HashMap();
  private Map _wildcardNonSelectorSubs = new HashMap();
  private Map _exactSelectorSubs = new HashMap();
  private Map _wildcardSelectorSubs = new HashMap();

  // Convenience flags
  private boolean _areExactNonSelectorSubs = false;
  private boolean _areWildcardNonSelectorSubs = false;
  private boolean _areExactSelectorSubs = false;
  private boolean _areWildcardSelectorSubs = false;    

  // Back reference to mpm 
  private MessageProcessorMatching _mpm;

  //------------------------------------------------------------------------------
  // Constructors for ConsumerMonitorRegistrar
  //------------------------------------------------------------------------------
  
  public SubscriptionRegistrar(MessageProcessorMatching mpm)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "SubscriptionRegistrar", mpm);
    // Set ref to mpm
    _mpm = mpm;
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "SubscriptionRegistrar", this);
  }

  /**
   * Method isKnownNonSelectorExpression
   * 
   * Checks whether a consumer without a selector has already been registered on the 
   * specified topicexpression
   * 
   * @param topicExpression
   * @param isWildcarded
   * @return
   */
  public boolean isKnownNonSelectorExpression(
    String topicExpression,
    boolean isWildcarded)
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, 
                  "isKnownNonSelectorExpression", 
                  new Object[]{topicExpression,
                               new Boolean(isWildcarded)});

    boolean isCategorised = false;
    
    // No Selector expression
    if(isWildcarded)
    {
      // No selector and wildcarded
      isCategorised = _wildcardNonSelectorSubs.containsKey(topicExpression);
    }
    else
    {
      // No selector and not wildcarded
      isCategorised = _exactNonSelectorSubs.containsKey(topicExpression);
    }      
   
    if (tc.isEntryEnabled()) SibTr.exit(tc, "isKnownNonSelectorExpression", new Boolean(isCategorised));
    return isCategorised;
  }

  /**
   * Method isKnownSelectorExpression
   * 
   * Checks whether a consumer with a selector has already been registered on the 
   * specified topicexpression
   * 
   * @param topicExpression
   * @param isWildcarded
   * @return
   */  
  public boolean isKnownSelectorExpression(
    String topicExpression,
    boolean isWildcarded)
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, 
                  "isKnownSelectorExpression", 
                  new Object[]{topicExpression,
                               new Boolean(isWildcarded)});

    boolean isCategorised = false;

    if(isWildcarded)
    {
      // Test selector and wildcarded
      isCategorised = _wildcardSelectorSubs.containsKey(topicExpression);
    }
    else
    {
      // Selector but not wildcarded
      isCategorised = _exactSelectorSubs.containsKey(topicExpression);
    }
    
    if (tc.isEntryEnabled()) SibTr.exit(tc, "isKnownSelectorExpression", new Boolean(isCategorised));
    return isCategorised;
  }
    
  /**
   * Method addNewConsumerForExpression
   * 
   * Categorises a consumer and adds it to the appropriate hashtable.
   * 
   * @param topicExpression
   * @param mc
   * @param selector
   * @param isWildcarded
   */
  public void addNewConsumerForExpression(
    String topicExpression,
    MonitoredConsumer mc,
    boolean selector,
    boolean isWildcarded)
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, 
                  "addNewConsumerForExpression", 
                  new Object[]{topicExpression, 
                               mc,
                               new Boolean(selector),
                               new Boolean(isWildcarded)});

    ArrayList consumerList = new ArrayList(1);
      
    // Add new consumer into list      
    consumerList.add(mc);
        
    if(selector)
    {
      if(isWildcarded)
      {
        // Selector and wildcarded
        _wildcardSelectorSubs.put(topicExpression,consumerList);
        _areWildcardSelectorSubs = true;
      }
      else
      {
        // Selector but not wildcarded
        _exactSelectorSubs.put(topicExpression,consumerList);
        _areExactSelectorSubs = true;
      }
    }
    else
    {
      // No Selector expression
      if(isWildcarded)
      {
        // No selector and wildcarded
        _wildcardNonSelectorSubs.put(topicExpression,consumerList);
        _areWildcardNonSelectorSubs = true;        
      }
      else
      {
        // No selector and not wildcarded
        _exactNonSelectorSubs.put(topicExpression,consumerList);
        _areExactNonSelectorSubs = true;        
      }      
    }           
    if (tc.isEntryEnabled()) SibTr.exit(tc, "addNewConsumerForExpression");
  }

  /**
   * Method removeConsumerListForExpression
   * 
   * Removes the row from the appropriate table after the last consumer for
   * the topic expression has been removed.
   * 
   * @param topicExpression
   * @param selector
   * @param isWildcarded
   */
  public void removeConsumerListForExpression(
    String topicExpression,
    boolean selector,
    boolean isWildcarded)
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, 
                  "removeConsumerListForExpression", 
                  new Object[]{topicExpression, 
                               new Boolean(selector),
                               new Boolean(isWildcarded)});

    if(selector)
    {
      if(isWildcarded)
      {
        // Selector and wildcarded
        ArrayList consumerList = (ArrayList)_wildcardSelectorSubs.remove(topicExpression);
        if(consumerList == null)
        {
          // Couldn't find the consumerlist. This means we have an internal 
          // inconsistency in our tables, so throw an SIErrorException
          
          // Build the message for the Exception
          if (tc.isEntryEnabled()) 
            SibTr.exit(tc, "removeConsumerListForExpression", topicExpression);
      
          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0005",
            new Object[] {
              "com.ibm.ws.sib.processor.matching.SubscriptionRegistrar",
              "1:265:1.5",
              topicExpression });  
          throw new SIErrorException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0005",
              new Object[] {
                "com.ibm.ws.sib.processor.matching.SubscriptionRegistrar",
                "1:272:1.5",
                topicExpression },
              null));                       
        }                          
        // Test whether table is now empty
        if(_wildcardSelectorSubs.isEmpty())
          _areWildcardSelectorSubs = false;
      }
      else
      {
        // Selector but not wildcarded
        ArrayList consumerList = (ArrayList)_exactSelectorSubs.remove(topicExpression);
        if(consumerList == null)
        {
          // Couldn't find the consumerlist. This means we have an internal 
          // inconsistency in our tables, so throw an SIErrorException
        
          // Build the message for the Exception
          if (tc.isEntryEnabled()) 
            SibTr.exit(tc, "removeConsumerListForExpression", topicExpression);
      
          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0005",
            new Object[] {
              "com.ibm.ws.sib.processor.matching.SubscriptionRegistrar",
              "1:296:1.5",
              topicExpression });  
          throw new SIErrorException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0005",
              new Object[] {
                "com.ibm.ws.sib.processor.matching.SubscriptionRegistrar",
                "1:303:1.5",
                topicExpression },
              null));                       
        }                  
        // Test whether table is now empty
        if(_exactSelectorSubs.isEmpty())
          _areExactSelectorSubs = false;
      }
    }
    else
    {
      // No Selector expression
      if(isWildcarded)
      {
        // No selector and wildcarded
        ArrayList consumerList = (ArrayList)_wildcardNonSelectorSubs.remove(topicExpression);
        if(consumerList == null)
        {
          // Couldn't find the consumerlist. This means we have an internal 
          // inconsistency in our tables, so throw an SIErrorException
        
          // Build the message for the Exception
          if (tc.isEntryEnabled()) 
            SibTr.exit(tc, "removeConsumerListForExpression", topicExpression);
      
          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0005",
            new Object[] {
              "com.ibm.ws.sib.processor.matching.SubscriptionRegistrar",
              "1:331:1.5",
              topicExpression });  
          throw new SIErrorException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0005",
              new Object[] {
                "com.ibm.ws.sib.processor.matching.SubscriptionRegistrar",
                "1:338:1.5",
                topicExpression },
              null));                       
        }                       
        // Test whether table is now empty
        if(_wildcardNonSelectorSubs.isEmpty())
          _areWildcardNonSelectorSubs = false;        
      }
      else
      {
        // No selector and not wildcarded
        ArrayList consumerList = (ArrayList)_exactNonSelectorSubs.remove(topicExpression);
        if(consumerList == null)
        {
          // Couldn't find the consumerlist. This means we have an internal 
          // inconsistency in our tables, so throw an SIErrorException
        
          // Build the message for the Exception
          if (tc.isEntryEnabled()) 
            SibTr.exit(tc, "removeConsumerListForExpression", topicExpression);
      
          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0005",
            new Object[] {
              "com.ibm.ws.sib.processor.matching.SubscriptionRegistrar",
              "1:362:1.5",
              topicExpression });  
          throw new SIErrorException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0005",
              new Object[] {
                "com.ibm.ws.sib.processor.matching.SubscriptionRegistrar",
                "1:369:1.5",
                topicExpression },
              null));                       
        }                            
        // Test whether table is now empty
        if(_exactNonSelectorSubs.isEmpty())
          _areExactNonSelectorSubs = false;        
      }      
    }           
    if (tc.isEntryEnabled()) SibTr.exit(tc, "removeConsumerListForExpression");
  }
  
  /**
   * Method getConsumerListForExpression
   * 
   * Locates the list of consumers associated with a specified topic expression.
   * 
   * @param topicExpression
   * @param selector
   * @param isWildcarded
   * @return
   */
  public ArrayList getConsumerListForExpression(
    String topicExpression,
    boolean selector,
    boolean isWildcarded)
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, 
                  "getConsumerListForExpression", 
                  new Object[]{topicExpression,
                               new Boolean(selector),
                               new Boolean(isWildcarded)});

    ArrayList consumerList = null;
    if(selector)
    {
      if(isWildcarded)
      {
        // Selector and wildcarded
        consumerList = (ArrayList)_wildcardSelectorSubs.get(topicExpression);
      }
      else
      {
        // Selector but not wildcarded
        consumerList = (ArrayList)_exactSelectorSubs.get(topicExpression);
      }
    }
    else
    {
      // No Selector expression
      if(isWildcarded)
      {
        // No selector and wildcarded
        consumerList = (ArrayList)_wildcardNonSelectorSubs.get(topicExpression);
      }
      else
      {
        // No selector and not wildcarded
        consumerList = (ArrayList)_exactNonSelectorSubs.get(topicExpression);
      }      
    } 
    if (tc.isEntryEnabled()) SibTr.exit(tc, "getConsumerListForExpression", consumerList);
    return consumerList;
  }

  /**
   * Method findMatchingSelectorSubs
   * 
   * Used to determine whether there are any subscriptions with selectors
   * that might match the specified topic expression.
   * 
   * @param theTopic
   * @param consumerSet
   * @throws SIDiscriminatorSyntaxException
   */
  public void findMatchingSelectorSubs(String theTopic,
                                       Set consumerSet)
  throws SIDiscriminatorSyntaxException                                      
  {
    if (tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "findMatchingSelectorSubs",
          new Object[] { theTopic}); 
    // We can string match against consumers with fully qualified discriminators 
    findMatchingExactSelectorSubs(theTopic, consumerSet);

    // Use MatchSpace direct evaluation code once we've isolated candidate
    // expressions through string matching
    if(_areWildcardSelectorSubs)
    {
      // Iterate through the map
      Iterator i = _wildcardSelectorSubs.keySet().iterator();
      while (i.hasNext())
      {
        String consumerTopic = (String)i.next();     
        // Retrieve the non-wildcarded stem
        String stem = _mpm.retrieveNonWildcardStem(consumerTopic);
              
        if (tc.isDebugEnabled()) 
          SibTr.debug(tc, "Found consumer topic: " + consumerTopic + "with stem: " + stem);
        
        // Add candidates to the list
        if(theTopic.startsWith(stem))
        {
          if (tc.isDebugEnabled()) 
            SibTr.debug(tc, "Drive direct evaluation for topic: " + consumerTopic);
       
          if(_mpm.evaluateDiscriminator(theTopic,consumerTopic))
          {
            ArrayList consumerList = (ArrayList)_wildcardSelectorSubs.get(consumerTopic);
            if (tc.isDebugEnabled())
              SibTr.debug(tc, "Add members of list to set");
            consumerSet.addAll(consumerList); 
          }    
        }
      }                                 
    }
   
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "findMatchingSelectorSubs");
  }

  /**
   * Method findMatchingExactSelectorSubs
   * 
   * Used to determine whether there are any subscriptions on fully qualified topic
   * expressions and with selectors that might match the specified topic expression.
   * 
   * For this category of subscriptions we can use string matching.
   * 
   * @param topic
   * @param consumerSet
   */
  private void findMatchingExactSelectorSubs(String topic,
                                           Set consumerSet)
  {
    if (tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "findMatchingExactSelectorSubs",
          new Object[] { topic}); 
    if(_areExactSelectorSubs)
    {
      // Iterate through the map
      Iterator i = _exactSelectorSubs.keySet().iterator();
      while (i.hasNext())
      {
        String consumerTopic = (String)i.next();     
      
        if (tc.isDebugEnabled()) 
          SibTr.debug(tc, "Found consumer topic: " + consumerTopic);
        
        // Add the list of consumers to the matching Set if it matches
        if(topic.equals(consumerTopic))
        {
          ArrayList consumerList = (ArrayList)_exactSelectorSubs.get(topic);
          if (tc.isDebugEnabled())
            SibTr.debug(tc, "Add members of list to set");
          consumerSet.addAll(consumerList); 
        }
      }                                 
    }
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "findMatchingExactSelectorSubs");
  }
  

  /**
   * Method findCandidateSubsForWildcardExpr
   * 
   * Used where a new consumer set monitor is being added on a wildcarded expression.
   * Each category of subscription is checked to determine potential matches. The style of
   * matching might be through direct MatchSpace evaluation, if possible, or by string
   * matching.
   * 
   * @param topicExpression
   * @param topicStem
   * @param consumerSet
   * @throws SIDiscriminatorSyntaxException
   */
  public void findCandidateSubsForWildcardExpr(String topicExpression,
                                      String topicStem,
                                      Set consumerSet)
  throws SIDiscriminatorSyntaxException                                      
  {
    if (tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "findCandidateSubsForWildcardExpr",
          new Object[] { topicExpression, topicStem}); 

    // Inspect non-wildcarded and non-selector consumers 
    if(_areExactNonSelectorSubs)
    {
      // Use MatchSpace direct evaluation code once we've isolated candidate
      // expressions through string matching
      evaluateCandidateExpression(topicExpression,
                                  _exactNonSelectorSubs,
                                  topicStem,
                                  consumerSet);
    }      

    // Inspect non-wildcarded, selector consumers 
    if(_areExactSelectorSubs)
    {
      // Use MatchSpace direct evaluation code once we've isolated candidate
      // expressions through string matching
      evaluateCandidateExpression(topicExpression,
                                  _exactSelectorSubs,
                                  topicStem,
                                  consumerSet);
                 
    }      

    // Inspect wildcarded and non-selector consumers 
    if(_areWildcardNonSelectorSubs)
    {
      // Use string matching
      isolateCandidateWildcardSubs(topicExpression,
                                   topicStem,
                                   _wildcardNonSelectorSubs,
                                   consumerSet);
    }      

    // Inspect wildcarded and selector consumers 
    if(_areWildcardSelectorSubs)
    {
      // Use string matching
      isolateCandidateWildcardSubs(topicExpression,
                                   topicStem,
                                   _wildcardSelectorSubs,
                                   consumerSet);
    }      
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "findCandidateSubsForWildcardExpr");
  }

  /**
   * Method evaluateCandidateExpression
   * 
   * Use MatchSpace direct evaluation code once we've isolated candidate
   * expressions through string matching in order to find matching consumers
   * in a map.
   *  
   * @param topicExpression
   * @param subscriptions
   * @param topicStem
   * @param consumerSet
   * @throws SIDiscriminatorSyntaxException
   */
  private void evaluateCandidateExpression(String topicExpression,
                                          Map subscriptions,
                                          String topicStem,
                                          Set consumerSet)
  throws SIDiscriminatorSyntaxException                                      
  {
    if (tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "evaluateCandidateExpression",
          new Object[] { topicExpression, topicStem}); 

    // Iterate through the map
    Iterator i = subscriptions.keySet().iterator();
    while (i.hasNext())
    {
      String consumerTopic = (String)i.next();     
      
      if (tc.isDebugEnabled()) 
        SibTr.debug(tc, "Found consumer topic: " + consumerTopic);
        
      // Test candidates
      if(consumerTopic.startsWith(topicStem))
      {
        if (tc.isDebugEnabled()) 
          SibTr.debug(tc, "Drive direct evaluation for topic: " + consumerTopic);
        
        if(_mpm.evaluateDiscriminator(consumerTopic,topicExpression))
        {
          ArrayList consumerList = (ArrayList)subscriptions.get(consumerTopic);
          if (tc.isDebugEnabled())
            SibTr.debug(tc, "Add members of list to set");
          consumerSet.addAll(consumerList); 
        }    
      }
    }                                 
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "evaluateCandidateExpression");
  }

  /**
   * Method isolateCandidateWildcardSubs
   * 
   * Use string matching in order to find matching wildcarded consumers.
   * 
   * @param topicExpression
   * @param topicStem
   * @param subscriptions
   * @param consumerSet
   * @throws SIDiscriminatorSyntaxException
   */
  private void isolateCandidateWildcardSubs(String topicExpression,
                                      String topicStem,
                                      Map subscriptions,
                                      Set consumerSet)
  {
    if (tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "isolateCandidateWildcardSubs",
          new Object[] { topicExpression, topicStem}); 

    // Iterate through the map
    Iterator i = subscriptions.keySet().iterator();
    while (i.hasNext())
    {
      String consumerTopic = (String)i.next();     
      
      if (tc.isDebugEnabled()) 
        SibTr.debug(tc, "Found consumer topic: " + consumerTopic);
        
      String consumerStem = _mpm.retrieveNonWildcardStem(consumerTopic);
        
      // Test candidates
      if(consumerStem.length() >= topicStem.length())
      {
        if(consumerStem.startsWith(topicStem))
        {
          // This is as far as we can get with such topic expressions, the 
          // best that we can say is that there "might" be some overlap
          ArrayList consumerList = (ArrayList)subscriptions.get(consumerTopic);
            
          if (tc.isDebugEnabled())
            SibTr.debug(tc, "Topic expressions might overlap, add members of list to set");
          consumerSet.addAll(consumerList); 
        }
      }
      else
      {
        if(topicStem.startsWith(consumerStem))
        {
          // This is as far as we can get with such topic expressions, the 
          // best that we can say is that there "might" be some overlap
          ArrayList consumerList = (ArrayList)subscriptions.get(consumerTopic);
          
          if (tc.isDebugEnabled())
            SibTr.debug(tc, "Topic expressions might overlap, add members of list to set");
          consumerSet.addAll(consumerList);  
        }          
      }
    }                                 

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "isolateCandidateWildcardSubs");
  }

  /**
   * The following methods are for integrity checks - used in unit testing
   * 
   */
  public int getExactNonSelectorSubsSize(String topicExpression)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "getExactNonSelectorSubsSize", topicExpression);
    int numSubs = 0;
    // Test whether table is now empty
    if(!_exactNonSelectorSubs.isEmpty())
    {
      if(_exactNonSelectorSubs.containsKey(topicExpression))
      { 
        ArrayList consumerList = (ArrayList)_exactNonSelectorSubs.get(topicExpression);
        if(consumerList != null && !consumerList.isEmpty())
        {
          numSubs = consumerList.size();  
        }
      }
    }
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "getExactNonSelectorSubsSize", new Integer(numSubs));
   return  numSubs;
  }
  
  public int getWildcardNonSelectorSubs(String topicExpression)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "getWildcardNonSelectorSubs", topicExpression);
   int numSubs = 0;
   // Test whether table is now empty
   if(!_wildcardNonSelectorSubs.isEmpty())
   {
     if(_wildcardNonSelectorSubs.containsKey(topicExpression))
     { 
       ArrayList consumerList = (ArrayList)_wildcardNonSelectorSubs.get(topicExpression);
       if(consumerList != null && !consumerList.isEmpty())
       {
         numSubs = consumerList.size();  
       }
     }
   }
  if (tc.isEntryEnabled())
    SibTr.exit(tc, "getWildcardNonSelectorSubs", new Integer(numSubs));
  return  numSubs;   
  }  
  
  public int getExactSelectorSubsSize(String topicExpression)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "getExactSelectorSubsSize", topicExpression);
   int numSubs = 0;
   // Test whether table is now empty
   if(!_exactSelectorSubs.isEmpty())
   {
     if(_exactSelectorSubs.containsKey(topicExpression))
     { 
       ArrayList consumerList = (ArrayList)_exactSelectorSubs.get(topicExpression);
       if(consumerList != null && !consumerList.isEmpty())
       {
         numSubs = consumerList.size();  
       }
     }
   }
  if (tc.isEntryEnabled())
    SibTr.exit(tc, "getExactSelectorSubsSize", new Integer(numSubs));
  return  numSubs;      
  }
  
  public int getWildcardSelectorSubs(String topicExpression)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "getWildcardSelectorSubs", topicExpression);
   int numSubs = 0;
   // Test whether table is now empty
   if(!_wildcardSelectorSubs.isEmpty())
   {
     if(_wildcardSelectorSubs.containsKey(topicExpression))
     { 
       ArrayList consumerList = (ArrayList)_wildcardSelectorSubs.get(topicExpression);
       if(consumerList != null && !consumerList.isEmpty())
       {
         numSubs = consumerList.size();  
       }
     }
   }
  if (tc.isEntryEnabled())
    SibTr.exit(tc, "getWildcardSelectorSubs", new Integer(numSubs));
  return  numSubs;         
  }     

    
}
