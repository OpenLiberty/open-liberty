package test.jakarta.data.jpa.hibernate.integration.web;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

@StaticMetamodel(County.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _County {

	String NAME = "name";
	String COUNTY_SEAT = "countySeat";
	String ZIPCODES = "zipcodes";
	String POPULATION = "population";

	
	/**
	 * @see County#name
	 **/
	TextAttribute<County> name = new TextAttributeRecord<>(NAME);
	
	/**
	 * @see County#countySeat
	 **/
	TextAttribute<County> countySeat = new TextAttributeRecord<>(COUNTY_SEAT);
	
	/**
	 * @see County#zipcodes
	 **/
	SortableAttribute<County> zipcodes = new SortableAttributeRecord<>(ZIPCODES);
	
	/**
	 * @see County#population
	 **/
	SortableAttribute<County> population = new SortableAttributeRecord<>(POPULATION);

}

