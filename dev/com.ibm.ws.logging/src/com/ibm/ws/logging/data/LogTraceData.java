/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.data;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.ws.logging.collector.LogFieldConstants;
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
                                              LogFieldConstants.OBJECT_ID
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
                                                LogFieldConstants.OBJECT_ID
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
                                              LogFieldConstants.OBJECT_ID
    };

    private static NameAliases jsonLoggingNameAliasesMessages = new NameAliases(MESSAGE_NAMES1_1);

    public static void newJsonLoggingNameAliasesMessage(Map<String, String> newAliases) {
        jsonLoggingNameAliasesMessages.newAliases(newAliases);
    }

    private static NameAliases jsonLoggingNameAliasesTrace = new NameAliases(TRACE_NAMES1_1);

    public static void newJsonLoggingNameAliasesTrace(Map<String, String> newAliases) {
        jsonLoggingNameAliasesTrace.newAliases(newAliases);
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

    public String getDatetimeKeyJSON(boolean isMessageEvent) {
        if (!isMessageEvent) {
            return jsonLoggingNameAliasesTrace.getAlias(0);
        } else {
            return jsonLoggingNameAliasesMessages.getAlias(0);
        }

    }

    public String getMessageIdKeyJSON(boolean isMessageEvent) {
        if (!isMessageEvent) {
            return jsonLoggingNameAliasesTrace.getAlias(1);
        } else {
            return jsonLoggingNameAliasesMessages.getAlias(1);
        }
    }

    public String getThreadIdKeyJSON(boolean isMessageEvent) {
        if (!isMessageEvent) {
            return jsonLoggingNameAliasesTrace.getAlias(2);
        } else {
            return jsonLoggingNameAliasesMessages.getAlias(2);
        }
    }

    public String getModuleKeyJSON(boolean isMessageEvent) {
        if (!isMessageEvent) {
            return jsonLoggingNameAliasesTrace.getAlias(3);
        } else {
            return jsonLoggingNameAliasesMessages.getAlias(3);
        }
    }

    public String getSeverityKeyJSON(boolean isMessageEvent) {
        if (!isMessageEvent) {
            return jsonLoggingNameAliasesTrace.getAlias(4);
        } else {
            return jsonLoggingNameAliasesMessages.getAlias(4);
        }
    }

    public String getLoglevelKeyJSON(boolean isMessageEvent) {
        if (!isMessageEvent) {
            return jsonLoggingNameAliasesTrace.getAlias(5);
        } else {
            return jsonLoggingNameAliasesMessages.getAlias(5);
        }
    }

    public String getMethodNameKeyJSON(boolean isMessageEvent) {
        if (!isMessageEvent) {
            return jsonLoggingNameAliasesTrace.getAlias(6);
        } else {
            return jsonLoggingNameAliasesMessages.getAlias(6);
        }
    }

    public String getClassNameKeyJSON(boolean isMessageEvent) {
        if (!isMessageEvent) {
            return jsonLoggingNameAliasesTrace.getAlias(7);
        } else {
            return jsonLoggingNameAliasesMessages.getAlias(7);
        }
    }

    public String getLevelValueKeyJSON(boolean isMessageEvent) {
        if (!isMessageEvent) {
            return jsonLoggingNameAliasesTrace.getAlias(8);
        } else {
            return jsonLoggingNameAliasesMessages.getAlias(8);
        }
    }

    public String getThreadNameKeyJSON(boolean isMessageEvent) {
        if (!isMessageEvent) {
            return jsonLoggingNameAliasesTrace.getAlias(9);
        } else {
            return jsonLoggingNameAliasesMessages.getAlias(9);
        }
    }

    public String getCorrelationIdKeyJSON(boolean isMessageEvent) {
        if (!isMessageEvent) {
            return jsonLoggingNameAliasesTrace.getAlias(10);
        } else {
            return jsonLoggingNameAliasesMessages.getAlias(10);
        }
    }

    public String getOrgKeyJSON(boolean isMessageEvent) {
        if (!isMessageEvent) {
            return jsonLoggingNameAliasesTrace.getAlias(11);
        } else {
            return jsonLoggingNameAliasesMessages.getAlias(11);
        }
    }

    public String getProductKeyJSON(boolean isMessageEvent) {
        if (!isMessageEvent) {
            return jsonLoggingNameAliasesTrace.getAlias(12);
        } else {
            return jsonLoggingNameAliasesMessages.getAlias(12);
        }
    }

    public String getComponentKeyJSON(boolean isMessageEvent) {
        if (!isMessageEvent) {
            return jsonLoggingNameAliasesTrace.getAlias(13);
        } else {
            return jsonLoggingNameAliasesMessages.getAlias(13);
        }

    }

    public String getSequenceKeyJSON(boolean isMessageEvent) {
        if (!isMessageEvent) {
            return jsonLoggingNameAliasesTrace.getAlias(14);
        } else {
            return jsonLoggingNameAliasesMessages.getAlias(14);
        }
    }

    public String getThrowableKeyJSON(boolean isMessageEvent) {
        if (!isMessageEvent) {
            return jsonLoggingNameAliasesTrace.getAlias(15);
        } else {
            return jsonLoggingNameAliasesMessages.getAlias(15);
        }
    }

    public String getThrowableLocalizedKeyJSON(boolean isMessageEvent) {
        if (!isMessageEvent) {
            return jsonLoggingNameAliasesTrace.getAlias(16);
        } else {
            return jsonLoggingNameAliasesMessages.getAlias(16);
        }
    }

    public String getMessageKeyJSON(boolean isMessageEvent) {
        if (!isMessageEvent) {
            return jsonLoggingNameAliasesTrace.getAlias(17);
        } else {
            return jsonLoggingNameAliasesMessages.getAlias(17);
        }
    }

    public String getFormattedMsgKeyJSON(boolean isMessageEvent) {
        if (!isMessageEvent) {
            return jsonLoggingNameAliasesTrace.getAlias(18);
        } else {
            return jsonLoggingNameAliasesMessages.getAlias(18);
        }
    }

    public String getExtensionsKeyJSON(boolean isMessageEvent) {
        if (!isMessageEvent) {
            return jsonLoggingNameAliasesTrace.getAlias(19);
        } else {
            return jsonLoggingNameAliasesMessages.getAlias(19);
        }
    }

    public String getObjectIdKeyJSON(boolean isMessageEvent) {
        if (!isMessageEvent) {
            return jsonLoggingNameAliasesTrace.getAlias(20);
        } else {
            return jsonLoggingNameAliasesMessages.getAlias(20);
        }
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
