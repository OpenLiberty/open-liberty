/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.exceptionmappers.nomapper;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "comments")
public class CommentList {

    private List<Comment> comments = new ArrayList<Comment>();

    public CommentList() {
        /* do nothing */
    }

    @XmlElement(name = "comment")
    public List<Comment> getComments() {
        return comments;
    }

    public void setMessages(List<Comment> comments) {
        this.comments = comments;
    }
}
