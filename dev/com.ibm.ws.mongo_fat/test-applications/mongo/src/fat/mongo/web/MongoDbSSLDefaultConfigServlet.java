package fat.mongo.web;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.mongodb.DB;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/MongoDbSSLDefaultConfigServlet")
public class MongoDbSSLDefaultConfigServlet extends FATServlet {

    @Resource(name = "mongo/testdb-no-sslRef")
    DB noSSLRef;

    @Test
    public void testInsertFindNoSslRef() throws Exception {
        MongoTestServlet.insertFind(noSSLRef);
    }

}
