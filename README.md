# JPA
QueryDSL

## fetch ##
- fetch : 조회 대상이 여러건일 경우. 컬렉션 반환
- fetchOne : 조회 대상이 1건일 경우(1건 이상일 경우 에러). generic에 지정한 타입으로 반환
- fetchFirst : 조회 대상이 1건이든 1건 이상이든 무조건 1건만 반환. 내부에 보면 return limit(1).fetchOne() 으로 되어있음
- fetchCount : 개수 조회. long 타입 반환
- fetchResults : 조회한 리스트 + 전체 개수를 포함한 QueryResults 반환. count 쿼리가 추가로 실행된다.

## 프로덕션 ##
- ntity 전체를 가져오는 방법 말고, 조회 대상을 지정하여 원하는 값만 조회하는 것
- 프로젝션 대상이 하나일 경우에는 반환되는 타입이 프로젝션 대상의 타입입니다.
````java
	public List<String> findWbs(){
		return jpaQueryFactory
				.select(wbs.pjtCd)
				.from(wbs)
				.fetch();
	}
````
