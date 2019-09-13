/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.couchdb.fat.web;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.DbAccessException;
import org.ektorp.DbInfo;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.UpdateConflictException;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult;
import org.ektorp.ViewResult.Row;
import org.ektorp.support.OpenCouchDbDocument;

import componenttest.app.FATServlet;

public abstract class AbstractCouchDbServlet extends FATServlet {
    private static final long serialVersionUID = -6716642817759178715L;
    static final String DATABASE = "my_database";

    public void drop() throws Exception {
        l("deleting DB: " + DATABASE);
        CouchDbInstance dbi = getDBInstance();
        dbi.deleteDatabase(DATABASE);
        l("DB deleted");
    }

    public void dumpConfig() throws Exception {
        CouchDbConnector db;
        try {
            db = this.getDB();
        } catch (DbAccessException ex) {
            l("dumpConfig: caught expected exception = " + ex);
            return;
        }

        // Something went wrong, so log config and fail.
        DbInfo dbInfo = db.getDbInfo();

        StringBuffer out = new StringBuffer();
        out.append("databaseName=");
        out.append(dbInfo.getDbName() + "\n");
        out.append("diskFormatVersion=");
        out.append(dbInfo.getDiskFormatVersion() + "\n");
        out.append("diskSize=");
        out.append(dbInfo.getDiskSize() + "\n");
        out.append("docCount=");
        out.append(dbInfo.getDocCount() + "\n");
        out.append("docDelCount=");
        out.append(dbInfo.getDocDelCount() + "\n");
        out.append("instanceStartTime=");
        out.append(dbInfo.getInstanceStartTime() + "\n");
        out.append("purgeSeq=");
        out.append(dbInfo.getPurgeSeq() + "\n");
        out.append("updateSeq=");
        out.append(dbInfo.getUpdateSeqAsString() + "\n");
        out.append("unknownFields=");
        out.append(dbInfo.getUnknownFields() + "\n");

        String res = out.toString();
        l("dumpingConfig from db : " + dbInfo.getDbName() + " data: " + res);
        fail("dumpConfig: expected exception did not occur; check incorrect configuration");
    }

    private String insert(String entry) throws Exception {
        CouchDbConnector db = getDB();

        l("inserting CouchDocument with entry <" + entry + "> to dbName:" + db.getDatabaseName());
        CouchDocument cd = new CouchDocument();
        cd.setContent(entry);
        try {
            db.create(cd);
            l("result=success,id=" + cd.getId());
        } catch (UpdateConflictException e) {
            l("result=error,data=" + e.toString());
            throw e;
        }
        return cd.getId();
    }

    private boolean find(String entry, String id) throws Exception {
        CouchDbConnector db = getDB();

        l("finding document with id:" + id + " from dbName:" + db.getDatabaseName());
        try {
            CouchDocument document = db.get(CouchDocument.class, id);
            l("result=success, entry=" + document.getContent());
            return (entry.equals(document.getContent()));
        } catch (DocumentNotFoundException e) {
            l("result=failure, exception=" + e);
            throw e;
        }
    }

    private boolean delete(String entry, String id) throws Exception {
        CouchDbConnector db = getDB();

        l("Deleting CouchDocument with id <" + id + "> from dbName:" + db.getDatabaseName());
        try {
            CouchDocument document = db.get(CouchDocument.class, id);
            db.delete(document);
        } catch (UpdateConflictException e) {
            l("result=failure, exception=" + e);
            throw e;
        }

        try {
            db.get(CouchDocument.class, id);
        } catch (DocumentNotFoundException e) {
            l("result=success");
            return true;
        }
        return false;
    }

    private static String generateEntry() {
        return "data_" + System.nanoTime();
    }

    protected void insertFindScenario() throws Exception {
        final String entry = generateEntry();
        String id = insert(entry);
        assertTrue("found document did not match entry=" + entry, find(entry, id));
        assertTrue("document not deleted for id=" + id, delete(entry, id));
    }

    protected void insertStateDocument(CouchDbConnector db) throws Exception {
        try {
            Map<String, String> minPopulationDensityView = new TreeMap<String, String>();
            minPopulationDensityView.put("map",
                                         "function(doc) { if (doc.population && doc.area) { emit(doc.name, doc.population/doc.area); } }");
            minPopulationDensityView.put("reduce",
                                         "function(key, values, rereduce) { min = values[0]; values.forEach(function(value) { if (value < min) min = value; }); return min; }");

            OpenCouchDbDocument designDoc = new OpenCouchDbDocument();
            String designDocId = "_design/stateInfo_" + System.nanoTime();
            designDoc.setId(designDocId);
            designDoc.setAnonymous("views", Collections.singletonMap("minPopulationDensity", minPopulationDensityView));

            l("inserting OpenCouchDbDocument designDoc with id <" + designDocId + "> to dbName:" + db.getDatabaseName());
            db.create(designDoc);

            StateDocument mnDoc = new StateDocument();
            mnDoc.setArea(86939);
            mnDoc.setCapital("Saint Paul");
            mnDoc.setName("Minnesota");
            mnDoc.setPopulation(5458333);
            db.create(mnDoc);
            mnDoc.getId();

            StateDocument iaDoc = new StateDocument();
            iaDoc.setArea(56272);
            iaDoc.setCapital("Des Moines");
            iaDoc.setName("Iowa");
            iaDoc.setPopulation(3107124);
            db.create(iaDoc);
            iaDoc.getId();

            StateDocument wiDoc = new StateDocument();
            wiDoc.setArea(65497);
            wiDoc.setCapital("Madison");
            wiDoc.setName("Wisconsin");
            wiDoc.setPopulation(5757564);
            db.create(wiDoc);
            wiDoc.getId();

            ViewResult result = db.queryView(new ViewQuery().designDocId(designDocId).viewName("minPopulationDensity"));
            Row r = result.iterator().next();
            int minPopulationDensity = r.getValueAsInt();
            if (minPopulationDensity != 55) {
                fail("result=error,data=Unexpected result " + minPopulationDensity + " in row " + r + " of " + result);
            }

            db.delete(wiDoc);
            db.delete(iaDoc);
            db.delete(mnDoc);
            db.delete(designDoc);

            l("result=success");
        } catch (UpdateConflictException e) {
            l("result=error,data=" + e.toString());
            throw e;
        }
    }

    abstract CouchDbConnector getDB();

    abstract CouchDbInstance getDBInstance();

    void l(String message) {
        System.out.println(message);
    }
}
