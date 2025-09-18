package test.jakarta.data.jpa.hibernate.integration.web;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link test.jakarta.data.jpa.hibernate.integration.web.Segment}
 **/
@StaticMetamodel(Segment.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Segment_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #x1
	 **/
	public static final String X1 = "x1";
	
	/**
	 * @see #y1
	 **/
	public static final String Y1 = "y1";
	
	/**
	 * @see #x2
	 **/
	public static final String X2 = "x2";
	
	/**
	 * @see #y2
	 **/
	public static final String Y2 = "y2";

	
	/**
	 * Static metamodel type for {@link test.jakarta.data.jpa.hibernate.integration.web.Segment}
	 **/
	public static volatile EntityType<Segment> class_;
	
	/**
	 * Static metamodel for attribute {@link test.jakarta.data.jpa.hibernate.integration.web.Segment#id}
	 **/
	public static volatile SingularAttribute<Segment, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link test.jakarta.data.jpa.hibernate.integration.web.Segment#x1}
	 **/
	public static volatile SingularAttribute<Segment, Integer> x1;
	
	/**
	 * Static metamodel for attribute {@link test.jakarta.data.jpa.hibernate.integration.web.Segment#y1}
	 **/
	public static volatile SingularAttribute<Segment, Integer> y1;
	
	/**
	 * Static metamodel for attribute {@link test.jakarta.data.jpa.hibernate.integration.web.Segment#x2}
	 **/
	public static volatile SingularAttribute<Segment, Integer> x2;
	
	/**
	 * Static metamodel for attribute {@link test.jakarta.data.jpa.hibernate.integration.web.Segment#y2}
	 **/
	public static volatile SingularAttribute<Segment, Integer> y2;

}

