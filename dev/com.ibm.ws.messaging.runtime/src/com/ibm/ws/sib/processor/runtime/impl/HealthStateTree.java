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
package com.ibm.ws.sib.processor.runtime.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPException;
import com.ibm.ws.sib.processor.impl.interfaces.HealthStateListener;
import com.ibm.ws.sib.processor.runtime.HealthState;
import com.ibm.ws.sib.utils.ras.SibTr;

public class HealthStateTree implements HealthStateListener
{
  
  private HashMap<Integer, HealthState> states;
  private int state;
  private Integer reason = null;
  private boolean isLeaf;
  private HealthStateTree parent;
  private String[] inserts;
  
  private static final TraceComponent tc =
    SibTr.register(
      HealthStateTree.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
  
  public HealthStateTree() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "HealthStateTree");
 
    state = HealthState.GREEN;
    reason = HealthStateListener.OK_STATE;
    isLeaf = false;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "HealthStateTree", this);
  }

  private HealthStateTree(Integer reason, int state, HealthStateTree parent, String[] inserts) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "HealthStateTree", new Object[]{reason,Integer.valueOf(state),parent, Arrays.toString(inserts)});
    this.state = state;
    this.reason = reason;
    this.isLeaf = true;
    this.parent = parent;
    this.inserts = inserts;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "HealthStateTree", this);
  }

  public synchronized int getState() 
  {    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "getState");
    
    int worstState = getWorstState().state;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getState", Integer.valueOf(worstState));
    
    return worstState;
  }
  
  private HealthStateTree getWorstState() 
  {    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "getWorstState");
    
    // We find the worst state in any of these three places:
    //  1) The state of our parent, or their parent, etc.
    //     (this doesn't take into account the state of the parent's children,
    //      only their direct state).
    //  2) This node's state.
    //  3) The state of any of our children, or our children's children, etc.
    
    // First get the worst state from any parent health node (not including the parent's children)
    HealthStateTree currentState = null;
    if (parent!=null)
      currentState = parent.getParentState();
    // Otherwise, start from a green state
    else 
      currentState = new HealthStateTree();
    
    // If we have states (which may be leaf states of this node or child nodes with their
    // own leaves/children) iterate over them, finding the worst state amongst them
    if (states!=null)
    {
      Iterator it = states.values().iterator();
      while (it.hasNext())
      {
        // Compare the current worst state against this child. The worst one will be returned
        currentState = ((HealthStateTree)it.next()).getChildState(currentState);
      }
    }
    // If we have no states we must be a leaf, so compare the current worst state (possibly our parent)
    // against ourselves
    else if (currentState.compareTo(this) > 0)
      currentState = this;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getWorstState", currentState);
    
    return currentState;
  }
  
  private synchronized HealthStateTree getParentState()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "getParentState");
    
    // Keep moving up the tree, picking the worst state from our immediate relations (not their children)
    HealthStateTree currentState = null;
    if (parent!=null)
      currentState = parent.getParentState();
    // Or, if we're the root, start green
    else 
      currentState = new HealthStateTree();
    
    // If we have any leaves, find the worst one.
    if (states!=null)
    {
      // Get the worst health state in our list
      Iterator it = states.values().iterator();
      while (it.hasNext())
      {
        HealthStateTree nextState = (HealthStateTree)it.next();

        // Only consider the parent's state, not its children (we're only interested in the
        // current branch)
        if(nextState.isLeaf())
          currentState = nextState.getChildState(currentState);
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getParentState", currentState);
    return currentState;
  }
  
  private synchronized HealthStateTree getChildState(HealthStateTree currentState)
  {    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "getChildState", currentState);
    
    // If we're not a leaf we may have children or leaves hung from us, so walk our way
    // down ths branch, finding the worst state amongst them
    if (states!=null) 
    {
      // Get the worst health state in our list
      Iterator it = states.values().iterator();
      while (it.hasNext())
      {
        currentState = ((HealthStateTree)it.next()).getChildState(currentState);
      }
    }
    // Otherwise, we're a leaf, so see if we're the worst one so far
    else
    {
      if(this.state != HealthState.GREEN)
      {
        if (currentState.compareTo(this) > 0)
          currentState = this;
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getChildState", currentState);
    
    return currentState;
  }
  
  public synchronized String getHealthReason(Locale l)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getHealthReason", l);
    
    HealthStateTree worstStateObj = getWorstState();
    int worstState = worstStateObj.state; 
    Integer worstReason = worstStateObj.reason;
    String[] inserts = worstStateObj.inserts;
    
    String nlsMessage = 
      TraceNLS.getFormattedMessage(SIMPConstants.RESOURCE_BUNDLE,
                                   "HEALTHSTATE_STATUS_CWSIP09"+worstState+worstReason,
                                   l,
                                   inserts,
                                   "HEALTHSTATE_STATUS_CWSIP09"+worstState+worstReason);

    // If no msg found in nls then it returns what we gave it
    if (nlsMessage.equals("HEALTHSTATE_STATUS_CWSIP09"+worstState+worstReason))
    {
      SIMPException e = new SIMPException("Invalid health state " + worstState + ", " + worstReason);
      // Invalid state/reason combination
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.runtime.HealthStateTree.getHealthReason",
        "1:232:1.11",
        this);

      SibTr.exception(tc, e);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getHealthReason", nlsMessage);
    
    return nlsMessage;    
  }

  
  public void updateHealth(Integer key, int state)
  {
    updateHealth(key, state, null);
  }
    
  public synchronized void updateHealth(Integer key, int state, String[] inserts)
  {     
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "updateHealth", new Object[] {key,
                                                          Integer.valueOf(state),
                                                          Arrays.toString(inserts)});
    if (states == null)
      states = new HashMap<Integer,HealthState>();
    
    HealthStateTree updatingState = (HealthStateTree)states.get(key);
    if (updatingState != null)
    {
      if (updatingState.isLeaf())
      {
        if(state == HealthState.GREEN)
          states.remove(key);
        else
          updatingState.updateLeafHealth(key, state, inserts);
      }
      else
        updatingState.updateHealth(key, state, inserts);
    }
    else if(state != HealthState.GREEN) // Adds a new leaf node (ignore GREEN ones, that's implied)
      states.put(key, new HealthStateTree(key, state, this, inserts));
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "updateHealth", this);
  }
  
  private boolean isLeaf() {
    return isLeaf;
  }

  private synchronized void updateLeafHealth(Integer key, int state, String[] inserts)
  { 
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this, tc, "updateLeafHealth", new Object[]{key, Integer.valueOf(state), Arrays.toString(inserts)});
      SibTr.exit(tc, "updateLeafHealth");
    }
    this.state = state;
    this.reason = key;
    this.inserts = inserts;
  }

  public synchronized void addHealthStateNode(HealthState state)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "addHealthStateNode", state);

    if (states == null)
      states = new HashMap<Integer,HealthState>();
    
    HealthStateTree updatingState = (HealthStateTree)states.get(state.hashCode());
    if (updatingState == null) 
    {
      ((HealthStateTree)state).setParent(this);
      states.put(state.hashCode(), state);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addHealthStateNode");
    
  }
  
  public synchronized void register(Integer key)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "register", key);
    if (states!=null)
      states.put(key, new HealthStateTree());
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "register");
  }
  
  public synchronized void deregister(Integer key)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "deregister", key);
    if (states!=null)
      states.remove(key);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "deregister");
  }
    
  /**
   * greater than zero : This state is healthier than state o
   * less than zero    : This state is worse than state o
   * zero              : This state is comparable to o
   * 
   * @param o
   * @return
   */
  public int compareTo(HealthState o) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "compareTo", new Object[] {this, o});
    
    int equal = 0;
    
    // A greater state value means a healthier state, so a 
    if (state > ((HealthStateTree)o).state)
      equal = 1; // This is the healthier state
    else if (((HealthStateTree)o).state > state)
      equal = -1; // o is the healthier state
    // If the states are the same (e.g. both AMBER), then we need to work out which is the
    // worse reason for being in this state
    else
    {
      // If the reason's match, return zero
      if(reason.intValue() == ((HealthStateTree)o).reason.intValue())
        equal = 0;
      // The this reason has a greater value that o's reason, this state is worse than o's
      else if(HealthStateListener.orderedReasons[state][reason] >
              HealthStateListener.orderedReasons[state][((HealthStateTree)o).reason])
        equal = -1;
      // Otherwise, o's reason is worse than us
      else
        equal = 1;
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "compareTo", equal);
    return equal;
  }
  
  private void setParent(HealthStateTree state)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this,tc, "setParent", state);
      SibTr.exit(tc, "setParent");
    }
    this.parent = state;
  }
  
  @Override
  public String toString() {
    StringBuilder output = new StringBuilder("<HealthStateTree@"+Integer.toHexString(this.hashCode()));
    
    boolean closer = false;
    boolean impliedGreen = true;
    if (parent!=null)
    {
      output.append(parent.parentToString());
      closer = true;
    }
    if (states != null)
    {
      Iterator leaves = states.keySet().iterator();
      while (leaves.hasNext())
      {
        impliedGreen = false;
        closer = true;
        Integer key = (Integer)leaves.next();
        HealthStateTree s = (HealthStateTree)states.get(key);
        if (s.isLeaf())
        {
          output.append("\n<Leaf@"+Integer.toHexString(s.hashCode())+ " Leaf : ");
          output.append(key);
          output.append(" State : ");
          output.append(stateToString(s.state));
          output.append(" reason : ");
          output.append(s.reason);
          output.append(" inserts : ");
          output.append(Arrays.toString(s.inserts) + ">");
        }
        else
        {
          output.append(s.childToString());
        }
      }
    }
    if(isLeaf())
    {
      impliedGreen = false;
      if(parent!=null)
        output.append("\n<NodeState");
      output.append(" State : ");
      output.append(stateToString(state));
      output.append(", Reason : ");
      output.append(reason);
      output.append(" inserts : ");
      output.append(Arrays.toString(inserts) + ">");
      if(parent!=null)
        output.append(">");
    }
    
    if(impliedGreen)
    {
      if(parent!=null)
        output.append("\n<NodeState");
      output.append(" State: GREEN, Reason: OK");
      if(parent!=null)
        output.append(">");
    }
    
    if(closer)
      output.append("\n\\HealthStateTree>");
    else
      output.append(">");
    
    return output.toString();
  }

  
  private String childToString() {
    StringBuilder output = new StringBuilder("\n<Child@"+Integer.toHexString(this.hashCode()));

    boolean impliedGreen = true;
    if (states != null)
    {
      Iterator leaves = states.keySet().iterator();
      while (leaves.hasNext())
      {
        Integer key = (Integer)leaves.next();
        HealthStateTree s = (HealthStateTree)states.get(key);
        impliedGreen = false;
        if (s.isLeaf())
        {          
          output.append("\n<Leaf@"+Integer.toHexString(s.hashCode()));
          output.append(" State: ");
          output.append(stateToString(s.state)); 
          output.append(", Reason: ");
          output.append(s.reason);
          output.append(" inserts : ");
          output.append(Arrays.toString(s.inserts) + ">");
        }
        else
        {
          output.append(s.childToString());
        }
      }
    }
    
    if(impliedGreen)
      output.append(" State: GREEN, Reason: OK>");
    else
      output.append("\n\\Child>");
    
    return output.toString();
  }

  private String parentToString()
  {
    StringBuilder output = new StringBuilder("\n<Parent@"+Integer.toHexString(this.hashCode()));
    boolean closer = false;
    if (parent!=null)
    {
      closer = true;
      output.append(parent.parentToString());
    }
    
    boolean impliedGreen = true;
    if (states!=null)
    {
      Iterator leaves = states.keySet().iterator();
      while (leaves.hasNext())
      {
        Integer key = (Integer)leaves.next();
        HealthStateTree s = (HealthStateTree)states.get(key);
        if (s.isLeaf())
        {
          closer = true;
          impliedGreen = false;
          output.append("\n<Leaf@"+Integer.toHexString(s.hashCode()));
          output.append(" State: ");
          output.append(stateToString(s.state)); 
          output.append(", Reason: ");
          output.append(s.reason);
          output.append(" inserts : ");
          output.append(Arrays.toString(s.inserts) + ">");
        }
      }
    }
    if(impliedGreen)
    {
      if(parent!=null)
        output.append("\n<ParentState");
      output.append(" State: GREEN, Reason: OK");
      if(parent!=null)
        output.append(">");
    }
    
    if(closer)
      output.append("\n\\Parent>");
    else
      output.append(">");
    
    return output.toString();
  }
  
  String stateToString(int state)
  {
    if(state == HealthState.GREEN)
      return "GREEN";
    else if(state == HealthState.AMBER)
      return "AMBER";
    else
      return "RED";
  }
}
