# JPA
QueryDSL

## fetch ##
- fetch : 조회 대상이 여러건일 경우. 컬렉션 반환
- fetchOne : 조회 대상이 1건일 경우(1건 이상일 경우 에러). generic에 지정한 타입으로 반환
- fetchFirst : 조회 대상이 1건이든 1건 이상이든 무조건 1건만 반환. 내부에 보면 return limit(1).fetchOne() 으로 되어있음
- fetchCount : 개수 조회. long 타입 반환
- fetchResults : 조회한 리스트 + 전체 개수를 포함한 QueryResults 반환. count 쿼리가 추가로 실행된다.

## 프로덕션 ##
###### entity 전체를 가져오는 방법 말고, 조회 대상을 지정하여 원하는 값만 조회하는 것 ######
- 프로젝션 대상이 하나일 경우에는 반환되는 타입이 프로젝션 대상의 타입이다.
````java
public List<String> findWbs(){
	return jpaQueryFactory
		.select(wbs.wbsNm)
		.from(wbs)
		.fetch();
}
````
> 위의 결과를 보면 wbs 엔티티의 wbsNm은 String 타입이므로 프로젝션 대상이 하나일 때, List<String> 타입이 조회 결과로 반환되는 것을 볼 수 있다.

````java
// 프로젝션 대상이 둘 이상이라면 Tuple 타입을 반환
public List<Tuple> findWbss(){
	List<Tuple> list = jpaQueryFactory
		.select(wbs.wbsNm, wbs.useYn)
		.from(wbs)
		.fetch();
		
	// Tuple을 조회할 때는 get() 메서드를 이용하면 된다.
	// get() 으로 조회하는 방법으로 두 가지가 있다.
	// 1. 첫 번째 파라미터로 프로젝션 대상의 순번 
	// 2. 파라미터는 해당 값의 타입을 명시하는 방법이다.
	list.stream()
        .forEach(tuple -> {
        	log.info("wbsNm is " + tuple.get(0, String.class));
        	log.info("useYn is " + tuple.get(wbs.useYn));
        });
		
	return list;
}	
````
> 프로젝션 대상이 둘 이상이라면 Tuple 타입을 반환한다. Tuple을 조회할 때는 get() 메서드를 이용하면 된다.
> get() 으로 조회하는 방법으로 두 가지가 있다.
> 1. 첫 번째 파라미터로 프로젝션 대상의 순번 
> 2. 파라미터는 해당 값의 타입을 명시하는 방법이다.

## Projections 클래스 ##
- 쿼리의 결과를 특정 객체로 받고 싶을 때는 QueryDSL에서 제공하는 Projections 클래스를 이용하면 된다.
- Projections 클래스를 이용하여 객체를 생성하는 방법 3가지
1. 프로퍼티 접근
2. 필드 직접 접근
3. 생성자 사용

````java
package com.edu.querydsl_training.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberDTO {

    private String name;
    private Long age;
}
````
