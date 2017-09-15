/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.mfp.schema;

import java.io.StringReader;

import com.ibm.ws.sib.mfp.jmf.impl.JSType;
import com.ibm.ws.sib.mfp.jmf.impl.JSchema;
import com.ibm.ws.sib.mfp.jmf.parser.JSParser;

/*
 *  Generated from D:\WORK\PROJECT\SiBus\Code\sib_developer_WAS855_cf021407.02\SIB\ws\code\sib.mfp.impl\src\com\ibm\ws\sib\mfp\schema\JsHdr2Schema.schema:
 *  do not edit directly
 */
public final class JsHdr2Schema extends JSchema {
    public JsHdr2Schema() {
        super((JSType) JSParser.parse(new StringReader("com.ibm.ws.sib.mfp.schema.JsHdr2Schema: [ *( forwardRoutingPath: [ destinationName: string, meId: byte8, busName: string ] )*, forwardRoutingPathLocalOnly: hexBinary, *( reverseRoutingPath: [ destinationName: string, meId: byte8, busName: string ] )*, reverseRoutingPathLocalOnly: hexBinary, priority: int, reliability: byte, timeToLive: long, timestamp: long, messageWaitTime: long, redeliveredCount: int, bus: string, producerType: byte, flags: byte, xappid: { empty: [] | compact: byte | stringvalue: string }, routingDestination: { empty: [] | value: [ name: string, meId: byte8, busName: string ] }, requestMetrics: { unset: [] | correlator: [ arm: string, rm: string ] }, reportExpiry: { unset: [] | value: byte }, reportCOD: { unset: [] | value: byte }, reportCOA: { unset: [] | value: byte }, guaranteed: { empty: [] | set: [ sourceMeUUID: byte8, targetMeUUID: byte8, targetDestDefUUID: byte12, streamUUID: byte12, protocolType: byte, protocolVersion: byte, gatheringTargetUUID: { empty: [] | value: byte12 } ] }, guaranteedXBus: { empty: [] | set: [ linkName: string, sourceBusUUID: byte8 ] }, guaranteedValue: { empty: [] | set: [ startTick: long, endTick: long, valueTick: long, completedPrefix: long, requestedOnly: boolean ] }, guaranteedRemoteBrowse: { empty: [] | set: [ browseId: long, seqNum: long ] }, guaranteedRemoteGet: { empty: [] | set: [ waitTime: long, prevTick: long, startTick: long, valueTick: long ] }, exception: { empty: [] | detail: [ reason: int, timestamp: long, *( inserts: string )*, problemDestination: { empty: [] | data: string }, problemSubscription: { empty: [] | data: string } ] }, jmsDeliveryMode: { empty: [] | data: byte }, jmsExpiration: { empty: [] | data: long }, jmsDestination: { empty: [] | data: hexBinary }, jmsReplyTo: { empty: [] | data: hexBinary }, jmsType: { empty: [] | data: string }, transportVersion: { empty: [] | data: anySimpleType }, auditSessionId: { empty: [] | data: string }, messageControlClassification: { empty: [] | data: string }, mqmdProperties: { empty: [] | *( map: [ name: string, value: anySimpleType ] )* }, fingerPrints: { empty: [] | *( rfpList: [ id: string ] )* }, xct_correlation_id: { empty: [] | data: string }, deliveryDelay: { empty: [] | data: long }, jmsDeliveryTime: { empty: [] | data: long } ]")));
    }
}
