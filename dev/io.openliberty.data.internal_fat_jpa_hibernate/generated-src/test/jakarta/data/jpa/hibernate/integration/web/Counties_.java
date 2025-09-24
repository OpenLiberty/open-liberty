package test.jakarta.data.jpa.hibernate.integration.web;

import jakarta.annotation.Generated;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.data.exceptions.DataException;
import jakarta.data.exceptions.OptimisticLockingFailureException;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PersistenceUnit;
import static java.util.Objects.requireNonNull;
import org.hibernate.SessionFactory;
import org.hibernate.StaleStateException;
import org.hibernate.StatelessSession;

/**
 * Implements Jakarta Data repository {@link test.jakarta.data.jpa.hibernate.integration.web.Counties}
 **/
@Dependent
@Generated("org.hibernate.processor.HibernateProcessor")
public class Counties_ implements Counties {


	
	protected @Nonnull StatelessSession session;
	
	public Counties_(@Nonnull StatelessSession session) {
		this.session = session;
	}
	
	public @Nonnull StatelessSession session() {
		return session;
	}
	
	@PersistenceUnit(unitName="HibernateProvider")
	private EntityManagerFactory sessionFactory;
	
	@PostConstruct
	private void openSession() {
		session = sessionFactory.unwrap(SessionFactory.class).openStatelessSession();
	}
	
	@PreDestroy
	private void closeSession() {
		session.close();
	}
	
	@Inject
	Counties_() {
	}
	
	@Override
	public void remove(@Nonnull County c) {
		requireNonNull(c, "Null c");
		try {
			session.delete(c);
		}
		catch (StaleStateException _ex) {
			throw new OptimisticLockingFailureException(_ex.getMessage(), _ex);
		}
		catch (PersistenceException _ex) {
			throw new DataException(_ex.getMessage(), _ex);
		}
	}
	
	@Override
	public void save(@Nonnull County c) {
		requireNonNull(c, "Null c");
		try {
			session.upsert(c);
		}
		catch (StaleStateException _ex) {
			throw new OptimisticLockingFailureException(_ex.getMessage(), _ex);
		}
		catch (PersistenceException _ex) {
			throw new DataException(_ex.getMessage(), _ex);
		}
	}

}

