package test.jakarta.data.jpa.hibernate.integration.web;

import jakarta.annotation.Generated;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.data.exceptions.DataException;
import jakarta.data.exceptions.OptimisticLockingFailureException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PersistenceUnit;
import org.hibernate.SessionFactory;
import org.hibernate.StaleStateException;
import org.hibernate.StatelessSession;

@RequestScoped
@Generated("org.hibernate.processor.HibernateProcessor")
public class Counties_ implements Counties {


	
	protected @Nonnull StatelessSession session;
	
	public Counties_(@Nonnull StatelessSession session) {
		this.session = session;
	}
	
	public @Nonnull StatelessSession session() {
		return session;
	}
	
	@Override
	public void remove(@Nonnull County c) {
		if (c == null) throw new IllegalArgumentException("Null c");
		try {
			session.delete(c);
		}
		catch (StaleStateException exception) {
			throw new OptimisticLockingFailureException(exception.getMessage(), exception);
		}
		catch (PersistenceException exception) {
			throw new DataException(exception.getMessage(), exception);
		}
	}
	
	@Override
	public void save(@Nonnull County c) {
		if (c == null) throw new IllegalArgumentException("Null c");
		try {
			session.upsert(c);
		}
		catch (StaleStateException exception) {
			throw new OptimisticLockingFailureException(exception.getMessage(), exception);
		}
		catch (PersistenceException exception) {
			throw new DataException(exception.getMessage(), exception);
		}
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

}

