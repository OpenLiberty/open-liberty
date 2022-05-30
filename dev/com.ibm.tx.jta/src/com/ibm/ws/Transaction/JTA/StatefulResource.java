package com.ibm.ws.Transaction.JTA;
/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

public interface StatefulResource
{
    // Be careful if you want to change these!
    // HeuristicOutcome assumes NONE is zero.
	public static final int NONE                 = 0;
	public static final int REGISTERED           = 1;
	public static final int PREPARED             = 2;                                      // Defect 1412.1
	public static final int COMPLETING           = 3;
	public static final int COMPLETED            = 4;
    public static final int COMPLETING_ONE_PHASE = 5;
    public static final int ROLLEDBACK           = 6;
    public static final int COMMITTED            = 7;
    public static final int HEURISTIC_COMMIT     = 8;
    public static final int HEURISTIC_ROLLBACK   = 9;
    public static final int HEURISTIC_MIXED      = 10;
    public static final int HEURISTIC_HAZARD     = 11;
    
    // If you add another state you've got to change this
    public static final int numStates = 12;

	public int getResourceStatus();

	public void setResourceStatus(int status);
}