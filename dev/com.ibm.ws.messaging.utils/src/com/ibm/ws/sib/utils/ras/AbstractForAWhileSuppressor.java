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


/* ************************************************************************** */
/**
 * The abstract ForAWhile suppressor that includes the common code between
 * all the ForAWhile suppressors
 *
 */
/* ************************************************************************** */
public abstract class AbstractForAWhileSuppressor extends AbstractSuppressor
{
  /** The default value of how long a while is */
  private static final String INTERVAL_PROPERTY_DEFAULT = "30"; // 30 Minutes

  /** The maximum size of an interval (in minutes) */
  private static final int MAXIMUM_INTERVAL = 24*60; // 24 hours (NOTE: MUST be less than INTEGER_MAX/2 for computations below to work)
  
  /** The initial value of the size of "a while" if not explicitly constructed */
  private static int _standardInterval;

  /* Read the variables that control the size of "a while" and the threshold */
  static
  {
    _standardInterval = Integer.parseInt(INTERVAL_PROPERTY_DEFAULT);
  }

  /** The size of "a while" in minutes */
  private int _interval;

  /* -------------------------------------------------------------------------- */
  /* AbstractForAWhileSuppressor constructor
  /* -------------------------------------------------------------------------- */
  /**
   * Construct a new AbstractForAWhileSuppressor. It will scan the environment
   * to determine how long "a while" is and the threshold above which informational
   * messages should be generated once "a while" has passed and a new message
   * needs to be generated.
   */
  public AbstractForAWhileSuppressor()
  {
    this(_standardInterval);
  }

  /* -------------------------------------------------------------------------- */
  /* AbstractForAWhileSuppressor constructor
  /* -------------------------------------------------------------------------- */
  /**
   * Construct a new AbstractForAWhileSuppressor. This constructor explicitly
   * defines how long "a while" is and the informational message threshold.
   * @param interval How long "a while" is (measured in minutes)
   */
  public AbstractForAWhileSuppressor(int interval)
  {
    initProperties(interval);
  }

  /* -------------------------------------------------------------------------- */
  /* initProperties method
  /* -------------------------------------------------------------------------- */
  /**
   * Initialize this instance of an AbstractForAWhileSuppressor.
   * @param interval How long "a while" is (measured in minutes)
   */
  private void initProperties(int interval)
  {
    // Limit to 1 .. MAXIMUM_INTERVAL
    if (interval > MAXIMUM_INTERVAL) interval = MAXIMUM_INTERVAL;
    if (interval < 1) interval = 1;
    _interval = interval;
  }

  /* -------------------------------------------------------------------------- */
  /* reinitProperties
  /* -------------------------------------------------------------------------- */
  /**
   * Instruct this instance of the suppressor to rescan the properties
   * that determine how long a while is and the info message threshold and then
   * reinitialize the corresponding instance variables.
   * Used by unit tests
   */
  public void reinitProperties()
  {
    initProperties(_standardInterval);
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
    return 60000L * _interval;
  }
}
