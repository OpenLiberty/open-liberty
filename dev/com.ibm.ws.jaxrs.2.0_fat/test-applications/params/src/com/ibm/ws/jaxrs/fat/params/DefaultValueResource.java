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
package com.ibm.ws.jaxrs.fat.params;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Path("defaultvalue")
public class DefaultValueResource {

    private final String version;

    @DefaultValue("100")
    @QueryParam("limit")
    private String limit;

    private String sort;

    public DefaultValueResource(@HeaderParam("requestVersion") @DefaultValue("1.0") String version) {
        this.version = version;
    }

    public static class Page {

        private final String offset;

        public Page(String offset, int dummy) {
            this.offset = offset;
            System.out.println("Executed constructor");
        }

        public String getOffset() {
            return offset;
        }

        public int getPage() {
            return Integer.valueOf(offset) * 1; // Integer.valueOf(limit);
        }

        public static Page valueOf(String offset) {
            return new Page(offset, 123);
        }
    }

    @GET
    public String getRow(@QueryParam("offset") @DefaultValue("0") Page page) {
        return "getRow:" + "offset="
               + page.getOffset()
               + ";version="
               + version
               + ";limit="
               + limit
               + ";sort="
               + sort;
    }

    @DefaultValue("normal")
    @QueryParam("sort")
    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getSort() {
        return sort;
    }
}
