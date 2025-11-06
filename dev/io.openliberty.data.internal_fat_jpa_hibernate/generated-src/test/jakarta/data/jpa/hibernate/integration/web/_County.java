package test.jakarta.data.jpa.hibernate.integration.web;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

/**
 * Jakarta Data static metamodel for {@link test.jakarta.data.jpa.hibernate.integration.web.County}
 **/
@StaticMetamodel(County.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _County {

	
	/**
	 * @see #name
	 **/
	String NAME = "name";
	
	/**
	 * @see #population
	 **/
	String POPULATION = "population";
	
	/**
	 * @see #zipcodes
	 **/
	String ZIPCODES = "zipcodes";
	
	/**
	 * @see #countySeat
	 **/
	String COUNTY_SEAT = "countySeat";

	
	/**
	 * Static metamodel for attribute {@link County#name}
	 **/
	TextAttribute<County> name = new TextAttributeRecord<>(NAME);
	
	/**
	 * Static metamodel for attribute {@link County#population}
	 **/
	SortableAttribute<County> population = new SortableAttributeRecord<>(POPULATION);
	
	/**
	 * Static metamodel for attribute {@link County#zipcodes}
	 **/
	SortableAttribute<County> zipcodes = new SortableAttributeRecord<>(ZIPCODES);
	
	/**
	 * Static metamodel for attribute {@link County#countySeat}
	 **/
	TextAttribute<County> countySeat = new TextAttributeRecord<>(COUNTY_SEAT);

}

