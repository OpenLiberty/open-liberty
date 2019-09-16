package org.aguibert.liberty;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
@ApplicationScoped
public class TestService {

    @Resource
    UserTransaction tx;

    @PersistenceContext(unitName = "JPAPU")
    private EntityManager em;

    @Inject
    SampleBean bean;

    @GET
    public String test() {
        try {
            log(">>> ENTER");
            doTest();
            log("<<< EXIT SUCCESSFUL");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            e.printStackTrace(new PrintWriter(sb));
            log("<<< EXIT FAILED");
        }
        String result = sb.toString();
        sb = new StringWriter();
        return result;
    }

    private void doTest() throws Exception {
        log("Hello world");
        log("This class is in: " + getClass().getModule());

        assertEquals("Hello world!", bean.sayHello());

        testJPA();
    }

    private StringWriter sb = new StringWriter();

    private void log(String msg) {
        System.out.println(msg);
        sb.append(msg);
        sb.append("<br/>");
    }

    public void testJPA() throws Exception {
        tx.begin();
        Book b = new Book();
        b.author = "Bob";
        b.id = 1;
        b.pages = 100;
        b.title = "The Joy of Painting";
        em.persist(b);
        tx.commit();

        em.clear();
        Book findEntity = em.find(Book.class, b.id);
        if (b == findEntity)
            throw new RuntimeException("Instance found from EntityManger should not be same instance that was persisted");
        assertEquals(b.id, findEntity.id);
        assertEquals(b.title, findEntity.title);
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual))
            throw new RuntimeException("Expected <" + expected + "> but instead got <" + actual + ">");
    }

}
