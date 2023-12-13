/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.service.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;


public abstract class AbstractMessageContainer extends AbstractPropertiesHolder implements NamedItem {
    protected QName mName;
    private OperationInfo operation;
    private Map<QName, MessagePartInfo> messageParts
        = new LinkedHashMap<>(4);
    private List<MessagePartInfo> outOfBandParts;
    private String documentation;


    /**
     * Initializes a new instance of the <code>MessagePartContainer</code>.
     * @param op the operation.
     * @param nm
     */
    AbstractMessageContainer(OperationInfo op, QName nm) {
        operation = op;
        mName = nm;
    }

    public String getMessageDocumentation() {
        return documentation;
    }
    public void setMessageDocumentation(String doc) {
        documentation = doc;
    }

    public QName getName() {
        return mName;
    }

    /**
     * Returns the operation of this container.
     *
     * @return the operation.
     */
    public OperationInfo getOperation() {
        return operation;
    }

    /**
     * Adds a message part to this container.
     *
     * @param name  the qualified name of the message part
     * @return name  the newly created <code>MessagePartInfo</code> object
     */
    public MessagePartInfo addMessagePart(QName name) {
        if (name == null) {
            throw new IllegalArgumentException("Invalid name [null]");
        }

        MessagePartInfo part = new MessagePartInfo(name, this);
        addMessagePart(part);
        return part;
    }

    public QName getMessagePartQName(String name) {
        return new QName(this.getOperation().getInterface().getName().getNamespaceURI(), name);
    }

    public MessagePartInfo addMessagePart(String name) {
        return addMessagePart(getMessagePartQName(name));
    }
    /**
     * Adds a message part to this container.
     *
     * @param part the message part.
     */
    public void addMessagePart(MessagePartInfo part) {
        if (messageParts.containsKey(part.getName())) {
            part.setIndex(messageParts.get(part.getName()).getIndex());
        } else {
            part.setIndex(messageParts.size());
        }
        messageParts.put(part.getName(), part);
    }

    public int getMessagePartIndex(MessagePartInfo part) {
        int i = 0;
        for (MessagePartInfo p : messageParts.values()) {
            if (part == p) {
                return i;
            }
            i++;
        }
        for (MessagePartInfo p : getOutOfBandParts()) {
            if (part == p) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public MessagePartInfo getMessagePartByIndex(int i) {
        for (MessagePartInfo p : messageParts.values()) {
            if (p.getIndex() == i) {
                return p;
            }
        }
        for (MessagePartInfo p : getOutOfBandParts()) {
            if (p.getIndex() == i) {
                return p;
            }
        }
        return null;
    }

    /**
     * Removes an message part from this container.
     *
     * @param name the qualified message part name.
     */
    public void removeMessagePart(QName name) {
        MessagePartInfo messagePart = getMessagePart(name);
        if (messagePart != null) {
            messageParts.remove(name);
        }
    }

    /**
     * Returns the message part with the given name, if found.
     *
     * @param name the qualified name.
     * @return the message part; or <code>null</code> if not found.
     */
    public MessagePartInfo getMessagePart(QName name) {
        MessagePartInfo mpi = messageParts.get(name);
        if (mpi != null) {
            return mpi;
        }
        for (MessagePartInfo mpi2 : messageParts.values()) {
            if (name.equals(mpi2.getConcreteName())) {
                return mpi2;
            }
        }
        for (MessagePartInfo mpi2 : getOutOfBandParts()) {
            if (name.equals(mpi2.getName())
                || name.equals(mpi2.getConcreteName())) {
                return mpi2;
            }
        }
        return mpi;
    }

    /**
     * Returns the n'th message part.
     *
     * @param n the n'th part to retrieve.
     * @return the message part; or <code>null</code> if not found.
     */
    public MessagePartInfo getMessagePart(int n) {
        if (n == -1) {
            return null;
        }
        List<MessagePartInfo> mpis = getMessageParts();
        return n < mpis.size() ? mpis.get(n) : null;
    }


    public MessagePartInfo addOutOfBandMessagePart(QName name) {
        if (name == null) {
            throw new IllegalArgumentException("Invalid name [null]");
        }

        MessagePartInfo part = new MessagePartInfo(name, this);
        if (outOfBandParts == null) {
            outOfBandParts = new ArrayList<>(1);
        }
        part.setIndex(messageParts.size() + outOfBandParts.size());
        outOfBandParts.add(part);
        return part;
    }



    /**
     * Returns all message parts for this message.
     *
     * @return all message parts.
     */
    public List<MessagePartInfo> getMessageParts() {
        if (outOfBandParts == null) {
            return new ArrayList<>(messageParts.values());
        }
        List<MessagePartInfo> parts = new ArrayList<>(messageParts.values());
        parts.addAll(outOfBandParts);
        return parts;
    }
    public int getMessagePartsNumber() {
        if (outOfBandParts == null) {
            return messageParts.size();
        }
        return messageParts.size() + outOfBandParts.size();
    }
    public MessagePartInfo getFirstMessagePart() {
        if (!messageParts.isEmpty()) {
            return messageParts.values().iterator().next();
        } else if (outOfBandParts != null && !outOfBandParts.isEmpty()) {
            return outOfBandParts.get(0);
        } else {
            return null;
        }
    }
    public List<MessagePartInfo> getOutOfBandParts() {
        if (outOfBandParts == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(outOfBandParts);
    }

    public int size() {
        return messageParts.size() + getOutOfBandParts().size();
    }


    public int hashCode() {
        return mName == null ? -1 : mName.hashCode();
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof AbstractMessageContainer)) {
            return false;
        }
        AbstractMessageContainer oi = (AbstractMessageContainer)o;
        return equals(mName, oi.mName)
            && equals(messageParts, oi.messageParts)
            && equals(outOfBandParts, oi.outOfBandParts);
    }

}
