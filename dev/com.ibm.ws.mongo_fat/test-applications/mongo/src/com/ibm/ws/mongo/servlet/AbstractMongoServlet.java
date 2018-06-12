/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.mongo.servlet;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.mongo.fat.shared.MongoServletAction;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoOptions;
import com.mongodb.ServerAddress;
import com.mongodb.WriteResult;

public abstract class AbstractMongoServlet extends HttpServlet {
    private static final long serialVersionUID = -6716642817759178715L;
    static final String COLLECTION = "user";
    static final String KEY = "key";
    static final String DATA = "data";
    boolean log = true;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        StringBuffer sb = new StringBuffer();
        sb.append(request.getContextPath());
        sb.append(request.getServletPath());
        sb.append(" -- doGet() called : params [");
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(Arrays.toString(entry.getValue()));
            sb.append(", ");
        }
        sb.append("]");
        l(sb.toString());
        ServletOutputStream sos = response.getOutputStream();
        String a = request.getParameter("action");
        if (a == null) {
            // Dump debug stuff if no action.
            sos.println("<html>");
            sos.println("no action provided,");
            for (MongoServletAction mos : MongoServletAction.values()) {
                sos.println("<a href='?action=" + mos.toString() + "'>" + mos.toString() + "</a><br>");
            }
            sos.println("</html><br>");
            return;
        }
        MongoServletAction action = MongoServletAction.valueOf(a);
        boolean authenticated = Boolean.valueOf(request.getParameter("authenticated"));
        DB db = getDB(authenticated);

        if (db == null) {
            sos.println("result=fail,message=null db!,authenticated=" + authenticated);
            return;
        }
        switch (action) {
            case INSERT:
                String key = request.getParameter("key");
                String data = request.getParameter("data");
                insert(sos, db, key, data);
                break;
            case FIND:
                String k = request.getParameter("key");
                find(sos, db, k);
                break;
            case DUMP_CONFIG:
                dumpConfig(sos, db);
                break;
            case DROP:
                drop(sos, db);
                break;
            case AFTER_CONFIG_UPDATE:
            case BEFORE_CONFIG_UPDATE:
                sos.println("result=success");
                break;
        }

    }

    void drop(ServletOutputStream sos, DB db) throws IOException {
        DBCollection col = db.getCollection(COLLECTION);
        col.drop();
    }

    void dumpConfig(ServletOutputStream sos, DB db) throws IOException {
        StringBuffer out = new StringBuffer();

        MongoClient mc = (MongoClient) db.getMongo();
        MongoOptions mo = mc.getMongoOptions();

        out.append("databaseName=");
        out.append(db.getName());
        // jndi?
        // mongoRef?
        out.append(";autoConnectRetry=");
        out.append(mo.isAutoConnectRetry());
        out.append(";connectionsPerHost=");
        out.append(mo.getConnectionsPerHost());
        out.append(";connectTimeout=");
        out.append(mo.getConnectTimeout());
        out.append(";cursorFinalizerEnabled=");
        out.append(mo.isCursorFinalizerEnabled());
        out.append(";description=");
        out.append(mo.getDescription());
        out.append(";maxAutoConnectRetryTime=");
        out.append(mo.getMaxAutoConnectRetryTime());
        out.append(";maxWaitTime=");
        out.append(mo.getMaxWaitTime());
        out.append(";readPreference=");
        out.append(mo.getReadPreference());
        out.append(";safe=");
        out.append(mo.isSafe());
        out.append(";socketKeepAlive=");
        out.append(mo.isSocketKeepAlive());
        out.append(";socketTimeout=");
        out.append(mo.getSocketTimeout());
        out.append(";threadsAllowedToBlockForConnectionMultiplier=");
        out.append(mo.getThreadsAllowedToBlockForConnectionMultiplier());
        out.append(";writeConcern=");
        out.append(mo.getWriteConcern());
        // extract hostName and ports
        List<ServerAddress> servers = mc.getServerAddressList();
        StringBuffer hostNames = new StringBuffer();
        StringBuffer ports = new StringBuffer();

        for (ServerAddress server : servers) {
            hostNames.append(server.getHost()).append(",");
            ports.append(server.getPort()).append(",");
        }
        String hostStr = hostNames.toString();
        hostStr = hostStr.substring(0, hostStr.length() - 1);
        String portStr = ports.toString();
        portStr = portStr.substring(0, portStr.length() - 1);

        out.append(";hostNames=");
        out.append(hostStr);
        out.append(";ports=");
        out.append(portStr);

        String res = out.toString();
        sos.println(res);
        l("dumpingConfig from db : " + db.getName() + " data: " + res);
    }

    void insert(ServletOutputStream sos, DB db, String key, String data) throws IOException {
        l("inserting key:" + key + " data:" + data + " to dbName:" + db.getName());
        DBCollection col = db.getCollection(COLLECTION);
        BasicDBObject d = new BasicDBObject(KEY, key);
        d.append(DATA, data);

        WriteResult wr = col.insert(d);
        String error = wr.getError();
        String res = null;
        if (error != null) {
            res = "result=error,data=" + error;
        } else {
            res = "result=success";
        }
        sos.println(res);
        l("inserting key:" + key + " data:" + data + " to dbName:" + db.getName() + " res: " + res);
    }

    void find(ServletOutputStream sos, DB db, String key) throws IOException {
        DBCollection col = db.getCollection(COLLECTION);

        DBObject dbo = col.findOne(new BasicDBObject(KEY, key));
        String res = "result=success, data=" + dbo.get(DATA);
        sos.println(res);

        l("finding key:" + key + "from dbName:" + db.getName() + " res = " + res);
    }

    abstract DB getDB(boolean authenticated);

    void l(String message) {
        if (log) {
            System.out.println(message);
        }
    }
}
