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
package com.ibm.ws.jaxrs.fat.ibm.json;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

/**
 * Resource class for IBMJSON4JTest
 */
@Path("listsongs")
public class SongListResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JSONObject getListOfSongs() {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("Stormy Weather", "Lena Horne");
        jsonObj.put("Pennies from Heaven", "Billie Holiday");
        jsonObj.put("Hootchie Cootchie Man", "Junior Wells");
        return jsonObj;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("array")
    public JSONArray getArrayOfSongList() {
        JSONArray jsonArray = new JSONArray();
        JSONObject obj0 = new JSONObject();
        obj0.put("Got to Give It Up", "Marvin Gaye");
        obj0.put("Summertime", "Billy Stewart");

        JSONObject obj1 = new JSONObject();
        obj1.put("Cielito Lindo", "Various");
        obj1.put("O Solo Mio", "Various");

        JSONObject obj2 = new JSONObject();
        obj2.put("Smile", "Lily Allen");
        obj2.put("Me and Mr. Jones", "Amy Winehouse");

        jsonArray.add(0, obj0);
        jsonArray.add(1, obj1);
        jsonArray.add(2, obj2);
        return jsonArray;
    }

    // TODO: Need a method that uses XMLToJSONTransformer
}