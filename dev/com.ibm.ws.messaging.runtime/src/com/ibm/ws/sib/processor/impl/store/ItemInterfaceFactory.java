/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.processor.impl.store;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.messaging.lifecycle.Singleton;
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

@Component(configurationPolicy=IGNORE, property={"type=com.ibm.ws.sib.processor.ItemInterface"})
public class ItemInterfaceFactory implements ItemInterface, Singleton {
    public AbstractItem getItemStreamInstance(String name) {
        switch (name) {
        case "com.ibm.ws.sib.processor.impl.store.MessageProcessorStore": return new MessageProcessorStore();
        case "com.ibm.ws.sib.processor.impl.DestinationManager": return new DestinationManager();
        case "com.ibm.ws.sib.processor.impl.BaseDestinationHandler": return new BaseDestinationHandler();
        case "com.ibm.ws.sib.processor.impl.store.itemstreams.TargetProtocolItemStream": return new TargetProtocolItemStream();
        case "com.ibm.ws.sib.processor.impl.store.itemstreams.SourceProtocolItemStream": return new SourceProtocolItemStream();
        case "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPLocalMsgsItemStream": return new PtoPLocalMsgsItemStream();
        case "com.ibm.ws.sib.processor.impl.store.itemstreams.PubSubMessageItemStream": return new PubSubMessageItemStream();
        case "com.ibm.ws.sib.processor.impl.store.itemstreams.ProxyReferenceStream": return new ProxyReferenceStream();
        case "com.ibm.ws.sib.processor.impl.store.itemstreams.DurableSubscriptionItemStream": return new DurableSubscriptionItemStream();
        case "com.ibm.ws.sib.processor.impl.store.itemstreams.SubscriptionItemStream": return new SubscriptionItemStream();
        case "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPXmitMsgsItemStream": return new PtoPXmitMsgsItemStream();
        case "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPReceiveMsgsItemStream": return new PtoPReceiveMsgsItemStream();
        case "com.ibm.ws.sib.mfp.impl.SchemaStoreItemStream": return new SchemaStoreItemStream();
        case "com.ibm.ws.sib.mfp.impl.SchemaStoreItem": return new SchemaStoreItem();
        case "com.ibm.ws.sib.processor.impl.store.items.MessageItem": return new MessageItem();
        case "com.ibm.ws.sib.processor.impl.store.items.MessageItemReference": return new MessageItemReference();
        }
        return null;
    }
}
