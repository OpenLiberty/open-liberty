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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.exceptions.InvalidFlowsException;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.messagecontrol.Flow;

/**
 * Class to wrap the flows and weightings for the ConsumerSet.
 *
 */
public class JSConsumerClassifications
{ 
  //trace
  private static final TraceComponent tc =
    SibTr.register(
        JSConsumerClassifications.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
  
  /** The Flows associated with this ConsumerSet */
  private Flow[] flows = null;
  
  /** The number of classifications */
  private int numberOfClasses;
  
  /** Pseudo random number */
  private Random rand;  
  
  /** HashMap of classifications and weights provided by XD */
  private HashMap<String, ClassWeight> messageClasses = null;

  /** List of classification names */
  private ArrayList<String> classificationNames = null;

  /** Map of index versus weight. Supports message retrieval by a consumer */
  private HashMap<Integer, Integer> weightMap = null;
  
  public JSConsumerClassifications(String csLabel)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, 
                  "JSConsumerClassifications", csLabel);
    
    numberOfClasses = 0;
    
    // Seed the random number generator
    rand = new Random(System.currentTimeMillis());
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "JSConsumerClassifications", this);    
  }

  /**
   * Supports the setting of a new set of classifications based on a set of
   * Flows provided by the caller.
   * 
   * The method is called by JSConsumerSet.setFlowProperites() under a Java 5 write lock that
   * ensures exclusive access when classifications are being set or changed. The specific lock 
   * taken is JSConsumerSet.classificationWriteLock.
   * 
   * @param flows
   */
  public boolean setClassifications(Flow[] newFlows)
    throws InvalidFlowsException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setClassifications", newFlows);
    boolean noClassChange = true;
    
    // Do we have the same set of flows but with different weightings only?
    if(flows == null || (flows.length != newFlows.length))
    {
      // We know that this is more than a weight change
      noClassChange = false;
    }

    // Set up the new data structures to hold classification information
    ArrayList<String> newClassificationNames = new ArrayList<String>();
    HashMap<String, ClassWeight> newMessageClasses = new HashMap<String, ClassWeight>();
    HashMap<Integer, Integer> newWeightMap = new HashMap<Integer, Integer>();
    
    // Set the new default classification
    newClassificationNames.add(0, "$SIdefault");
 
    // Iterate over the new flows
    for(int i=0;i<newFlows.length;i++)
    {
      // Get the next classification name
      String nextClassificationName = newFlows[i].getClassification();
      if(noClassChange && !classificationNames.contains(nextClassificationName))
      {
        // This classification was not in the old list
        noClassChange = false;
      }
        
      // The zeroth position is reserved for default, non-classified messages
      ClassWeight classWeight = new ClassWeight(i+1, newFlows[i].getWeighting());
      
      // Put the new weight in the messageClasses map
      ClassWeight cw = newMessageClasses.put(nextClassificationName, classWeight);
      // Check for duplicate classification names
      if(cw != null)
      {
        InvalidFlowsException ife = new InvalidFlowsException();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "setClassifications", ife);          
        throw ife;
      }
      newClassificationNames.add(i+1, nextClassificationName);               
      
      // Put the new value in the weightMap
      newWeightMap.put(Integer.valueOf(i+1), Integer.valueOf(newFlows[i].getWeighting()));      
    }
      
    // Have set up the new data structures, reset the old ones.
    classificationNames = newClassificationNames;
    messageClasses = newMessageClasses;
    weightMap = newWeightMap;
    
    // And clone the flows
    this.flows = newFlows.clone();

    // The number of classifications
    numberOfClasses = flows.length;
     
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setClassifications", Boolean.valueOf(noClassChange));  
    return noClassChange;
  }
    
  /**
   * Retrieve the number of classifications specified in the current set.
   * 
   * @return
   */
  public int getNumberOfClasses()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getNumberOfClasses");
      SibTr.exit(tc, "getNumberOfClasses", Integer.valueOf(numberOfClasses));    
    }
    return numberOfClasses;
  } 
    
  /**
   * Get the index of a named classification.
   * 
   * @param classificationName
   * @return
   */
  public int getPosition(String classificationName)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getPosition", classificationName);
    
    int classPos = 0;
    // Get the position of the classification
    ClassWeight cw = messageClasses.get(classificationName);
    if(cw != null)
      classPos = cw.getPosition();
     
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getPosition", Integer.valueOf(classPos));
      
    return classPos;
  }    

  /**
   * Get the weight of a named classification.
   * 
   * @param classificationName
   * @return
   */
  public int getWeight(String classificationName)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getWeight", classificationName);
    
    int weight = 0;
    // Get the weight of the classification
    ClassWeight cw = messageClasses.get(classificationName);
    if(cw != null)
      weight = cw.getWeight();
     
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getWeight", Integer.valueOf(weight));
      
    return weight;
  }      
  
  /**
   * Retrieve the map of the weightings associated with each classification. This method relies on the
   * classifications not being changed under our feet. It is called where a consumer is getting
   * a message under (ultimately) LocalQPConsumerKey.getMessageLocked() which takes the 
   * JSConsumerSet.classificationReadLock.
   * 
   * @return
   */
  public HashMap<Integer, Integer> getWeightings()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getWeightings");
    
    // Clone the map, the caller will start with the full set of classifications from which to choose 
    // a class but will remove that class from its copy of the map if no message with the initial
    // choice is found. 
    HashMap<Integer, Integer> clonedWeightMap = (HashMap<Integer, Integer>) weightMap.clone();    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getWeightings", clonedWeightMap);
      
    return clonedWeightMap;
  }        
  
  /**
   * Find a pseudo random classification index based on the weightings
   * table. 
   * 
   * @param weightMap
   * @return
   */
  public int findClassIndex(HashMap<Integer, Integer> weightMap)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "findClassIndex");
    
    int classPos = 0;
    int randWeight = 0;
    // First of all determine total weight
    int totalWeight = 0;
    Iterator<Integer> iter = weightMap.values().iterator();
     while(iter.hasNext())
     {
      Integer weight = iter.next();
      totalWeight = totalWeight + weight.intValue();
    }
  
    // TotalWeight might be zero if a zero weighting has been applied to a class. In
    // that case we need to return a zero classPos
    if(totalWeight > 0)
    {
      // Generate a random number that is between zero and the total weight
      randWeight = Math.abs(rand.nextInt() % totalWeight);
      
      // Iterate over the weightMap again so that we can find a class index to use.
      // We search for the index that corresponds to the generated random value.
      //
      // So if we had 3 classes with weights 7, 2 and 1, then we'll have a total
      // weight of 10 and will generate a pseudo random number between 0 and 10. The
      // code will see if the random number...
      // * is less than 7, in which case it chooses index 1,
      // * is between 7 and 9, in which case it chooses index 2, 
      // * is between 9 and 10, in which case it chooses index 3.
      Iterator<Map.Entry<Integer, Integer>> iter2 = weightMap.entrySet().iterator();

      // An aggregate of the weight
      int aggregateWeight = 0;

      while(iter2.hasNext())
      {
        Map.Entry entry = (Map.Entry) iter2.next();
        Integer mappos = (Integer) entry.getKey();
        Integer weight = (Integer) entry.getValue();
      
        aggregateWeight = aggregateWeight + weight.intValue();
        
        // Move the classPos to the next value
        classPos = mappos.intValue(); 
        if(randWeight <= aggregateWeight)
          break;
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "findClassIndex", classPos);
      
    return classPos;
  }        
  
  /**
   * Returns the name of a classification specified by XD.
   * 
   * @return
   */
  public synchronized String getClassification(int index)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getClassification", Integer.valueOf(index));
    String name = classificationNames.get(index);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getClassification", name);
    
    return name;
  }   

  /**
   * Returns the array of flows defined in this classification specified by XD.
   * 
   * @return
   */
  public Flow[] getFlows()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getFlows");
    }
    Flow[] clonedFlows = flows.clone();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {    
      SibTr.exit(tc, "getFlows", clonedFlows);
    }    
    return clonedFlows;
  }
  
  /**
   * Class to wrap the position and weight combination.
   *
   */
  private static class ClassWeight
  {
    private int position;
    private int weight;
    
    public ClassWeight(int position, int weight)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      {
        SibTr.entry(tc, "ClassWeight", new Object[] {Integer.valueOf(position),Integer.valueOf(weight)});
        SibTr.exit(tc, "ClassWeight");  
      }      
      this.position = position;
      this.weight = weight;
    }
    
    public String toString()
    {
      return "index: " 
        + position 
        + ", weight: " 
        + weight;
    }

    public int getPosition() 
    {
      return position;
    }

    public int getWeight() 
    {
      return weight;
    }
  }  
  
}
