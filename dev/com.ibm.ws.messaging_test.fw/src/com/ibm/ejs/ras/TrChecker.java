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
package com.ibm.ejs.ras;

import java.util.*;

/**
 * @author matrober
 *
 * This class implements the extra functionality required to validate the entry
 * and exit trace statements as part of the run of a testcase.
 * 
 * Enabling the checker:
 * To enable the checker onto a standard RAS component follow these steps;
 * 
 * 1. Copy this TrChecker class into the same package as Tr
 * 2. Alter the Tr class to extend TrChecker
 * 3. Add the text "TrChecker.checkedEntry(methodName);" to the two Tr.entry methods inside
 *    the ivEntryEnabled block.
 * 4. Add the text "TrChecker.checkedExit(methodName);" to the two Tr.exit methods inside
 *    the ivEntryEnabled block.
 * 5. Add a call to "TrChecker.reset();" to the setup stage of your testcase.
 * 6. Add a call to "TrChecker.assertEmpty();" to the teardown stage of your testcase.
 * 7. Enable the trace component that you are interested in using your favourite method.
 */
public class TrChecker
{

	/**
	 * Give an indication to user when the JVM starts that the TrChecker is being used.
	 */
	static
	{
			
		System.out.println("TrChecker in use.");
		
	}//static initializer

	/**
	 * A map keyed off the Thread object, the value of which is an instance of
	 * ThreadCallStack,
	 */
  private static Map threadStacks = new HashMap();

  /**
   * This method should be called prior to each separate block of code. It resets
   * the state storage to a clean state, ready for the next test run.
   */
  public static void reset()
  {

    threadStacks.clear();

  } //reset

  /**
   * This method should be called at the end of each test run to ensure that the
   * stack has been unwound fully. This method catches problems where there are less
   * "exits" than "entries".
   */
  public static void assertEmpty()
  {

    ThreadCallStack tcs = getTCS(Thread.currentThread());

    // Retrieve the current indentation size.
    int l = tcs.getLevel();

    // If the indentation size is zero then everything is OK.
    if (l == 0)
      return;

    // If there are more entries than exits.
    if (l > 0)
    {
      // Prepare a string to receive the contents of the entry stack.
      String methods = "";

      // Enumerate through the entry stack
      Enumeration en = tcs.stackElements();
      while (en.hasMoreElements())
      {
        methods += en.nextElement().toString() + " ";
      }

      // Throw an exception showing the problem.
      throw new ApplicationTraceMismatch(
        "Missing exit statements for entries; " + methods);

    } else
    {
      // There were more exits than entries.
      throw new ApplicationTraceMismatch(
        "Too many exit statements! (" + Math.abs(l) + ")");

    } //if

  } //assertEmpty

  public static ThreadCallStack getTCS(Thread t)
  {
  	// Attempt to retrieve the state from the map
    ThreadCallStack temp =
      (ThreadCallStack) threadStacks.get(Thread.currentThread());

		// If we have not seen this thread before.
    if (temp == null)
    {
      temp = new ThreadCallStack(Thread.currentThread());
      threadStacks.put(Thread.currentThread(), temp);
      
    }

    return temp;

  }

  /**
   * This method should be called by every Tr.entry method (see step 3 above).
   */
  public static void checkedEntry(String methodName)
  {

    ThreadCallStack tcs = getTCS(Thread.currentThread());

    // Increment the level counter
    tcs.incLevel();

    // If this is one of the trace components we are interested in.
    tcs.push(methodName);

  } //entry

  /**
   * This method should be called by every Tr.exit method (see step 4 above).
   */
  public static void checkedExit(String methodName)
  {

    ThreadCallStack tcs = getTCS(Thread.currentThread());

    // Decrement the level counter
    tcs.decLevel();

    // If this is one of the trace components we are interested in.
    String popped = (String) tcs.pop();

    // Check that it matches
    if (!popped.equals(methodName))
    {
      throw new ApplicationTraceMismatch(
        "Entry '"
          + popped
          + "' was not matched - got '"
          + methodName
          + "'");
    }

    if (tcs.getLevel() < 0)
    {
      throw new ApplicationTraceMismatch(
        "There are more exits than entries! ("
          + Math.abs(tcs.getLevel())
          + ")");
    }

  } //exit

  /**
   * This RuntimeException subclass is used to report all trace related errors.
   * 
   */
  private static class ApplicationTraceMismatch extends RuntimeException
  {
    // Added at version 1.10
    private static final long serialVersionUID = -1762996883164708635L;
    
    ApplicationTraceMismatch(String message)
    {
      super(message);
    }

  } //ApplicationTraceMismatch

  /**
   * This object represents the call stack for a given Thread.
   * Each of the access methods is synchronized internally on the callStack 
   * to ensure that the state remains consistent, however this should never
   * be a problem(!) since the ThreadCallStack object is only altered by the
   * Thread for which it stores state.
   */
  private static class ThreadCallStack
  {

    private Thread threadRef = null;
    private int level = 0;
    private Stack callStack = null;

    ThreadCallStack(Thread t)
    {
      threadRef = t;
      level = 0;
      callStack = new Stack();
    }

    public void push(String methodName)
    {
      synchronized (callStack)
      {
        callStack.push(methodName);
      }
    }

    public String pop()
    {
      synchronized (callStack)
      {
        return (String) callStack.pop();
      }
    }

    public void incLevel()
    {
      synchronized (callStack)
      {
        level++;
      }
    }

    public void decLevel()
    {
      synchronized (callStack)
      {
        level--;
      }
    }

    public int getLevel()
    {
      synchronized (callStack)
      {
        return level;
      }
    }

    public Enumeration stackElements()
    {
      synchronized (callStack)
      {
        return callStack.elements();
      }
    }

  } //ThreadCallStack

}
