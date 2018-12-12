package ejb;

import java.util.stream.Stream;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import jpa.GuestEntity;

@Stateless
public class GuestManagementService {

    @PersistenceContext
    private EntityManager em;

    public void signInGuest(GuestEntity entity) {
        em.merge(entity);
    }

    public Stream<GuestEntity> retrieveAllGuests() {
        return em.createNamedQuery("findAllGuests", GuestEntity.class).getResultStream();
    }
}
