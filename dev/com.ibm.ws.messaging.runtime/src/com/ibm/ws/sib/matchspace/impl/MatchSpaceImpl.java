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
package com.ibm.ws.sib.matchspace.impl;

// Import required classes

import java.io.PrintWriter;

import com.ibm.ws.sib.matchspace.BadMessageFormatMatchingException;
import com.ibm.ws.sib.matchspace.Conjunction;
import com.ibm.ws.sib.matchspace.EvalCache;
import com.ibm.ws.sib.matchspace.Identifier;
import com.ibm.ws.sib.matchspace.MatchSpace;
import com.ibm.ws.sib.matchspace.selector.impl.OrdinalPosition;
import com.ibm.ws.sib.matchspace.utils.FFDC;
import com.ibm.ws.sib.matchspace.utils.MatchSpaceConstants;
import com.ibm.ws.sib.matchspace.utils.Trace;
import com.ibm.ws.sib.matchspace.utils.TraceComponent;
import com.ibm.ws.sib.matchspace.utils.TraceUtils;
import com.ibm.ws.sib.matchspace.MatchSpaceKey;
import com.ibm.ws.sib.matchspace.MatchTarget;
import com.ibm.ws.sib.matchspace.MatchingException;
import com.ibm.ws.sib.matchspace.SearchResults;
import com.ibm.ws.sib.matchspace.Selector;
import com.ibm.ws.sib.matchspace.SimpleTest;

//------------------------------------------------------------------------------
// MatchSpaceImpl class
//------------------------------------------------------------------------------
/**
 * The MatchSpaceImpl is the primary class of the package.
 * 
 * The Matcher and MatchCache classes encapsulated in a MatchSpace are not designed 
 *   to be instantiated on their own.
 **/

public class MatchSpaceImpl implements MatchSpace, MatchCache.RehashFilter
{

  // Standard trace boilerplate
  private static final Class cclass = MatchSpaceImpl.class;
  private static Trace tc = TraceUtils.getTrace(MatchSpaceImpl.class,
      MatchSpaceConstants.MSG_GROUP_LISTS);
  

  // Tuning parameter:  controls the rehash limit of the MatchCache.  MatchSpace endeavors
  // to avoid rehashing by eliminating cache-only entries from the MatchCache via the 
  // RehashFilter interface.
  private static final int MATCH_CACHE_INITIAL_CAPACITY = 10000;

  /** Cache, keyed by values of the root identifier.  Each entry contains
   * <ol><li>The Matcher subtree for ==key matches on the root identifier.
   * <li>A cache of subtrees for non-equality matches on the root identifier that are
   *    consistent with the entry's key.
   * <li>(Where applicable) a cache of results for the last lookup that had the same
   *    key.</ol>
   * The latter two entries are invalidated when the matchTree changes
   * and the result cache is only maintained if none of the subtrees specify tests on
   * any identifier other than the root.
   * <p>If the MatchSpace is instantiated without cacheing, then the cache is never
   * allocated.
   */
  private MatchCache matchCache;

  /** Current matchTree generation, used to determine the validity of cached
   * information.  Generation 0 is the initial state where nothing has been entered in
   * that tree.  Odd generations represent "in process" changes performed by addTarget or
   * removeTarget.  Even generations represent "correct" states of MatchSpace
   **/
  private volatile long matchTreeGeneration = 0;

  /** Root of the main matchTree.  When cacheing is inactive, this contains all the
   * Matchers in MatchSpace.  Otherwise, it contains only subtrees that represent
   * nonEquality tests on the root identifier:  equality tests on the root identifier are
   * kept only in the cache.
   */
  private ContentMatcher matchTree;
  
  /** The root identifier.  This is only present when cacheing is enabled.  The ordinal
   * position of this Identifier <em>must</em> be zero for correct operation at present.
   **/
  Identifier rootId;

  /** InternTable for use by Selector.intern so that DifficultMatcher subexpressions can
   * be uniquely identified and cached.  There is one of these for all the
   * DifficultMatcher nodes in MatchSpace.
   **/
  InternTable subExpr = new InternTable();

  /** Statistical counters.  These are for performance testing purposes. */
  private int exactPuts;
  private int wildPuts;
  private int resultCacheHitGets;
  private int wildCacheHitGets;
  private int wildCacheMissGets;
  private int exactMatches;
  private int resultsCached;
  private int removals;
  private int cacheCreates;
  private int cacheRemoves;
  private int optimisticGets;
  private int pessimisticGets;
  private int puntsDueToCache;

  //------------------------------------------------------------------------------
  // MatchSpace.CacheEntry class
  //------------------------------------------------------------------------------
  /** This class serves as a record holding various kinds of cacheable information about a
   * particular value of the root identifier.  These records are stored in the matchCache
   * Hashtable */

  private class CacheEntry
  {
    // The root of the match subtree for == this rootId value
    ContentMatcher exactMatcher;
    
    // Counts changes to exactMatcher using the same odd/even discipline as
    // matchTreeGeneration
    volatile long exactGeneration;

    // Roots of match subtrees for non-equality tests on the root Id that are consistent
    // with this rootId value.
    ContentMatcher[] otherMatchers;

    // The incarnation of matchTree at which otherMatches and cached results were last
    // valid.
    long matchTreeGeneration;

    // Processed search results from the last search with the same value of rootId
    // iff results cacheing is enabled for this entry
    Object cachedResults;

    // True if result cacheing is disabled, which will be the case if any targets
    // with this rootId value specify the values of any Identifiers other than rootId
    boolean noResultCache;
  }

  //------------------------------------------------------------------------------
  // Constructor for a MatchSpace
  //------------------------------------------------------------------------------
  /** Construct a new MatchSpace
   * @param rootId the Identifier to place at the root of the Matching hierarchy.
   * @param enableCache if true, cacheing will be enabled using values of rootId as
   *    a key.  Otherwise, there will be no cacheing.  RootId must have ordinalPosition
   *    == 0 and must be of a type that supports equality tests (not BOOLEAN).
   */
  public MatchSpaceImpl(Identifier rootId, boolean enableCache)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(cclass, "MatchSpaceImpl", new Object[] {rootId, new Boolean(enableCache)});

    initialise(rootId, enableCache);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(cclass, "MatchSpaceImpl", this);
  }
  
  /**
   * Initialise a newly created MatchSpace
   * 
   * @param rootId
   * @param enableCache
   */
  public void initialise(Identifier rootId, boolean enableCache)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(cclass, "initialise", new Object[] {rootId, new Boolean(enableCache)});

    switch (rootId.getType()) {
      case Selector.UNKNOWN :
      case Selector.OBJECT :
        matchTree = new EqualityMatcher(rootId);
        break;
      case Selector.STRING :
      case Selector.TOPIC :
        matchTree = new StringMatcher(rootId);
        break;
      case Selector.BOOLEAN :
        matchTree = new BooleanMatcher(rootId);
        break;
      default:
        matchTree = new NumericMatcher(rootId);
        break;
    }
    if (enableCache) {
      this.rootId = rootId;
      matchCache = new MatchCache(MATCH_CACHE_INITIAL_CAPACITY);
      matchCache.setRehashFilter(this);
      ((EqualityMatcher) matchTree).setCacheing(true);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(cclass, "MatchSpaceImpl", this);
  }

  //------------------------------------------------------------------------------
  // Method: MatchSpace.addTarget
  //------------------------------------------------------------------------------
  /** Adds a Conjunction to the space and associates a MatchTarget with it.
   *
   * @param conjunction the Conjunction
   *
   * @param target the MatchTarget
   * 
   * @exception MatchingException thrown for serious errors, including an ill-formed
   * conjunction.  The Conjunction will be well-formed if it was constructed from a 
   * syntactically valid expression using a sound parser and Resolver.
   **/

  public synchronized void addTarget(
    Conjunction conjunction,
    MatchTarget object)
    throws MatchingException
  {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(
        this,cclass,
        "addTarget",
        new Object[] { conjunction, object });
    // Deal with Conjunctions that test equality on rootId when cacheing is enabled
    if (rootId != null) 
    {
      // Cacheing is enabled.
      OrdinalPosition rootOrd = new OrdinalPosition(0,0);      
      SimpleTest test = Factory.findTest(rootOrd, conjunction);
      if (test != null && test.getKind() == SimpleTest.EQ) {
        // This is an equality test, so it goes in the cache only.      
        CacheEntry e = getCacheEntry(test.getValue(), true);
        e.exactGeneration++; // even-odd transition: show we are changing it
        ContentMatcher exact = e.exactMatcher;
        e.exactMatcher = exact = Factory.createMatcher(rootOrd, conjunction, exact);
        e.cachedResults = null;
        try
        {
          exact.put(conjunction, object, subExpr);
          e.noResultCache |= exact.hasTests();
        }
        catch (RuntimeException exc)
        {
          // No FFDC Code Needed.
          // FFDC driven by wrapper class.
          FFDC.processException(this,
              cclass,
              "com.ibm.ws.sib.matchspace.impl.MatchSpaceImpl.addTarget",
              exc,
              "1:303:1.44");          
//TODO:          tc.exception(tc, exc);
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            tc.exit(this,cclass, "addTarget", e);
          throw new MatchingException(exc);
        }
        finally
        {
          e.exactGeneration++; // odd-even transition: show change is complete
        }
        exactPuts++;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          tc.exit(this,cclass, "addTarget");
        return;
      }
    }
    // Either cacheing is not enabled or this isn't an equality test on rootId.
    matchTreeGeneration++; // even-odd transition: show we are changing it
    try
    {
      matchTree.put(conjunction, object, subExpr);
    }
    catch (RuntimeException e)
    {
      // No FFDC Code Needed.
      // FFDC driven by wrapper class.
      FFDC.processException(this,
          cclass,
          "com.ibm.ws.sib.matchspace.impl.MatchSpaceImpl.addTarget",
          e,
          "1:333:1.44");               
//TODO:       tc.exception(tc, e);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        tc.exit(this,cclass, "addTarget", e);  
      throw new MatchingException(e);
    }
    finally
    {
      matchTreeGeneration++;
      /* odd-even transition: show change is complete.  Also
         invalidates non-equality information in the cache */
    }
    wildPuts++;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(this,cclass, "addTarget");
  }

  //------------------------------------------------------------------------------
  // Method: MatchSpace.getCacheEntry
  //------------------------------------------------------------------------------
  /** Gets the appropriate CacheEntry for a value of the root Identifier
   *
   * @param value the value whose CacheEntry is desired
   *
   * @param create if true, the CacheEntry is created (empty) if it doesn't already exist.
   * This should only be done in synchronized methods.
   **/

  private CacheEntry getCacheEntry(Object value, boolean create)
  {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(
        this,cclass,
        "getCacheEntry",
        new Object[] { value, new Boolean(create), matchCache });

    CacheEntry e = (CacheEntry) matchCache.get(value);
    if (e == null)
    {
      if (create)
      {
        e = new CacheEntry();
        // The following method call may stimulate multiple callbacks to the shouldRetain
        // method if the Hashtable is at the rehash threshold.
        matchCache.put(value, e);
        cacheCreates++;
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(this,cclass, "getCacheEntry", e);

    return e;
  }

  // Implementation of MatchCache.RehashTable.shouldRetain.  Decides whether to retain
  // a cache entry when the cache is too full and some entries must be shed.  We can shed
  // entries that don't have an exactMatcher.

  public boolean shouldRetain(Object key, Object val)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(this,cclass, "shouldRetain", new Object[] { key, val });
    CacheEntry e = (CacheEntry) val;
    if (e.exactMatcher != null)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        tc.exit(this,cclass, "shouldRetain", Boolean.TRUE);
      return true;
    }
    cacheRemoves++;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(this,cclass, "shouldRetain", Boolean.FALSE);
    return false;
  }

  //------------------------------------------------------------------------------
  // Method: MatchSpace.search
  //------------------------------------------------------------------------------
  /** Gets a the result of a matching traversal for a Message.  This method should be used
   * when you want to get the results of a traversal.  This method is designed to be
   * called while NOT holding the MatchSpace lock.  The lock is acquired internal to
   * its logic if that is found to be necessary.
   * 
   * @param rootValue if non-null, this value is used for the
   *    root Identifier instead of calling the MatchSpaceKey
   * @param msg message to match (based on content).
   * @param cache the EvalCache to use during the match
   * @param result The results of the match are added to this object.
   **/
  public void search(
    Object rootValue,
    MatchSpaceKey msg,
    EvalCache cache,
    SearchResults result)
    throws MatchingException, BadMessageFormatMatchingException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(
        this,cclass,
        "search",
        new Object[] {rootValue, msg, cache, result});

    // Update rootValue from msg if necessary and possible
    if (rootValue == null && rootId != null)
      rootValue = msg.getIdentifierValue(rootId, false, null, false);
    
    // Set up a root (document) context for evaluation. This will be 
    // exploited by XPath selectors.
    SetValEvaluationContext evalContext = new SetValEvaluationContext(); 
 
    // Obtain the relevant cache entry.  If it does not exist, we immediately punt to
    // pessimisticGet.  If cacheing is inactive we effectively skip this step.
    CacheEntry e = null;
    if (rootId != null) {
      e = getCacheEntry(rootValue, false);
      if (e == null)
      {
        puntsDueToCache++;
        pessimisticGet(rootValue, msg, cache, evalContext, result);
  
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          tc.exit(this,cclass, "search");
  
        return;
      }
    }

    // Read the two volatile generation counters which, together, express the current
    // version of the subscription set under this rootId value.  The two are (1) the
    // matchTree generation counter, (2) the exact generation counter for the CacheEntry e.

    long wildG = matchTreeGeneration;
    long exactG = (e == null) ? 0 : e.exactGeneration;

    // If the present version is unstable, do pessimisticGet.  The present version is
    // unstable if any of the generation counters is odd.
    if ((wildG & 1) == 1 || (exactG & 1) == 1)
    {
      if (e == null)
        // uncached case:  trivial logic, bypass pessimisticGet
        synchronized(this) {
          matchTree.get(rootValue, msg, cache, evalContext, result);
        }
      else
        pessimisticGet(rootValue, msg, cache, evalContext, result);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        tc.exit(this,cclass, "search");
      return;
    }

    // If the cache will have to be updated, we do pessimisticGet.  The cache will have to
    // be updated if (1) its otherMatchers are invalid or (2) it is still eligible for
    // result caching and its result cache is invalid (assuming cacheing is active
    // at all).

    if (e != null && (wildG != e.matchTreeGeneration
      || ((!e.noResultCache) && (e.cachedResults == null))))
    {
      puntsDueToCache++;
      pessimisticGet(rootValue, msg, cache, evalContext, result);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        tc.exit(this,cclass, "search");

      return;
    }

    // Looks like we can do this get optimistically.  Before continuing, we synchronize
    // vacuously just for the side-effect of forcing a read barrier.  We want to ensure
    // that any variable we read hereafter cannot show a value that the processor cached
    // before we read the volatile generation counters.  We separately ensure that all
    // even-odd transitions of the generation counters appear in memory before updating
    // the associated structure and odd-even transitions appear after such an update.

    synchronized (result)
    {
    }

    // An optimistic get can conceivably get an exception due only to MatchSpace being in
    // an inconsistent state.  We guard against that by using try/catch.

    Throwable oops = null;
    try
    {

      // Look in the cache for a usable cachedResults, using them to satisfy the request
      // iff the results object is willing to accept them.
      if (e != null && e.cachedResults != null && result.acceptCacheable(e.cachedResults))
      {
        // We are actually done with the match, but we need to do the final version check
        // below to make sure the answer is valid.
        resultCacheHitGets++;
      }
      else
      { // result cache not valid
        // Prepare the EvalCache, since we'll be running real matchers.
        cache.prepareCache(subExpr.evalCacheSize());
        if (e == null)
          // No cacheing.  Just run the matchTree
          matchTree.get(rootValue, msg, cache, evalContext, result);
        else {
          // Cacheing is active.
          // Run the other matchers, if any.  We already know that the otherMatchers cache
          // is valid, so we don't look in the real matchTree at all.
          if (e.otherMatchers != null)
          {
            for (int i = 0; i < e.otherMatchers.length; i++)
              e.otherMatchers[i].get(null, msg, cache, evalContext, result);
            wildCacheHitGets++;
          }
          // Now do exact match (if any)
          if (e.exactMatcher != null)
          {
            e.exactMatcher.get(null, msg, cache, evalContext, result);
            exactMatches++;
          } 
        } // end else "Cacheing is active"
      } // end else "result cache not valid"
    }
    catch (RuntimeException exc)
    {
      // No FFDC code needed
      // FFDC driven by wrapper class.
        
//TODO:      tc.exception(tc, exc);
        
      // We don't yet know if this exception is "real" or due to inconsistency
      oops = exc;
    }
    catch (Error exc)
    {
      // No FFDC code needed
      // FFDC driven by wrapper class.

//TODO:      tc.exception(tc, exc);
       
     // We don't yet know if this exception is "real" or due to inconsistency
      oops = exc;
    }

    // Reevaluate the version numbers to make sure nothing changed under us.
    if (wildG != matchTreeGeneration || (e != null && exactG != e.exactGeneration))
    {
      result.reset(); // discard results.  They can't be trusted.
      if (e == null)
        synchronized(this) {
          matchTree.get(rootValue, msg, cache, evalContext, result);
        }
      else
        pessimisticGet(rootValue, msg, cache, evalContext, result);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        tc.exit(this,cclass, "search");
      return;
    }

    // The result of this optimistic get are "valid."  Now make sure there was no
    // exception.
    if (oops != null)
    { 
      // FFDC
      FFDC.processException(this,
          cclass,
          "com.ibm.ws.sib.matchspace.impl.MatchSpaceImpl.search",
          oops,
          "1:604:1.44");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        tc.exit(this,cclass, "search", oops);   
      throw new MatchingException(oops.toString());
    }

    optimisticGets++;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(this,cclass, "search");
  }

  // The pessimistic form of get, which synchronizes with put and remove.  This is called
  // when the normally optimistic version is (1) unable to find a consistent view due to
  // concurrent change activity, or (2) determines that some update must be made to the
  // cache.  Cacheing is always active when this method is called (the uncached case is
  // handled without ever calling this routine).
  private synchronized void pessimisticGet(
    Object key,
    MatchSpaceKey msg,
    EvalCache cache,
    Object rootContext,
    SearchResults result)
    throws MatchingException, BadMessageFormatMatchingException
  {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(
        this,cclass,
        "pessimisticGet", new Object[] {key,msg,cache,result});

    // Everything must execute under the MatchSpace lock
    // Obtain the cache entry and determine if its otherMatchers and results entries are
    // usable, invalidating them if not.
    CacheEntry e = getCacheEntry(key, true);
    if (e.matchTreeGeneration != matchTreeGeneration)
    {
      e.cachedResults = null;
      e.otherMatchers = null;
    }
    // Look in the cache for a usable cachedResults, using them to satisfy the request
    // iff the results object is willing to accept them.
    if (e.cachedResults != null && result.acceptCacheable(e.cachedResults))
    {
      resultCacheHitGets++;
      pessimisticGets++;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        tc.exit(this,cclass, "pessimisticGet");
      return;
    }
    // Prepare the EvalCache, since we will be passing the msg through at least some
    // matchers.
    cache.prepareCache(subExpr.evalCacheSize());
    // Next, look in cache for reusable otherMatchers, to avoid searching
    // the full matchTree.
    if (e.otherMatchers != null)
    {
      // Have a cache hit that covers other matchers, so just run them.
      for (int i = 0; i < e.otherMatchers.length; i++)
      {
        try
        {
          e.otherMatchers[i].get(null, msg, cache, rootContext, result);
        }
        catch (RuntimeException exc)
        {
          // No FFDC Code Needed.
          // FFDC driven by wrapper class.
          FFDC.processException(this,
              cclass,
              "com.ibm.ws.sib.matchspace.impl.MatchSpaceImpl.pessimisticGet",
              exc,
              "1:677:1.44");      
//TODO:          tc.exception(tc, exc);
           
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            tc.exit(this,cclass, "pessimisticGet", exc);
          throw new MatchingException(exc);
        }
      }
      wildCacheHitGets++;
    }
    else
      // Have to do wildcards the slow way
      if (matchTree != null)
      {
        CacheingSearchResults csr = new CacheingSearchResults(result);
        try
        {
          matchTree.get(key, msg, cache, rootContext, csr);
        }
        catch (RuntimeException exc)
        {
          // No FFDC Code Needed.
          // FFDC driven by wrapper class.
          FFDC.processException(this,
              cclass,
              "com.ibm.ws.sib.matchspace.impl.MatchSpaceImpl.pessimisticGet",
              exc,
              "1:704:1.44");               
//TODO:          tc.exception(tc, exc);
            
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            tc.exit(this,cclass, "pessimisticGet", exc);
          throw new MatchingException(exc);
        }
        e.otherMatchers = csr.getMatchers();
        e.matchTreeGeneration = matchTreeGeneration;
        e.noResultCache |= csr.hasContent;
        wildCacheMissGets++;
      }
    // Now do exact match (if any)
    if (e.exactMatcher != null)
    {
      try
      {
        e.exactMatcher.get(null, msg, cache, rootContext, result);
      }
      catch (RuntimeException exc)
      {
        // No FFDC Code Needed.
        // FFDC driven by wrapper class.
        FFDC.processException(this,
            cclass,
            "com.ibm.ws.sib.matchspace.impl.MatchSpaceImpl.pessimisticGet",
            exc,
            "1:731:1.44");            
//TODO:        tc.exception(tc, exc);
          
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          tc.exit(this,cclass, "pessimisticGet", exc);
        throw new MatchingException(exc);
      }
      exactMatches++;
    }
    // If we are able, place something in the cache for next time
    if (!e.noResultCache)
    {
      e.cachedResults = result.provideCacheable(key);
      if (e.cachedResults != null)
        resultsCached++;
    }
    // And we are done
    pessimisticGets++;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(this,cclass, "pessimisticGet");
    return;
  }

  //------------------------------------------------------------------------------
  // Method: MatchSpace.removeTarget
  //------------------------------------------------------------------------------
  /**Removes a conjunction/MatchTarget pair from the MatchSpace
   *
   * @param conjunction the conjunction identifying what is to be removed
   * 
   * @param target the MatchTarget to be removed
   *
   * @exception MatchingException on serious error
   **/

  public synchronized void removeTarget(
    Conjunction conjunction,
    MatchTarget target)
    throws MatchingException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(this,cclass, "removeTarget", new Object[] { conjunction, target, matchCache });
    OrdinalPosition rootOrd = new OrdinalPosition(0,0);
    SimpleTest test = Factory.findTest(rootOrd, conjunction);
    if (rootId == null || test == null || test.getKind() != SimpleTest.EQ)
    {
      // removal of non-equality on rootId or removal with cacheing disabled
      if (matchTree == null)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          tc.exit(this,cclass, "removeTarget", "matchTree == null");
        throw new MatchingException();
      }
      matchTreeGeneration++;
      try
      {
        matchTree.remove(conjunction, target, subExpr, rootOrd);
      }
      catch (RuntimeException e)
      {
        // No FFDC Code Needed.
        // FFDC driven by wrapper class.
        FFDC.processException(this,
            cclass,
            "com.ibm.ws.sib.matchspace.impl.MatchSpaceImpl.removeTarget",
            e,
            "1:798:1.44");          
//TODO:        tc.exception(tc, e);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          tc.exit(this,cclass, "removeTarget", e);  
        throw new MatchingException(e);
      }
      finally
      {
        matchTreeGeneration++;
      }
    }
    else
    {
      // removal of equality match on rootId with cacheing active
      CacheEntry e = (CacheEntry) matchCache.get(test.getValue());

      if (e == null || e.exactMatcher == null)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          tc.exit(this,cclass, "removeTarget", "MatchingException");
        throw new MatchingException();
      }
      e.exactGeneration++;

      try
      {
        e.exactMatcher =
          e.exactMatcher.remove(conjunction, target, subExpr, rootOrd);
      }
      catch (RuntimeException exc)
      {
        // No FFDC Code Needed.
        // FFDC driven by wrapper class.
        FFDC.processException(this,
            cclass,
            "com.ibm.ws.sib.matchspace.impl.MatchSpaceImpl.removeTarget",
            exc,
            "1:835:1.44");   
//TODO:        tc.exception(tc, exc);
          
        e.exactGeneration++; // just in case, tho this is probably terminal
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          tc.exit(this,cclass, "removeTarget", "MatchingException");
        throw new MatchingException(exc);
      }

      if (e.exactMatcher == null)
      {
        if (e.otherMatchers == null
          || e.matchTreeGeneration != matchTreeGeneration
          || e.otherMatchers.length == 0)
        {
          // This entry can be safely removed and isn't serving an obvious purpose
          matchCache.remove(test.getValue());
          cacheRemoves++;
        }
      }
      e.cachedResults = null; // in any case, result cache is now invalid
      e.exactGeneration++;
    }
    removals++;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(this,cclass, "removeTarget");
  }

  //------------------------------------------------------------------------------
  // Method: MatchSpace.statistics();
  //------------------------------------------------------------------------------
  /** Only used when doing isolated performance testing of the MatchSpace. */

  public void statistics(PrintWriter wtr)
  {
    int truePessimisticGets = pessimisticGets - puntsDueToCache;
    wtr.println(
      "Exact puts: "
        + exactPuts
        + ", Wildcard generation: "
        + matchTreeGeneration
        + ", Wildcard puts: "
        + wildPuts
        + ", Wildcard-Cache-hit gets: "
        + wildCacheHitGets
        + ", Wildcard-Cache-miss gets: "
        + wildCacheMissGets
        + ", Result-Cache-hit gets: "
        + resultCacheHitGets
        + ", Exact matches: "
        + exactMatches
        + ", Results cached: "
        + resultsCached
        + ", Removals:"
        + removals
        + ", Cache entries created:"
        + cacheCreates
        + ", Cache entries removed:"
        + cacheRemoves
        + ", Optimistic gets:"
        + optimisticGets
        + ", True Pessimistic gets:"
        + truePessimisticGets
        + ", Mutating gets:"
        + puntsDueToCache);
  }

  //------------------------------------------------------------------------------
  // Method: MatchSpace.clear
  //------------------------------------------------------------------------------
  /** Removes all objects from the MatchSpace, resetting it to the 'as new'
   * condition.
   **/

  public synchronized void clear(Identifier rootId, boolean enableCache)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(this,cclass, "clear");
    matchTree = null;
    matchTreeGeneration = 0;
    subExpr.clear();
    
    // Now reinitialise the matchspace
    initialise(rootId, enableCache);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(this,cclass, "clear");
  }

}
