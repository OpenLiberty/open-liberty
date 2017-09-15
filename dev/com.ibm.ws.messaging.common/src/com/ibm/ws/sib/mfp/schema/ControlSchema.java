/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
// Generated from C:\SIB\Code\WASX.SIB\dd\SIB\ws\code\sib.mfp.impl\src\com\ibm\ws\sib\mfp\schema\ControlSchema.schema: do not edit directly
package com.ibm.ws.sib.mfp.schema;
import com.ibm.ws.sib.mfp.jmf.impl.JSchema;
import com.ibm.ws.sib.mfp.jmf.impl.JSType;
import com.ibm.ws.sib.mfp.jmf.parser.JSParser;
import java.io.StringReader;
public final class ControlSchema extends JSchema {
  public ControlSchema() {
    super((JSType) JSParser.parse(new StringReader("com.ibm.ws.sib.mfp.schema.ControlSchema: [ subType: byte, priority: int, reliability: byte, flags: byte, routingDestination: { empty: [] | value: [ name: string, meId: byte8, busName: string ] }, sourceMeUUID: byte8, targetMeUUID: byte8, targetDestDefUUID: byte12, streamUUID: byte12, protocolType: byte, protocolVersion: byte, guaranteedXBus: { empty: [] | set: [ linkName: string, sourceBusUUID: byte8 ] }, body: { empty: [] | ackExpected: [ tick: long ] | silence: [ startTick: long, endTick: long, completedPrefix: long, force: boolean, requestedOnly: boolean ] | ack: [ ackPrefix: long ] | nack: [ startTick: long, endTick: long, force: boolean ] | prevalue: [ startTick: long, endTick: long, valueTick: long, completedPrefix: long, force: boolean, requestedOnly: boolean ] | accept: [ *( tick: long )* ] | reject: [ *( startTick: long )*, *( endTick: long )*, recovery: boolean, rmeUnlockCount: { unset: [] | *( value: long )* } ] | decision: [ startTick: long, endTick: long, completedPrefix: long ] | request: [ *( filter: string )*, *( discriminator: string )*, *( selectorDomain: int )*, *( rejectStartTick: long )*, *( getTick: long )*, *( timeout: long )* ] | requestAck: [ dmeVersion: long, *( tick: long )* ] | requestHighestGeneratedTick: [ requestId: long ] | highestGeneratedTick: [ requestId: long, tick: long ] | resetRequestAck: [ dmeVersion: long ] | resetRequestAckAck: [ dmeVersion: long ] | browseGet: [ browseId: long, sequenceNumber: long, filter: string, discriminator: string, selectorDomain: int ] | browseEnd: [ browseId: long, exceptionCode: int ] | browseStatus: [ browseId: long, status: int ] | completed: [ *( startTick: long )*, *( endTick: long )* ] | decisionExpected: [ *( tick: long )* ] | createStream: [ requestId: long, subName: string, discriminator: string, selector: string, selectorDomain: int, securitySentBySystem: boolean, securityUserId: string, noLocal: { unset: [] | value: boolean }, cloned: { unset: [] | value: boolean } ] | createDurable: [ requestId: long, subName: string, discriminator: string, selector: string, selectorDomain: int, securitySentBySystem: boolean, securityUserId: string, noLocal: { unset: [] | value: boolean }, cloned: { unset: [] | value: boolean }, namespaceMap: { unset: [] | *( map: [ name: string, value: string ] )* } ] | deleteDurable: [ requestId: long, subName: string, securityUserId: string ] | durableConfirm: [ requestId: long, status: int ] | areYouFlushed: [ requestId: long ] | flushed: [] | notFlushed: [ requestId: long, *( completedQOS: int )*, *( completedPriority: int )*, *( completedPrefix: long )*, *( duplicateQOS: int )*, *( duplicatePriority: int )*, *( duplicatePrefix: long )* ] | requestFlush: [ requestId: long, inDoubtDiscard: boolean ] | requestCardinalityInfo: [ requestId: long ] | cardinalityInfo: [ requestId: long, currentCardinality: int ] }, gatheringTargetUUID: { empty: [] | value: byte12 } ]")));
  }
}
