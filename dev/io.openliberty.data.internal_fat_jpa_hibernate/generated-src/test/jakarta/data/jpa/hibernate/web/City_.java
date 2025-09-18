package test.jakarta.data.jpa.hibernate.web;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.util.Set;

/**
 * Static metamodel for {@link test.jakarta.data.jpa.hibernate.web.City}
 **/
@StaticMetamodel(City.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class City_ {

	
	/**
	 * @see #changeCount
	 **/
	public static final String CHANGE_COUNT = "changeCount";
	
	/**
	 * @see #name
	 **/
	public static final String NAME = "name";
	
	/**
	 * @see #stateName
	 **/
	public static final String STATE_NAME = "stateName";
	
	/**
	 * @see #population
	 **/
	public static final String POPULATION = "population";
	
	/**
	 * @see #areaCodes
	 **/
	public static final String AREA_CODES = "areaCodes";

	
	/**
	 * Static metamodel type for {@link test.jakarta.data.jpa.hibernate.web.City}
	 **/
	public static volatile EntityType<City> class_;
	
	/**
	 * Static metamodel for attribute {@link test.jakarta.data.jpa.hibernate.web.City#changeCount}
	 **/
	public static volatile SingularAttribute<City, Long> changeCount;
	
	/**
	 * Static metamodel for attribute {@link test.jakarta.data.jpa.hibernate.web.City#name}
	 **/
	public static volatile SingularAttribute<City, String> name;
	
	/**
	 * Static metamodel for attribute {@link test.jakarta.data.jpa.hibernate.web.City#stateName}
	 **/
	public static volatile SingularAttribute<City, String> stateName;
	
	/**
	 * Static metamodel for attribute {@link test.jakarta.data.jpa.hibernate.web.City#population}
	 **/
	public static volatile SingularAttribute<City, Integer> population;
	
	/**
	 * Static metamodel for attribute {@link test.jakarta.data.jpa.hibernate.web.City#areaCodes}
	 **/
	public static volatile SingularAttribute<City, Set<Integer>> areaCodes;

}

