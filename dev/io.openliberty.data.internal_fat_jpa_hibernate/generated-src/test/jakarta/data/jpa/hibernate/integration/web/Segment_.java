package test.jakarta.data.jpa.hibernate.integration.web;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(Segment.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Segment_ {

	public static final String Y1 = "y1";
	public static final String X1 = "x1";
	public static final String Y2 = "y2";
	public static final String X2 = "x2";
	public static final String ID = "id";

	
	/**
	 * @see test.jakarta.data.jpa.hibernate.integration.web.Segment#y1
	 **/
	public static volatile SingularAttribute<Segment, Integer> y1;
	
	/**
	 * @see test.jakarta.data.jpa.hibernate.integration.web.Segment#x1
	 **/
	public static volatile SingularAttribute<Segment, Integer> x1;
	
	/**
	 * @see test.jakarta.data.jpa.hibernate.integration.web.Segment#y2
	 **/
	public static volatile SingularAttribute<Segment, Integer> y2;
	
	/**
	 * @see test.jakarta.data.jpa.hibernate.integration.web.Segment#x2
	 **/
	public static volatile SingularAttribute<Segment, Integer> x2;
	
	/**
	 * @see test.jakarta.data.jpa.hibernate.integration.web.Segment#id
	 **/
	public static volatile SingularAttribute<Segment, Integer> id;
	
	/**
	 * @see test.jakarta.data.jpa.hibernate.integration.web.Segment
	 **/
	public static volatile EntityType<Segment> class_;

}

