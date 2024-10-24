/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf.conversationscoped.bean;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@ConversationScoped
public class ConversationScopedBean implements Serializable {

    @Inject
    private Conversation conversation;

    private int count;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @PostConstruct
    public void init(){
          conversation.begin();
          count = 0;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void incrementCount(){
        this.count++;
    }

    public String nextPage(){
        return "page2?faces-redirect=true";
    }

    public String end(){
        conversation.end();
        return "index?faces-redirect=true";
    }

}
