/*******************************************************************************
 * Copyright 2025,2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.sib.admin.internal;

import java.io.PrintWriter;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.admin.JsEngineComponent;
import com.ibm.ws.sib.admin.JsMain;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.processor.impl.ConsumerDispatcher;
import com.ibm.ws.sib.processor.impl.DestinationManager;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.indexes.DestinationIndex;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.impl.ControlAdapter;
import com.ibm.ws.sib.processor.runtime.impl.MessageProcessorControl;
import com.ibm.ws.sib.processor.runtime.impl.VirtualLinkControl;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.wsspi.logging.Introspector;

@Component(immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE, property = {
		Constants.SERVICE_VENDOR + "=" + "IBM" })
public class SibIntrospectorImpl implements Introspector {
	private static final TraceComponent tc = Tr.register(SibIntrospectorImpl.class);

	/**
	 * This spec controls what will be dumped. It splits on ":" into tokens and then
	 * dumps classNames that match the token. Only these two classes will be
	 * searched for
	 * MessageStoreImpl can be upgraded to MessageStoreImpl=all (see
	 * MessageStoreInterface) but I think the null value is what we want.
	 */
	private final static String DUMP_SPEC = "com.ibm.ws.sib.processor.impl.MessageProcessor:com.ibm.ws.sib.msgstore.impl.MessageStoreImpl";

	@Reference
	JsMain jsMain;

	@Override
	public String getIntrospectorName() {
		return "MessagingIntrospector";
	}

	@Override
	public String getIntrospectorDescription() {
		return "Messaging Diagnostics";
	}

	@Override
	public void introspect(PrintWriter out) throws Exception {

		if (!(jsMain instanceof JsMainImpl)) {
			out.println();
			out.println("The JsMain class was not an instance of JsMainImpl");
			return;
		}

		// This dump lists the message stores and their configuration.
		JsMainImpl mainImpl = (JsMainImpl) jsMain;
		Enumeration<JsMessagingEngine> engines = mainImpl.listMessagingEngines();

		JsMessagingEngine engine = null; // The API returns a list but the impl is hardcoded to one.
		while (engines.hasMoreElements()) {
			engine = engines.nextElement();


			if (engine instanceof JsMessagingEngineImpl) {
				dumpEngineContents(out, (JsMessagingEngineImpl) engine);
			}
			out.println();
		}

		JsEngineComponent messageProcessor = engine.getMessageProcessor(); //Note that there is one engine, and it has one MessageProcessor

		if (messageProcessor != null && messageProcessor instanceof MessageProcessor) {
			dumpMessageProcessor(out, (MessageProcessor) messageProcessor);
		}
	}

	private void dumpEngineContents(PrintWriter out, JsMessagingEngineImpl engine ) {
		JsMessagingEngineImpl jsEngine = (JsMessagingEngineImpl) engine;
		FormattedWriter fw = new FormattedWriter(out);

		out.println("Engine name: " + engine.getName());//hardcoded but why not
		out.println("Engine UUID: " + engine.getUuid());
		out.println("Engine state: " + jsEngine.getState());
		out.println("Threshold: " + jsEngine.getMEThreshold());

		jsEngine.dump(DUMP_SPEC, fw, new Date());

		// Next lets add info about the prepared transactions.

		out.println("=== prepared transactions ===");
		Optional<String[]> preparedTransactions = Optional.ofNullable(((JsMessagingEngineImpl) engine).listPreparedTransactions());
		preparedTransactions.ifPresent(s -> out.println(String.join(", ", s)));
		out.println();
	}

	private void dumpMessageProcessor(PrintWriter out, MessageProcessor messageProcessor) {

		//As per comments on DM:
		//* Cold start constructor. There should be only one destination
		//* manager per ME.

		DestinationManager destinationManager = ((MessageProcessor) messageProcessor).getDestinationManager();
		Map<String, ConsumerDispatcher> durableSubscriptions = destinationManager.getDurableSubscriptionsTable();	
		out.println("=== Durable Subscriptions ===");
		durableSubscriptions.forEach((k,v) -> out.println(k + ":" + v));

		out.println("=== Nondurable Subscriptions ===");
		ConcurrentHashMap<String, ConsumerDispatcher> nonDurableSubscirptions = destinationManager.getNondurableSharedSubscriptions();
		nonDurableSubscirptions.forEach((k,v) -> out.println(k + ":" + v));

		out.println("=== Destinations ===");
		DestinationIndex destinationIndex = destinationManager.getDestinationIndex();
		SIMPIterator destinationIterator = destinationIndex.iterator();
		while (destinationIterator.hasNext()) {
			Object destiation = destinationIterator.next();

			if (destiation != null && destiation instanceof DestinationHandler) {
				DestinationHandler destinationHandler = (DestinationHandler) destiation;
				out.println("Destination. Name: " + destinationHandler.getName() + " description : "+ destinationHandler.getDescription() +  "alias? " + destinationHandler.isAlias());
			} else {
				out.println("Unexpected value for destination. Destination was " + destiation == null ? "null" : destiation.getClass().getName()); 
			}
		}


		//Connectivity

		//Not needed, part of jsEngine.dump
		//HashMap<SICoreConnection, SICoreConnection> connections = ((MessageProcessor) messageProcessor).getConnections();

		out.println("=== Virtual Links ===");
		ControlAdapter controlAdapter = ((MessageProcessor) messageProcessor).getControlAdapter();
		if (controlAdapter instanceof MessageProcessorControl) {
			SIMPIterator virtualLinkIterator = ((MessageProcessorControl) controlAdapter).getVirtualLinkIterator();
			while (virtualLinkIterator.hasNext()) {
				Object virtualLink = virtualLinkIterator.next();
				if (virtualLink != null && virtualLink instanceof VirtualLinkControl) {
					VirtualLinkControl virtualLinkControl = (VirtualLinkControl) virtualLink;
					out.println(virtualLinkControl.getDebugInfo());
				} else {
					out.println("Unexpected value for virtualLink. virtualLink was " + virtualLink == null ? "null" : virtualLink.getClass().getName());
				}
			}
		}
	}
}