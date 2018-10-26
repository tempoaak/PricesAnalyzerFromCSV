package com.akartkam.priceanalyzer;

import au.com.anthonybruno.annotation.Range;

public class DomainObject implements Comparable<DomainObject> {

	private int ID;
	private String Name;
	private String Condition;
	private String State;
	private Float Price=0f;
	
	public DomainObject() {
		
	}
	
	public DomainObject(int iD, String name, String condition, String state,
			Float price) {
		super();
		ID = iD;
		Name = name;
		Condition = condition;
		State = state;
		Price = price;
	}


	public int getID() {
		return ID;
	}


	public void setID(int iD) {
		ID = iD;
	}


	public String getName() {
		return Name;
	}


	public void setName(String name) {
		Name = name;
	}


	public String getCondition() {
		return Condition;
	}


	public void setCondition(String condition) {
		Condition = condition;
	}


	public String getState() {
		return State;
	}


	public void setState(String state) {
		State = state;
	}


	public Float getPrice() {
		return Price;
	}


	public void setPrice(Float price) {
		Price = price;
	}

	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ID;
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DomainObject other = (DomainObject) obj;
		if (ID != other.ID)
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "DomainObject [ID=" + ID + ", Price=" + Price + "]";
	}


	@Override
	public int compareTo(DomainObject o) {
		return getPrice().compareTo(o.getPrice());
	}
	
}
