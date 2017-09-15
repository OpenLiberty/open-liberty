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
package com.ibm.ws.sib.utils.ras;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/* ************************************************************************** */
/**
 * A base class for all the standard suppressors
 */
/* ************************************************************************** */
public class AbstractSuppressor
{
  private static final long MILLISECONDS_PER_MINUTE = 60000L;

  /** The maximum size of an interval (in minutes) */
  private static final long MAXIMUM_INTERVAL = 24L*60L*MILLISECONDS_PER_MINUTE; // 24 hours (NOTE: MUST be less than LONG_MAX/2 for computations below to work)

  /* ************************************************************************** */
  /**
   * A Details object records the last time a message was emitted and how many
   * times the messages has been suppressed (plus one for the initial emission
   * of the message)
   */
  /* ************************************************************************** */
  private static class Details
  {
    /** The time when the associated message was last emitted */
    private long _lastEmitTime;
    /** How many times has the message been suppressed */
    private int  _suppressions = 0;
    /** The standard size of the interval for this variant */
    private final long _standardInterval;
    /** The actual size of the interval for this variant */
    private long _interval;
    /** When was the first suppression? */
    private long _firstSuppression;
    /** When was the last instance of the message */
    private long _mostRecentSuppression;

    /* -------------------------------------------------------------------------- */
    /* Details constructor
    /* -------------------------------------------------------------------------- */
    /**
     * Construct a new Details object
     *
     * @param initialEmitTime The initial time when the message for which
     * this Details object is being created was being added to the logs
     * @param standardInterval The standard size of the interval
     */
    public Details(long initialEmitTime, long standardInterval)
    {
      resetEmitTime(initialEmitTime);
      _standardInterval = standardInterval;
      resetInterval();
    }

    /* -------------------------------------------------------------------------- */
    /* resetEmitTime method
    /* -------------------------------------------------------------------------- */
    /**
     * Reset the emission time (and hence the suppression count)
     *
     * @param newEmitTime
     */
    public void resetEmitTime(long newEmitTime)
    {
      _lastEmitTime = newEmitTime;
      _suppressions = 0;
    }

    /* -------------------------------------------------------------------------- */
    /* getEmitTime method
    /* -------------------------------------------------------------------------- */
    /**
     * Return the last time the message was emitted
     *
     * @return The last time the message was emitted
     */
    public long getEmitTime()
    {
      return _lastEmitTime;
    }

    /* -------------------------------------------------------------------------- */
    /* incrementInstances method
    /* -------------------------------------------------------------------------- */
    /**
     * Record that another instance of the message has been suppressed
     *
     * @param now When is this instance being recorded
     */
    public void incrementInstances(long now)
    {
      if (_suppressions == 0) _firstSuppression = now;
      _suppressions++;
      _mostRecentSuppression = now;
    }

    /* -------------------------------------------------------------------------- */
    /* getNumberOfSuppressions method
    /* -------------------------------------------------------------------------- */
    /**
     * @return the number of times the message has been suppressed
     */
    public int getNumberOfSuppressions()
    {
      return _suppressions;
    }
    /* -------------------------------------------------------------------------- */
    /* getInterval method
    /* -------------------------------------------------------------------------- */
    /**
     * Return the size of "a while" in milliseconds.
     *
     * @return the size of "a while"
     */
    public long getInterval()
    {
      return _interval;
    }

    /* -------------------------------------------------------------------------- */
    /* doubleInterval method (up to the MAXIMUM_INTERVAL)
    /* -------------------------------------------------------------------------- */
    /**
     * Double the interval value
     */
    protected void doubleInterval()
    {
      _interval *= 2;
      if (_interval > MAXIMUM_INTERVAL) _interval = MAXIMUM_INTERVAL;
    }

    /* -------------------------------------------------------------------------- */
    /* resetInterval method
    /* -------------------------------------------------------------------------- */
    /**
     * Reset the size of the interval
     */
    protected void resetInterval()
    {
      _interval = _standardInterval;
    }

    /* -------------------------------------------------------------------------- */
    /* getFirstSuppressionTime method
    /* -------------------------------------------------------------------------- */
    /**
     * @return The time recorded for the first suppressed message (undefined if getNumberOfSuppressions returns 0)
     */
    public long getFirstSuppressionTime()
    {
      return _firstSuppression;
    }

    /* -------------------------------------------------------------------------- */
    /* getMostRecentSuppressionTime method
    /* -------------------------------------------------------------------------- */
    /**
     * @return The time recorded for the most recently suppressed message (undefined if getNumberOfSuppressions returns 0)
     */
    public long getMostRecentSuppressionTime()
    {
      return _mostRecentSuppression;
    }
  }

  /** The mapping of message to the Details objects */
  private ConcurrentMap<String,Details> _emitMessageKeyAndInsertsTime = new ConcurrentHashMap<String,Details>();

  /** How much extra time does a unit test need to add to real time for an effective test */
  private long _extraTime = 0;

  /* -------------------------------------------------------------------------- */
  /* suppress method
  /* -------------------------------------------------------------------------- */
  /**
   * Determine if this message should be suppressed (assuming there's no
   * suppression interval)
   *
   * @param key The key of the message (either the message key itself or the actual message text)
   * @param variant The variant of the message to be suppressed (or null for all variants)
   * @return The suppression decision.
   */
  protected SibTr.Suppressor.Decision suppress(String key, String variant)
  {
    SibTr.Suppressor.Decision decision = SibTr.Suppressor.Decision.makeDecision(key,variant);

    Details details = _emitMessageKeyAndInsertsTime.get(key); // get the details (if any) for the key
    
    // If the details weren't in the map, attempt to add a new details object now (if we lose the race
    // we'll set the details reference to the object created by the other thread)    
    if (details == null)
    {
      // This is a new message
      Details newDetails = new Details(getNow(),getInterval());
      details = _emitMessageKeyAndInsertsTime.putIfAbsent(key,newDetails);
      if (details == null)
      {
        // We won any race and our newDetails will be used as the details for this message 
        if (getInterval() == Long.MAX_VALUE)
        {
          // We're never going to emit the message after this
          decision.suppressFutureMesages(SibTr.Suppressor.Decision.INFINITE);
          return decision;
        }
        else
        {
          // We're going to emit message again, eventually
          decision.suppressFutureMesages((int)(getInterval()/MILLISECONDS_PER_MINUTE));
          return decision;
        }
      }
    }

    // There was already a Details object in the map, so we're not the first occurrence of the message
    synchronized(details) // Keep a consistent view over the details object while we make our decision and update it
    {
      if (details.getInterval() == Long.MAX_VALUE)
      {
        // We're never going to emit the message again
        decision.suppressThisMessage();
        return decision;
      }
      else
      {
        long now = getNow(); // Snapshot the current time (note: Needs to be done while we hold the details lock)
        
        // Is it time to re-emit the message?
        long interval = now - details.getEmitTime();

        if (interval > details.getInterval())
        {
          // It's been a long time since the message was *not* suppressed, so report if any were suppressed

          // Decide on what to do based on how many messages were actually suppressed
          int suppressedMessageCount = details.getNumberOfSuppressions(); // Remember, details counts the first unsuppressed as one of the instances
          if (suppressedMessageCount == 0)
          {
            // Reset the size of the time interval during which to suppress messages - the messages seems to be rare
            // in comparison to the time interval, so we're happy to see it occasionally
            details.resetInterval();
          }
          else if (suppressedMessageCount == 1)
          {
            // We need to report on the one we suppressed, but it's still rare so reset the time interval
            decision.reportEarlierSuppressedMessages(suppressedMessageCount
                                                    ,details.getFirstSuppressionTime()
                                                    ,details.getMostRecentSuppressionTime()
                                                    );
            details.resetInterval();
          }
          else
          {
            // It's happening a lot, we need to report on the suppressed messages, but double the time interval
            decision.reportEarlierSuppressedMessages(suppressedMessageCount
                                                    ,details.getFirstSuppressionTime()
                                                    ,details.getMostRecentSuppressionTime()
                                                    );
            details.doubleInterval();
          }

          // Reset the time we last emitted the messages (since we're not suppressing this instance of the message)
          details.resetEmitTime(now);

          // We need to say we're suppressing future messages (and now how long for)
          decision.suppressFutureMesages((int) (details.getInterval() / MILLISECONDS_PER_MINUTE));

          return decision;
        }
        else
        {
          // It's not long enough and not suppressed enough, so suppress this message
          details.incrementInstances(now);
          decision.suppressThisMessage();
          return decision;
        }
      }
    }      
  }

  /* -------------------------------------------------------------------------- */
  /* getInterval method
  /* -------------------------------------------------------------------------- */
  /**
   * @return the length of the current interval (in milliseconds)
   */
  protected long getInterval()
  {
    // Overriden by subclasses if they support the concept of intervals
    return Long.MAX_VALUE;
  }

  /* -------------------------------------------------------------------------- */
  /* pretendTimeElapsed method
  /* -------------------------------------------------------------------------- */
  /**
   * Pretend that some additional time has passed (for unit test programs)
   *
   * @param minutes The additional time (in minutes)
   */
  public void pretendTimeElapsed(int minutes)
  {
    _extraTime += minutes;
  }

  /* -------------------------------------------------------------------------- */
  /* getNow method
  /* -------------------------------------------------------------------------- */
  /**
   * Return the current time (adjusted for time added for unit test purposes)
   *
   * @return The "current" time since the beginning of the epoch
   */
  private long getNow()
  {
    return System.currentTimeMillis() + _extraTime*MILLISECONDS_PER_MINUTE;
  }

  /* -------------------------------------------------------------------------- */
  /* getMessageNumber method
  /* -------------------------------------------------------------------------- */
  /**
   * Return the message number at the beginning of a formatted message.
   *
   * @param message The formatted message
   * @return Its message number
   */
  protected String getMessageNumber(String message)
  {
    return message.substring(0,10);
  }
}
