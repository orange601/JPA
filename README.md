# Querydsl #


## Querydsl을 이용한 확장 ##
- findById, existsById 같은 유니크 값을 메서드로 표현하는 것이 가독성 및 생산성에 좋다.
- 하지만, JpaRepository를 이용해서 복잡한 쿼리는 작성하기가 어렵다.
- @Query을 이용해서 JPQL을 작성하는 것도 방법이지만, **type safe** 하지 않아 유지 보수하기 어려운 단점이 있다.
- Querydsl를 통해서 해결할 수 있지만 조회용 DAO 클래스 들이 남발되어 다양한 DAO를 DI 받아 비즈니스 로직을 구현하게 되는 현상이 발생하게 된다.

### 1. Custom-Repository를 이용한 확장 ###
- impl 네이밍 규칙은 Repository Interface + Impl 이어야 한다. 그래야 JPA가 사용자 정의 구현 클래스로 인식할 수 있다. 

````java
public interface AccountRepository extends JpaRepository<Account, Long>, AccountCustomRepository {
    Account findByEmail(Email email);
    boolean existsByEmail(Email email);
}

public interface AccountCustomRepository {
    List<Account> findRecentlyRegistered(int limit);
}

@Transactional(readOnly = true)
public class AccountCustomRepositoryImpl extends QuerydslRepositorySupport implements AccountCustomRepository {

    public AccountCustomRepositoryImpl() {
        super(Account.class);
    }

    @Override
    // 최근 가입한 limit 갯수 만큼 유저 리스트를 가져온다
    public List<Account> findRecentlyRegistered(int limit) {
        final QAccount account = QAccount.account;
        return from(account)
                .limit(limit)
                .orderBy(account.createdAt.desc())
                .fetch();
    }
}
````
- AccountRepository는 AccountCustomRepository, JpaRepository를 구현하고 있다.
- 그러므로 findById, save 등의 메서드를 정의하지 않고도 사용 가능했듯이 AccountCustomRepository에 있는 메서드도 AccountRepository에서 그대로 사용 가능하다.
- **핵심: AccountCustomRepositoryImpl에게 복잡한 쿼리를 구현을 시키고 AccountRepository 통해서 마치 JpaRepository를 사용하는 것처럼 편리하게 사용할 수 있다.**
- SRP와 OCP에 어긋나지도 않는다.

출처: https://github.com/cheese10yun/spring-jpa-best-practices/blob/master/doc/step-15.md

### 2. QuerydslPredicateExecutor를 이용한 확장 ###
- findById, existsById 같은 유니크 값을 메서드로 표현하는 것이 가독성 및 생산성에 좋다.
- 하지만, 유사한 쿼리가 필요해지면 쿼리 메서드를 지속적으로 추가해야 하는 단점이 있다.
- QuerydslPredicateExecutor를 사용하면 매우 효과적이다.

````java
// springframework-querydsl 안에 있음
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
public interface AccountRepository extends JpaRepository<Account, Long>, AccountSupportRepository, QuerydslPredicateExecutor<Account> {
        ...
}
````

````java
@DataJpaTest
@RunWith(SpringRunner.class)
public class AccountRepositoryTest {
  @Autowired
  private AccountRepository accountRepository;
  private final QAccount qAccount = QAccount.account;
  @Test
  public void predicate_test_001() {
    //given
    final Predicate predicate = qAccount.email.eq(Email.of("test001@test.com"));
    //when
    final boolean exists = accountRepository.exists(predicate);
    //then
    assertThat(exists).isTrue();
  }
}
````

출처: https://github.com/cheese10yun/spring-jpa-best-practices/blob/master/doc/step-16.md

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

### 1. 프로퍼티를 이용한 접근 방법 ###
- 프로퍼티 접근 방법은 Projections.bean() 메서드를 이용하여 객체를 생성할 수 있고, setter() 메서드를 사용해서 값을 주입시켜준다.
````java
@Test
public void simpleProjectionTest() {
	QMember m = QMember.member;
        query.select(Projections.bean(MemberDTO.class, m.name, m.age.as("age")))
                .from(m)
                .fetch()
                .stream()
                .forEach(memberDTO -> {
                    log.info("member name : " + memberDTO.getName());
                    log.info("member age : " + memberDTO.getAge());
           	});
}
````

### 2. 필드직접접근방법 ###
- Projections.fields() 메서드를 이용하여 값을 주입한다. 
- 필드의 접근 지정자를 private로 지정해도 동작하며, setter() 메서드가 없어도 정상적으로 값이 주입된다.
````java
    @Test
    public void simpleProjectionTest_2() {
        QMember m = QMember.member;

        query.select(Projections.fields(MemberDTO.class, m.name, m.age))
                .from(m)
                .fetch()
                .stream()
                .forEach(memberDTO -> {
                    log.info("member name : " + memberDTO.getName());
                    log.info("member age : " + memberDTO.getAge());
                });
    }
````
	
### 3. 생성자를 이용하는 방법 ### 
- Projections.constructor() 메서드를 이용면 된다.   
- 해당 메서드는 생성자를 이용하여 객체에 값을 주입하며, 지정한 프로젝션과 생성자의 파라미터 순서가 같아야 한다.
````java
    @Test
    public void simpleProjectionTest_3() {
        QMember m = QMember.member;

        query.select(Projections.constructor(MemberDTO.class, m.name, m.age))
                .from(m)
                .fetch()
                .stream()
                .forEach(value -> {
                    log.info("member name : " + value.getName());
                    log.info("member age : " + value.getAge());
                });
    }
````

## 이름으로 검색 + 정렬 + 페이징 ##
https://ict-nroo.tistory.com/117

````java
public interface MemberRepository extends JpaRepository<Member, Long> {
   Page<Member> findByName(String username, Pageable pageable);
}
````
````java
// 예를 들어 이런식으로 사용할 수 있다.
@GetMapping("/hello")
public Page<Member> member() {
   PageRequest request = PageRequest.of(1, 10);
   return repository.findByName("hello1", request);
}
````
````java
사용 예시
Pageable page = new PageRequest(1, 20, new Sort...);
Page<Member> result = memberRepository.findByName("hello", page);

//전체 수
int total = result.getTotalElements();

//데이터
List<Member> members = result.getContent();

전체 페이지 수, 다음 페이지 및 페이징을 위한 API 다 구현되어 있음
````
