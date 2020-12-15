package com.orange.querydsl.pratice.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Member {
	
	public Member() {
		
	}
	
	@Id
	@GeneratedValue(strategy= GenerationType.IDENTITY)
	private long id;
	private String name;

	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public Member(String name, String nickName, int age) {
		this.name = name;
	}

	@Override
	public String toString() {
		return String.format("Member [id=%d, name=%s]", this.id, this.name);
	}
}
