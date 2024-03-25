/**
 *
 */
package io.openliberty.jpa.tests.jpa32.json.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

import org.junit.Test;

import com.ibm.ws.testtooling.vehicle.web.JPADBTestServlet;

import io.openliberty.jpa.tests.jpa32.json.models.JSONEntity;
import jakarta.annotation.Resource;
import jakarta.json.Json;
import jakarta.json.JsonValue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.UserTransaction;

@WebServlet(urlPatterns = "/JPAJSONTestServlet")
public class JPAJSONTestServlet extends JPADBTestServlet {
    private final static String PUNAME = "JPAJSON";

    @PersistenceUnit(unitName = PUNAME + "_JTA")
    private EntityManagerFactory emfJta;

    @PersistenceUnit(unitName = PUNAME + "_RL")
    private EntityManagerFactory emfRl;

    @PersistenceContext(unitName = PUNAME + "_JTA")
    private EntityManager em;

    @Resource
    private UserTransaction tx;

    @Test
    public void testJSON_JTA() throws Exception {
        final long id = System.currentTimeMillis();
        JsonValue jval = Json.createObjectBuilder()
                        .add("id", Long.toString(id))
                        .add("name", "Dr. Doom")
                        .add("defeats", 42)
                        .build();

        EntityManager em = emfJta.createEntityManager();
        try (em) {
            JSONEntity entity = new JSONEntity();
            entity.setId(id);
            entity.setJson(jval);

            tx.begin();
            em.joinTransaction();
            em.persist(entity);
            tx.commit();

            em.clear();

            JSONEntity findEntity = em.find(JSONEntity.class, id);
            assertNotNull(findEntity);
            assertNotSame(findEntity, entity);
            assertEquals(findEntity.getId(), entity.getId());
            assertEquals(findEntity.getJson(), jval);

        }

    }
}
