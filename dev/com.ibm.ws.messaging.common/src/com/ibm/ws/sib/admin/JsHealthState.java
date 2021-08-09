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

package com.ibm.ws.sib.admin;

public abstract class JsHealthState
{
	// define default implementations of JsHealthState methods. These are selectively
	// overridden by the anonymous inner sub-classes.
	public boolean isOK() { return false; }
	public boolean isLocalError() { return false; }
	public boolean isGlobalError() { return false; }
	public String toString() { return null; }
	// no sensible default implementation of mostSevere or couldBeWorse...so abstract
	public abstract JsHealthState moreSevere(JsHealthState in);
    public abstract boolean couldBeWorse();

	// define anonymous inner sub-classes, one for each state.
	private static final JsHealthState ok = new JsHealthState()
	{
		public boolean isOK() { return true; } 
		public String toString() { return "State=OK"; }
		public JsHealthState moreSevere(JsHealthState in) { return in; } // anything is at least as severe as ok
		public boolean couldBeWorse() { return true; }
	};
	
	private static final JsHealthState localError = new JsHealthState() 
	{ 
		public boolean isLocalError() { return true; } 
		public String toString() { return "State=LocalError"; }
		public JsHealthState moreSevere(JsHealthState in) { 
			// if passed a global error return that, else return self
			if ( in.isGlobalError() ) 
				return in;
			else
				return this;
			}
		public boolean couldBeWorse() { return true; }
	};
	
	private static final JsHealthState globalError = new JsHealthState() 
	{ 
		public boolean isGlobalError() { return true; } 
		public String toString() { return "State=GlobalError"; }
		public JsHealthState moreSevere(JsHealthState in) { return this; } // nothing is more severe than global error
		public boolean couldBeWorse() { return false; }
	};
	
	// access methods to get specific health states
	public static final JsHealthState getOK() {
		return ok;
	}
	
	public static final JsHealthState getLocalError() {
		return localError;
	}
	
	public static final JsHealthState getGlobalError() {
		return globalError;
	}
    
}
