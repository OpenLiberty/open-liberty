/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.tu;

import java.io.Serializable;

import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.servlets.OutgoingSipServletResponse;
import com.ibm.ws.sip.container.servlets.SipServletMessageImpl;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.servlets.SipSessionImplementation;
import com.ibm.ws.sip.container.util.SipUtil;
import com.ibm.ws.sip.container.util.wlm.DialogAux;
import com.ibm.ws.sip.container.util.wlm.SipDialogContext;

/**
 * Class which is responsible to manage dialog state
 * @author anat
 *
 */
public class DialogState implements Serializable, Cloneable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 8342447519148817197L;

	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(DialogState.class);

	/**
	 * Dialog state
	 */
	private SipSessionImplementation.State _state;

	/**
	 * This is a new added state flag that doesn't exist in jsr116 and in JSR 289.
	 * Immediately after new dialog created request sent the flag will be true
	 * No new requests can be sent when _afterInitialState is tried.
	 * After a 1xx response will be received the state will be changed to EARLY
	 * according to the JSR and from this point state machine will
	 * be the same as defined in jsr116.
	 * This value can be transient as we are not replicate dialog in that state.
	 */
	private transient boolean _afterInitialState;

	/**
	 * Variable used to set the isDialog field;
	 */
	public static final int IS_DIALOG = 1;

	/**
	 * Variable used to set the isDialog field;
	 */
	public static final int NOT_DIALOG = 2;

	/**
	 * Variable used to set the isDialog field;
	 */
	public static final int UNSET = 0;

	/**
	 * Variable that defines if the SIP Message that had created this TU was a dialog or not
	 * 0 = was not set yet - UNSET
	 * 1 = dialog - IS_DIALOG
	 * 2 = not dialog - NOT_DIALOG
	 */
	private int _dialogMode = UNSET;

	/**
	 * a set containing all dialog usage keys for 
	 */
	private DialogUsageKeySet _dialogUsages = new DialogUsageKeySet();
	
	
    /**
     * the DailogAux associated with this dialog
     */
    private transient DialogAux _dialogAux = null;
	

	/**
	 * Ctor
	 *
	 */
	DialogState(){
		reset();
	}

	/**
	 * resets the dialog state object, as if it was just constructed
	 */
	public final void reset() {
		_state = SipSessionImplementation.State.INITIAL;
		_afterInitialState = false;
		_dialogMode = UNSET;
		_dialogUsages.clear();
		_dialogAux = null;
	}

	/**
	 * Returns the state of the Dialog.
	 * In the non dialog requests will return INITIAL  
	 * @return
	 */
	public SipSessionImplementation.State getState() {
		return _state;
	}

	/**
	 * Checks if the methods from the request can create a dialog
	 * 
	 * @param method
	 */
	public void setDialogState(String method){

		if(_dialogMode != UNSET){
			//Should not happened
			return ;
		}     	
		if(SipUtil.isDialogInitialRequest(method)){
			_dialogMode = IS_DIALOG;
		}
		else{
			_dialogMode = NOT_DIALOG;
		}     	
	}

	/**
	 * Sets the state of the related dialog
	 * In the non dialog requests the state wont be set.
	 * @param _state
	 */
	public void setSessionState(SipSessionImplementation.State state,
			SipServletMessage sipMessage){
		
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "setSessionState", state);
		}

		SipServletMessageImpl messageImpl = (SipServletMessageImpl)sipMessage;
		DialogUsageKey usageKey = null;
		
		if(_dialogMode == IS_DIALOG){
			
			switch (state){
			//if this usage is being created, we add it to the usage set.
			//if it already exists there, it will be overridden
			case INITIAL:
			case EARLY:
			case CONFIRMED:
				usageKey = SipUtil.getDialogUsageKey(messageImpl);
				//add usage to active usage key set
				_dialogUsages.add(usageKey);
				//if the state is more advanced than the last saved state, we update it.
				//Moti: defect 581678 : I don't know the reason
				// why the newer state should be more advanced than the older state
				// therefore, I simply will update state if state has changed.
				if (_state != state){
					_state = state;
				}
				break;
			case TERMINATED:
				
				boolean applyToAllDialogUsages = SipUtil.shouldTerminateAllDialogUsages(messageImpl);
				//we need to make a difference between a termination ending a single usage
				//versus a termination ending all usages  
				if (applyToAllDialogUsages){
					//this usage termination should affect all usages.
					//we clear them all from the active usage table
					_dialogUsages.clear();
					//and set the dialog's state to terminated.
					_state = SipSessionImplementation.State.TERMINATED;
					reportDialogTerminated();
				}else{
					usageKey = SipUtil.getDialogUsageKey(messageImpl);
					//this usage has ended, we need to remove it from the active usage set
					_dialogUsages.remove(usageKey);
					//only if all usages are terminated, we can go ahead and change the dialog state.
					if (_dialogUsages.isEmpty()){
						_state = SipSessionImplementation.State.TERMINATED;
						reportDialogTerminated();
					}
				}
				break;
			}
		}
		else{
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "setSessionState", " will not update the state - not a dialog");
			}
		}
		
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "setSessionState", _state);
		}
	}

	/**
	 * Helper method that will test if the mode of the TU is a SIP DIALOG or not
	 * @return
	 */
	public boolean isDialog(){
		return  (_dialogMode == IS_DIALOG ? true : false);
	}


	/**
	 * Helper method which updates the sessionState.
	 * This method works for Incoming and Outgoing 
	 * responses
	 * @param response
	 */
	public SipSessionImplementation.State updateState(SipServletResponse response){
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "updateState", _state);
		}

		if (_dialogMode == IS_DIALOG) {
			if (_state != SipSessionImplementation.State.TERMINATED)
			{	
				//In case we have another invite in the same dialog, don't change back to EARLY.
				if ((is1xxResponse(response.getStatus())) && (_state != SipSessionImplementation.State.CONFIRMED)) {
					// In the case it was 1xx Reliable response - will not change to
					// CONFIRMED.If it will be changed - the UAS will be not able to
					// after send 2xx it will send 1xx reliable response.
					setSessionState(SipSessionImplementation.State.EARLY,response);
				}
				else if (is2xxResponse(response.getStatus())) {
					setSessionState(SipSessionImplementation.State.CONFIRMED,response);
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "updateState", " dialog has been marked as CONFIRMED.");
					}
				} 
				else if (response.getStatus() >= 300) {
					if (_state == SipSessionImplementation.State.CONFIRMED){
						if (c_logger.isTraceDebugEnabled()) {
							c_logger.traceDebug(this, "updateState", "Confirmed state can not be changed by error response");
						}
					}else {
						if (response instanceof OutgoingSipServletResponse){
							//When UAS sends error response the transaction is terminated 
							if (c_logger.isTraceDebugEnabled()) {
								c_logger.traceDebug(this, "updateState", "UAS sends error response on state Initial/Early and will be terminated");
							}
							setSessionState(SipSessionImplementation.State.TERMINATED, response);
						}else{
							// if in INITIAL or EARLY state, go back to INITIAL.
							//Moti : 24/Mar/2009 commenting next line.
							setSessionState(SipSessionImplementation.State.INITIAL,response);
						}
					}
				}
				// and in all cases, set this flag to false.
				_afterInitialState = false;
			}
		}
		else {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "updateState", "This is not DIALOG Session = " + response);
			}
		}

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "updateState", _state);
		}

		return _state;
	}
	/**
	 * Method which runs on Requests and checkes if state should
	 * be changed to TERMINATED (in case of dialog requests).
	 * Works for Incoming and Outging requests.
	 * @param msg
	 */
	void checkIfTerminateRequest(SipServletRequest msg)throws IllegalArgumentException{
		if (_dialogMode == IS_DIALOG) {

			if (SipUtil.isUsageTerminatingRequest(msg)) {
				setSessionState(SipSessionImplementation.State.TERMINATED, msg);
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "checkIfTerminatState", msg.getMethod() + " TERMAINTED session:"
							+ ((SipServletRequestImpl) msg).getTransactionUser().getId());
				}
			} else {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "checkIfTerminateRequest", "Failed to find Subscription-State header");
				}
			}
		}
	}

	/**
	 * Helper method decides if dialog in the current state can be invalidated
	 * @return
	 */
	boolean canBeInvalidated() {
		boolean canInvalidate = false;

		switch (_state) {
		case INITIAL:
		case TERMINATED:
			canInvalidate = true;
			break;
		}
		return canInvalidate;
	}

	/**
	 * Checks whether the specified response is in the 1xx range.
	 * 
	 * @param status
	 */
	private final boolean is1xxResponse(int status) {
		if (status >= 100 && status < 200) {
			return true;
		}
		return false;
	}

	/**
	 * Checks whether the specified response is in the 2xx range.
	 * 
	 * @param status
	 */
	private final boolean is2xxResponse(int status) {
		if (status >= 200 && status < 300) {
			return true;
		}
		return false;
	}

	/**
	 * Change the state to AFTER_INITIAL state
	 */
	public void setStateToAfterInitial() {
		_afterInitialState = true;
	}

	/**
	 * Returns true if state is AFTER_INITIAL
	 */
	public boolean isAfterInitialState() {
		return _afterInitialState;
	}
	
	public String toString()
	{
		return _state.toString();
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		DialogState state = (DialogState) super.clone();
		state._dialogUsages = (DialogUsageKeySet) _dialogUsages.clone();
		
		return state; 
	}
	
	/**
	 * @see com.ibm.ws.sip.container.util.wlm.SipDialogContext#setDialogAux(com.ibm.ws.sip.container.util.wlm.DialogAux)
	 */
	public void setDialogAux(DialogAux da){
		_dialogAux = da;
	}

	/**
	 * @see com.ibm.ws.sip.container.util.wlm.SipDialogContext#getDialogAux()
	 */
	public DialogAux getDialogAux(){
		return _dialogAux;
	}
	
	/**
	 * @see com.ibm.ws.sip.container.util.wlm.SipDialogContext#getDialogState()
	 */
	public int getWLMDialogState(){
		if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(this,"getWLMDialogState");
		}
		
		int state;
		
		switch (_state) {
		case EARLY:
			state = SipDialogContext.EARLY;
			break;
		case CONFIRMED:
			state = SipDialogContext.CONFIRMED;
			break;
			
		case TERMINATED:
			state = SipDialogContext.TERMINATED;
			break;

		default:
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "getWLMDialogState", "State cannot be INITIAL when call to this method. !!! ");
			}
			state = -1;
			break;
		}
		
		if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(this,"getWLMDialogState",new Integer(state));
		}
		
		return  state;
	}

	/**
	 * Trigger aux API to notify that dialog is terminated 
	 */
	private void reportDialogTerminated() {
		if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntryExit(this, "getWLMDialogState", _dialogAux);
		}
		
   		// call WLM hook
   		if (_dialogAux != null){
   			if(c_logger.isTraceDebugEnabled()){
   				c_logger.traceDebug(this,"reportDialogTerminated", "calling WLM hook on termination of dailog");
   			}
   			_dialogAux.done();
   			_dialogAux = null;
   		}
		
	}
	
	
}
