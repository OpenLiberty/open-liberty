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
package com.ibm.mqst.jetsam;

import javax.jms.*;

/**
 * @author matrober
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class JMSLogImpl extends JETSAMLogImpl
{
  // Added at version 1.8
  private static final long serialVersionUID = -2830658151300795750L;

  /**
   * Constructor for JMSLog.
   * @param fn
   * @param v
   */
  public JMSLogImpl(String fn, boolean v)
  {
    super(fn, v);
  }
  
  public JMSLogImpl(String fn, boolean v, boolean f)
  {
    super(fn, v, f);
  }

  public static JETSAMLog createJMSLog(
    String fileName,
    boolean outputToScreen,
    boolean outputToFile)
  {

    JETSAMLog log = new JMSLogImpl(fileName, outputToScreen, outputToFile);
    return log;

  }

  // *************************** METHODS *********************************

	/*
	public void error(Exception e)
	{
		
		super.error(e);
		
    // There will only be a linked exception if it is a JMSException
    if (e instanceof JMSException)
    {
      // Get it and log it
      Exception ex = ((JMSException) e).getLinkedException();
      if (ex != null)
      {
        comment("Linked Exception:");
        super.comment(ex);
      } else
      {
        comment("No LinkedException");
      }
    } //if
		
	} */
	
	
  /** This method overrides the JETSAMLog.comment(java.lang.Exception) method by 
      * using the overridden method to log the Exception but then will also log any
      * linked exceptions.
      *
      * @param e The Exception to log
      */
  public void comment(Exception e)
  {
    super.comment(e);
    // There will only be a linked exception if it is a JMSException
    if (e instanceof JMSException)
    {
      // Get it and log it
      Exception ex = ((JMSException) e).getLinkedException();
      if (ex != null)
      {
        comment("Linked Exception:");
        comment(ex);
      } else
      {
        comment("No LinkedException");
      }
    } //if

  } //comment

}
