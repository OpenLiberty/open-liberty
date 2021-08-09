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

// Import files
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.ibm.ws.sib.matchspace.selector.impl.OrdinalPosition;
import com.ibm.ws.sib.matchspace.utils.MatchSpaceConstants;
import com.ibm.ws.sib.matchspace.utils.Trace;
import com.ibm.ws.sib.matchspace.utils.TraceComponent;
import com.ibm.ws.sib.matchspace.utils.TraceUtils;
import com.ibm.ws.sib.matchspace.BadMessageFormatMatchingException;
import com.ibm.ws.sib.matchspace.impl.Matching;
import com.ibm.ws.sib.matchspace.MatchSpaceKey;
import com.ibm.ws.sib.matchspace.MatchTarget;
import com.ibm.ws.sib.matchspace.MatchingException;
import com.ibm.ws.sib.matchspace.Conjunction;
import com.ibm.ws.sib.matchspace.Selector;
import com.ibm.ws.sib.matchspace.EvalCache;
import com.ibm.ws.sib.matchspace.SearchResults;

/**
 * This class matches all expressions not handled byte other Matcher subclasses. <p>
 *
 * Expressions are decomposed into a subexpression graph, where each unique expression has
 * a unique "root" or start node associated with the appropiate MatchTarget.  Rather than
 * maintaining a set of expression trees, common subexpressions are shared between expressions,
 * creating a subexpression graph.  At evaluation time, no subexpression will be evaluated more
 * than once.<p>
 *
 * Common subexpression elimination is handled by the Selector.intern method.  The
 * subexpression nodes are stored in a Dictionary shared by all DifficultMatcher nodes
 * (i.e.a static table).  <p>
 *
 * Evaluation of the node involves the evaulation in turn of each expression root (all of which
 * will return boolean values) and the addition for each such sucessful evalution of the corresponding
 * MatchTarget.
 *
 */
public final class DifficultMatcher extends ContentMatcher
{

  // Standard trace boilerplate
  private static final Class cclass = DifficultMatcher.class;
  private static Trace tc = TraceUtils.getTrace(DifficultMatcher.class,
      MatchSpaceConstants.MSG_GROUP_LISTS);

  private static final int INIT_MTTL_SIZE = 0;

  //------------------------------------------------------------------------------
  // Class DifficultMatcher.MatchTargetTypeList
  //------------------------------------------------------------------------------
  /** Maintains a list of MatchTargets organized by type.
   * This class is used only locally.
   */ //---------------------------------------------------------------------------
  private class MatchTargetTypeList
  {

    // Each type of MatchTarget is maintained as a
    // vector in the appropriate position in this array.
    List[] lists;

    MatchTargetTypeList()
    {
      lists = new List[INIT_MTTL_SIZE];
    }

    //------------------------------------------------------------------------------
    // Method: DifficultMatcher.MatchTargetTypeList.addTarget
    //------------------------------------------------------------------------------
    /**  Add a MatchTarget to the appropriate type vector in the lists array.
     *
     * Created: 98-10-21
     */
    //---------------------------------------------------------------------------
    public void addTarget(MatchTarget t)
    {

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        tc.entry(this,cclass, "addTarget", "target: " + t);
      int type = t.type();

      resize(type);

      if (lists[type] == null)
        lists[type] = new ArrayList(2);

      t.setIndex(lists[type].size());
      lists[type].add(t);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        tc.exit(this,cclass, "addTarget");
    } //addTarget

    //------------------------------------------------------------------------------
    // Method: DifficultMatcher.MatchTargetTypeList.deleteTarget
    //------------------------------------------------------------------------------
    /** Remove a target from the type list.
     *
     * @return True if the element was found (and removed).  False otherwise.
     * Created: 98-10-21
     */
    //---------------------------------------------------------------------------
    public boolean deleteTarget(MatchTarget t)
    {

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        tc.entry(this,cclass, "addTarget", "target: " + t);
      // The t we are passed is the identical target to the one we are removing.  It
      // contains its own index position.  However, this method may be called with several
      // different lists, only one of which actually contains what we are removing.  So,
      // if we fail any of the tests, we return false rather than complaining.

      int type = t.type();
      boolean result = false;
      if (type < lists.length)
      {
        List set = lists[type];
        if (set != null)
        {
          int index = t.getIndex();
          if (index < set.size() && t == set.get(index))
          {

            // We are indeed removing the element at 'index.'  We remove it by decrementing
            // m_count and moving the last element of the Vector into the hole created by the
            // removal.  Of course, if we are removing the last element there is nothing left to
            // remove.
            if (set.size() == 1)
              set.clear();
            else
            {
              MatchTarget toMove = (MatchTarget) set.get(set.size() - 1);
              toMove.setIndex(index);
              set.set(index, toMove);
              set.remove(set.size() - 1);
            }

            // Now clean up the list of tables
            if (set.isEmpty())
            {
              lists[type] = null;
              // Special case: this was the last vector in the lists.
              // Decrease list size.
              if (type == lists.length - 1)
              {
                int newlen = lists.length;

                // Find new length to be first position
                // without a null vector.
                for (; newlen > 0 && lists[newlen - 1] == null; newlen--);

                if (newlen > 0)
                {
                  List[] list2 = new List[newlen];
                  System.arraycopy(lists, 0, list2, 0, newlen);
                  lists = list2;
                }
                else
                {
                  lists = new List[0];
                }
              }
            }

            result = true;
          }
        }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        tc.exit(this,cclass, "deleteTarget", "result: " + new Boolean(result));
      return result;
    } //deleteTarget

    //------------------------------------------------------------------------------
    // Method: DifficultMatcher.MatchTargetTypeList.getTypeList
    //------------------------------------------------------------------------------
    /** Return a vector with all the elements of a particular type.
     *
     * @return The vector or null if no such vector exists.
     * Created: 98-10-21
     */
    //---------------------------------------------------------------------------
    public List getTypeList(int type)
    {

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        tc.entry(this,cclass, "getTypeList", "type: " + new Integer(type));

      List result = null;
      if (type < lists.length)
        result = lists[type];

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        tc.exit(this,cclass, "getTypeList", "result: " + result);
      return result;
    } //getTypeList

    //------------------------------------------------------------------------------
    // Method: DifficultMatcher.MatchTargetTypeList.resize
    //------------------------------------------------------------------------------
    /** Resize the type list to ensure capacity for a particular type of MatchTarget.
     *
     * Created: 98-10-21
     */ //---------------------------------------------------------------------------
    private void resize(int maxType)
    {

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        tc.entry(this,cclass, "resize", "maxType: " + new Integer(maxType));

      int size = lists.length;
      if (size <= maxType)
      {
        List[] list2 = new List[maxType + 1];
        System.arraycopy(lists, 0, list2, 0, size);
        lists = list2;
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        tc.exit(this,cclass, "resize");

    }

    //------------------------------------------------------------------------------
    // Method: DifficultMatcher.MatchTargetTypeList.size
    //------------------------------------------------------------------------------
    /** Return the size of this MatchTargetTypeList.
     *
     * Created: 98-10-21
     */ //---------------------------------------------------------------------------
    public int size()
    {
      return lists.length;
    } //size

  } // MatchTargetTypeList

  /** Vector of all roots into the subexpression graph.  */
  List roots;

  /** Vector of MatchTargetTypeLists corresponding to the expressions
   * rooted in the roots instance variable.
   **/
  List objs;

  /** The MatchTargetTypeList for all targets with no corresponding expression and are
   * therefore always matched when this node is visited.
   **/
  MatchTargetTypeList alwaysMatch;

  // Statistics
  public static int totalDifficultEntries = 0;

  //------------------------------------------------------------------------------
  // Constructor DifficultMatcher
  //------------------------------------------------------------------------------
  /** Create a new DifficultMatcher node.
   *
   * @param pid ordinal position of this matcher
   **/
  //---------------------------------------------------------------------------
  public DifficultMatcher(OrdinalPosition pid)
  {
    super(pid);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(cclass, "DifficultMatcher", "pid: " + pid);
    roots = new ArrayList(2);
    objs = new ArrayList(2);
    alwaysMatch = new MatchTargetTypeList();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(cclass, "DifficultMatcher", this);

  }

  //------------------------------------------------------------------------------
  // Method: Matcher.put
  //------------------------------------------------------------------------------
  /** Add a new MatchTarget into the tree.
   *
   * @param selector The parsed representation of the content selector.
   * @param object The object to add to the tree.
   * @param targets Array to hold MatchTargets that match the type INIT_STATE_TYPE, for
   * this subscription.  This array can be null. The current implementation will put
   * atmost 1 MatchTarget in the array.
   **/
  //---------------------------------------------------------------------------
  public void put(
    Conjunction selector,
    MatchTarget object,
    InternTable subExpr)
    throws MatchingException
  {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(this,cclass, "put", new Object[] { selector, object, subExpr });

    // The subscription should go into Matcher if there are no SimpleTests in the
    // Conjunction whose ordinal positions are greater in magnitude (lower in the tree)
    // than that of this DifficultMatcher.

    if (selector == null || noEligibleTests(selector))
    {
      Selector expr = (selector == null) ? null : selector.getResidual();
      MatchTargetTypeList mttl;

      if (expr == null)
      {
        mttl = alwaysMatch;
      }
      else
      {
        expr = expr.intern(subExpr);
        totalDifficultEntries = subExpr.size();

        int pos = roots.indexOf(expr);

        // If this expression hasn't been put in the tree previously,
        // then add it in.
        if (pos == -1)
        {
          MatchTargetTypeList tlist = new MatchTargetTypeList();
          mttl = tlist;
          //Add new type list with corresponding expression.
          objs.add(tlist);
          roots.add(expr);
        }
        else
        {
          // Type list for this expression.
          mttl = (MatchTargetTypeList) objs.get(pos);
        }
      }

      // Add element
      mttl.addTarget(object);
    }
    else
      // Continue through this DifficultMatcher onto the rest of the tree.
      super.put(selector, object, subExpr);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(this,cclass, "put");

  }

  // Subroutine: determine whether a Conjunction has no eligible tests that should be
  // positioned below this node in the Matching tree.  This is true if the Conjunction has
  // no simple tests whose ordinal position is numerically greater than parentId.

  private boolean noEligibleTests(Conjunction selector)
  {
    for (int i = 0; i < selector.getSimpleTests().length; i++)
    {
      OrdinalPosition ordPos = (OrdinalPosition) selector.getSimpleTests()[i].getIdentifier().getOrdinalPosition();
      if (ordPos.compareTo( ordinalPosition) > 0)
        return false;
    }
    return true;
  }

  //------------------------------------------------------------------------------
  // Method: DifficultMatcher.get
  //------------------------------------------------------------------------------
  /** Find all matching MatchTargets in the tree.
   *
   * @param msg The message itself.
   * @param result Vector of reults.
   */ //---------------------------------------------------------------------------
  public void get(
    Object rootVal,
    MatchSpaceKey msg,
    EvalCache cache,
    Object contextValue,
    SearchResults result)
    throws MatchingException, BadMessageFormatMatchingException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(
          this,cclass,
        "get",
        "msg: " + msg + ", result: " + result);

    // type never used so removed
    //int type;

    List[] v = alwaysMatch.lists;
    if (v.length > 0)
      result.addObjects(v);

    if (msg != null)
    {
      int numExpr = roots.size();

      for (int current = 0; current < numExpr; current++)
      {
        Boolean res = null;

        // May need to call MFP multiple times, if our context has multiple nodes
        if(contextValue != null)
        {
          // Currently this must be a list of nodes
          if (contextValue instanceof SetValEvaluationContext)
          {
            SetValEvaluationContext evalContext = (SetValEvaluationContext)contextValue;
            
            // Get the node list
            ArrayList wrappedParentList = evalContext.getWrappedNodeList(); 
            // If the list is empty, then we have yet to get the document root
            if(wrappedParentList.isEmpty())
            {
        	    //Set up a root (document) context for evaluation
            	Object docRoot = Matching.getEvaluator().getDocumentRoot(msg);
                
                // Create an object to hold the wrapped node
                WrappedNodeResults wrapper = new WrappedNodeResults(docRoot);
                // Set the root into the evaluation context
                evalContext.addNode(wrapper);
            }
            
            // Iterate over the nodes
            Iterator iter = wrappedParentList.iterator();

            while(iter.hasNext())
            {
              WrappedNodeResults nextWrappedNode = (WrappedNodeResults)iter.next();       
              Object nextNode = nextWrappedNode.getNode();
                
                
              // If no cached value we'll need to call MFP
              //TODO: No caching here

              // Call MFP to get the results for this node
              res = 
                (Boolean) Matching.
                  getEvaluator().
                    eval((Selector) roots.get(current),
                         msg,
                         cache,
                         nextNode,
                         true); // Permissive is true  

              if(res !=null && res.booleanValue())
              {
                break;
              }     
     
            } // eof while
          } // eof instanceof XPathEvaluationContext
        }
        else
        {
          res = (Boolean) Matching.getEvaluator().eval(
              (Selector) roots.get(current),
              msg,
              cache,
              contextValue,
              false);            
        }
        
        if (res != null && res.booleanValue())
        {
          MatchTargetTypeList tlist = (MatchTargetTypeList) objs.get(current);
          v = tlist.lists;
          if (v.length > 0)
            result.addObjects(v);
        }
      }
    }
    // ContinueBranch has to be last to treat this node as if it was at the end of
    // a long chain of *, or a #.
    super.get(null, msg, cache, contextValue, result);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(this,cclass, "get");

  }

  //------------------------------------------------------------------------------
  // Method: DifficultMatcher.remove
  //------------------------------------------------------------------------------
  /** Remove a subscription from the matching tree.<p>
   *
   *
   * @param selector The parsed representation of the content selector.
   * @param object The object to add to the tree.
   * @return the Matcher that should replace this Matcher in its parent
   * Created: 98-08-10
   **/
  //---------------------------------------------------------------------------
  public ContentMatcher remove(
    Conjunction selector,
    MatchTarget object,
    InternTable subExpr,
    OrdinalPosition parentId)
    throws MatchingException
  {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(
          this,cclass,
        "remove",
        "selector: " + selector + ", object: " + object);

    if (objs == null)
      throw new IllegalStateException();

    // type removed as it is never used
    //int type = object.type();

    if (selector == null || noEligibleTests(selector))
    {
      int len = objs.size();

      boolean removed = false;
      // Check the list of MatchTargets for each subscription.
      for (int i = 0; !removed && i < len; i++)
      {
        MatchTargetTypeList tlist = (MatchTargetTypeList) objs.get(i);

        removed = tlist.deleteTarget(object);

        if (removed)
        {
          Selector node = (Selector) roots.get(i);

          if (node == null)
            throw new IllegalStateException();

          // Remove the node subscription from the subExpr table
          node.unintern(subExpr);

          if (tlist.size() == 0)
          {
            objs.remove(i);
            roots.remove(i);
          }
        }
      }

      if (!removed)
      {
        // If the subscription wasn't found anywhere else, remove it
        // from the alwaysMatch list.  If its not there, we have an error.
        removed = alwaysMatch.deleteTarget(object);
        if (!removed)
          throw new IllegalStateException();
      }
    }
    else
      // Case where subscription was not entered in this node, but simply passed through
      // to nodes lower on the tree.
      super.remove(selector, object, subExpr, ordinalPosition);

    // Tell our parent that this node should be deleted
    // if there are no subscriptions left in it.
    ContentMatcher result = this;
    if (roots.size() == 0 && alwaysMatch.size() == 0)
      result = vacantChild;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(this,cclass, "remove", "result: " + result);

    return result;
  }
  
  // Override the hasTests method to answer according to whether this DifficultMatcher
  // is a leaf containing only alwaysMatch cases (in which case it has no 'tests').
  boolean hasTests() {
    return roots.size() > 0 || vacantChild != null;
  }
} // DifficultMatcher
