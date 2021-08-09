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
package com.ibm.ws.jaxrs20.fat.bookstore;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Link.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

public class CustomResponse extends Response {
    private final Response r;

    public CustomResponse(Response r) {
        this.r = r;
    }

    @Override
    public boolean bufferEntity() {
        return false;
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

    @Override
    public Set<String> getAllowedMethods() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getDate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getEntity() {
        return r.getEntity();
    }

    @Override
    public EntityTag getEntityTag() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getHeaderString(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Locale getLanguage() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getLastModified() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getLength() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Link getLink(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Builder getLinkBuilder(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<Link> getLinks() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public URI getLocation() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MediaType getMediaType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MultivaluedMap<String, Object> getMetadata() {
        return r.getMetadata();
    }

    @Override
    public int getStatus() {
        return r.getStatus();
    }

    @Override
    public StatusType getStatusInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasEntity() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasLink(String arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public <T> T readEntity(Class<T> arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T readEntity(GenericType<T> arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T readEntity(Class<T> arg0, Annotation[] arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T readEntity(GenericType<T> arg0, Annotation[] arg1) {
        // TODO Auto-generated method stub
        return null;
    }
}
