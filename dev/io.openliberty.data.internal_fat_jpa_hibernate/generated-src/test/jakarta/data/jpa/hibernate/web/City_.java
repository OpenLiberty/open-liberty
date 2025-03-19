package test.jakarta.data.jpa.hibernate.web;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.util.Set;

@StaticMetamodel(City.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class City_ {

	public static final String AREA_CODES = "areaCodes";
	public static final String ID = "id";
	public static final String POPULATION = "population";

	
	/**
	 * @see test.jakarta.data.jpa.hibernate.web.City#areaCodes
	 **/
	public static volatile SingularAttribute<City, Set<Integer>> areaCodes;
	
	/**
	 * @see test.jakarta.data.jpa.hibernate.web.City#id
	 **/
	public static volatile SingularAttribute<City, CityId> id;
	
	/**
	 * @see test.jakarta.data.jpa.hibernate.web.City
	 **/
	public static volatile EntityType<City> class_;
	
	/**
	 * @see test.jakarta.data.jpa.hibernate.web.City#population
	 **/
	public static volatile SingularAttribute<City, Integer> population;

}

