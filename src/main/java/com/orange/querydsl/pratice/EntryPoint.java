package com.orange.querydsl.pratice;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import com.orange.querydsl.pratice.entity.Member;
import com.orange.querydsl.pratice.entity.QMember;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

public class EntryPoint {
	public static void main(String[] args) throws Exception {
		
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("main");
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		
		try {
			tx.begin();
			
			QMember member = QMember.member;
			
			Member someMember = new JPAQuery<Member>(em).from(member)
					.where(member.name.eq("111"))
					.fetchOne();
			
			System.out.println(someMember);
			
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			em.close();
		}
		emf.close();
	}
}
