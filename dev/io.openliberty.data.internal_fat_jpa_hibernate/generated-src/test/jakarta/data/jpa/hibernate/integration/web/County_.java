package test.jakarta.data.jpa.hibernate.integration.web;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(County.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class County_ {

	public static final String COUNTY_SEAT = "countySeat";
	public static final String ZIPCODES = "zipcodes";
	public static final String NAME = "name";
	public static final String POPULATION = "population";

	
	/**
	 * @see test.jakarta.data.jpa.hibernate.integration.web.County#countySeat
	 **/
	public static volatile SingularAttribute<County, String> countySeat;
	
	/**
	 * @see test.jakarta.data.jpa.hibernate.integration.web.County#zipcodes
	 **/
	public static volatile SingularAttribute<County, int[]> zipcodes;
	
	/**
	 * @see test.jakarta.data.jpa.hibernate.integration.web.County#name
	 **/
	public static volatile SingularAttribute<County, String> name;
	
	/**
	 * @see test.jakarta.data.jpa.hibernate.integration.web.County
	 **/
	public static volatile EntityType<County> class_;
	
	/**
	 * @see test.jakarta.data.jpa.hibernate.integration.web.County#population
	 **/
	public static volatile SingularAttribute<County, Integer> population;

}

