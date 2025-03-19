package test.jakarta.data.jpa.hibernate.web;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.impl.SortableAttributeRecord;

@StaticMetamodel(City.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _City {

	String ID = "id";
	String POPULATION = "population";

	
	/**
	 * @see City#id
	 **/
	SortableAttribute<City> id = new SortableAttributeRecord<>(ID);
	
	/**
	 * @see City#population
	 **/
	SortableAttribute<City> population = new SortableAttributeRecord<>(POPULATION);

}

