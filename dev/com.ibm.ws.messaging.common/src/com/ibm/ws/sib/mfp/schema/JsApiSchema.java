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
// Generated from C:\SIB\Code\WASX.SIB\dd\SIB\ws\code\sib.mfp.impl\src\com\ibm\ws\sib\mfp\schema\JsApiSchema.schema: do not edit directly
package com.ibm.ws.sib.mfp.schema;
import com.ibm.ws.sib.mfp.jmf.impl.JSchema;
import com.ibm.ws.sib.mfp.jmf.impl.JSType;
import com.ibm.ws.sib.mfp.jmf.parser.JSParser;
import java.io.StringReader;
public final class JsApiSchema extends JSchema {
  public JsApiSchema() {
    super((JSType) JSParser.parse(new StringReader("com.ibm.ws.sib.mfp.schema.JsApiSchema: [ messageId: hexBinary, correlationId: { empty: [] | binaryValue: hexBinary | stringValue: string }, userId: string, connectionUuid: { unset: [] | value: byte12 }, replyDiscriminator: { unset: [] | value: string }, replyPriority: { unset: [] | value: int }, replyReliability: { unset: [] | value: byte }, replyTimeToLive: { unset: [] | value: long }, reportException: { unset: [] | value: byte }, reportPAN: { unset: [] | value: boolean }, reportNAN: { unset: [] | value: boolean }, reportPassMsgId: { unset: [] | value: boolean }, reportPassCorrelId: { unset: [] | value: boolean }, reportDiscardMsg: { unset: [] | value: boolean }, reportFeedback: { unset: [] | value: byte }, psc: { empty: [] | data: [ *( topics: string )*, subPoint: string, filter: string, options: int, pubTime: long, seqNum: int, queueManagerName: string, queueName: string, replyQueueManagerName: string, replyQueueName: string ] }, pscr: { empty: [] | data: [ completion: int, *( responses: [ topic: string, reason: int ] )*, command: string, topic: string, queueManagerName: string, queueName: string, options: int, correlationId: string, userid: string ] }, *( systemProperty: [ name: string, value: anySimpleType ] )*, *( jmsProperty: [ name: string, value: anySimpleType ] )*, *( otherProperty: [ name: string, value: anySimpleType ] )*, *( systemContext: [ name: string, value: anySimpleType ] )*, reportFeedbackInt: { unset: [] | value: int } ]")));
  }
}
