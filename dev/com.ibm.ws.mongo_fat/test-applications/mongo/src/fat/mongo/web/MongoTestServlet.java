package fat.mongo.web;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoOptions;
import com.mongodb.ServerAddress;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/MongoTestServlet")
public class MongoTestServlet extends FATServlet {

    public static final String COLLECTION = "user";
    public static final String KEY = "key";
    public static final String DATA = "data";

    private static String HOST_NAME;
    static {
        try {
            HOST_NAME = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            HOST_NAME = "localhost-" + System.nanoTime();
        }
    }

    @Resource(name = "mongo/testdb")
    DB dbNoAuth;

    @Resource(name = "mongo/testdb-auth")
    DB dbAuthenticated;

    @Test
    public void testResourceEnvRef() throws Exception {
        DB db = InitialContext.doLookup("java:comp/env/mongo/testdb-jndi-resource-env-ref");
        insertFind(db);
    }

    @Test
    public void testInsertFindAuthenticatedInject() throws Exception {
        insertFind(dbAuthenticated);
    }

    // See RTC work item 238129
    // @Test
    public void testInsertFindUnauthenticatedInject() throws Exception {
        insertFind(dbNoAuth);
    }

    // See RTC work item 238129
    // @Test
    public void testInsertFindUnauthenticatedJNDI() throws Exception {
        DB db = InitialContext.doLookup("java:comp/env/mongo/testdb-jndi");
        insertFind(db);
    }

    @Test
    public void testInsertFindAuthenticatedJNDI() throws Exception {
        DB db = InitialContext.doLookup("java:comp/env/mongo/testdb-auth-jndi");
        insertFind(db);
    }

    /**
     * Called possibly multiple times by some test cases to verify Mongo works in a basic sense.
     * Pass in the 'forTest' request param to log which test is calling this method
     */
    public void basicInsertFind(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String forTest = req.getParameter("forTest");
        System.out.println(">>> BEGIN: " + forTest);
        try {
            testInsertFindUnauthenticatedJNDI();
        } finally {
            System.out.println("<<< END:  " + forTest);
        }
    }

    public void configDump(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String forTest = req.getParameter("forTest");
        System.out.println(">>> BEGIN: " + forTest);
        PrintWriter pw = resp.getWriter();

        try {
            DB db = InitialContext.doLookup("java:comp/env/mongo/testdb-jndi");
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

            String configDump = out.toString();
            System.out.println("Dumping config:");
            System.out.println(configDump);
            pw.println(configDump);
        } finally {
            System.out.println("<<< END:  " + forTest);
        }
    }

    public static void insertFind(DB db) throws Exception {
        String key = generateKey();
        String value = generateData();
        insert(db, key, value);
        find(db, key, value);
    }

    public static void insert(DB db, String key, String data) throws IOException {
        System.out.println("inserting key=" + key + " data=" + data + " to dbName=" + db.getName());
        DBCollection col = db.getCollection(COLLECTION);
        BasicDBObject d = new BasicDBObject(KEY, key);
        d.append(DATA, data);
        col.insert(d);
    }

    public static void find(DB db, String key, String expectedValue) throws IOException {
        DBCollection col = db.getCollection(COLLECTION);
        DBObject dbo = col.findOne(new BasicDBObject(KEY, key));
        String actualValue = (String) dbo.get(DATA);
        System.out.println("found key=" + key + " value=" + actualValue + " from dbName=" + db.getName());
        assertEquals(expectedValue, actualValue);
    }

    private static String generateKey() {
        return "key_" + System.nanoTime() + "-" + HOST_NAME;
    }

    private static String generateData() {
        return "data_" + System.nanoTime();
    }

}
