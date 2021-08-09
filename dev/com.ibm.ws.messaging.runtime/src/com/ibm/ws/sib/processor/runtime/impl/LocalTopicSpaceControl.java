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
package com.ibm.ws.sib.processor.runtime.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.ControllableType;
import com.ibm.ws.sib.admin.LocalizationDefinition;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.exceptions.SIMPException;
import com.ibm.ws.sib.processor.gd.TargetStreamManager;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.interfaces.ProducerInputHandler;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PubSubMessageItemStream;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.SIMPLocalTopicSpaceControllable;
import com.ibm.ws.sib.processor.runtime.SIMPMessageHandlerControllable;
import com.ibm.ws.sib.processor.runtime.SIMPTopicSpaceControllable;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * 
 */
public class LocalTopicSpaceControl extends AbstractRegisteredControlAdapter
		implements SIMPLocalTopicSpaceControllable {
	private final BaseDestinationHandler destinationHandler;
	private PubSubMessageItemStream itemStream;
	private String id;

	private static TraceComponent tc = SibTr.register(
			LocalTopicSpaceControl.class, SIMPConstants.MP_TRACE_GROUP,
			SIMPConstants.RESOURCE_BUNDLE);


	private static final TraceNLS nls = TraceNLS
			.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

	public LocalTopicSpaceControl(MessageProcessor messageProcessor,
			PubSubMessageItemStream itemStream) {
		super(messageProcessor, ControllableType.PUBLICATION_POINT);

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.entry(tc, "LocalTopicSpaceControl", new Object[] {
					messageProcessor, itemStream });

		this.itemStream = itemStream;
		destinationHandler = itemStream.getDestinationHandler();
		// NOTE: we perform lazy init for all other values as the BDH
		// is not yet fully initialized.
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.exit(tc, "LocalTopicSpaceControl");
	}

	private ProducerInputHandler getInputHandler() {
		return (ProducerInputHandler) destinationHandler.getInputHandler();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.ibm.ws.sib.processor.runtime.SIMPLocalTopicSpaceControllable#
	 * getTopicSpace()
	 */
	public SIMPTopicSpaceControllable getTopicSpace() {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.entry(tc, "getTopicSpace");

		SIMPTopicSpaceControllable tsControl = (SIMPTopicSpaceControllable) getMessageHandler();

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.exit(tc, "getTopicSpace", tsControl);

		return tsControl;
	}

	public SIMPMessageHandlerControllable getMessageHandler() {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.entry(tc, "getMessageHandler");

		SIMPMessageHandlerControllable messageHandlerControl = (SIMPMessageHandlerControllable) destinationHandler
				.getControlAdapter();

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.exit(tc, "getMessageHandler", messageHandlerControl);

		return messageHandlerControl;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.ibm.ws.sib.processor.runtime.SIMPLocalTopicSpaceControllable#
	 * getPubSubInboundReceiver()
	 */
	public SIMPIterator getPubSubInboundReceiverIterator() {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.entry(tc, "getPubSubInboundReceiverIterator");

		TargetStreamManager tsm = getInputHandler().getTargetStreamManager();
		SIMPIterator itr = tsm.getTargetStreamSetControlIterator();

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.exit(tc, "getPubSubInboundReceiverIterator", itr);

		return itr;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.ws.sib.admin.Controllable#getId()
	 */
	public String getId() {
		try {
			if (id == null)
				id = "" + itemStream.getID();
		} catch (MessageStoreException e) {
			FFDCFilter
					.processException(
							e,
							"com.ibm.ws.sib.processor.runtime.LocalTopicSpaceControl.getId",
							"1:176:1.34", this);
			SibTr.exception(tc, e);
		}

		return id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.ws.sib.admin.Controllable#getName()
	 */
	public String getName() {
		return getMessageHandler().getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ibm.ws.sib.processor.runtime.AbstractControllable#checkValidControllable
	 * ()
	 */
	public void assertValidControllable()
			throws SIMPControllableNotFoundException {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.entry(tc, "checkValidControllable");

		if (itemStream == null || !itemStream.isInStore()) {
			SIMPControllableNotFoundException finalE = new SIMPControllableNotFoundException(
					nls
							.getFormattedMessage(
									"INTERNAL_MESSAGING_ERROR_CWSIP0005",
									new Object[] {
											"LocalTopicSpaceControl.assertValidControllable",
											"1:207:1.34", id }, null));

			SibTr.exception(tc, finalE);
			if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
				SibTr.exception(tc, finalE);
			throw finalE;
		}

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.exit(tc, "checkValidControllable");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ibm.ws.sib.processor.runtime.AbstractControllable#dereferenceControllable
	 * ()
	 */
	@Override
	public void dereferenceControllable() {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.entry(tc, "dereferenceControllable");

		super.dereferenceControllable();
		itemStream = null;

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.exit(tc, "dereferenceControllable");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.ibm.ws.sib.processor.runtime.SIMPLocalTopicSpaceControllable#
	 * isSendAllowed()
	 */
	public boolean isSendAllowed() {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.entry(tc, "isSendAllowed");

		boolean isSendAllowed = itemStream.isSendAllowed();

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.exit(tc, "isSendAllowed", new Boolean(isSendAllowed));

		return isSendAllowed;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ibm.ws.sib.processor.runtime.SIMPLocalTopicSpaceControllable#getDepth
	 * ()
	 */
	public long getNumberOfQueuedMessages() {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.entry(tc, "getNumberOfQueuedMessages");

		long depth = itemStream.getTotalMsgCount();

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.exit(tc, "getNumberOfQueuedMessages", Long.valueOf(depth));

		return depth;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.ws.sib.processor.runtime.SIMPLocalTopicSpaceControllable#setDestinationHighMsgs(long)
	 */
	public void setDestinationHighMsgs(long newDestHighMsgs) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr
					.entry(tc, "setDestinationHighMsgs", new Long(
							newDestHighMsgs));

		itemStream.setDestHighMsgs(newDestHighMsgs);

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.exit(tc, "setDestinationHighMsgs");
	}

	/**
	 * @see com.ibm.ws.sib.processor.runtime.SIMPLocalTopicSpaceControllable#getDestinationHighMsgs()
	 * 
	 */
	public long getDestinationHighMsgs() {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.entry(tc, "getDestinationHighMsgs");

		long destHighMsgs = itemStream.getDestHighMsgs();

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.exit(tc, "getDestinationHighMsgs", new Long(destHighMsgs));

		return destHighMsgs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.ibm.ws.sib.processor.runtime.SIMPLocalTopicSpaceControllable#
	 * setSendAllowed(boolean)
	 */
	public void setSendAllowed(boolean newSendAllowedValue) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.entry(tc, "setSendAllowed", new Boolean(newSendAllowedValue));

		itemStream.setSendAllowed(newSendAllowedValue);

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.entry(tc, "setSendAllowed", new Boolean(newSendAllowedValue));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.ws.sib.admin.Controllable#getUuid()
	 */
	@Override
	public String getUuid() {
		LocalizationDefinition def = itemStream.getLocalizationDefinition();
		String uuid = null;
		if (def != null) {
			uuid = def.getUuid();
		}
		return uuid;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.ws.sib.admin.Controllable#getRemoteEngineUuid()
	 */
	@Override
	public String getRemoteEngineUuid() {
		return getMessageProcessor().getMessagingEngineUuid().toString();
	}

	/**
	 * Registers this control adapter with the mbean interface.
	 * <p>
	 * Will not re-register if already registered.
	 */
	@Override
	public synchronized void registerControlAdapterAsMBean() {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.entry(tc, "registerControlAdapterAsMBean");

		if (isRegistered() || getMessageHandler().isTemporary()) {
			// We're a temporary queue or Registered already. Don't register a
			// 2nd time.
		} else {
			super.registerControlAdapterAsMBean();
		}
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.exit(tc, "registerControlAdapterAsMBean");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.ibm.ws.sib.processor.runtime.SIMPLocalTopicSpaceControllable#
	 * getPubSubInboundReceiverStreamSets
	 */
	public SIMPIterator getPubSubInboundReceiverStreamSets() {

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.entry(tc, "getPubSubInboundReceiverIterator");

		TargetStreamManager tsm = getInputHandler().getTargetStreamManager();
		SIMPIterator itr = tsm.getTargetStreamSetControlIterator();

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.exit(tc, "getPubSubInboundReceiverIterator", itr);

		return itr;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.ibm.ws.sib.processor.runtime.SIMPLocalTopicSpaceControllable#
	 * getLocalSubscriptions
	 */
	public SIMPIterator getLocalSubscriptions() {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.entry(tc, "getLocalSubscriptions");

		SIMPIterator iterator = null;
		try {
			iterator = this.getTopicSpace().getLocalSubscriptionIterator();
		} catch (SIMPException e) {
			FFDCFilter.processException(e,
					"com.ibm.ws.sib.processor.runtime.LocalTopicSpaceControl",
					"1:409:1.34", this);

			SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002", new Object[] {
					"com.ibm.ws.sib.processor.runtime.LocalTopicSpaceControl",
					"1:414:1.34", SIMPUtils.getStackTrace(e) });
			SibTr.exception(tc, e);
		}
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.exit(tc, "getLocalSubscriptions", iterator);
		return iterator;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.ibm.ws.sib.processor.runtime.SIMPLocalTopicSpaceControllable#
	 * getPubSubDeliveryStreamSetReceiverIterator
	 */
	public SIMPIterator getPubSubDeliveryStreamSetReceiverIterator() {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.entry(tc, "getPubSubDeliveryStreamSetReceiverIterator");

		SIMPIterator iterator = this.getInputHandler().getTargetStreamManager()
				.getTargetStreamSetControlIterator();

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.entry(tc, "getPubSubDeliveryStreamSetReceiverIterator");
		return iterator;
	}
}
