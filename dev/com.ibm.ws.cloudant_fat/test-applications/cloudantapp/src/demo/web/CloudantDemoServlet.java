/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package demo.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.DesignDocument;
import com.cloudant.client.api.model.DesignDocument.MapReduce;
import com.cloudant.client.api.views.Key;
import com.cloudant.client.api.views.ViewRequest;
import com.cloudant.client.api.views.ViewRequestBuilder;
import com.cloudant.client.org.lightcouch.CouchDbException;

@WebServlet("/*")
public class CloudantDemoServlet extends HttpServlet {
    private static final long serialVersionUID = 7709282314904580334L;

    @Resource(lookup = "cloudant/builder", name = "java:app/env/cloudant/builderRef")
    private ClientBuilder builder;

    @Resource(lookup = "cloudant/db", name = "java:module/env/cloudant/dbRef")
    private Database db;

    public static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final String test = request.getParameter("testMethod");
        PrintWriter out = response.getWriter();
        System.out.println("Starting test " + test + " " + request.getQueryString());
        try {
            getClass().getMethod(test, HttpServletRequest.class, HttpServletResponse.class).invoke(this, request, response);
            System.out.println("Completed test " + test);
            out.println(SUCCESS_MESSAGE);
        } catch (Throwable x) {
            if (x instanceof InvocationTargetException)
                x = x.getCause();
            out.println("<pre>ERROR in " + test + ":");
            x.printStackTrace(out);
            out.println("</pre>");
            x.printStackTrace(System.out);
        }
    }

    /**
     * Inject and look up a cloudant resource as a ClientBuilder.
     */
    public void testClientBuilder(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Test the injected ClientBuilder instance
        CloudantClient client = builder.build();
        try {
            System.out.println("Cloudant server version: " + client.serverVersion());
        } finally {
            client.shutdown();
        }

        // Test lookup by resource reference
        String databaseName = request.getParameter("databaseName");

        ClientBuilder builder = (ClientBuilder) new InitialContext().lookup("java:app/env/cloudant/builderRef");
        builder.maxConnections(18);
        client = builder.build();
        try {
            Database db = client.database(databaseName, false);
            String dbName = db.info().getDbName();

            if (!databaseName.equals(dbName))
                throw new Exception("Database info reported database name of " + dbName + " instead of expected " + databaseName);
        } finally {
            client.shutdown();
        }

        // Ensure changes don't remain across separate lookups
        builder.username("notvalid");
        try {
            client = builder.build();
        } catch (CouchDbException x) {
            if (x.getMessage().indexOf("400") < 0) // 400 Bad Request
                throw x;
        }
        builder = (ClientBuilder) new InitialContext().lookup("java:app/env/cloudant/builderRef");

        client = builder.build();
        try {
            List<String> dbNames = client.getAllDbs();
            if (!dbNames.contains(databaseName))
                throw new Exception("List of databases is missing " + databaseName + ". Result: " + dbNames);
        } finally {
            client.shutdown();
        }
    }

    /**
     * Direct lookup of a Cloudant Database.
     */
    public void testDirectLookup(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Database db = (Database) new InitialContext().lookup("cloudant/db");

        // insert entries into database
        db.save(new Shape("octagon", 8, 135.0f));

        db.save(new Shape("hexagon", 6, 120.0f));

        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("_id", "triangle");
        map.put("sides", 3);
        map.put("angle", 180.0f);
        db.save(map);

        // query for multiple
        PrintWriter writer = response.getWriter();
        for (String shapeName : new String[] { "triangle", "hexagon", "octagon" }) {
            Shape shape = db.find(Shape.class, shapeName);
            writer.println("A " + shape._id + " has " + shape.sides + " equal sides, with angles of " + shape.angle + " degrees.");
        }

        // query for single documents
        Map<?, ?> result = db.find(LinkedHashMap.class, "hexagon");
        if (((Double) result.get("sides")).intValue() != 6)
            throw new Exception("Unexpected number of sides for hexagon: " + result);
        if (((Double) result.get("angle")).intValue() != 120)
            throw new Exception("Unexpected angle for hexagon: " + result);

        result = db.find(LinkedHashMap.class, "octagon");
        if (((Double) result.get("sides")).intValue() != 8)
            throw new Exception("Unexpected number of sides for octagon: " + result);
        if (((Double) result.get("angle")).intValue() != 135)
            throw new Exception("Unexpected angle for octagon: " + result);
    }

    /**
     * Test injection of a Cloudant Database instance
     */
    public void testInjectDatabase(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String databaseName = request.getParameter("databaseName");

        String dbName = db.info().getDbName();
        if (!dbName.equals(databaseName))
            throw new Exception("Expecting database name " + databaseName + ". Instead found " + dbName);
    }

    /**
     * Resource reference lookup of a Cloudant Database.
     */
    public void testResourceRef(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Database db = (Database) new InitialContext().lookup("java:module/env/cloudant/dbRef");

        MapReduce minPopulationDensityView = new MapReduce();
        minPopulationDensityView.setMap("function(doc) { if (doc.population && doc.area) { emit(doc.name, doc.population/doc.area); } }");
        minPopulationDensityView.setReduce("function(key, values, rereduce) { min = values[0]; values.forEach(function(value) { if (value < min) min = value; }); return min; }");
        DesignDocument design_stateInfo = new DesignDocument();
        design_stateInfo.setId("_design/stateInfo");
        design_stateInfo.setViews(Collections.<String, MapReduce> singletonMap("minPopulationDensity", minPopulationDensityView));
        db.getDesignDocumentManager().put(design_stateInfo);

        StateDocument mnDoc = new StateDocument();
        mnDoc.setArea(86939);
        mnDoc.setCapital("Saint Paul");
        mnDoc.setName("Minnesota");
        mnDoc.setPopulation(5458333);
        mnDoc.setId("MN"); // unlike ektorp, cloudant doesn't insert id unless the serialized object's JSON doesn't contain id field
        db.save(mnDoc);

        StateDocument iaDoc = new StateDocument();
        iaDoc.setArea(56272);
        iaDoc.setCapital("Des Moines");
        iaDoc.setName("Iowa");
        iaDoc.setPopulation(3107124);
        iaDoc.setId("IA");
        db.save(iaDoc);

        StateDocument wiDoc = new StateDocument();
        wiDoc.setArea(65497);
        wiDoc.setCapital("Madison");
        wiDoc.setName("Wisconsin");
        wiDoc.setPopulation(5757564);
        wiDoc.setId("WI");
        db.save(wiDoc);

        ViewRequestBuilder viewBuilder = db.getViewRequestBuilder("stateInfo", "minPopulationDensity");
        ViewRequest<String, Integer> viewRequest = viewBuilder.newRequest(Key.Type.STRING, Integer.class).build();
        Integer value = viewRequest.getSingleValue();
        if (value != 55)
            throw new Exception("Unexpected value " + value + " returned from map-reduce query.");
    }
}
