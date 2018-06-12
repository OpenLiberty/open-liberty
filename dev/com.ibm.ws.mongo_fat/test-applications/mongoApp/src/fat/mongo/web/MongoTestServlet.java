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
package fat.mongo.web;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import org.junit.Assert;
import org.junit.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/MongoTestServlet")
public class MongoTestServlet extends FATServlet {

    private static final String COLLECTION = "users";
    private static final String KEY = "key";
    private static final String DATA = "data";

    @Resource(lookup = "mongo/testdb")
    protected DB db;

    private String HOSTNAME;

    @Override
    public void init() throws ServletException {
        try {
            HOSTNAME = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            HOSTNAME = "localhost-" + System.nanoTime();
        }
    }

    @Test
    public void basicTest() throws Exception {
        System.out.println("Test is running in an HttpServlet");
        Assert.assertTrue("Can also use JUnit assertions", true);
    }

    @Test
    public void testMongo() throws Exception {
        String dbName = db.getName();
        System.out.println("@AGG got db name: " + dbName);
        assertEquals("db-test", dbName);
    }

    @Test
    public void testInsertFindAuthenticatedInject() throws IOException {
        final String key = generateKey();
        final String data = generateData();
        insert(db, key, data);
        find(db, key, data);
    }

    String generateKey() {
        return "key_" + System.nanoTime() + "-" + HOSTNAME;
    }

    String generateData() {
        return "data_" + System.nanoTime();
    }

    private void insert(DB db, String key, String data) throws IOException {
        System.out.println("inserting key:" + key + " data:" + data + " to dbName:" + db.getName());
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
        System.out.println(res);
        System.out.println("inserting key:" + key + " data:" + data + " to dbName:" + db.getName() + " res: " + res);
    }

    private void find(DB db, String key, String expected) throws IOException {
        DBCollection col = db.getCollection(COLLECTION);

        DBObject dbo = col.findOne(new BasicDBObject(KEY, key));
        String result = (String) dbo.get(DATA);
        System.out.println("For key=" + key + "  found data=" + result);
        assertEquals(expected, result);
    }

}
