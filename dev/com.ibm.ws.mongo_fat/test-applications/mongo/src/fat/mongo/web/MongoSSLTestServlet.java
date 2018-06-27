package fat.mongo.web;

import static org.junit.Assert.fail;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import com.mongodb.DB;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/MongoSSLTestServlet")
public class MongoSSLTestServlet extends FATServlet {

    @Resource(name = "mongo/testdb-nested-ssl")
    DB nestedSslDB;

    @Resource(name = "mongo/testdb-sslEnabled-false")
    DB sslEnabledFalseDB;

    @Resource(name = "mongo/testdb-different-sslref")
    DB differentSSLRefDB;

    @Resource(name = "mongo/testdb-valid-certificate-valid-alias")
    DB certAuthAliasValid;

    @Resource(name = "mongo/testdb-valid-certificate-no-alias-reqd")
    DB certAuthAliasNotReqd;

    @Test
    public void testInsertFindNestedSSL() throws Exception {
        MongoTestServlet.insertFind(nestedSslDB);
    }

    @Test
    public void testInsertFindSSLEnabledFalse() throws Exception {
        MongoTestServlet.insertFind(sslEnabledFalseDB);
    }

    @Test
    public void testInsertFindDifferentSSLRef() throws Exception {
        MongoTestServlet.insertFind(differentSSLRefDB);
    }

    @Test
    public void testCertAuthAliasValid() throws Exception {
        MongoTestServlet.insertFind(certAuthAliasValid);
    }

    @Test
    public void testCertAuthAliasNotReqd() throws Exception {
        MongoTestServlet.insertFind(certAuthAliasNotReqd);
    }

    // No @Test because we manually call and verify error msg in server logs
    public void testInvalidConfig(HttpServletRequest request, HttpServletResponse resp) throws Exception {
        String testName = request.getParameter("forTest");
        String jndiName = request.getParameter("jndiName");
        System.out.println(">>> BEGIN " + testName);
        System.out.println("Attempting to lookup " + jndiName);
        try {
            DB db = InitialContext.doLookup(jndiName);
            MongoTestServlet.insertFind(db);
            fail("Expected Exception when coding password with useCertificateAuthentication");
        } catch (Exception e) {
            System.out.println("Got expected exception: " + e);
        } finally {
            System.out.println("<<< END   " + testName);
        }
    }
}
