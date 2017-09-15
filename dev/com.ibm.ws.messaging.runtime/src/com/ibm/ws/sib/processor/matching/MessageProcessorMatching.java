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
import com.ibm.websphere.sib.SIRCConstants;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.JsBus;
import com.ibm.ws.sib.matchspace.BadMessageFormatMatchingException;
import com.ibm.ws.sib.matchspace.Conjunction;
import com.ibm.ws.sib.matchspace.EvalCache;
import com.ibm.ws.sib.matchspace.Identifier;
import com.ibm.ws.sib.matchspace.InvalidTopicSyntaxException;
import com.ibm.ws.sib.matchspace.MatchSpace;
import com.ibm.ws.sib.matchspace.MatchSpaceKey;
import com.ibm.ws.sib.matchspace.MatchTarget;
import com.ibm.ws.sib.matchspace.impl.Matching;
import com.ibm.ws.sib.matchspace.MatchingException;
import com.ibm.ws.sib.matchspace.QuerySyntaxException;
import com.ibm.ws.sib.matchspace.Selector;
import com.ibm.ws.sib.matchspace.tools.Evaluator;
import com.ibm.ws.sib.matchspace.tools.MatchParser;
import com.ibm.ws.sib.matchspace.tools.PositionAssigner;
import com.ibm.ws.sib.matchspace.tools.Resolver;
import com.ibm.ws.sib.matchspace.tools.TopicSyntaxChecker;
import com.ibm.ws.sib.matchspace.tools.Transformer;
import com.ibm.ws.sib.matchspace.tools.XPath10Parser;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.wsspi.sib.core.ConsumerSetChangeCallback;
import com.ibm.ws.sib.processor.MPSelectionCriteria;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPDiscriminatorSyntaxException;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.ConnectionImpl;
import com.ibm.ws.sib.processor.impl.ConsumerDispatcher;
import com.ibm.ws.sib.processor.impl.ControllableProxySubscription;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.PubSubOutputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.DispatchableKey;
import com.ibm.ws.sib.processor.impl.store.itemstreams.SubscriptionItemStream;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.SelectorDomain;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException;
import com.ibm.wsspi.sib.messagecontrol.ApplicationSignature;

/** A version of MatchSpace in which addTarget uses JMS-style selector strings
 * and removeTarget uses arbitrary keys.
 */
public class MessageProcessorMatching
{
  private static final TraceComponent tc =
    SibTr.register(
      MessageProcessorMatching.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  // Are we operating in unit test mode (for z/OS WLM classifier)
  public static boolean unitTestMode = false;

  // Flag that determines whether full XPath optimization is used
  private boolean disableXPathOptimizer = false;
  // A MatchParser instance to use for all parsing
  private MatchParser _sibParser;
  private MatchParser _xpathParser;

  // A record of all targets that are currently active
  private Map _targets = new HashMap();

  // This object monitors the set of current registered MatchTargets
  private ConsumerMonitoring _consumerMonitoring;

  // The Matching instance
  private Matching _matching = null;
  // The root (topic) Identifier
  private Identifier _rootId;
  // The underlying MatchSpace
  private MatchSpace _matchSpace = null;

  // A MinimalResolver to use if no Resolver is suppled on addTarget
  private Resolver _defaultResolver = null;
  // Resolvers that support the respective selector domains
  private JSResolver _simessageResolver = null;
  private JSResolver _jmsResolver = null;

  // The PositionAssigner for this MatchSpace
  private PositionAssigner _positionAssigner = null;

  // The topic syntax checker for this MatchSpace
  private static TopicSyntaxChecker _syntaxChecker = null;

  /** Back reference to the messageprocessor */
  private MessageProcessor _messageProcessor;
  private boolean _isBusSecure = false;

  // Targets in MessageProcessorMatching are tracked using the following record so that they can be
  // removed by key.
  private static class Target
  {
    private Conjunction[] expr; // Returned by the Transformer during addTarget
    private MatchTarget[] targets; // list of targets, same length as expr

    Target(Conjunction[] expr, MatchTarget[] targets)
    {
      this.expr = expr;
      this.targets = targets;
    }
  }

  //------------------------------------------------------------------------------
  // Constructors for MessageProcessorMatching
  //------------------------------------------------------------------------------

  // No parameter flavour of constructor, for use by WLM classifier
  public MessageProcessorMatching()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "MessageProcessorMatching");

    _matching = getMatchingInstance();

    _rootId = _matching.createIdentifier("topic");
    _rootId.setType(Selector.TOPIC);
    // Use XPath style topic syntax checking
    _syntaxChecker = _matching.createXPathTopicSyntaxChecker();

    _defaultResolver = _matching.createMinimalResolver();
    // Set up resolvers that support the respective selector domains
    _simessageResolver = new JSResolver(SelectorDomain.SIMESSAGE);
    _jmsResolver = new JSResolver(SelectorDomain.JMS);
    _positionAssigner = _matching.createPositionAssigner();
    _defaultResolver.resolve(_rootId, _positionAssigner);
    _matchSpace = _matching.createMatchSpace(_rootId, true);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "MessageProcessorMatching", this);
  }

  // Takes a messageProcessor parameter
  public MessageProcessorMatching(MessageProcessor messageProcessor)
  {
    // Call no parameter constructor
    this();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "MessageProcessorMatching", messageProcessor);

    // Set ref to messageProcessor
    _messageProcessor = messageProcessor;
    // Set the security enablement flag
    _isBusSecure = messageProcessor.isBusSecure();

    // Check for the custom property that disables XPath optimization and
    // switches to a mode of operation where XPath expressions are processed
    // whole.
    JsBus jsBus = messageProcessor.getBus();
    String propVal = jsBus.getCustomProperty("com.ibm.ws.sib.matching.XPathOptimizer");

    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "custom prop value: "+propVal);

    if ((propVal != null) && (!"".equals(propVal.trim())))
    {
      // Now parse the property
      if(propVal.equalsIgnoreCase("disabled"))
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "XPath Optimization is disabled at the Bus");
        disableXPathOptimizer = true;
      }
      else if(propVal.equalsIgnoreCase("enabled"))
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "XPath Optimization is enabled at the Bus");
        disableXPathOptimizer = false;
      }
      else
      {
        // Value MIS set, treat as NOT set. Write to console that the property
        //  has been set to an unexpected value
        SibTr.info(tc, "INVALID_XPATH_CUSTOM_PROPERTY_CWSIP0377",
        new Object[] { propVal});
      }
    }

    // Create monitoring agent
    _consumerMonitoring = new ConsumerMonitoring(_messageProcessor, this);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "MessageProcessorMatching", this);
  }

  /**
   * Returns an instance of Matching.
   * <p>
   *
   * @return the instance of Matching.
   */
  private Matching getMatchingInstance()
  {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.entry(tc, "getMatchingInstance", Boolean.valueOf(unitTestMode));
        Matching result = null;
        if (unitTestMode)
        {
          // In unit test mode, go directly to Matching to get an instance.
          // This avoids the need for MessageProcessor to be initialised.
          try
          {
        result = Matching.getInstance();
          }
          catch(Exception e)
          {
                // No FFDC code needed.
                // This path is possible only in unit test
          }
        }
        else
        {
          result = (Matching) MessageProcessor.getSingletonInstance(
                SIMPConstants.MATCHING_INSTANCE);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "getMatchingInstance", result);
        return result;
  }


  /** Add a target to the MatchSpace
   * @param key the key to use to remove this target
   * @param topic the topic to use
   * @param selector the JMS selector string to use
   * @param resolver the Resolver to use in resolving identifiers in the selector string.
   * If null, a MinimalResolver that is global to the MatchSpace is used.
   * @param target the target to use.  If the selector is parsed into multiple conjunctions,
   * the target will be duplicated.
   * @exception QuerySyntaxException if the selector syntax is invalid
   * @exception InvalidTopicSyntaxException if the topic syntax is invalid
   * @exception MatchingException if something more serious goes wrong
   */
  private void addTarget(
    Object key,
    String topic,
    String selectorString,
    SelectorDomain domain,
    Resolver resolver,
    MatchTarget target,
    Selector inputSelector,
    Map<String, Object> selectorProperties)
    throws InvalidTopicSyntaxException,
           SIDiscriminatorSyntaxException,
           QuerySyntaxException,
           MatchingException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc,
                  "addTarget",
                  new Object[]{key,
                               topic,
                               selectorString,
                               domain,
                               resolver,
                               target});

    _syntaxChecker.checkTopicSyntax(topic);
    Selector selectorTree = _matching.createTopicLikeOperator(_rootId, topic);
    Transformer transformer = Matching.getTransformer();
    // get single instance of Transformer
    if (selectorString != null && selectorString.trim().length() != 0)
    {
      try
      {
        // Synchronization block to protect the parser object from being reset
        // whilst in use.
        synchronized (this)
        {
          Selector parsed;
          if (inputSelector == null)
          {
            MatchParser parser = null;
            if(domain.equals(SelectorDomain.JMS))
            {
              // Drive the parser
              _sibParser = _matching.primeMatchParser(_sibParser,
                                                 selectorString,
                                                 SelectorDomain.JMS.toInt());
              parser = _sibParser;
            }
            else if(domain.equals(SelectorDomain.SIMESSAGE))
            {
              // Drive the parser
              _sibParser = _matching.primeMatchParser(_sibParser,
                                                 selectorString,
                                                 SelectorDomain.SIMESSAGE.toInt());
              parser = _sibParser;
            }
            else // In the XPath1.0 case we use the default resolver
            {
              // Drive the parser
              _xpathParser = _matching.primeMatchParser(_xpathParser,
                                                 selectorString,
                                                 SelectorDomain.XPATH1.toInt());

              // The selector expression may incorporate namespace prefix mappings, so we
              // handle those here.
              if(selectorProperties != null)
              {
                Map<String, String> prefixMappings = null;
                prefixMappings = (Map<String, String>)selectorProperties.get("namespacePrefixMappings");
                if(prefixMappings != null)
                {
                  XPath10Parser xpParser = (XPath10Parser)_xpathParser;
                  xpParser.setNamespaceMappings(prefixMappings);
                }
              }
              parser = _xpathParser;
            }

            // We're ready to parse the selector
            if(disableXPathOptimizer && domain.equals(SelectorDomain.XPATH1))
            {
              // In this special case we'll disable the MatchSpace XPath
              // optimizations and process XPath expressions as whole entities.
              XPath10Parser xpParser = (XPath10Parser)parser;

              // The selector expression may incorporate namespace prefix mappings, so we
              // handle those here.
              if(selectorProperties != null)
              {
                Map<String, String> prefixMappings = null;
                prefixMappings = (Map<String, String>)selectorProperties.get("namespacePrefixMappings");
                if(prefixMappings != null)
                {
                  xpParser.setNamespaceMappings(prefixMappings);
                }
              }

              parsed = xpParser.parseWholeSelector(selectorString);
            }
            else
            {
              // Parse in the usual manner
              parsed = parser.getSelector(selectorString);
            }

            if (parsed.getType() == Selector.INVALID)
            {
              if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "addTarget", "QuerySyntaxException");

              throw new QuerySyntaxException(selectorString);
            }
            if (resolver == null)
              resolver = _defaultResolver;
            parsed = transformer.resolve(parsed, resolver, _positionAssigner);
            if (parsed.getType() == Selector.INVALID)
            {
              if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "addTarget", "QuerySyntaxException");

              throw new QuerySyntaxException(selectorString);
            }
          }
          else
          {
            parsed = inputSelector;
          }

          selectorTree =
            _matching.createOperator(Selector.AND, selectorTree, parsed);
        }
      }
      catch (RuntimeException e)
      {
        // No FFDC code needed

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "addTarget", "QuerySyntaxException");
        throw new QuerySyntaxException(e.getMessage());
      }
    }

    // Dont do DNF transformation in XPATH Case.
    Conjunction[] expr = null;
    if(domain != null && domain.equals(SelectorDomain.XPATH1))
      expr = transformer.organizeTests(selectorTree);
    else
      expr = transformer.organizeTests(transformer.DNF(selectorTree));

    if (expr == null)
      expr = new Conjunction[] { null };

    MatchTarget[] targ = new MatchTarget[expr.length];

    for (int i = 0; i < targ.length; i++)
    {
      targ[i] = (i == 0) ? target : target.duplicate();
      _matchSpace.addTarget(expr[i], targ[i]);
    }

    // Add to the HashMap, with synch, there may be other accessors
    synchronized(_targets)
    {
      // If this is a subscription, then we need to modify
      // the monitoring state.
      if(target instanceof MonitoredConsumer)
      {
        _consumerMonitoring.addConsumer(key,
                                      topic,
                                      selectorString,
                                      target,
                                      isWildCarded(topic));
      }

      // Now add the target to the _targets table
      _targets.put(key, new Target(expr, targ));
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "addTarget");
  }

  /** Remove a target from the MatchSpace
   * @param key the identity of the target to remove (established at addTarget time)
   * @exception MatchingException if key undefined or on serious error
   */
  public void removeTarget(Object key) throws MatchingException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc,
                  "removeTarget",
                  new Object[]{key});
    // Remove from the HashMap, with synch, there may be other accessors
    synchronized(_targets)
    {
      Target targ = (Target) _targets.get(key);
      if (targ == null)
        throw new MatchingException();
      for (int i = 0; i < targ.targets.length; i++)
        _matchSpace.removeTarget(targ.expr[i], targ.targets[i]);

      // Now remove the Target from the cache
      _targets.remove(key);

      // Remove from the Monitor state, with synch, there may be other accessors
      if(targ.targets.length > 0) // Defect 417084, check targets is non-empty
      {
        if(targ.targets[0] instanceof MonitoredConsumer)
          _consumerMonitoring.removeConsumer((MonitoredConsumer)targ.targets[0]);
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "removeTarget");
  }

  /**
   * Method addConsumerPointMatchTarget
   * Used to add a wrapped ConsumerKey to the MatchSpace.
   * @param consumerPointData  The consumer key
   * @param destUuid  The destination uuid
   * @param cmUuid The consumer manager uuid
   * @param selector  The selector
   */

  public void addConsumerPointMatchTarget(
    DispatchableKey consumerPointData,
    SIBUuid12 destUuid,
    SIBUuid8 cmUuid,
    SelectionCriteria criteria)
    throws
      SIDiscriminatorSyntaxException,
      SISelectorSyntaxException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "addConsumerPointMatchTarget",
        new Object[] { consumerPointData, destUuid, cmUuid, criteria });

    // Set the CP into a wrapper that extends MatchTarget
    MatchingConsumerPoint mcp = new MatchingConsumerPoint(consumerPointData);

    // Form a branch for the MatchSpace from a combination of the destination uuid and
    // the CM uuid
    String mSpacePrefix = destUuid.toString() +
                          MatchSpace.SUBTOPIC_SEPARATOR_CHAR +
                          cmUuid.toString();

    // Combine the topicSpace and topic
    String theTopic = null;
    if (criteria != null && criteria.getDiscriminator() != null)
    {
      theTopic = buildAddTopicExpression(mSpacePrefix, criteria.getDiscriminator());
    }
    else
    {
      theTopic = mSpacePrefix; // destination and consumer manager combo
    }

    // Put the consumer point into the matchspace
    try
    {
      // Set up the other parameters for addTarget
      String selectorString = null;
      SelectorDomain selectorDomain = SelectorDomain.SIMESSAGE;
      Map<String, Object> selectorProperties = null;
      Resolver resolver = null;
      if(criteria != null)
      {
        selectorString = criteria.getSelectorString();
        selectorDomain = criteria.getSelectorDomain();
        // See if these criteria have any selector properties. They might if they are MPSelectionCriteria
        if(criteria instanceof MPSelectionCriteria)
        {
          MPSelectionCriteria mpCriteria = (MPSelectionCriteria)criteria;
          selectorProperties = mpCriteria.getSelectorProperties();
        }
        if(selectorString != null &&
           selectorString.trim().length() != 0)
        {
          if(selectorDomain.equals(SelectorDomain.JMS))
          {
            resolver = _jmsResolver;
          }
          else if(selectorDomain.equals(SelectorDomain.SIMESSAGE))
          {
            resolver = _simessageResolver;
          }
          else // In the XPath1.0 case we use the default resolver
          {
            resolver = _defaultResolver;
          }
        }
      }

      addTarget(consumerPointData, // N.B we use the raw CP as key
      theTopic, // destination name
      selectorString,
      selectorDomain,
      resolver,
      mcp,
      consumerPointData.getSelector(),
      selectorProperties);
    }
    catch (QuerySyntaxException e)
    {
      // No FFDC code needed

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "addConsumerPointMatchTarget", "SISelectorSyntaxException");

      throw new SISelectorSyntaxException(
        nls.getFormattedMessage(
          "INVALID_SELECTOR_ERROR_CWSIP0371",
          new Object[] { criteria },
          null));
    }
    catch (InvalidTopicSyntaxException e)
    {
      // No FFDC code needed

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "addConsumerPointMatchTarget", "SIDiscriminatorSyntaxException");
      String theTopicString;
      if (criteria != null && criteria.getDiscriminator() != null)
      {
        theTopicString = criteria.getDiscriminator();
      }
      else
      {
        theTopicString = mSpacePrefix; // destination name and consumer manager combo
      }
      throw new SIDiscriminatorSyntaxException(
        nls.getFormattedMessage(
          "INVALID_TOPIC_ERROR_CWSIP0372",
          new Object[] { theTopicString },
          null));
    }
    catch (MatchingException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.matching.MessageProcessorMatching.addConsumerPointMatchTarget",
        "1:652:1.117.1.11",
        this);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "addConsumerPointMatchTarget", "SIErrorException");

      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
          "1:661:1.117.1.11",
          e });

      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
            "1:669:1.117.1.11",
            e },
          null),
        e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addConsumerPointMatchTarget");
  }

  /**
   * Method retrieveMatchingConsumerPoints
   * Used to retrieve matching ConsumerPoints from the MatchSpace.
   * @param destUuid  The destination name to retrieve matches
   * @param cmUuid The consumer manager uuid
   * @param msg The msg to key against for selector support
   * @param searchResults  The search results object to use to locate matches
   *
   * @return MessageProcessorSearchResults  The results object containing the matches
   */
  public void retrieveMatchingConsumerPoints(
    SIBUuid12 destUuid,
    SIBUuid8 cmUuid,
    JsMessage msg,
    MessageProcessorSearchResults searchResults) throws SIDiscriminatorSyntaxException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "retrieveMatchingConsumerPoints",
        new Object[] { destUuid, cmUuid, msg, searchResults });

    // Form a branch for the MatchSpace from a combination of the destination uuid and
    // the CM uuid
    String mSpacePrefix = destUuid.toString() +
                          MatchSpace.SUBTOPIC_SEPARATOR_CHAR +
                          cmUuid.toString();

    //Retrieve the set of wrapped consumer points from the matchspace
    try
    {
      // Set up Results object to hold the results from the MatchSpace traversal
      searchResults.reset();

      // Set up an evaluation cache (need to keep one of these per thread. Newing up is expensive)
      EvalCache cache = _matching.createEvalCache();
      String discriminator = msg.getDiscriminator();
      if (discriminator != null)
      {
        try
        {

          _syntaxChecker.checkEventTopicSyntax(discriminator);
          String theTopic = buildSendTopicExpression(mSpacePrefix, discriminator);

          search(theTopic, // keyed on destination name
           (MatchSpaceKey) msg, cache, searchResults);

        }
        catch (InvalidTopicSyntaxException e)
        {
          // No FFDC code needed

          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "retrieveMatchingConsumerPoints", e);

          throw new SIDiscriminatorSyntaxException(
            nls.getFormattedMessage(
              "INVALID_TOPIC_ERROR_CWSIP0372",
              new Object[] { discriminator },
              null));
        }

      }
      else
      {
        //no discriminator
        search(mSpacePrefix, // keyed on destination name/concumer manager combo
         (MatchSpaceKey) msg, cache, searchResults);

      }

    }
    catch (BadMessageFormatMatchingException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.matching.MessageProcessorMatching.retrieveMatchingConsumerPoints",
        "1:759:1.117.1.11",
        this);

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "retrieveMatchingConsumerPoints", e);

      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
          "1:770:1.117.1.11",
          e });

      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
            "1:778:1.117.1.11",
            e },
          null),
        e);
    }
    catch (MatchingException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.matching.MessageProcessorMatching.retrieveMatchingConsumerPoints",
        "1:789:1.117.1.11",
        this);

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "retrieveMatchingConsumerPoints", "SIErrorException");

      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
          "1:800:1.117.1.11",
          e });

      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
            "1:808:1.117.1.11",
            e },
          null),
        e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "retrieveMatchingConsumerPoints");

  }

  /**
   * Method removeConsumerPointMatchTarget
   * Used to remove a ConsumerPoint from the MatchSpace.
   * @param consumerPointData  The consumer to remove
   */
  public void removeConsumerPointMatchTarget(DispatchableKey consumerPointData)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeConsumerPointMatchTarget", consumerPointData);

    // Remove the consumer point from the matchspace
    try
    {
      removeTarget(consumerPointData);
    }
    catch (MatchingException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.matching.MessageProcessorMatching.removeConsumerPointMatchTarget",
        "1:840:1.117.1.11",
        this);

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "removeConsumerPointMatchTarget", "SICoreException");

      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
      new Object[] {
        "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
        "1:851:1.117.1.11",
        e });

      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
            "1:859:1.117.1.11",
            e },
          null),
        e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeConsumerPointMatchTarget");
  }

  /**
   * Method addConsumerDispatcherMatchTarget
   * Used to add a ConsumerDispatcher to the MatchSpace.
   * @param handler  The consumer to add
   * @param topicSpace  The name of the topic space to add against
   * @param discriminator  The topic name
   * @param selector  The filter for the consumer
   */

  public void addConsumerDispatcherMatchTarget(
    ConsumerDispatcher consumerDispatcher,
    SIBUuid12 topicSpace,
    SelectionCriteria criteria)
    throws
      SIDiscriminatorSyntaxException,
      SISelectorSyntaxException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "addConsumerDispatcherMatchTarget",
        new Object[] { consumerDispatcher, topicSpace, criteria });

    // Combine the topicSpace and topic
    String topicSpaceStr = topicSpace.toString();
    String theTopic = buildAddTopicExpression(topicSpaceStr,
                                  (criteria == null) ? null : criteria.getDiscriminator());
    // Store the CD in the MatchSpace
    try
    {
      // Set up the other parameters for addTarget
      String selectorString = null;
      SelectorDomain selectorDomain = SelectorDomain.SIMESSAGE;
      Map<String, Object> selectorProperties = null;
      Resolver resolver = null;
      if(criteria != null)
      {
        selectorString = criteria.getSelectorString();
        selectorDomain = criteria.getSelectorDomain();
        // See if these criteria have any selector properties. They might if they are MPSelectionCriteria
        if(criteria instanceof MPSelectionCriteria)
        {
          MPSelectionCriteria mpCriteria = (MPSelectionCriteria)criteria;
          selectorProperties = mpCriteria.getSelectorProperties();
        }

        if(selectorString != null &&
           selectorString.trim().length() != 0)
        {
          if(selectorDomain.equals(SelectorDomain.JMS))
          {
            resolver = _jmsResolver;
          }
          else if(selectorDomain.equals(SelectorDomain.SIMESSAGE))
          {
            resolver = _simessageResolver;
          }
          else // In the XPath1.0 case we use the default resolver
          {
            resolver = _defaultResolver;
          }
        }
      }

      // Set the CD and selection criteria into a wrapper that extends MatchTarget
      MatchingConsumerDispatcher Matchkey = new MatchingConsumerDispatcher(consumerDispatcher);
      MatchingConsumerDispatcherWithCriteira key = new MatchingConsumerDispatcherWithCriteira (consumerDispatcher,criteria);

      addTarget(key,    // Use the CD and selection criteria as key
                theTopic,
                selectorString,
                selectorDomain,
                resolver, // null will pick up the default resolver
                Matchkey, // Use the wrapped cd as the Match key
                null,
                selectorProperties);

      // Now that this subscription is in the MatchSpace, let the subscription know.
      consumerDispatcher.setIsInMatchSpace(true);
    }
    catch (QuerySyntaxException e)
    {
      // No FFDC code needed
      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "addConsumerDispatcherMatchTarget", "SISelectorSyntaxException");

      throw new SISelectorSyntaxException(
        nls.getFormattedMessage(
          "INVALID_SELECTOR_ERROR_CWSIP0371",
          new Object[] { criteria },
          null));
    }
    catch (InvalidTopicSyntaxException e)
    {
      // No FFDC code needed
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "addConsumerDispatcherMatchTarget", e);

      throw new SIDiscriminatorSyntaxException(
        nls.getFormattedMessage(
          "INVALID_TOPIC_ERROR_CWSIP0372",
          new Object[] { (criteria == null) ? null : criteria.getDiscriminator() },
          null));
    }
    catch (MatchingException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.matching.MessageProcessorMatching.addConsumerDispatcherMatchTarget",
        "1:981:1.117.1.11",
        this);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "addConsumerDispatcherMatchTarget", "SIErrorException");

      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
          "1:990:1.117.1.11",
          e });

      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
            "1:998:1.117.1.11",
            e },
          null),
        e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addConsumerDispatcherMatchTarget");
  }

  public ConsumerDispatcher addSimConsumerDispatcherMatchTarget(
    BaseDestinationHandler handler,
    SelectionCriteria criteria)
    throws
      SIDiscriminatorSyntaxException,
      SISelectorSyntaxException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "addSimConsumerDispatcherMatchTarget",
        new Object[] { handler, criteria });
    SubscriptionItemStream si = new SubscriptionItemStream();
    ConsumerDispatcher cd = new ConsumerDispatcher(handler, si, null);
    SIBUuid12 topicSpace = handler.getUuid();
    addConsumerDispatcherMatchTarget(cd,topicSpace,criteria);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addSimConsumerDispatcherMatchTarget", cd);

    return cd;
  }

  /**
   * Used to add a PubSubOutputHandler for ME-ME comms to the MatchSpace.
   * @param handler  The PubSubOutputHandler
   * @param topicSpace  The topicSpace to add the outputhandler against
   * @param discriminator  The topic for the subscription
   * @param foreignSecuredProxy Flag to indicate whether a proxy sub
   *                      originated from a foreign bus where the home
   *                      bus is secured.
   * @param MESubUserId   Userid to be stored when securing foreign proxy subs
   */
  public ControllableProxySubscription addPubSubOutputHandlerMatchTarget(
    PubSubOutputHandler handler,
    SIBUuid12 topicSpace,
    String discriminator,
    boolean foreignSecuredProxy,
    String MESubUserId)
    throws
      SIDiscriminatorSyntaxException,
      SISelectorSyntaxException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "addPubSubOutputHandlerMatchTarget",
        new Object[] { handler, topicSpace, discriminator, Boolean.valueOf(foreignSecuredProxy), MESubUserId});

    // Combine the topicSpace and topic
    String topicSpaceStr = topicSpace.toString();
    String theTopic = buildAddTopicExpression(topicSpaceStr, discriminator);

    ControllableProxySubscription key = new ControllableProxySubscription(handler,
                                                                          theTopic,
                                                                          foreignSecuredProxy,
                                                                          MESubUserId);

    // Store the CPS in the MatchSpace
    try
    {
      addTarget(key,
      // Use the output handler as key
      theTopic,
      null, // selector string
      null,  // selector domain
      null, // this'll pick up the default resolver
      key,  // defect 240115: Store the  wrappered PSOH
      null,
      null); // selector properties
    }
    catch (QuerySyntaxException e)
    {
      // No FFDC code needed
      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "addPubSubOutputHandlerMatchTarget", e);

      throw new SISelectorSyntaxException(
        nls.getFormattedMessage(
          "INVALID_SELECTOR_ERROR_CWSIP0371",
          new Object[] { null },
          null));
    }
    catch (InvalidTopicSyntaxException e)
    {
      // No FFDC code needed
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "addPubSubOutputHandlerMatchTarget", e);

      throw new SIDiscriminatorSyntaxException(
        nls.getFormattedMessage(
          "INVALID_TOPIC_ERROR_CWSIP0372",
          new Object[] { discriminator },
          null));
    }
    catch (MatchingException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.matching.MessageProcessorMatching.addPubSubOutputHandlerMatchTarget",
        "1:1111:1.117.1.11",
        this);

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "addPubSubOutputHandlerMatchTarget", e);

      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
          "1:1122:1.117.1.11",
          e });

      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
            "1:1130:1.117.1.11",
            e },
          null),
        e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addPubSubOutputHandlerMatchTarget", key);

    return key;
  }

  /**
   * Method retrieveMatchingOutputHandlers
   * Used to retrieve matching OutputHandlers from the MatchSpace.
   *
   * Returns an array of matching targets.  The first element in the array
   * is the matching Local ConsumerDispatchers
   * The second element is the matching PubSubOutput handlers.
   *
   *
   * @param topicSpace  The topic space (destination) to receive on
   * @param discriminator  The topic to match
   * @param msg          The message (for selector matching)
   * @param searchResults  The object to gain the match space results.
   *
   * @throws SICoreException  Thrown when there is an error parsing the message.
   */

  public void retrieveMatchingOutputHandlers(
    DestinationHandler topicSpace,
    String discriminator,
    MatchSpaceKey msg,
    MessageProcessorSearchResults searchResults) throws SIDiscriminatorSyntaxException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "retrieveMatchingOutputHandlers",
        new Object[] { topicSpace, discriminator, msg, searchResults });

    // Get the uuid for the topicspace
    SIBUuid12 topicSpaceUuid = topicSpace.getBaseUuid();
    String topicSpaceStr = topicSpaceUuid.toString();
    // Combine the topicSpace and topic
    String theTopic = buildSendTopicExpression(topicSpaceStr, discriminator);

    // Check that the topic doesn't contain wildcards
    try
    {
      _syntaxChecker.checkEventTopicSyntax(theTopic);
    }
    catch (InvalidTopicSyntaxException e)
    {
      // No FFDC code needed

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "retrieveMatchingOutputHandlers", e);

      throw new SIDiscriminatorSyntaxException(
        nls.getFormattedMessage(
          "INVALID_TOPIC_ERROR_CWSIP0372",
          new Object[] { discriminator },
          null));
    }

    // Retrieve the set of wrapped output handlers from the matchspace
    try
    {
      // Set up Results object to hold the results from the MatchSpace traversal
      searchResults.reset();

      // Set a reference to the destination into the search results
      // This is used to avoid ACL checks where a topicspace does not require access
      // checks (e.g admin flag set, temporary topicspace, etc)
      if(_isBusSecure)
      {
        searchResults.setTopicSpace(topicSpace);
      }

      // Set up an evaluation cache (need to keep one of these per thread. Newing up is expensive)
      EvalCache cache = _matching.createEvalCache();

      search(theTopic, // keyed on destination name
      msg, cache, searchResults);

    }
    catch (BadMessageFormatMatchingException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.matching.MessageProcessorMatching.retrieveMatchingOutputHandlers",
        "1:1222:1.117.1.11",
        this);

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "retrieveMatchingOutputHandlers", e);

      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
          "1:1233:1.117.1.11",
          e });

      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
            "1:1241:1.117.1.11",
            e },
          null),
        e);

    }
    catch (MatchingException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.matching.MessageProcessorMatching.retrieveMatchingOutputHandlers",
        "1:1252:1.117.1.11",
        this);

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "retrieveMatchingOutputHandlers", e);

      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
          "1:1263:1.117.1.11",
          e });

      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
            "1:1271:1.117.1.11",
            e },
          null),
        e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "retrieveMatchingOutputHandlers", searchResults);
  }

  /**
   * Method removeConsumerDispatcherMatchTarget
   * Used to remove a ConsumerDispatcher from the MatchSpace.
   * @param consumerDispatcher  The consumer dispatcher to remove
   */

  public void removeConsumerDispatcherMatchTarget(ConsumerDispatcher consumerDispatcher, SelectionCriteria selectionCriteria)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "removeConsumerDispatcherMatchTarget",
        new Object[] {consumerDispatcher, selectionCriteria});

    // Remove the consumer point from the matchspace
    // Set the CD and selection criteria into a wrapper that extends MatchTarget

    MatchingConsumerDispatcherWithCriteira key = new MatchingConsumerDispatcherWithCriteira (consumerDispatcher,selectionCriteria);

    // Reset the CD flag to indicate that this subscription is not in the MatchSpace.
    consumerDispatcher.setIsInMatchSpace(false);

    try
    {
      removeTarget(key);
    }
    catch (MatchingException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.matching.MessageProcessorMatching.removeConsumerDispatcherMatchTarget",
        "1:1312:1.117.1.11",
        this);

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "removeConsumerDispatcherMatchTarget", "SICoreException");

      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
          "1:1323:1.117.1.11",
          e });

      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
            "1:1331:1.117.1.11",
            e },
          null),
        e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeConsumerDispatcherMatchTarget");
  }

  /**
   * Method removePubSubOutputHandlerMatchTarget
   * Used to remove a PubSubOutputHandler from the MatchSpace.
   * @param sub  the subscription to remove
   */

  public void removePubSubOutputHandlerMatchTarget(
    ControllableProxySubscription sub)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removePubSubOutputHandlerMatchTarget", sub);

    // Remove the PubSub OutputHandler from the matchspace
    try
    {
      removeTarget(sub);
    }
    catch (MatchingException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.matching.MessageProcessorMatching.removePubSubOutputHandlerMatchTarget",
        "1:1363:1.117.1.11",
        this);

      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
          "1:1370:1.117.1.11",
          e });

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "removePubSubOutputHandlerMatchTarget", "SICoreException");

      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
            "1:1381:1.117.1.11",
            e },
          null),
        e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removePubSubOutputHandlerMatchTarget");
  }

  /**
   * Concatenates topicspace and a topic expression with a level separator between
   *
   * Null or empty topics are treated as topics at the root level so the
   * combination topic becomes topicSpace/
   *
   * @param topicSpace  The topicspace name
   * @param discriminator  The topic
   */
  private String buildSendTopicExpression(
    String destName,
    String discriminator)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "buildSendTopicExpression",
        new Object[] { destName, discriminator });

    String combo = null;

    if (discriminator == null || discriminator.trim().length() == 0)
      combo = destName;
    else
      combo =
        destName
          + MatchSpace.SUBTOPIC_SEPARATOR_CHAR
          + discriminator;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "buildSendTopicExpression", combo);

    return combo;
  }

  /**
   * Concatenates topicspace and a topic expression with a level separator between.
   *
   * Null topics are treated as wildcarded topics at the root level so the
   * combination topic becomes topicSpace//.
   * Empty topics are treated as a subscription to the root level so becomes
   * topicSpace/
   * Topics that begin // need to be treated as topicSpace//
   * Topics that begin / are rejected
   *
   * @param topicSpace  The topicspace name
   * @param discriminator  The topic
   *
   * @exception SIDiscriminatorSyntaxException If the topic starts with a leading /
   */
  public String buildAddTopicExpression(
    String destName,
    String discriminator) throws SIDiscriminatorSyntaxException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "buildAddTopicExpression",
        new Object[] { destName, discriminator });

    String combo = null;

    if (discriminator == null)
      combo =
        destName
          + MatchSpace.SUBTOPIC_DOUBLE_SEPARATOR_STOP_STRING;
    else if (discriminator.trim().length() == 0)
      combo =
        destName;
    else if (discriminator.startsWith(MatchSpace.SUBTOPIC_DOUBLE_SEPARATOR_STRING))
      combo =
        destName
          + discriminator;
    else if (discriminator.startsWith(""+MatchSpace.SUBTOPIC_SEPARATOR_CHAR))
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "buildAddTopicExpression", "SISelectorSyntaxException");

      throw new SIDiscriminatorSyntaxException(
        nls.getFormattedMessage(
          "INVALID_TOPIC_ERROR_CWSIP0372",
          new Object[] { discriminator },
          null));
    }
    else
      combo =
        destName
          + MatchSpace.SUBTOPIC_SEPARATOR_CHAR
          + discriminator;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "buildAddTopicExpression", combo);

    return combo;
  }

  /**
   * Method getAllCDMatchTargets
   * Used to return a list of all ConsumerDispatchers stored in the matchspace
   * @return List of matching consumers
   */

  public ArrayList getAllCDMatchTargets()
  {
    ArrayList consumerDispatcherList;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getAllCDMatchTargets");

    // Get from the HashMap, with synch, there may be other accessors
    synchronized(_targets)
    {
      consumerDispatcherList = new ArrayList(_targets.size());

      Iterator itr = _targets.keySet().iterator();

      while (itr.hasNext())
      {
        Object key = itr.next();
        if (key instanceof MatchingConsumerDispatcherWithCriteira)
        {
          consumerDispatcherList.add(((MatchingConsumerDispatcherWithCriteira)key).getConsumerDispatcher());
        }
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getAllCDMatchTargets");

    return consumerDispatcherList;
  }

  /**
   * Method getAllCDMatchTargetsForTopicSpace
   * Used to return a list of all ConsumerDispatchers stored in the matchspace
   * for a specific topicspace.
   * @return List of matching consumers
   */

  public ArrayList getAllCDMatchTargetsForTopicSpace(String tSpace)
  {
    ArrayList consumerDispatcherList;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getAllCDMatchTargetsForTopicSpace");

    // Get from the HashMap, with synch, there may be other accessors
    synchronized(_targets)
    {
      consumerDispatcherList = new ArrayList(_targets.size());
      Iterator itr = _targets.keySet().iterator();

      while (itr.hasNext())
      {
        Object key = itr.next();
        if (key instanceof MatchingConsumerDispatcherWithCriteira)
        {
          ConsumerDispatcher cd =(((MatchingConsumerDispatcherWithCriteira)key).getConsumerDispatcher());
          String storedDest = cd.getDestination().getName();
          if (storedDest.equals(tSpace))
          {
            consumerDispatcherList.add(cd);
          }
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getAllCDMatchTargetsForTopicSpace");

    return consumerDispatcherList;
  }

  /**
   * Method getAllPubSubOutputHandlerMatchTargets
   * Used to return a list of all PubSubOutputHandlers stored in the matchspace
   * @return List of matching outputhandlers
   */

  public ArrayList getAllPubSubOutputHandlerMatchTargets()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getAllPubSubOutputHandlerMatchTargets");

    ArrayList outputHandlerList;

    // Get from the HashMap, with synch, there may be other accessors
    synchronized(_targets)
    {
      outputHandlerList = new ArrayList(_targets.size());
      Iterator itr = _targets.keySet().iterator();

      while (itr.hasNext())
      {
        Object key = itr.next();

        if (key instanceof ControllableProxySubscription)
          outputHandlerList.add(
            ((ControllableProxySubscription) key).getOutputHandler());

      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(
        tc,
        "getAllPubSubOutputHandlerMatchTargets",
        outputHandlerList);

    return outputHandlerList;
  }

  public Selector parseDiscriminator(String discriminator)
    throws SIDiscriminatorSyntaxException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "parseDiscriminator", discriminator);

    Selector discriminatorTree = null;

    try
    {
      if (discriminator != null)
      {
        // Check syntax of the discrimnator expression
          _syntaxChecker.checkTopicSyntax(discriminator);

        // Parse the discriminator expression
        discriminatorTree =
          _matching.createTopicLikeOperator(_rootId, discriminator);
      }
    }
    catch (InvalidTopicSyntaxException e)
    {
      // No FFDC code needed
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "parseDiscriminator", e);

      throw new SIDiscriminatorSyntaxException(
        nls.getFormattedMessage(
          "INVALID_TOPIC_ERROR_CWSIP0372",
          new Object[] { discriminator },
          null));
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "parseDiscriminator", discriminatorTree);

    return discriminatorTree;
  }

  /**
   * Method parseSelector
   * Used to parse the string representation of a selector into a MatchSpace selector tree.
   * @param selector
   */

  public Selector parseSelector(String selectorString,
                                SelectorDomain domain)
    throws SISelectorSyntaxException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "parseSelector", new Object[] { selectorString, domain });

    Selector selectorTree = null;
    Transformer transformer = Matching.getTransformer();
    Resolver resolver = null;

    try
    {
      // Synchronization block to protect the parser object from being reset
      // whilst in use.
      synchronized (this)
      {
        MatchParser parser = null;
        if(domain.equals(SelectorDomain.JMS))
        {
          // Set the correct resolver
          resolver = _jmsResolver;
          // Drive the parser
          _sibParser = _matching.primeMatchParser(_sibParser,
                                             selectorString,
                                             SelectorDomain.JMS.toInt());
          parser = _sibParser;
        }
        else if(domain.equals(SelectorDomain.SIMESSAGE))
        {
          // Set the correct resolver
          resolver = _simessageResolver;
          // Drive the parser
          _sibParser = _matching.primeMatchParser(_sibParser,
                                             selectorString,
                                             SelectorDomain.SIMESSAGE.toInt());
          parser = _sibParser;
        }
        else // In the XPath1.0 case we use the default resolver
        {
          // Set the correct resolver
          resolver = _defaultResolver;
          // Drive the parser
          _xpathParser = _matching.primeMatchParser(_xpathParser,
                                             selectorString,
                                             SelectorDomain.XPATH1.toInt());
          parser = _xpathParser;
        }

        // We're ready to parse the selector
        if(disableXPathOptimizer && domain.equals(SelectorDomain.XPATH1))
        {
          // In this special case we'll disable the MatchSpace XPath
          // optimizations and process XPath expressions as whole entities.
          XPath10Parser xpParser = (XPath10Parser)parser;
          selectorTree = xpParser.parseWholeSelector(selectorString);
        }
        else
        {
          // Parse in the usual manner
          selectorTree = parser.getSelector(selectorString);
        }

        // Check that the selector parsed ok.
        if (selectorTree.getType() == Selector.INVALID)
        {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                  SibTr.exit(tc, "parseSelector", "SISelectorSyntaxException");
          throw new SISelectorSyntaxException(
            nls.getFormattedMessage(
              "INVALID_SELECTOR_ERROR_CWSIP0371",
              new Object[] { selectorString },
              null));
        }

        selectorTree = transformer.resolve(selectorTree, resolver, _positionAssigner);

        // Check that the selector resolved ok.
        if (selectorTree.getType() == Selector.INVALID)
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "parseSelector", "SISelectorSyntaxException");
          throw new SISelectorSyntaxException(
            nls.getFormattedMessage(
              "INVALID_SELECTOR_ERROR_CWSIP0371",
              new Object[] { selectorString },
              null));
        }
      }
    }
    catch (RuntimeException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.matching.MessageProcessorMatching.parseSelector",
        "1:1741:1.117.1.11",
        this);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "parseSelector", "SISelectorSyntaxException");

      throw new SISelectorSyntaxException(
        nls.getFormattedMessage(
          "INVALID_SELECTOR_ERROR_CWSIP0371",
          new Object[] { null },
          null));
    }
    catch (MatchingException e)
    {
      // No FFDC code needed

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "parseSelector", "SISelectorSyntaxException");

      throw new SISelectorSyntaxException(
        nls.getFormattedMessage(
          "INVALID_SELECTOR_ERROR_CWSIP0376",
          new Object[] { e },
          null));
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "parseSelector", selectorTree);
    return selectorTree;
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
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "evaluateMessage", new Object[] { selectorTree, msg });

    boolean ret = false;
    Object result = null;
    boolean discriminatorMatches = true; //assume it does match

    // Use the dummy evaluation cache, we don't need one hear as we're not
    // searching the MatchSpace
    EvalCache cache = EvalCache.DUMMY;

    try
    {
      Evaluator evaluator = Matching.getEvaluator();
      //    See if we need to process a discriminator expression first
      if (discriminatorTree != null)
      {
        discriminatorMatches = false; //can no longer assume it does match
        String discriminator = msg.getDiscriminator();

        MatchSpaceKey msk = new DiscriminatorMatchSpaceKey(discriminator);
        // Evaluate message against selector tree
        result = evaluator.eval(discriminatorTree, msk, cache, null, false);
        if (result != null && ((Boolean) result).booleanValue())
        {
          discriminatorMatches = true;
        }
      }

      // Evaluate message against selector tree
      if (selectorTree != null && discriminatorMatches)
      {
        result =
          evaluator.eval(selectorTree, (MatchSpaceKey) msg, cache, null, false);
      }

    }
    catch (BadMessageFormatMatchingException qex)
    {
      FFDCFilter.processException(
        qex,
        "com.ibm.ws.sib.processor.matching.MessageProcessorMatching.evaluateMessage",
        "1:1824:1.117.1.11",
        this);

      SibTr.exception(tc, qex);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "evaluateMessage", "SICoreException");

      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
          "1:1834:1.117.1.11",
          qex });

      // For now, I'll throw a core exception, but in due course this (or something with a
      // better name) will be externalised in the Core API.
      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
            "1:1844:1.117.1.11",
            qex },
          null),
        qex);
    }

    // The result of expression evaluation can be either a
    // boolean, or not, which is interpreted as false.
    if (result instanceof Boolean)
    {
      // This turns a BooleanValue into a boolean
      // it will return false for unknown which is what we want.
      ret = ((Boolean) result).booleanValue();
    }
    else if (result == null)
    {
      ret = false;
    }
    else
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "evaluateMessage", "SICoreException");
      // For now, I'll throw a core exception, but in due course this (or something with a
      // better name) will be externalised in the Core API.
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
        new Object[] {
          "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
          "1:1870:1.117.1.11" });

      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0001",
          new Object[] {
            "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
            "1:1877:1.117.1.11" },
          null));
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "evaluateMessage", Boolean.valueOf(ret));
    return ret;
  }

  /**
   * Method addTopicAcl
   * Used to add a TopicAcl to the MatchSpace.
   * @param acl  The TopicAcl
   */
  public void addTopicAcl(SIBUuid12 destName, TopicAcl acl)
    throws
      SIDiscriminatorSyntaxException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "addTopicAcl", new Object[] { destName, acl });

    String discriminator = null;
    // Postpend # wildcard to the fully qualified topic supplied
    // TO DO check the topicsyntax

    String theTopic = "";
    if(destName != null)
    {
      String destNameStr = destName.toString();
      theTopic = buildAddTopicExpression(destNameStr, acl.getTopic());
    }

    // Careful, only do this if the original topic is not null
    if(acl.getTopic() != null)
    {
      discriminator = theTopic + "//.";
    }
    else
    {
      discriminator = theTopic;
    }

    // Put the acl into the matchspace
    try
    {
      addTarget(acl, // N.B we use the raw CP as key
              discriminator, // wildcarded topic expression
              null, // selector string
              null, // selector domain
              null, // this'll pick up the default resolver
              acl,
              null,
              null); // selector properties
    }
    catch (QuerySyntaxException e)
    {
      // No FFDC code needed
    }
    catch (InvalidTopicSyntaxException e)
    {
      // No FFDC code needed

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "addTopicAcl", "SIDiscriminatorSyntaxException");

      throw new SIDiscriminatorSyntaxException(
        nls.getFormattedMessage(
          "INVALID_TOPIC_ERROR_CWSIP0372",
          new Object[] { theTopic },
          null));
    }
    catch (MatchingException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.matching.MessageProcessorMatching.addTopicAcl",
        "1:1954:1.117.1.11",
        this);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "addTopicAcl", "SIErrorException");

      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
          "1:1962:1.117.1.11",
          e });

      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
            "1:1970:1.117.1.11",
            e },
          null),
        e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addTopicAcl");
  }

  /**
   * Method retrieveMatchingTopicAcls
   * Used to retrieve matching TopicAcls from the MatchSpace.
   * @param destName  The destination name to retrieve matches
   * @param msg The msg to key against for selector support
   * @param searchResults  The search results object to use to locate matches
   *
   */
  public void retrieveMatchingTopicAcls(
    DestinationHandler topicSpace,
    String discriminator,
    JsMessage msg,
    MessageProcessorSearchResults searchResults)
    throws SIDiscriminatorSyntaxException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "retrieveMatchingTopicAcls",
        new Object[] { topicSpace, discriminator, msg, searchResults });

    //  Get the uuid for the topicspace
    SIBUuid12 topicSpaceUuid = topicSpace.getBaseUuid();
    String topicSpaceStr = topicSpaceUuid.toString();
    // Combine the topicSpace and topic
    String theTopic = buildSendTopicExpression(topicSpaceStr, discriminator);

    // Check that the topic doesn't contain wildcards
    try
    {
        _syntaxChecker.checkEventTopicSyntax(theTopic);
    }
    catch (InvalidTopicSyntaxException e)
    {
      // No FFDC code needed

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "retrieveMatchingTopicAcls", e);

      SIMPDiscriminatorSyntaxException ee = new SIMPDiscriminatorSyntaxException(
        nls.getFormattedMessage(
          "INVALID_TOPIC_ERROR_CWSIP0372",
          new Object[] { theTopic },
          null));

      ee.setExceptionReason(SIRCConstants.SIRC0901_INTERNAL_MESSAGING_ERROR);
      ee.setExceptionInserts(new String[] {"com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
                                           "1:2027:1.117.1.11",
                                              SIMPUtils.getStackTrace(e)});
      throw ee;
    }

    //Retrieve the set of wrapped consumer points from the matchspace
    try
    {
      // Set up an evaluation cache (need to keep one of these per thread. Newing up is expensive)
      EvalCache cache = _matching.createEvalCache();

      // Set up Results object to hold the results from the MatchSpace traversal
      searchResults.reset();

      // Set a reference to the destination into the search results
      // This is used to avoid ACL checks where a topicspace does not require access
      // checks (e.g admin flag set, temporary topicspace, etc)
      if(_isBusSecure)
      {
        searchResults.setTopicSpace(topicSpace);
      }

      search(theTopic, // keyed on destination name
       (MatchSpaceKey) msg, cache, searchResults);

    }
    catch (BadMessageFormatMatchingException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.matching.MessageProcessorMatching.retrieveMatchingTopicAcls",
        "1:2059:1.117.1.11",
        this);

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "retrieveMatchingTopicAcls", e);

      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
          "1:2070:1.117.1.11",
          e });

      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
            "1:2078:1.117.1.11",
            e },
          null),
        e);
    }
    catch (MatchingException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.matching.MessageProcessorMatching.retrieveMatchingTopicAcls",
        "1:2089:1.117.1.11",
        this);

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "retrieveMatchingTopicAcls", "SIErrorException");

      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
          "1:2099:1.117.1.11",
          e });

      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
            "1:2107:1.117.1.11",
            e },
          null),
        e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "retrieveMatchingTopicAcls");
  }

  public boolean isWildCarded(String discriminator)
  {
    boolean result = false;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "isWildCarded", discriminator);

    try
    {
      _syntaxChecker.checkEventTopicSyntax(discriminator);
    }
    catch (InvalidTopicSyntaxException e)
    {
        // No FFDC code needed
      result = true;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isWildCarded", Boolean.valueOf(result));

    return result;
  }

  /**
   * Method removeAllTopicAcls
   * Used to remove all TopicAcls stored in the matchspace
   */

  public void removeAllTopicAcls()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeAllTopicAcls");

    ArrayList topicAclList = null;

    try
    {
      // Remove from the HashMap, with synch, there may be other accessors
      synchronized(_targets)
      {
        // Build a list of the existing ACLs
        topicAclList = new ArrayList(_targets.size());

        Iterator itr = _targets.keySet().iterator();

        while (itr.hasNext())
        {
          Object key = itr.next();
          if (key instanceof TopicAcl)
          {
            topicAclList.add(key);
          }
        }

        // Now remove the ACLs from the targets Map
        Iterator aclIter = topicAclList.iterator();

        while (aclIter.hasNext())
        {
          Object key = aclIter.next();
          removeTarget(key);
        }
      }
    }
    catch (MatchingException e)
     {
       FFDCFilter.processException(
         e,
         "com.ibm.ws.sib.processor.matching.MessageProcessorMatching.removeAllTopicAcls",
         "1:2188:1.117.1.11",
         this);

       SibTr.exception(tc, e);

       if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
         SibTr.exit(tc, "removeAllTopicAcls", "SICoreException");

       SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
         new Object[] {
           "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
           "1:2199:1.117.1.11",
           e });

       throw new SIErrorException(
         nls.getFormattedMessage(
           "INTERNAL_MESSAGING_ERROR_CWSIP0002",
           new Object[] {
             "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
             "1:2207:1.117.1.11",
             e },
           null),
         e);
     }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeAllTopicAcls");

  }

  /**
   * Method search
   * Used to retrieve results from the MatchSpace, with protection from dynamic ACL
   * updates.
   * @param theTopic  The topic expression
   * @param msg The msg to key against for selector support
   * @param searchResults  The search results object to use to locate matches
   *
   * @return MessageProcessorSearchResults  The results object containing the matches
   */
  public void search(
    String theTopic,
    MatchSpaceKey msg,
    EvalCache cache,
    MessageProcessorSearchResults searchResults)
    throws BadMessageFormatMatchingException,
           MatchingException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "search",
        new Object[] { theTopic, msg, cache, searchResults });

    // If security is switched on then we need to maintain the integrity of
    // the ACLs in the cache in the face of dynamic updating
    if(_isBusSecure)
    {
      int aclVersion = _messageProcessor.
                         getDiscriminatorAccessChecker().
                         getAclRefreshVersion();

      boolean searchAgain = true;
      //Retrieve the searchresults from the matchspace
      while(searchAgain)
      {
        _matchSpace.search(theTopic, // keyed on destination name
           msg, cache, searchResults);
        // We need to be sure of the integrity of the traversal
        int newAclVersion =  _messageProcessor.
                              getDiscriminatorAccessChecker().
                              getAclRefreshVersion();

        // This should catch those rare cases where a traversal overlapped
        // a refresh.
        if(newAclVersion == aclVersion)
          searchAgain = false;
        else
          aclVersion = newAclVersion;
      }
    }
    else
    {
      _matchSpace.search(theTopic, // keyed on destination name
       msg, cache, searchResults);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "search");

  }

  /**
   * Method registerConsumerSetMonitor
   *
   * Checks whether there are any potential consumers for messages published on the
   * specified topic(s). The potential set will be determined using an optimistic
   * approach that caters for wildcards and selectors by returning true if there is
   * any possibility of a match.
   *
   * Additionally registers a callback that will be driven when the potential set of
   * consumers drops to zero or rises above zero.
   *
   * Returns true if the potential set of consumers is currently greater than zero,
   * false if it is zero.
   *
   * @param topicSpace
   * @param discriminatorExpression
   * @param callback
   * @return
   * @throws SIDiscriminatorSyntaxException
   * @throws SIErrorException
   */
  public boolean registerConsumerSetMonitor(
    DestinationHandler topicSpace,
    String discriminatorExpression,
    ConnectionImpl connection,
    ConsumerSetChangeCallback callback)
  throws
      SIDiscriminatorSyntaxException,
      SIErrorException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "registerConsumerSetMonitor",
          new Object[] { topicSpace, discriminatorExpression, connection, callback });

    boolean isWildcarded = isWildCarded(discriminatorExpression);

    // Get the uuid for the topicspace
    SIBUuid12 topicSpaceUuid = topicSpace.getBaseUuid();
    String topicSpaceStr = topicSpaceUuid.toString();
    // Combine the topicSpace and topic
    String tExpression = buildAddTopicExpression(topicSpaceStr, discriminatorExpression);

    String wildcardStem = null;
    if(isWildcarded)
    {
      // Retrieve the non-wildcarded stem
      wildcardStem = retrieveNonWildcardStem(tExpression);
    }

    boolean areConsumers = false;

    // Register under a lock on the targets table
    synchronized(_targets)
    {
      areConsumers =
        _consumerMonitoring.registerConsumerSetMonitor(connection,
                                                       topicSpace,
                                                       topicSpaceUuid,
                                                       discriminatorExpression,
                                                       tExpression,
                                                       callback,
                                                       isWildcarded,
                                                       wildcardStem,
                                                       this);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "registerConsumerSetMonitor", Boolean.valueOf(areConsumers));
    return areConsumers;
  }

  /**
   * Method retrieveNonSelectorConsumers
   *
   * Performs a search against the MatchSpace in order to retrieve the set of
   * consumers (registered without selectors) that match a fully qualified
   * topic expression.
   *
   * @param topicSpace
   * @param discriminatorExpression
   * @param localConsumers
   * @param remoteConsumers
   * @throws SIDiscriminatorSyntaxException
   * @throws SIErrorException
   */
  public void retrieveNonSelectorConsumers(
    DestinationHandler topicSpace,
    String discriminatorExpression,
    Set localConsumers,
    Set remoteConsumers)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "retrieveNonSelectorConsumers",
          new Object[] { topicSpace, discriminatorExpression });

    // Get the uuid for the topicspace
    SIBUuid12 topicSpaceUuid = topicSpace.getBaseUuid();
    String topicSpaceStr = topicSpaceUuid.toString();
    String theTopic = null;

    try
    {
      // If fully qualified drive a dummy publication against the MatchSpace
      MatchSpaceKey msk = new DiscriminatorMatchSpaceKey(discriminatorExpression);

      // Combine the topicSpace and topic
      theTopic = buildSendTopicExpression(topicSpaceStr, discriminatorExpression);

      // Get a search results object to use
      MessageProcessorSearchResults searchResults =
        (MessageProcessorSearchResults)_messageProcessor.
           getSearchResultsObjectPool().
           remove();

      // Set up Results object to hold the results from the MatchSpace traversal
      searchResults.reset();

      // Set a reference to the destination into the search results
      // This is used to avoid ACL checks where a topicspace does not require access
      // checks (e.g admin flag set, temporary topicspace, etc)
      if(_isBusSecure)
      {
        searchResults.setTopicSpace(topicSpace);
      }

      // Set up an evaluation cache (need to keep one of these per thread. Newing up is expensive)
      EvalCache cache = _matching.createEvalCache();

      search(theTopic, // keyed on destination name
             msk,
             cache,
             searchResults);

      // MatchSpace searching is complete, add any matching consumers to the
      // Sets
      Object allResults[] = searchResults.getResults(theTopic);
      localConsumers.addAll(
              (Set)allResults[MessageProcessorMatchTarget.JS_SUBSCRIPTION_TYPE]);
      remoteConsumers.addAll(
              (Set)allResults[MessageProcessorMatchTarget.JS_NEIGHBOUR_TYPE]);

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
        SibTr.debug(tc, "Found " + localConsumers.size() + " local consumers and " +
                        remoteConsumers.size() + "remote consumers in MatchSpace search");
      }

      // Return search results object to cache
      _messageProcessor.getSearchResultsObjectPool().add(searchResults);

    }
    catch (BadMessageFormatMatchingException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.matching.MessageProcessorMatching.retrieveNonSelectorConsumers",
        "1:2439:1.117.1.11",
        this);

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "retrieveNonSelectorConsumers", e);

      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
          "1:2450:1.117.1.11",
          e });

      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
            "1:2458:1.117.1.11",
            e },
           null),
        e);

    }
    catch (MatchingException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.matching.MessageProcessorMatching.retrieveNonSelectorConsumers",
        "1:2469:1.117.1.11",
        this);
       SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "retrieveNonSelectorConsumers", e);

      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
          "1:2479:1.117.1.11",
          e });
       throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
            "1:2486:1.117.1.11",
            e },
          null),
        e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "retrieveNonSelectorConsumers");
  }

  /**
   * Method deregisterConsumerSetMonitor
   *
   * Deregisters a previously registered callback.
   * @param connection
   * @param callback
   * @throws SINotPossibleInCurrentConfigurationException
   */
  public void deregisterConsumerSetMonitor(ConnectionImpl connection,
                                           ConsumerSetChangeCallback callback)
  throws SINotPossibleInCurrentConfigurationException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "deregisterConsumerSetMonitor", new Object[] { connection, callback });

    // Deregister under a lock on the targets table
    synchronized(_targets)
    {
      _consumerMonitoring.deregisterConsumerSetMonitor(connection, callback);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "deregisterConsumerSetMonitor");

  }

  /**
   * Method removeConsumerSetMonitors
   *
   * Removes all callbacks associated with a connection.
   *
   * @param connection
   * @throws SINotPossibleInCurrentConfigurationException
   */
  public void removeConsumerSetMonitors(ConnectionImpl connection)
  throws SINotPossibleInCurrentConfigurationException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeConsumerSetMonitors", new Object[] { connection });

    // Renmove under a lock on the targets table
    synchronized(_targets)
    {
      _consumerMonitoring.removeConsumerSetMonitors(connection);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeConsumerSetMonitors");
  }

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
  throws SIDiscriminatorSyntaxException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "evaluateDiscriminator", new Object[] { fullTopic, wildcardTopic });

    boolean discriminatorMatches =
      evaluateDiscriminator(fullTopic, wildcardTopic, null);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "evaluateDiscriminator", Boolean.valueOf(discriminatorMatches));
    return discriminatorMatches;
  }

  /**
   * Method evaluateDiscriminator
   *
   * Used to determine whether a supplied fully qualified discriminator matches
   * a supplied wildcarded discriminator expression.
   *
   * @param fullTopic
   * @param wildcardTopic
   * @param discriminatorTree
   * @return
   * @throws SIDiscriminatorSyntaxException
   */
  public boolean evaluateDiscriminator(
    String fullTopic,
    String wildcardTopic,
    Selector discriminatorTree)
  throws SIDiscriminatorSyntaxException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc,
                  "evaluateDiscriminator",
                  new Object[] { fullTopic, wildcardTopic, discriminatorTree });

    Object result = null;
    boolean discriminatorMatches = false;

    // Use the dummy evaluation cache, we don't need one here as we're not
    // searching the MatchSpace
    EvalCache cache = EvalCache.DUMMY;

    try
    {
      Evaluator evaluator = Matching.getEvaluator();

      MatchSpaceKey msk = new DiscriminatorMatchSpaceKey(fullTopic);

      if(discriminatorTree == null)
        discriminatorTree = parseDiscriminator(wildcardTopic);

      // Evaluate message against discriminator tree
      result = evaluator.eval(discriminatorTree, msk, cache, null, false);
      if (result != null && ((Boolean) result).booleanValue())
      {
        discriminatorMatches = true;
      }
    }
    catch (BadMessageFormatMatchingException qex)
    {
      FFDCFilter.processException(
        qex,
        "com.ibm.ws.sib.processor.matching.MessageProcessorMatching.evaluateDiscriminator",
        "1:2623:1.117.1.11",
        this);

      SibTr.exception(tc, qex);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "evaluateDiscriminator", "SICoreException");

      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
          "1:2633:1.117.1.11",
          qex });

      // For now, I'll throw a core exception, but in due course this (or something with a
      // better name) will be externalised in the Core API.
      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
            "1:2643:1.117.1.11",
            qex },
          null),
        qex);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "evaluateDiscriminator", Boolean.valueOf(discriminatorMatches));
    return discriminatorMatches;
  }

  /**
   * Method retrieveNonWildcardStem
   *
   * Used to retrieve the fully qualified substring that precedes the first wildcard
   * in a wildcarded discriminator expression.
   *
   * @param topic
   * @return
   * @throws SIDiscriminatorSyntaxException
   */
  public String retrieveNonWildcardStem(String topic)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "retrieveNonWildcardStem",
        new Object[] { topic});

    String stem = null;

    int wildMany = topic.indexOf(MatchSpace.SUBTOPIC_DOUBLE_SEPARATOR_STRING);
    int wildOne = topic.indexOf(MatchSpace.SUBTOPIC_MATCHONE_CHAR);

    if( wildOne > -1)
    {
      if(wildMany > wildOne)
        stem = topic.substring(0, wildOne - 1); // Careful need to account for "/"
      else
      {
        if(wildMany > -1)
          stem = topic.substring(0,wildMany);
        else
          stem = topic.substring(0, wildOne - 1); // Careful need to account for "/"
      }
    }
    else
    {
      if(wildMany > -1)
      {
        stem = topic.substring(0,wildMany);
      }
      else
      {
        // No wildcards
        stem = topic;
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "retrieveNonWildcardStem", stem);

    return stem;
  }

  /**
   * @return
   */
  public ConsumerMonitoring getConsumerMonitoring()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getConsumerMonitoring");
      SibTr.exit(tc, "getConsumerMonitoring", _consumerMonitoring);
    }
    return _consumerMonitoring;
  }

  /**
   * Method addApplicationSignatureMatchTarget
   * Used to add a wrapped ApplicationSignature to the MatchSpace.
   * @param appSignature
   * @param destUuid  The destination uuid
   * @param cmUuid The consumer manager uuid
   * @param criteria
   * @throws SIDiscriminatorSyntaxException
   * @throws SISelectorSyntaxException
   */
  public void addApplicationSignatureMatchTarget(
    ApplicationSignature appSignature,
    SIBUuid12 destUuid,
    SIBUuid8 cmUuid,
    SelectionCriteria criteria)
    throws
      SIDiscriminatorSyntaxException,
      SISelectorSyntaxException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "addApplicationSignatureMatchTarget",
        new Object[] { appSignature, destUuid, cmUuid, criteria });

    // Set the appSignature into a wrapper that extends MatchTarget
    MatchingApplicationSignature mas = new MatchingApplicationSignature(appSignature);

    // Form a branch for the MatchSpace from a combination of the destination uuid and
    // the CM uuid
    String mSpacePrefix = destUuid.toString() +
                          MatchSpace.SUBTOPIC_SEPARATOR_CHAR +
                          cmUuid.toString();

    // Combine the topicSpace and topic
    String theTopic = null;
    if (criteria != null && criteria.getDiscriminator() != null)
    {
      theTopic = buildAddTopicExpression(mSpacePrefix,
                                         criteria.getDiscriminator());
    }
    else
    {
      theTopic = mSpacePrefix; // destination name
    }

    // Put the App Signature into the matchspace
    try
    {
      // Set up the other parameters for addTarget
      String selectorString = null;
      SelectorDomain selectorDomain = SelectorDomain.SIMESSAGE;
      Map<String, Object> selectorProperties = null;
      Resolver resolver = null;
      if(criteria != null)
      {
        selectorString = criteria.getSelectorString();
        selectorDomain = criteria.getSelectorDomain();
        // See if these criteria have any selector properties. They might if they are MPSelectionCriteria
        if(criteria instanceof MPSelectionCriteria)
        {
          MPSelectionCriteria mpCriteria = (MPSelectionCriteria)criteria;
          selectorProperties = mpCriteria.getSelectorProperties();
        }

        if(selectorString != null &&
           selectorString.trim().length() != 0)
        {
          if(selectorDomain.equals(SelectorDomain.JMS))
          {
            resolver = _jmsResolver;
          }
          else if(selectorDomain.equals(SelectorDomain.SIMESSAGE))
          {
            resolver = _simessageResolver;
          }
          else // In the XPath1.0 case we use the default resolver
          {
            resolver = _defaultResolver;
          }
        }
      }

      addTarget(appSignature, // N.B we use the raw appSignature as key
      theTopic,
      selectorString,
      selectorDomain,
      resolver,
      mas,
      null,
      selectorProperties);
    }
    catch (QuerySyntaxException e)
    {
      // No FFDC code needed

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "addApplicationSignatureMatchTarget", "SISelectorSyntaxException");

      throw new SISelectorSyntaxException(
        nls.getFormattedMessage(
          "INVALID_SELECTOR_ERROR_CWSIP0371",
          new Object[] { criteria },
          null));
    }
    catch (InvalidTopicSyntaxException e)
    {
      // No FFDC code needed

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "addApplicationSignatureMatchTarget", "SIDiscriminatorSyntaxException");
      String theTopicString;
      if (criteria != null && criteria.getDiscriminator() != null)
      {
        theTopicString = criteria.getDiscriminator();
      }
      else
      {
        theTopicString = mSpacePrefix; // destination uuid and cmuuid combo
      }
      throw new SIDiscriminatorSyntaxException(
        nls.getFormattedMessage(
          "INVALID_TOPIC_ERROR_CWSIP0372",
          new Object[] { theTopicString },
          null));
    }
    catch (MatchingException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.matching.MessageProcessorMatching.addApplicationSignatureMatchTarget",
        "1:2856:1.117.1.11",
        this);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "addApplicationSignatureMatchTarget", "SIErrorException");

      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
          "1:2865:1.117.1.11",
          e });

      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
            "1:2873:1.117.1.11",
            e },
          null),
        e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addApplicationSignatureMatchTarget");
  }

  /**
   * Method retrieveMatchingApplicationSignatures
   * Used to retrieve matching ApplicationSignatures from the MatchSpace.
   *
   * @param destUuid  The destination uuid
   * @param cmUuid The consumer manager uuid
   * @param msg
   * @param searchResults
   * @param isPreMediated
   * @return The results object containing the matches
   * @throws SIDiscriminatorSyntaxException
   */
  public void retrieveMatchingApplicationSignatures(
    SIBUuid12 destUuid,
    SIBUuid8 cmUuid,
    JsMessage msg,
    MessageProcessorSearchResults searchResults)
    throws SIDiscriminatorSyntaxException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "retrieveMatchingApplicationSignatures",
        new Object[] { destUuid, cmUuid, msg, searchResults});

    // Form a branch for the MatchSpace from a combination of the destination uuid and
    // the CM uuid
    String mSpacePrefix = destUuid.toString() +
                          MatchSpace.SUBTOPIC_SEPARATOR_CHAR +
                          cmUuid.toString();

    //Retrieve the set of wrapped consumer points from the matchspace
    try
    {
      // Set up Results object to hold the results from the MatchSpace traversal
      searchResults.reset();

      // Set up an evaluation cache (need to keep one of these per thread. Newing up is expensive)
      EvalCache cache = _matching.createEvalCache();
      String discriminator = msg.getDiscriminator();
      if (discriminator != null)
      {
        try
        {

          _syntaxChecker.checkEventTopicSyntax(discriminator);
          String theTopic = buildSendTopicExpression(mSpacePrefix, discriminator);

          search(theTopic, // keyed on destination name
           (MatchSpaceKey) msg, cache, searchResults);

        }
        catch (InvalidTopicSyntaxException e)
        {
          // No FFDC code needed

          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "retrieveMatchingApplicationSignatures", e);

          throw new SIDiscriminatorSyntaxException(
            nls.getFormattedMessage(
              "INVALID_TOPIC_ERROR_CWSIP0372",
              new Object[] { discriminator },
              null));
        }

      }
      else
      {
        //no discriminator
        search(mSpacePrefix, // keyed on destination name
         (MatchSpaceKey) msg, cache, searchResults);

      }

    }
    catch (BadMessageFormatMatchingException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.matching.MessageProcessorMatching.retrieveMatchingApplicationSignatures",
        "1:2965:1.117.1.11",
        this);

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "retrieveMatchingApplicationSignatures", e);

      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
          "1:2976:1.117.1.11",
          e });

      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
            "1:2984:1.117.1.11",
            e },
          null),
        e);
    }
    catch (MatchingException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.matching.MessageProcessorMatching.retrieveMatchingApplicationSignatures",
        "1:2995:1.117.1.11",
        this);

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "retrieveMatchingApplicationSignatures", "SIErrorException");

      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
          "1:3006:1.117.1.11",
          e });

      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
            "1:3014:1.117.1.11",
            e },
          null),
        e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "retrieveMatchingApplicationSignatures");
  }

  /**
   * Method removeApplicationSignatureMatchTarget
   * Used to remove a ApplicationSignature from the MatchSpace.
   * @param consumerPointData  The ApplicationSignature to remove
   */
  public void removeApplicationSignatureMatchTarget(ApplicationSignature appSignature)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeApplicationSignatureMatchTarget", appSignature);

    // Remove the consumer point from the matchspace
    try
    {
      removeTarget(appSignature);
    }
    catch (MatchingException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.matching.MessageProcessorMatching.removeApplicationSignatureMatchTarget",
        "1:3045:1.117.1.11",
        this);

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "removeApplicationSignatureMatchTarget", "SICoreException");

      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
      new Object[] {
        "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
        "1:3056:1.117.1.11",
        e });

      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.matching.MessageProcessorMatching",
            "1:3064:1.117.1.11",
            e },
          null),
        e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeApplicationSignatureMatchTarget");
  }

  private class DiscriminatorMatchSpaceKey implements MatchSpaceKey
  {
    private String value;

    /** Insert a name/value pair in the DiscriminatorMatchSpaceKey */
    public DiscriminatorMatchSpaceKey(String discriminator)
    {
      value = discriminator;
    }

    // Implement the getIdentifierValue method
    public Object getIdentifierValue(Identifier id, boolean ignoreType)
    {
      return value;
    }

    public Object getIdentifierValue(Identifier id, boolean ignoreType, Object contextValue, boolean returnList)
    {
      return value;
    }

    public Object getRootContext()
    {
      return null;
    }
  }

}
