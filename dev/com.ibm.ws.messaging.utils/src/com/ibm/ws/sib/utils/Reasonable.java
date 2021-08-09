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
package com.ibm.ws.sib.utils;

/* ************************************************************************** */
/**
 * The Reasonable interface should be implemented by any exception that
 * might result in a SIB message being rerouted to an exception destination
 *
 * An outline implementation of this interface is something like:
 *
 * <pre>
 * public class ExampleException extends Exception implements Reasonable
 * {
 *   private static final int      REASON_IF_NO_WRAPPED_EXCEPTION = xxxx;  // Use DEFAULT_REASON as a last resort
 *   private static final String[] INSERTS_IF_NO_WRAPPED_EXCEPTION = xxxx; // Use DEFAULT_INSERTS as a last resort
 * 
 *   public ExampleException()
 *   {
 *     super();
 *   }
 *   public ExampleException(String msg)
 *   {
 *     super(msg);
 *   }
 *   public ExampleException(Throwable t)
 *   {
 *     super(t);
 *   }
 *   public ExampleException(String msg, Throwable t)
 *   {
 *     super(msg,t);
 *   }
 *
 * &#x2F;**
 *  * &#x40;see com.ibm.ws.sib.utils.Reasonable#getExceptionReason()
 *  * &#x40;return a reason code that can be used if this exception causes a message
 *  *         to be rerouted to the exception destination. The reason code of
 *  *         any wrapped exception will be used if it exists
 *  *&#x2F;
 *   public int getExceptionReason()
 *   {
 *     if (getCause() instanceof Reasonable)
 *       return ((Reasonable)getCause()).getExceptionReason();
 *     else if (getCause() instanceof SIException)
 *       return ((SIException)getCause()).getExceptionReason();
 *     else if (getCause() instanceof SIErrorException)
 *       return ((SIErrorException)getCause()).getExceptionReason();
 *     else 
 *       return REASON_IF_NO_WRAPPED_EXCEPTION;
 *   }
 *
 *  &#x2F;**
 *   * &#x40;see com.ibm.ws.sib.utils.Reasonable#getExceptionInserts()
 *   * &#x40;return a set of inserts (that can be inserted into the message of the corresponding exception reason) if
 *   *         this exception causes a message to be rerouted to the exception destination
 *   *&#x2F;
 *   public String[] getExceptionInserts()
 *   {
 *     if (getCause() instanceof Reasonable)
 *       return ((Reasonable)getCause()).getExceptionInserts();
 *     else if (getCause() instanceof SIException)
 *      return ((SIException)getCause()).getExceptionInserts();
 *     else if (getCause() instanceof SIErrorException)
 *       return ((SIErrorException)getCause()).getExceptionInserts();
 *     else
 *       return INSERTS_IF_NO_WRAPPED_EXCEPTION;
 *   }
 * }
 * </pre>
 *
 * @author David Vines
 *
 */
/* ************************************************************************** */
public interface Reasonable
{
  /** The DEFAULT_REASON is the reason code that a Reasonable exception
   * should use if a better reason code cannot be determined. When wrapping
   * another exception, the innermost non-default reason code should be
   * the one returned.
   */
  // NOTE: This value MUST be the same as SIRCConstants.SIRC0001_DELIVERY_ERROR
  // but I can't say so in this declaration 'cos it would cause circular build dependencies
  // sadly we can't change SIRCConstants either since that would mess up
  // the public javadoc for that class!
  public static final int      DEFAULT_REASON = 1;
  
  /** The DEFAULT_INSERTS are the exception inserts to be used
   * with the DEFAULT_REASON
   */
  public static final String[] DEFAULT_INSERTS = new String[] { };

  /* ------------------------------------------------------------------------ */
  /* getExceptionReason method                                                */
  /* ------------------------------------------------------------------------ */
  /**
   * Return the reason code that should be used if, as a result of exception
   * processing of the object (exception) that implements the Reasonable
   * interface, a SIB message being rerouted to an exception destination.
   *
   * @author David Vines
   *
   * @return int The reason code to be inserted into a SIB message if it gets
   *         rerouted. DEFAULT_REASON if the exact reason cannot be determined.
   */
  /* ------------------------------------------------------------------------ */
  public int getExceptionReason();

  /* ------------------------------------------------------------------------ */
  /* getExceptionInserts method                                               */
  /* ------------------------------------------------------------------------ */
  /**
   * Return the inserts that accompany the exception reason.
   *
   * @author David Vines
   *
   * @return String[] The inserts that accompany the reason code. null or
   *         an array of zero length if there are no inserts. DEFAULT_INSERTS
   *         if getExceptionReason() returns DEFAULT_REASON
   */
  /* ------------------------------------------------------------------------ */
  public String[] getExceptionInserts();
}