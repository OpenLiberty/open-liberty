/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.repository.resources.internal;

import java.util.Collection;

public class Link {

    private final com.ibm.ws.repository.transport.model.Link _link;

    public Link() {
        _link = new com.ibm.ws.repository.transport.model.Link();
    }

    public Link(com.ibm.ws.repository.transport.model.Link link) {
        _link = link;
    }

    public Collection<String> getQuery() {
        return _link.getQuery();
    }

    public void setQuery(Collection<String> query) {
        _link.setQuery(query);
    }

    public String getLabel() {
        return _link.getLabel();
    }

    public void setLabel(String label) {
        _link.setLabel(label);
    }

    public String getLinkLabelProperty() {
        return _link.getLinkLabelProperty();
    }

    public void setLinkLabelProperty(String linklabel) {
        _link.setLinkLabelProperty(linklabel);
    }

    public void setLinkLabelPrefix(String linkLabelPrefix) {
        _link.setLinkLabelPrefix(linkLabelPrefix);
    }

    public String getLinkLabelPrefix() {
        return _link.getLinkLabelPrefix();
    }

    public void setLinkLabelSuffix(String linkLabelSuffix) {
        _link.setLinkLabelSuffix(linkLabelSuffix);
    }

    public String getLinkLabelSuffix() {
        return _link.getLinkLabelSuffix();
    }

    public com.ibm.ws.repository.transport.model.Link getLink() {
        return _link;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                 + ((_link == null) ? 0 : _link.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Link other = (Link) obj;
        if (_link == null) {
            if (other._link != null)
                return false;
        } else if (!_link.equals(other._link))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return _link.toString();
    }

}
