package test.jakarta.data.jpa.hibernate.integration.web;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link test.jakarta.data.jpa.hibernate.integration.web.County}
 **/
@StaticMetamodel(County.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class County_ {

	
	/**
	 * @see #name
	 **/
	public static final String NAME = "name";
	
	/**
	 * @see #population
	 **/
	public static final String POPULATION = "population";
	
	/**
	 * @see #zipcodes
	 **/
	public static final String ZIPCODES = "zipcodes";
	
	/**
	 * @see #countySeat
	 **/
	public static final String COUNTY_SEAT = "countySeat";

	
	/**
	 * Static metamodel type for {@link test.jakarta.data.jpa.hibernate.integration.web.County}
	 **/
	public static volatile EntityType<County> class_;
	
	/**
	 * Static metamodel for attribute {@link test.jakarta.data.jpa.hibernate.integration.web.County#name}
	 **/
	public static volatile SingularAttribute<County, String> name;
	
	/**
	 * Static metamodel for attribute {@link test.jakarta.data.jpa.hibernate.integration.web.County#population}
	 **/
	public static volatile SingularAttribute<County, Integer> population;
	
	/**
	 * Static metamodel for attribute {@link test.jakarta.data.jpa.hibernate.integration.web.County#zipcodes}
	 **/
	public static volatile SingularAttribute<County, int[]> zipcodes;
	
	/**
	 * Static metamodel for attribute {@link test.jakarta.data.jpa.hibernate.integration.web.County#countySeat}
	 **/
	public static volatile SingularAttribute<County, String> countySeat;

}

