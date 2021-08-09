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

package com.ibm.ws.sib.processor.gd;

// Import required classes.
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.utils.BlockVector;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * An implementation of RangeList that uses a BlockVector for maintaining the
 * list. Linear or Interpolation search is used for locating a particular stamp
 * in the list. The choice of the search method can be tuned through a parameter
 * Concurrency: ARangeList is not safe for concurrent methods, except
 * for concurrent get() operations.
 *
 * get() is optimized for elements near the end of the list if the tuning parameter is
 * set to linear search.
 * replace()  is optimized for searching near the cursor position as usually called after
 * get().
 */

public class ARangeList implements RangeList
{

  private static TraceComponent tc =
    SibTr.register(
      ARangeList.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  

  private BlockVector blockVector;
  private int cursor;

  //NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  /**
   * @param ro The only object initially in the list. 
   *            The low and high will be reset to 0 and INFINITY
   *            if not already set.
   */
  public void init(RangeObject ro)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "init", ro);

    blockVector = new BlockVector();
    ro.startstamp = 0;
    ro.endstamp = INFINITY;
    blockVector.add(ro);
    cursor = 0;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "init");
  }

  public void setCursor(long stamp)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "setCursor", Long.valueOf(stamp));

    if (stamp == 0)
      cursor = 0;
    else
      cursor = getIndex(stamp);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setCursor", new Object[] {Integer.valueOf(cursor), Integer.valueOf(blockVector.size())} );
  }

  public Object getMark(Object mark)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "getMark", mark);

    Marker mmark = (Marker) mark;
    if (mmark == null)
      mmark = new Marker(cursor);
    else
      mmark.position = cursor;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getMark", mmark);

    return mmark;
  }

  public void setCursor(Object mark)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "setCursor", mark);

    cursor = ((Marker) mark).position;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setCursor", new Object[] { Integer.valueOf(cursor), Integer.valueOf(blockVector.size())});
  }

  /**
   * Returns the next range object
   */
  public RangeObject getNext()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "getNext", Integer.valueOf(cursor));

    int curr = cursor;
    cursor = cursor < (blockVector.size() - 1) ? cursor + 1 : cursor;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getNext", new Object[] {blockVector.get(curr), Integer.valueOf(cursor)});

    return (RangeObject) blockVector.get(curr);
  }

  public RangeObject getPrev()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "getPrev", Integer.valueOf(cursor));

    int curr = cursor;
    cursor = cursor > 0 ? cursor - 1 : cursor;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getPrev", new Object[] {blockVector.get(curr), Integer.valueOf(cursor)});

    return (RangeObject) blockVector.get(curr);
  }

  public RangeObject getCurr()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this, tc, "getCurr", Integer.valueOf(cursor));
      SibTr.exit(tc, "getCurr", blockVector.get(cursor));
    }
    
    return (RangeObject) blockVector.get(cursor);
  }

  public void get(RangeObject r, BlockVector readList)
  {
    get(r.startstamp, r.endstamp, readList);
  }

  public void get(long startstamp, long endstamp, BlockVector readList)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        this,
        tc,
        "get",
        new Object[] { Long.valueOf(startstamp), Long.valueOf(endstamp), readList });

    int findex = getIndex(startstamp); // get the first index
    int length = blockVector.size();
    for (int i = findex; i < length; i++)
    {
      RangeObject ro = (RangeObject) blockVector.get(i);
      if (ro.startstamp > endstamp) // done
        break;
      
      readList.add(ro);
    }
    cursor = findex; // set the cursor

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "get", Integer.valueOf(cursor));
  }

  /**
   * replace exisitng list entries with RangeObjects in writeList 
   */
  public void replace(BlockVector writeList)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "replace", writeList);

    boolean pushed = false;

    // examine lowest and highest stamp of writeList
    int wlength = writeList.size();
    long fstamp = ((RangeObject) writeList.get(0)).startstamp;
    long lstamp = ((RangeObject) writeList.get(wlength - 1)).endstamp;

    // Set index to position of this low. Using linear search as usually
    // called after a get() that has set the cursor to a 'good' index.

    int findex = linearSearch(fstamp, 0, blockVector.size() - 1);
    int lindex;
    RangeObject lastro;
    for (lindex = findex;; lindex++)
    {
      lastro = (RangeObject) blockVector.get(lindex);
      if ((lstamp <= lastro.endstamp) && (lstamp >= lastro.startstamp))
        break;
    }
    int index, count;
    if (findex == lindex)
    {
      if ((fstamp > lastro.startstamp) && (lstamp < lastro.endstamp))
      {
        // need to clone lastro
        RangeObject ro2 = (RangeObject) lastro.clone();
        ro2.startstamp = lstamp + 1;
        //and push it onto end of writeList
        writeList.add(ro2);
        wlength++;
        pushed = true;
        // adjust lastro.endstamp
        // lastro = (RangeObject) lastro.clone(); *no cloning unless necessary*
        lastro.endstamp = fstamp - 1;
        // blockVector.m_data[findex] = lastro; *no cloning unless necessary*
        // set the index to point to where need to add to put writeList
        index = findex + 1;
        count = wlength; // space to be created/destroyed
      }
      else
        if (fstamp > lastro.startstamp)
        {
          // adjust lastro.endstamp
          // lastro = (RangeObject) lastro.clone(); *no cloning unless necessary*
          lastro.endstamp = fstamp - 1;
          // blockVector.m_data[findex] = lastro; *no cloning unless necessary*
          // set the index to point to where need to add to put writeList
          index = findex + 1;
          count = wlength; // space to be created/destroyed
        }
        else
          if (lstamp < lastro.endstamp)
          {
            // adjust lastro.startstamp
            // lastro = (RangeObject) lastro.clone(); *no cloning unless necessary*
            lastro.startstamp = lstamp + 1;
            // blockVector.m_data[findex] = lastro; *no cloning unless necessary*
            // set the index to point to where need to add to put writeList
            index = findex;
            count = wlength; // space to be created/destroyed
          }
          else
          {
            // need to remove lastro
            index = findex;
            count = wlength - 1;
          }
    }
    else
    { // lindex > findex
      // in the worst case (lindex - findex + 1) elements will be replaced by
      // wlength elements, so only need the following extra space
      count = wlength - (lindex - findex + 1);
      index = findex;
      // adjust range in object at findex or remove it
      RangeObject firstro = (RangeObject) blockVector.get(findex);
      if (fstamp > firstro.startstamp)
      {
        // adjust firstro.endstamp
        // firstro = (RangeObject) firstro.clone();
        firstro.endstamp = fstamp - 1;
        // blockVector.m_data[findex] = firstro;
        // set the index to point to where need to add to put writeList
        index++;
        count++; // the element at findex will not be removed
      }
      // adjust range in object at lindex or remove it
      if (lstamp < lastro.endstamp)
      {
        // adjust lastro.startstamp
        // lastro = (RangeObject) lastro.clone();
        lastro.startstamp = lstamp + 1;
        // blockVector.m_data[lindex] = lastro;
        count++;
      }
    }

    // now do the removal and addition in one-shot
    if (count > 0)
      blockVector.insertNullElementsAt(index, count);
    else
      if (count < 0)
      {
        count = -count;
        blockVector.removeElementsAt(index, count);
      }

    // put the stuff in writeList into blockVector
    for (int i = 0; i < writeList.size(); i++)
    {
      if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Setting " + writeList.get(i) + " at index " + (i+ index) );
      
      blockVector.set(i + index, writeList.get(i));
      
      // Just to make sure that worked (see 240699)
      if(blockVector.get(i + index) == null)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "replace", "SIErrorException: null range at index " + (i + index));
        
        throw new SIErrorException( 
          nls.getFormattedMessage( 
          "INTERNAL_MESSAGING_ERROR_CWSIP0005", 
          new Object[] { "ARangeList", 
                         "1:347:1.25", 
                         "Null range at index " + (i + index)},  
          null)); 
      }
    }
    if (pushed) // remove the extraneous element pushed onto the end of writeList
      writeList.remove(writeList.size() - 1);

    cursor = index;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "replace", Integer.valueOf(cursor));
  }

  /**
   * return a list of RangeObjects that are removed
   */
  public void replacePrefix(RangeObject w)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "replacePrefix", w);

    long lstamp = w.endstamp;

    // Set index to position of lstamp.
    int lindex;
    RangeObject lastro;
    for (lindex = 0;; lindex++)
    {
      lastro = (RangeObject) blockVector.get(lindex);
      if ((lstamp <= lastro.endstamp) && (lstamp >= lastro.startstamp))
        break;
    }

    if (lstamp < lastro.endstamp)
    {
      // lastro will not be removed so change its range
      // lastro = (RangeObject) lastro.clone();
      lastro.startstamp = lstamp + 1;
      // blockVector.m_data[lindex] = lastro;

      // figure out how much space to add or remove
      if (lindex == 0)
      { // need to create 1 space
        blockVector.insertNullElementsAt(0, 1);
      }
      else
      { // remove lindex - 1 elements
        if ((lindex - 1) > 0)
          blockVector.removeElementsAt(0, lindex - 1);
      }
    }
    else
    { // everything upto and including lindex will be discarded
      if (lindex > 0)
        blockVector.removeElementsAt(0, lindex);
    }

    blockVector.set(0, w);
    cursor = 0;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "replacePrefix");
  }

  /**
   *  splits the range object [a, b] at the current cursor position, that contains stamp,
   *  into two objects, [a, stamp-1], [stamp, b]. The cursor is made to point to [stamp, b].
   * If a==stamp, nothing is done.
   * @param stamp > 0
   */
  public void splitStart(long stamp)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "splitStart", new Object[] {Long.valueOf(stamp), Integer.valueOf(cursor)});

    RangeObject ro = (RangeObject) blockVector.get(cursor);
    if (ro.startstamp == stamp)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "splitStart");
      return;
    }

    if (stamp > ro.endstamp)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(
          tc,
          "splitStart",
          new Object[] { "A", Long.valueOf(stamp), Long.valueOf(ro.endstamp)});

      SIErrorException e = 
        new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[] {
              "com.ibm.ws.sib.processor.gd.ARangeList",
              "1:445:1.25" },
            null));

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.exception(tc, e);
        
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
        new Object[] {
          "com.ibm.ws.sib.processor.gd.ARangeList",
          "1:454:1.25" });

      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.gd.ARangeList.splitStart",
        "1:460:1.25",
        this);

      throw e;
    }
    if (stamp < ro.startstamp)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(
          tc,
          "splitStart",
          new Object[] { "B", Long.valueOf(stamp), Long.valueOf(ro.startstamp)});

      SIErrorException e =
        new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[] {
              "com.ibm.ws.sib.processor.gd.ARangeList",
              "1:479:1.25" },
            null));

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
        new Object[] {
          "com.ibm.ws.sib.processor.gd.ARangeList",
          "1:487:1.25" });

      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.gd.ARangeList.splitStart",
        "1:493:1.25",
        this);

      throw e;

    }

    RangeObject ro2 = (RangeObject) ro.clone();
    ro2.endstamp = stamp - 1;
    ro.startstamp = stamp;
    blockVector.insertNullElementsAt(cursor, 1);
    blockVector.set(cursor, ro2);
    cursor++;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "splitStart", Integer.valueOf(cursor));
  }

  /**
   *  splits the range object [a, b] at the current cursor position, that contains stamp,
   *  into two objects, [a, stamp], [stamp+1, b]. The cursor is made to point to [a, stamp]
   * If stamp==b, nothing is done
   * @param stamp >= 0
   */
  public void splitEnd(long stamp)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "splitEnd", new Object[] {Long.valueOf(stamp), Integer.valueOf(cursor)});

    RangeObject ro = (RangeObject) blockVector.get(cursor);
    if (stamp == ro.endstamp)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "splitEnd");
      return;
    }

    if (stamp > ro.endstamp)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(
          tc,
          "splitEnd",
          new Object[] { "A", Long.valueOf(stamp), Long.valueOf(ro.endstamp)});

      SIErrorException e = 
        new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[] {
              "com.ibm.ws.sib.processor.gd.ARangeList",
              "1:544:1.25" },
            null));

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
        new Object[] {
          "com.ibm.ws.sib.processor.gd.ARangeList",
          "1:552:1.25" });

      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.gd.ARangeList.splitEnd",
        "1:558:1.25",
        this);

      throw e;
    }
    if (stamp < ro.startstamp)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(
          tc,
          "splitEnd",
          new Object[] { "B", Long.valueOf(stamp), Long.valueOf(ro.startstamp)});

      SIErrorException e =
        new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[] {
              "com.ibm.ws.sib.processor.gd.ARangeList",
              "1:577:1.25" },
            null));

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.exception(tc, e);
      
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
        new Object[] {
          "com.ibm.ws.sib.processor.gd.ARangeList",
          "1:586:1.25" });

      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.gd.ARangeList.splitEnd",
        "1:592:1.25",
        this);

      throw e;
    }

    RangeObject ro2 = (RangeObject) ro.clone();
    ro2.endstamp = stamp;
    ro.startstamp = stamp + 1;
    blockVector.insertNullElementsAt(cursor, 1);
    blockVector.set(cursor, ro2);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "splitEnd");
  }

  /**
   * gets the index in blockVector for the RangeObject containing stamp
   */
  protected final int getIndex(long stamp)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "getIndex", Long.valueOf(stamp));

    int first = 0;
    int last = blockVector.size();

    int index = linearSearch(stamp, first, last - 1);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getIndex", index);

    return index;
  }

  protected final int linearSearch(long stamp, int first, int last)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        this,
        tc,
        "linearSearch",
        new Object[] { Long.valueOf(stamp), Integer.valueOf(first), Integer.valueOf(last), Integer.valueOf(cursor)});

    // Hoping that using the current cursor position will satisfy
    // or sufficiently narrow the search.
    int i = cursor;
    RangeObject ro = (RangeObject) blockVector.get(i);
    
    if(ro == null)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "linearSearch", "SIErrorException: Null range under cursor at index " + i);
        
      throw new SIErrorException( 
        nls.getFormattedMessage( 
        "INTERNAL_MESSAGING_ERROR_CWSIP0005", 
        new Object[] { "ARangeList", 
                       "1:650:1.25", 
                       "Null range at index " + i},  
        null)); 
    }
        
    if ((stamp >= ro.startstamp) && (stamp <= ro.endstamp))
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "linearSearch - index matched", Integer.valueOf(i));

      return i;
    }
    else
      if (stamp > ro.endstamp)
        first = i + 1;
      else
        if (stamp < ro.startstamp)
          last = i - 1;

    // If not in current index then search rest of list
    for (i = last; i >= first; i--)
    {
      ro = (RangeObject) blockVector.get(i);
      
      if(ro == null)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "linearSearch", "SIErrorException: Null range at index " + i);
        
        throw new SIErrorException( 
          nls.getFormattedMessage( 
          "INTERNAL_MESSAGING_ERROR_CWSIP0005", 
          new Object[] { "ARangeList", 
                         "1:683:1.25", 
                         "Null range at index " + i},  
          null)); 
      }
        
      if ((stamp >= ro.startstamp) && (stamp <= ro.endstamp))
        break;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "linearSearch", Integer.valueOf(i));

    return i;
  }

  class Marker
  {
    int position;
    public Marker(int pos)
    {
      position = pos;
    }
    
    public String toString()
    {
      return super.toString() + "(" + position + ")";
    }
  }

}
