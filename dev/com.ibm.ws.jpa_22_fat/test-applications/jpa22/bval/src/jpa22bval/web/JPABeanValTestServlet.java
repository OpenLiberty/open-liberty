/**
 *
 */
package jpa22bval.web;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;
import javax.validation.ValidationException;

import org.junit.Test;

import componenttest.app.FATServlet;
import jpa22bval.entity.BeanValEntity;
import junit.framework.Assert;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestJPA22BeanValidation")
public class JPABeanValTestServlet extends FATServlet {
    @PersistenceContext(unitName = "JPABVAL")
    private EntityManager em;

    @Resource
    private UserTransaction tx;

    @Test
    public void testBeanValidation001() throws Exception {
        final BeanValEntity bve = new BeanValEntity();
        bve.setEmail("valid_email@somewhere.com");

        tx.begin();
        em.persist(bve);
        tx.commit();

        Assert.assertEquals("valid_email@somewhere.com", bve.getEmail());
    }

    @Test
    public void testBeanValidation002() throws Exception {
        final BeanValEntity bve = new BeanValEntity();
        bve.setEmail(null);

        try {
            tx.begin();
            em.persist(bve);
            tx.commit();
            Assert.fail("Expected Exception was not thrown.");
        } catch (ValidationException ve) {
            // Expected
            System.out.println("Caught expected ValidationException.");
        }
    }

    @Test
    public void testBeanValidation003() throws Exception {
        final BeanValEntity bve = new BeanValEntity();
        bve.setEmail("not_valid_email");

        try {
            tx.begin();
            em.persist(bve);
            tx.commit();
            Assert.fail("Expected Exception was not thrown.");
        } catch (ValidationException ve) {
            // Expected
            System.out.println("Caught expected ValidationException.");
        }
    }

    @Test
    public void testBeanValidation004() throws Exception {
        final BeanValEntity bve = new BeanValEntity();
        bve.setEmail("valid_email@somewhere.com"); // Must set email since it is @NotNull as well

        java.time.Instant instant = java.time.Instant.MAX; // Max should always be far off into the future
        bve.setFutureInstant(instant);

        tx.begin();
        em.persist(bve);
        tx.commit();
    }

    @Test
    public void testBeanValidation005() throws Exception {
        final BeanValEntity bve = new BeanValEntity();
        bve.setEmail("valid_email@somewhere.com"); // Must set email since it is @NotNull as well

        java.time.Instant instant = java.time.Instant.MIN; // Min should always be far off into the past
        bve.setFutureInstant(instant);

        try {
            tx.begin();
            em.persist(bve);
            tx.commit();
        } catch (ValidationException ve) {
            // Expected
            System.out.println("Caught expected ValidationException.");
        }
    }
}
