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
// Generated from C:\SIB\Code\WASX.SIB\dd\SIB\ws\code\sib.mfp.impl\src\com\ibm\ws\sib\mfp\schema\TrmFirstContactSchema.schema: do not edit directly
package com.ibm.ws.sib.mfp.schema;
import com.ibm.ws.sib.mfp.jmf.impl.JSchema;
import com.ibm.ws.sib.mfp.jmf.impl.JSType;
import com.ibm.ws.sib.mfp.jmf.parser.JSParser;
import java.io.StringReader;
public final class TrmFirstContactSchema extends JSchema {
  public TrmFirstContactSchema() {
    super((JSType) JSParser.parse(new StringReader("com.ibm.ws.sib.mfp.schema.TrmFirstContactSchema: [ messageType: int, body: { empty: [] | clientBootstrapRequest: [ busName: string, credentialType: string, userid: string, password: string, targetGroupName: string, targetGroupType: string, targetSignificance: string, connectionProximity: string, targetTransportChain: string, bootstrapTransportChain: string, update1: { empty: [] | connectionMode: string } ] | clientBootstrapReply: [ returnCode: int, *( failureReason: string )*, endPointData: hexBinary, busName: string, subnetName: string, messagingEngineName: string ] | clientAttachRequest: [ busName: string, credentialType: string, userid: string, password: string, meName: string, subnetName: string, credentials: { empty: [] | tokenParts: [ tokenValue: hexBinary, tokenType: string ] } ] | clientAttachRequest2: [ requiredBusName: string, credentialType: string, userid: string, password: string, meUuid: byte8 ] | clientAttachReply: [ returnCode: int, *( failureReason: string )* ] | meConnectRequest: [ magicNumber: long, requiredBusName: string, userid: string, password: string, requiredMeName: string, requiredSubnetName: string, requestingMeName: string, requestingMeUuid: byte8, *( subnetMessagingEngines: string )*, credentialType: { empty: [] | credentialType: string }, credentials: { empty: [] | tokenParts: [ tokenValue: hexBinary, tokenType: string ] } ] | meConnectReply: [ magicNumber: long, returnCode: int, replyingMeUuid: byte8, *( subnetMessagingEngines: string )*, *( failureReason: string )*, credentials: { empty: [] | tokenParts: [ tokenValue: hexBinary, tokenType: string ] } ] | meLinkRequest: [ magicNumber: long, requiredBusName: string, userid: string, password: string, requiredMeName: string, requiredSubnetName: string, requestingMeName: string, requestingMeUuid: byte8, requestingSubnetName: string ] | meLinkReply: [ magicNumber: long, returnCode: int, replyingMeUuid: byte8, *( failureReason: string )* ] | meBridgeRequest: [ magicNumber: long, requiredBusName: string, userid: string, password: string, requiredMeName: string, requestingBusName: string, requestingMeName: string, requestingMeUuid: byte8, LinkName: string ] | meBridgeReply: [ magicNumber: long, returnCode: int, replyingMeUuid: byte8, *( failureReason: string )* ] | meBridgeBootstrapRequest: [ requiredBusName: string, userid: string, password: string, requestingBusName: string, LinkName: string, requiredTransportChain: string ] | meBridgeBootstrapReply: [ returnCode: int, *( failureReason: string )*, endPointData: hexBinary, busName: string, subnetName: string, messagingEngineName: string ] } ]")));
  }
}
