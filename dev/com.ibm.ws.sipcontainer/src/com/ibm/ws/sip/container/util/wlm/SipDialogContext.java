/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.util.wlm;

/** SIP container's handle on the
 * incoming message and the associated container SIP Dailog data
 * 
 * @author dror yaffe
 *
 */
public interface SipDialogContext {
	
	//	Known Dialog States
    public static final int EARLY = 1;

    public static final int CONFIRMED = 2;

    public static final int TERMINATED = 3;
    
    
	/** The SIP container's data for a dialog includes an opaque slot called the
	 * auxiliary slot, and this method sets the value in that slot. This method
	 * is applicable for any incoming message, at any stage of its processing,
	 * regardless of whether it creates a dialog and what its transaction (or
	 * other) state is. This call commits the container to return da from calls
	 * on <code>sdc.getDialogAux()</code> for this and for any other
	 * <code>SipDialogContext sdc</code> that is passed, in the same running
	 * JVM, to {@link QueueableTransformer#wrap} along with another message
	 * related to the same dialog (in the cases where there is one). This call
	 * also commits the container to call <code>da.done()</code> soon after
	 * the container realizes that the dialog is over or that there is no
	 * real dialog.
	 * <p>
	 * Please note that in case of failover, once the dialog is activated on one
	 * of the peer containers in the cluster, <b>no<b> DialogAux will be
	 * associated with it. See also {@link WLMDailogFailoverListner}.
	 * 
	 * @param da the new value to put in the auxiliary slot of the dialog
	 * @throws Exception if this incoming message was neither
	 * part of a dialog nor initiated one
	 */
	public void setDialogAux(DialogAux da) throws Exception;
	
	/** Returns the value in the auxiliary slot of the dialog.
	 * Returns null if the slot has not been set yet or if there is no associated dialog.
	 * 
	 * @return the value in the auxiliary slot of the dialog
	 */
	public DialogAux getDialogAux();
	
	/**
	 * Test whether there could be an associated dialog.
	 * 
	 * @return the boolean false iff the associated message both (a) is not part
	 *         of a pre-existing dialog and (b)cannot initiate one
	 */
	public boolean canHaveDialog();

	/**
	 * Returns the state of the Dialog.
	 * In the non dialog requests will return INITIAL  
	 * @return
	 */
	public int getDialogState();
}
