/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.metatype.validator;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class ValidatorMessage {
    private static final ResourceBundle bundle = ResourceBundle.getBundle("com.ibm.ws.metatype.validator.events", Locale.US);

    public enum MessageType {
        Error,
        Warning,
        Info
    }

    private final MessageType msgType;
    private final String msg;
    private final String msgKey;
    private final String id;

    public ValidatorMessage(MessageType msgType, String id, String msgKey, Object... objs) {
        this.msgType = msgType;
        this.msgKey = msgKey;
        this.id = id;
        this.msg = MessageFormat.format(bundle.getString(msgKey), objs);
    }

    public MessageType getMsgType() {
        return msgType;
    }

    public String getMsg() {
        return msg;
    }

    public String getMsgKey() {
        return msgKey;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (msgType == MessageType.Error)
            sb.append("[error  ] ");
        else if (msgType == MessageType.Warning)
            sb.append("[warning] ");
        else if (msgType == MessageType.Info)
            sb.append("[info   ] ");

        sb.append(msg);
        return sb.toString();
    }

    public String getId() {
        return id;
    }
}
