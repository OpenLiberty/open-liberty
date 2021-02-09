/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.query.entities.ano;

import java.io.Serializable;
import java.util.List;
import java.util.Vector;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;

import com.ibm.ws.query.entities.interfaces.ICharityFund;
import com.ibm.ws.query.entities.interfaces.IDeptBean;
import com.ibm.ws.query.entities.interfaces.IEmpBean;
import com.ibm.ws.query.entities.interfaces.IProjectBean;

/**
 * Entity that extends interface IDeptBean
 */
@Entity
@Table(name = "JPADeptBean")

@SqlResultSetMappings({
                        @SqlResultSetMapping(name = "StringMapping",
                                             entities = { @EntityResult(entityClass = String.class)
                                             }),
                        @SqlResultSetMapping(name = "StringMapping2",
                                             columns = {
                                                         @ColumnResult(name = "NAME2") }),
                        @SqlResultSetMapping(name = "DeptBeanMapping",
                                             entities = @EntityResult(entityClass = DeptBean.class)),
                        @SqlResultSetMapping(name = "DeptBeanThenEmpBeanMapping",
                                             entities = { @EntityResult(entityClass = DeptBean.class),
                                                          @EntityResult(entityClass = EmpBean.class) }),
                        @SqlResultSetMapping(name = "EmpBeanThenDeptBeanMapping",
                                             entities = { @EntityResult(entityClass = EmpBean.class),
                                                          @EntityResult(entityClass = DeptBean.class)
                                             }),
                        @SqlResultSetMapping(name = "IntegerThenStringMapping",
                                             entities = { @EntityResult(entityClass = Integer.class),
                                                          @EntityResult(entityClass = String.class) }),
                        @SqlResultSetMapping(name = "IntegerThenStringMapping2",
                                             columns = { @ColumnResult(name = "DEPTNO2"),
                                                         @ColumnResult(name = "NAME2") }),
                        @SqlResultSetMapping(name = "EntityThenStringMapping",
                                             entities = @EntityResult(entityClass = DeptBean.class),
                                             columns = @ColumnResult(name = "name2")),
                        @SqlResultSetMapping(name = "EntityThenCountMapping",
                                             entities = @EntityResult(entityClass = DeptBean.class),
                                             columns = @ColumnResult(name = "resCount"))
})

@NamedQueries({
                @NamedQuery(
                            name = "findAllDepartmentsLikeName",
                            query = "SELECT d FROM DeptBean d WHERE d.name LIKE :deptName"),
                @NamedQuery(
                            name = "joinDeptWithEmps",
                            query = "SELECT d    FROM DeptBean d JOIN d.emps e   WHERE d.no > ?1 order by d.no asc"),
                @NamedQuery(
                            name = "deleteDepartmentsGreaterThan",
                            query = "delete FROM DeptBean d WHERE d.no > :deptNo"),
                @NamedQuery(
                            name = "updateDeptBudget",
                            query = "update DeptBean d set d.budget = (d.budget + ?1)"),
                @NamedQuery(
                            name = "updateDeptBudgetForParent",
                            query = "update DeptBean d set d.budget = (d.budget * ?1) where d.reportsTo.no = ?2"),
                @NamedQuery(
                            name = "updateDeptBudgetForParent2",
                            query = "update DeptBean d set d.budget = (d.budget * :factor) where d.reportsTo.no = :reports2"),
// fails due to known issue
                @NamedQuery(
                            name = "joinDeptWithEmps2",
                            query = "SELECT d,e    FROM DeptBean d JOIN d.emps e   WHERE d.no > :deptNo order by e.empid desc")
})
@NamedNativeQueries({
                      @NamedNativeQuery(
                                        name = "nativeFindAllDepartmentNumbersLikeName",
                                        query = "SELECT d.deptno FROM JPADeptBean d WHERE d.name LIKE ?"),
                      @NamedNativeQuery(
                                        name = "nativeFindAllDepartmentColumns",
                                        query = "SELECT d.*    FROM JPADeptBean d   WHERE d.deptno > ? order by d.deptno asc"),
                      @NamedNativeQuery(
                                        name = "nativeFindAllDepartments",
                                        query = "SELECT d.*    FROM JPADeptBean d   WHERE d.deptno > ? order by d.deptno asc",
                                        resultClass = DeptBean.class),
                      @NamedNativeQuery(
                                        name = "nativeFindAllDepartments2",
                                        query = "SELECT d.*    FROM JPADeptBean d   WHERE d.deptno > ? order by d.deptno asc",
                                        resultSetMapping = "DeptBeanMapping"),
                      @NamedNativeQuery(
                                        name = "nativeJoinDeptWithEmps",
                                        query = "SELECT d.*,e.*    FROM JPAEmpBean e JOIN JPADeptBean d on e.DEPT_DEPTNO = d.deptno  WHERE d.deptno > ?1 order by e.empid desc",
                                        resultSetMapping = "DeptBeanThenEmpBeanMapping"),
                      @NamedNativeQuery(
                                        name = "nativeJoinEmpsWithDept",
                                        query = "SELECT e.*,d.*    FROM JPAEmpBean e JOIN JPADeptBean d on e.DEPT_DEPTNO = d.deptno  WHERE d.deptno > ?1 order by e.empid desc",
                                        resultSetMapping = "EmpBeanThenDeptBeanMapping"),
                      @NamedNativeQuery(
                                        name = "nativeUpdateDeptBudget",
                                        query = "update JPADeptBean  set budget = (budget + ?1)"),
                      @NamedNativeQuery(
                                        name = "nativeUpdateDeptBudgetForParent",
                                        query = "update JPADeptBean  set budget = (budget * ?1) where REPORTSTO_DEPTNO = ?2")//,
})
public class DeptBean implements IDeptBean, Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 315661377721275053L;
    @Id

    @Column(name = "deptno")
    private Integer no;
    @Column(name = "name", length = 40)
    private String name;
    private float budget;//dw added
    @OneToMany(mappedBy = "dept", cascade = CascadeType.REMOVE) //if fk creates table need cascadeRemove on inverse
    private List<EmpBean> emps;
    @OneToMany(mappedBy = "dept", cascade = CascadeType.REMOVE) //gfh 7/17 if fk creates table need cascadeRemove on inverse
    private List<ProjectBean> projects;

    @ManyToOne(cascade = CascadeType.PERSIST) //gfh 7/17
    public EmpBean mgr;
    @ManyToOne(cascade = CascadeType.PERSIST) //gfh 7/17
    private DeptBean reportsTo;//dw added
    @Embedded
    @AttributeOverrides({
                          @AttributeOverride(name = "charityName",
                                             column = @Column(name = "charityName", length = 40)),
                          @AttributeOverride(name = "charityAmount",
                                             column = @Column(name = "charityAmount"))
    })
    private CharityFund charityFund;

    public DeptBean() {
    }

    public DeptBean(int no, String nam) {
        this.no = no;
        name = nam;
        budget = 2.1f;
        mgr = null;
        emps = new Vector();
        projects = new Vector();
        reportsTo = null;
    }

    public DeptBean(DeptBean dept) {
        this.no = dept.no;
        this.name = dept.name;
        this.budget = dept.budget;
        this.setMgr(dept.getMgr());
        this.setEmps(dept.getEmps());
        this.setProjects(dept.getProjects());
        this.setReportsTo(dept.getReportsTo());
    }

    @Override
    public String toString() {
        return "( DeptBean: no=" + getNo() + " name =" + getName() + ")";
    }

    @Override
    public Integer getNo() {
        return no;
    }

    @Override
    public void setNo(Integer no) {
        this.no = no;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public float getBudget() {
        return budget;
    }

    @Override
    public void setBudget(float budget) {
        this.budget = budget;
    }

    @Override
    public CharityFund getCharityFund() {
        return charityFund;
    }

    @Override
    public void setCharityFund(ICharityFund charityFund) {
        this.charityFund = (CharityFund) charityFund;
    }

    @Override
    public List<EmpBean> getEmps() {
        return emps;
    }

    @Override
    public void setEmps(List<? extends IEmpBean> emps) {
        this.emps = (List<EmpBean>) emps;
    }

    @Override
    public List<ProjectBean> getProjects() {
        return projects;
    }

    @Override
    public void setProjects(List<? extends IProjectBean> projects) {
        this.projects = (List<ProjectBean>) projects;
    }

    @Override
    public EmpBean getMgr() {
        return mgr;
    }

    @Override
    public void setMgr(IEmpBean mgr) {
        this.mgr = (EmpBean) mgr;
    }

    @Override
    public DeptBean getReportsTo() {
        return reportsTo;
    }

    @Override
    public void setReportsTo(IDeptBean reportsTo) {
        this.reportsTo = (DeptBean) reportsTo;
    }

}
