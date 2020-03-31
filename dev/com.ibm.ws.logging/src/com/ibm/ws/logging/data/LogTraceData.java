/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.data;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.ws.logging.collector.LogFieldConstants;
import com.ibm.ws.logging.data.NameAliases.ExtensionAliases;
import com.ibm.ws.logging.utils.SequenceNumber;

/**
 *
 */
public class LogTraceData extends GenericData {

    static Pattern messagePattern;
    private long rawSequenceNumber = -1;

    static {
        messagePattern = Pattern.compile("^([A-Z][\\dA-Z]{3,4})(\\d{4})([A-Z])(:)");
    }

    public final static String[] NAMES1_1 = {
                                              LogFieldConstants.IBM_DATETIME,
                                              LogFieldConstants.IBM_MESSAGEID,
                                              LogFieldConstants.IBM_THREADID,
                                              LogFieldConstants.MODULE,
                                              LogFieldConstants.SEVERITY,
                                              LogFieldConstants.LOGLEVEL,
                                              LogFieldConstants.IBM_METHODNAME,
                                              LogFieldConstants.IBM_CLASSNAME,
                                              LogFieldConstants.LEVELVALUE,
                                              LogFieldConstants.THREADNAME,
                                              LogFieldConstants.CORRELATION_ID,
                                              LogFieldConstants.ORG,
                                              LogFieldConstants.PRODUCT,
                                              LogFieldConstants.COMPONENT,
                                              LogFieldConstants.IBM_SEQUENCE,
                                              LogFieldConstants.THROWABLE,
                                              LogFieldConstants.THROWABLE_LOCALIZED,
                                              LogFieldConstants.MESSAGE,
                                              LogFieldConstants.FORMATTEDMSG,
                                              LogFieldConstants.EXTENSIONS_KVPL,
                                              LogFieldConstants.OBJECT_ID,
                                              LogFieldConstants.HOST,
                                              LogFieldConstants.IBM_USERDIR,
                                              LogFieldConstants.IBM_SERVERNAME,
                                              LogFieldConstants.TYPE
    };

    private final static String[] NAMES = {
                                            LogFieldConstants.DATETIME,
                                            LogFieldConstants.MESSAGEID,
                                            LogFieldConstants.THREADID,
                                            LogFieldConstants.LOGGERNAME,
                                            LogFieldConstants.SEVERITY,
                                            LogFieldConstants.LOGLEVEL,
                                            LogFieldConstants.METHODNAME,
                                            LogFieldConstants.CLASSNAME,
                                            LogFieldConstants.LEVELVALUE,
                                            LogFieldConstants.THREADNAME,
                                            LogFieldConstants.CORRELATION_ID,
                                            LogFieldConstants.ORG,
                                            LogFieldConstants.PRODUCT,
                                            LogFieldConstants.COMPONENT,
                                            LogFieldConstants.SEQUENCE,
                                            LogFieldConstants.THROWABLE,
                                            LogFieldConstants.THROWABLE_LOCALIZED,
                                            LogFieldConstants.MESSAGE,
                                            LogFieldConstants.FORMATTEDMSG,
                                            LogFieldConstants.EXTENSIONS_KVPL,
                                            LogFieldConstants.OBJECT_ID
    };

    public static String[] MESSAGE_NAMES1_1 = {
                                                LogFieldConstants.IBM_DATETIME,
                                                LogFieldConstants.IBM_MESSAGEID,
                                                LogFieldConstants.IBM_THREADID,
                                                LogFieldConstants.MODULE,
                                                LogFieldConstants.SEVERITY,
                                                LogFieldConstants.LOGLEVEL,
                                                LogFieldConstants.IBM_METHODNAME,
                                                LogFieldConstants.IBM_CLASSNAME,
                                                LogFieldConstants.LEVELVALUE,
                                                LogFieldConstants.THREADNAME,
                                                LogFieldConstants.CORRELATION_ID,
                                                LogFieldConstants.ORG,
                                                LogFieldConstants.PRODUCT,
                                                LogFieldConstants.COMPONENT,
                                                LogFieldConstants.IBM_SEQUENCE,
                                                LogFieldConstants.THROWABLE,
                                                LogFieldConstants.THROWABLE_LOCALIZED,
                                                LogFieldConstants.MESSAGE,
                                                LogFieldConstants.FORMATTEDMSG,
                                                LogFieldConstants.EXTENSIONS_KVPL,
                                                LogFieldConstants.OBJECT_ID,
                                                LogFieldConstants.HOST,
                                                LogFieldConstants.IBM_USERDIR,
                                                LogFieldConstants.IBM_SERVERNAME,
                                                LogFieldConstants.TYPE
    };

    public static String[] TRACE_NAMES1_1 = {
                                              LogFieldConstants.IBM_DATETIME,
                                              LogFieldConstants.IBM_MESSAGEID,
                                              LogFieldConstants.IBM_THREADID,
                                              LogFieldConstants.MODULE,
                                              LogFieldConstants.SEVERITY,
                                              LogFieldConstants.LOGLEVEL,
                                              LogFieldConstants.IBM_METHODNAME,
                                              LogFieldConstants.IBM_CLASSNAME,
                                              LogFieldConstants.LEVELVALUE,
                                              LogFieldConstants.THREADNAME,
                                              LogFieldConstants.CORRELATION_ID,
                                              LogFieldConstants.ORG,
                                              LogFieldConstants.PRODUCT,
                                              LogFieldConstants.COMPONENT,
                                              LogFieldConstants.IBM_SEQUENCE,
                                              LogFieldConstants.THROWABLE,
                                              LogFieldConstants.THROWABLE_LOCALIZED,
                                              LogFieldConstants.MESSAGE,
                                              LogFieldConstants.FORMATTEDMSG,
                                              LogFieldConstants.EXTENSIONS_KVPL,
                                              LogFieldConstants.OBJECT_ID,
                                              LogFieldConstants.HOST,
                                              LogFieldConstants.IBM_USERDIR,
                                              LogFieldConstants.IBM_SERVERNAME,
                                              LogFieldConstants.TYPE
    };

    private static NameAliases jsonLoggingNameAliasesMessages = new NameAliases(MESSAGE_NAMES1_1);
    private static NameAliases jsonLoggingNameAliasesTrace = new NameAliases(TRACE_NAMES1_1);

    public static void newJsonLoggingNameAliasesMessage(Map<String, String> newAliases) {
        jsonLoggingNameAliasesMessages.newAliases(newAliases);
    }

    public static void newJsonLoggingNameAliasesTrace(Map<String, String> newAliases) {
        jsonLoggingNameAliasesTrace.newAliases(newAliases);
    }

    public static void resetJsonLoggingNameAliasesMessage() {
        jsonLoggingNameAliasesMessages.resetAliases();
    }

    public static void resetJsonLoggingNameAliasesTrace() {
        jsonLoggingNameAliasesTrace.resetAliases();
    }

    public LogTraceData() {
        super(21);
    }

    private void setPair(int index, String s) {
        setPair(index, NAMES1_1[index], s);
    }

    private void setPair(int index, int i) {
        setPair(index, NAMES1_1[index], i);
    }

    private void setPair(int index, long l) {
        setPair(index, NAMES1_1[index], l);
    }

    private KeyValuePairList getValues(int index) {
        return (KeyValuePairList) getPairs()[index];
    }

    public void setDatetime(long l) {
        setPair(0, l);
    }

    public void setMessageId(String s) {
        setPair(1, s);
    }

    public void setThreadId(int i) {
        setPair(2, i);
    }

    public void setModule(String s) {
        setPair(3, s);
    }

    public void setSeverity(String s) {
        setPair(4, s);
    }

    public void setLoglevel(String s) {
        setPair(5, s);
    }

    public void setMethodName(String s) {
        setPair(6, s);
    }

    public void setClassName(String s) {
        setPair(7, s);
    }

    public void setLevelValue(int i) {
        setPair(8, i);
    }

    public void setThreadName(String s) {
        setPair(9, s);
    }

    public void setCorrelationId(String s) {
        setPair(10, s);
    }

    public void setOrg(String s) {
        setPair(11, s);
    }

    public void setProduct(String s) {
        setPair(12, s);
    }

    public void setComponent(String s) {
        setPair(13, s);
    }

    public void setSequence(String s) {
        setPair(14, s);
    }

    public void setThrowable(String s) {
        setPair(15, s);
    }

    public void setThrowableLocalized(String s) {
        setPair(16, s);
    }

    public void setMessage(String s) {
        setPair(17, s);
    }

    public void setFormattedMsg(String s) {
        setPair(18, s);
    }

    public void setExtensions(KeyValuePairList kvps) {
        setPair(19, kvps);
    }

    public void setObjectId(int i) {
        setPair(20, i);
    }

    public String getDatetimeKey() {
        return NAMES[0];
    }

    public String getMessageIdKey() {
        return NAMES[1];
    }

    public String getThreadIdKey() {
        return NAMES[2];
    }

    public String getModuleKey() {
        return NAMES[3];
    }

    public String getSeverityKey() {
        return NAMES[4];
    }

    public String getLoglevelKey() {
        return NAMES[5];
    }

    public String getMethodNameKey() {
        return NAMES[6];
    }

    public String getClassNameKey() {
        return NAMES[7];
    }

    public String getLevelValueKey() {
        return NAMES[8];
    }

    public String getThreadNameKey() {
        return NAMES[9];
    }

    public String getCorrelationIdKey() {
        return NAMES[10];
    }

    public String getOrgKey() {
        return NAMES[11];
    }

    public String getProductKey() {
        return NAMES[12];
    }

    public String getComponentKey() {
        return NAMES[13];
    }

    public String getSequenceKey() {
        return NAMES[14];
    }

    public String getThrowableKey() {
        return NAMES[15];
    }

    public String getThrowableLocalizedKey() {
        return NAMES[16];
    }

    public String getMessageKey() {
        return NAMES[17];
    }

    public String getFormattedMsgKey() {
        return NAMES[18];
    }

    public String getExtensionsKey() {
        return NAMES[19];
    }

    public String getObjectIdKey() {
        return NAMES[20];
    }

    public String getDatetimeKey1_1() {
        return NAMES1_1[0];
    }

    public String getMessageIdKey1_1() {
        return NAMES1_1[1];
    }

    public String getThreadIdKey1_1() {
        return NAMES1_1[2];
    }

    public String getModuleKey1_1() {
        return NAMES1_1[3];
    }

    public String getSeverityKey1_1() {
        return NAMES1_1[4];
    }

    public String getLoglevelKey1_1() {
        return NAMES1_1[5];
    }

    public String getMethodNameKey1_1() {
        return NAMES1_1[6];
    }

    public String getClassNameKey1_1() {
        return NAMES1_1[7];
    }

    public String getLevelValueKey1_1() {
        return NAMES1_1[8];
    }

    public String getThreadNameKey1_1() {
        return NAMES1_1[9];
    }

    public String getCorrelationIdKey1_1() {
        return NAMES1_1[10];
    }

    public String getOrgKey1_1() {
        return NAMES1_1[11];
    }

    public String getProductKey1_1() {
        return NAMES1_1[12];
    }

    public String getComponentKey1_1() {
        return NAMES1_1[13];
    }

    public String getSequenceKey1_1() {
        return NAMES1_1[14];
    }

    public String getThrowableKey1_1() {
        return NAMES1_1[15];
    }

    public String getThrowableLocalizedKey1_1() {
        return NAMES1_1[16];
    }

    public String getMessageKey1_1() {
        return NAMES1_1[17];
    }

    public String getFormattedMsgKey1_1() {
        return NAMES1_1[18];
    }

    public String getExtensionsKey1_1() {
        return NAMES1_1[19];
    }

    public String getObjectIdKey1_1() {
        return NAMES1_1[20];
    }

    //name aliases
    public static String getDatetimeKeyJSON(boolean isMessageEvent) {
        return isMessageEvent ? jsonLoggingNameAliasesMessages.aliases[0] : jsonLoggingNameAliasesTrace.aliases[0];
    }

    public static String getMessageIdKeyJSON(boolean isMessageEvent) {
        return isMessageEvent ? jsonLoggingNameAliasesMessages.aliases[1] : jsonLoggingNameAliasesTrace.aliases[1];
    }

    public static String getThreadIdKeyJSON(boolean isMessageEvent) {
        return isMessageEvent ? jsonLoggingNameAliasesMessages.aliases[2] : jsonLoggingNameAliasesTrace.aliases[2];
    }

    public static String getModuleKeyJSON(boolean isMessageEvent) {
        return isMessageEvent ? jsonLoggingNameAliasesMessages.aliases[3] : jsonLoggingNameAliasesTrace.aliases[3];
    }

    public static String getSeverityKeyJSON(boolean isMessageEvent) {
        return isMessageEvent ? jsonLoggingNameAliasesMessages.aliases[4] : jsonLoggingNameAliasesTrace.aliases[4];
    }

    public static String getLoglevelKeyJSON(boolean isMessageEvent) {
        return isMessageEvent ? jsonLoggingNameAliasesMessages.aliases[5] : jsonLoggingNameAliasesTrace.aliases[5];
    }

    public static String getMethodNameKeyJSON(boolean isMessageEvent) {
        return isMessageEvent ? jsonLoggingNameAliasesMessages.aliases[6] : jsonLoggingNameAliasesTrace.aliases[6];
    }

    public static String getClassNameKeyJSON(boolean isMessageEvent) {
        return isMessageEvent ? jsonLoggingNameAliasesMessages.aliases[7] : jsonLoggingNameAliasesTrace.aliases[7];
    }

    public static String getLevelValueKeyJSON(boolean isMessageEvent) {
        return isMessageEvent ? jsonLoggingNameAliasesMessages.aliases[8] : jsonLoggingNameAliasesTrace.aliases[8];
    }

    public static String getThreadNameKeyJSON(boolean isMessageEvent) {
        return isMessageEvent ? jsonLoggingNameAliasesMessages.aliases[9] : jsonLoggingNameAliasesTrace.aliases[9];
    }

    public static String getCorrelationIdKeyJSON(boolean isMessageEvent) {
        return isMessageEvent ? jsonLoggingNameAliasesMessages.aliases[10] : jsonLoggingNameAliasesTrace.aliases[10];
    }

    public static String getOrgKeyJSON(boolean isMessageEvent) {
        return isMessageEvent ? jsonLoggingNameAliasesMessages.aliases[11] : jsonLoggingNameAliasesTrace.aliases[11];
    }

    public static String getProductKeyJSON(boolean isMessageEvent) {
        return isMessageEvent ? jsonLoggingNameAliasesMessages.aliases[12] : jsonLoggingNameAliasesTrace.aliases[12];
    }

    public static String getComponentKeyJSON(boolean isMessageEvent) {
        return isMessageEvent ? jsonLoggingNameAliasesMessages.aliases[13] : jsonLoggingNameAliasesTrace.aliases[13];
    }

    public static String getSequenceKeyJSON(boolean isMessageEvent) {
        return isMessageEvent ? jsonLoggingNameAliasesMessages.aliases[14] : jsonLoggingNameAliasesTrace.aliases[14];
    }

    public static String getThrowableKeyJSON(boolean isMessageEvent) {
        return isMessageEvent ? jsonLoggingNameAliasesMessages.aliases[15] : jsonLoggingNameAliasesTrace.aliases[15];
    }

    public static String getThrowableLocalizedKeyJSON(boolean isMessageEvent) {
        return isMessageEvent ? jsonLoggingNameAliasesMessages.aliases[16] : jsonLoggingNameAliasesTrace.aliases[16];
    }

    public static String getMessageKeyJSON(boolean isMessageEvent) {
        return isMessageEvent ? jsonLoggingNameAliasesMessages.aliases[17] : jsonLoggingNameAliasesTrace.aliases[17];
    }

    public static String getFormattedMsgKeyJSON(boolean isMessageEvent) {
        return isMessageEvent ? jsonLoggingNameAliasesMessages.aliases[18] : jsonLoggingNameAliasesTrace.aliases[18];
    }

    public static String getExtensionsKeyJSON(boolean isMessageEvent) {
        return isMessageEvent ? jsonLoggingNameAliasesMessages.aliases[19] : jsonLoggingNameAliasesTrace.aliases[19];
    }

    public static String getObjectIdKeyJSON(boolean isMessageEvent) {
        return isMessageEvent ? jsonLoggingNameAliasesMessages.aliases[20] : jsonLoggingNameAliasesTrace.aliases[20];
    }

    public static String getHostKeyJSON(boolean isMessageEvent) {
        return isMessageEvent ? jsonLoggingNameAliasesMessages.aliases[21] : jsonLoggingNameAliasesTrace.aliases[21];
    }

    public static String getUserDirKeyJSON(boolean isMessageEvent) {
        return isMessageEvent ? jsonLoggingNameAliasesMessages.aliases[22] : jsonLoggingNameAliasesTrace.aliases[22];
    }

    public static String getServerNameKeyJSON(boolean isMessageEvent) {
        return isMessageEvent ? jsonLoggingNameAliasesMessages.aliases[23] : jsonLoggingNameAliasesTrace.aliases[23];
    }

    public static String getTypeKeyJSON(boolean isMessageEvent) {
        return isMessageEvent ? jsonLoggingNameAliasesMessages.aliases[24] : jsonLoggingNameAliasesTrace.aliases[24];
    }

    public static String getExtensionNameKeyJSON(boolean isMessageEvent, String extKey) {
        ExtensionAliases tempExt = null;
        if (isMessageEvent) {
            tempExt = jsonLoggingNameAliasesMessages.extensionAliases;
        } else {
            tempExt = jsonLoggingNameAliasesTrace.extensionAliases;
        }
        return tempExt.getAlias(extKey);

    }

    public long getDatetime() {
        return getLongValue(0);
    }

    public String getMessageId() {
        String messageId = getStringValue(1);
        if (messageId == null || messageId.isEmpty()) {
            String message = getMessage();
            if (message != null) {
                messageId = parseMessageId(message);
                this.setPair(1, NAMES1_1[1], messageId);
            }
        }
        return messageId;
    }

    public int getThreadId() {
        return getIntValue(2);
    }

    public String getModule() {
        return getStringValue(3);
    }

    public String getSeverity() {
        return getStringValue(4);
    }

    public String getLoglevel() {
        return getStringValue(5);
    }

    public String getMethodName() {
        return getStringValue(6);
    }

    public String getClassName() {
        return getStringValue(7);
    }

    public int getLevelValue() {
        return getIntValue(8);
    }

    public String getThreadName() {
        return getStringValue(9);
    }

    public String getCorrelationId() {
        return getStringValue(10);
    }

    public String getOrg() {
        return getStringValue(11);
    }

    public String getProduct() {
        return getStringValue(12);
    }

    public String getComponent() {
        return getStringValue(13);
    }

    public String getSequence() {
        String sequenceId = getStringValue(14);
        if (sequenceId == null || sequenceId.isEmpty()) {
            sequenceId = SequenceNumber.formatSequenceNumber(getDatetime(), rawSequenceNumber);
            this.setPair(14, NAMES1_1[14], sequenceId);
        }
        return sequenceId;
    }

    public String getThrowable() {
        return getStringValue(15);
    }

    public String getThrowableLocalized() {
        return getStringValue(16);
    }

    public String getMessage() {
        return getStringValue(17);
    }

    public String getFormattedMsg() {
        return getStringValue(18);
    }

    public KeyValuePairList getExtensions() {
        return getValues(19);
    }

    public int getObjectId() {
        return getIntValue(20);
    }

    public void setRawSequenceNumber(long l) {
        rawSequenceNumber = l;
    }

    public long getRawSequenceNumber(long l) {
        return rawSequenceNumber;
    }

    /**
     * @return the message ID for the given message.
     */
    protected String parseMessageId(String msg) {
        String messageId = null;
        Matcher matcher = messagePattern.matcher(msg);
        if (matcher.find())
            messageId = msg.substring(matcher.start(), matcher.end() - 1);
        return messageId;
    }

}