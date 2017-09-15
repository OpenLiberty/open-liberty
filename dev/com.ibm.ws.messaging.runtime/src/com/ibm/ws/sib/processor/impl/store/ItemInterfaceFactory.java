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

package com.ibm.ws.sib.processor.impl.store;

import com.ibm.ws.sib.mfp.impl.SchemaStoreItem;
import com.ibm.ws.sib.mfp.impl.SchemaStoreItemStream;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.processor.ItemInterface;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.DestinationManager;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.processor.impl.store.items.MessageItemReference;
import com.ibm.ws.sib.processor.impl.store.itemstreams.DurableSubscriptionItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.ProxyReferenceStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPLocalMsgsItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPReceiveMsgsItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPXmitMsgsItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PubSubMessageItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.SourceProtocolItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.SubscriptionItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.TargetProtocolItemStream;

public class ItemInterfaceFactory implements ItemInterface {

	public ItemInterfaceFactory() {
	}

	public AbstractItem getItemStreamInstance(String name) {
		if (name
				.equals("com.ibm.ws.sib.processor.impl.store.MessageProcessorStore")) {
			return new MessageProcessorStore();
		} else if (name
				.equals("com.ibm.ws.sib.processor.impl.DestinationManager")) {
			return new DestinationManager();
		} else if (name
				.equals("com.ibm.ws.sib.processor.impl.BaseDestinationHandler")) {
			return new BaseDestinationHandler();
		} else if (name
				.equals("com.ibm.ws.sib.processor.impl.store.itemstreams.TargetProtocolItemStream")) {
			return new TargetProtocolItemStream();
		} else if (name
				.equals("com.ibm.ws.sib.processor.impl.store.itemstreams.SourceProtocolItemStream")) {
			return new SourceProtocolItemStream();
		} else if (name
				.equals("com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPLocalMsgsItemStream")) {
			return new PtoPLocalMsgsItemStream();
		} else if (name
				.equals("com.ibm.ws.sib.processor.impl.store.itemstreams.PubSubMessageItemStream")) {
			return new PubSubMessageItemStream();
		} else if (name
				.equals("com.ibm.ws.sib.processor.impl.store.itemstreams.ProxyReferenceStream")) {
			return new ProxyReferenceStream();
		} else if (name
				.equals("com.ibm.ws.sib.processor.impl.store.itemstreams.DurableSubscriptionItemStream")) {
			return new DurableSubscriptionItemStream();
		} else if (name
				.equals("com.ibm.ws.sib.processor.impl.store.itemstreams.SubscriptionItemStream")) {
			return new SubscriptionItemStream();
		} else if (name
				.equals("com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPXmitMsgsItemStream")) {
			return new PtoPXmitMsgsItemStream();
		} else if (name
				.equals("com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPReceiveMsgsItemStream")) {
			return new PtoPReceiveMsgsItemStream();
		} else if (name.equals("com.ibm.ws.sib.mfp.impl.SchemaStoreItemStream")) {
			return new SchemaStoreItemStream();
		} else if (name.equals("com.ibm.ws.sib.mfp.impl.SchemaStoreItem")) {
			return new SchemaStoreItem();
		} else if (name
				.equals("com.ibm.ws.sib.processor.impl.store.items.MessageItem")) {
			return new MessageItem();
		} else if (name
				.equals("com.ibm.ws.sib.processor.impl.store.items.MessageItemReference")) {
			return new MessageItemReference();
		}
		return null;
	}
}
