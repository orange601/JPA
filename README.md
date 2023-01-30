# JPA #
#### ORM(Object-Relational Mapping) ####
- 객체(Object)와 관계형 데이터(Relational data)를 매핑하기 위한 기술이다. 

## 1. Bulk Insert ##
- RDBMS에서 bulk insert란 한번의 쿼리로 여러건의 데이터를 insert 할 수 있는 기능을 제공하는 것이다.
- 한번의 쿼리로 여러건의 데이터를 한번에 insert 할 수 있기 때문에 데이터베이스와 어플리케이션 사이의 통신에 들어가는 비용을 줄여주어 성능상 이득을 얻는다.
- 하지만 bulk insert를 원하는 테이블에서 auto_increment를 사용하고 있다면 bulk insert는 JPA를 통해서는 해결할 수 없다. 
- 프레임워크를 추가하거나 바꾸지 못 하고 JPA 해결해야되는 상황이라면 어쩔 수 없이 다수의 insert 쿼리를 통해 할 수 밖에 없다.
- 그래도 어떤 방법이 그나마 빠르게 이를 수행할 수 있을지 비교한다. 출처: https://sabarada.tistory.com/195

````java
// bulk insert example
insert into user (name, age)
values ('chd', 21),
       ('cha', 26),
       ('lolo', 15);
````

### 1-1. save 밖에서 for을 통해 insert ###

````java
@Test
void service() {
    long startTime = System.currentTimeMillis();
    
    for (int i = 0; i < 1000; i++) {
        bulkService.bulkService();
    }
    
    System.out.println("elapsed = " + (System.currentTimeMillis() - startTime) + "ms"); // 7531ms
}
````
````java
public void bulkService() {
    ...
    sampleRepository.save(review);
}
````

````java
// JPA save의 내부 코드에 @Transactional이 들어가 있다.
// save를 할 때 마다 트랜잭션을 잡는 행위를 하기 때문에 위 같은 경과시간을 보여주었다는 것을 확인할 수 있다.
@Transactional
@Override
public <S extends T> S save(S entity) {

    if (entityInformation.isNew(entity)) {
        em.persist(entity);
        return entity;
    } else {
        return em.merge(entity);
    }
}
````

### 1-2. Transactional 을 통한 save ###
- for 문을 도는 부분을 @Tranasactional로 묶어서 트랜잭션 전파(propagation)을 통해 save 한다면 save시 마다 트랜잭션을 새로 열지 않을것이며 성능개선을 할 수 있을 것으로 판단
````java
@Test
void service() {
    long startTime = System.currentTimeMillis();
    
    bulkService.bulkService();    
    
    System.out.println("elapsed = " + (System.currentTimeMillis() - startTime) + "ms"); // 4255ms
}
````
````java
@Transactional
public void bulkService() {
    for (int i = 0; i < 1000; i++) {
        ...
        sampleRepository.save(review);
    }
}
````

### 1-3. @Transaction 안에서 saveAll ###
````java
@Test
void service() { 
    long startTime = System.currentTimeMillis();
    
    bulkService.bulkService();    
    
    System.out.println("elapsed = " + (System.currentTimeMillis() - startTime) + "ms"); // 2850ms
}
````
````java
public void bulkService() {
    List<SampleReq> lists = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
        ...
        lists.add(SampleReq);
    }

    sampleRepository.saveAll(
            sampleReq.stream()
                    .map(req -> sampleMapper.buildReview(userId, sampleId, req))
                    .collect(Collectors.toList())
    )
}
````

### 1-4. Spring Data JDBC의 batchUpdate()를 활용 ###
- Spring Data JDBC는 Spring Data JPA와 함께 혼용해서 사용
- @Transactional을 통해 트랜잭션이 관리될 수 있으므로, 현실적으로 가장 나은 방법

````java
@Repository
public class SampleRepositoryJdbcImpl implements SampleRepositoryJdbc {
	private JdbcTemplate jdbcTemplate;

	public SampleRepositoryJdbcImpl(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
	
	public void batchInsert(List<User> users) {
		jdbcTemplate.batchUpdate(
				"insert into SP_SAMPLE(ID, PW) "
				+ "values(?, ?)",
				 new BatchPreparedStatementSetter() {
					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						User user = users.get(i);
						ps.setString(1, user.getId());
						ps.setString(2, user.getPw());
					}

					@Override
					public int getBatchSize() {
						return users.size();
					}
				});
	}

}
````

## DTO 클래스를 이용한 Request, Response 를 사용해야 한다. ##
- Request 경우 Entity를 사용하게된다면 원치 않은 데이터를 컨트롤러를 통해 넘겨받을 수 있게되고, 그로인한 변경이 발생할 수 있다.
- Response 경우, 비밀번호 같은 민감한정보를 포함해 모든 정보가 노출 된다.

#### DTO Sample ####
````java
public class AccountDto {
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class SignUpReq {
        private String email;
        private String address2;
        private String zip;

        @Builder
        public SignUpReq(String email, String fistName, String lastName, String password, String address1, String address2, String zip) {
            this.email = email;
            this.address2 = address2;
            this.zip = zip;
        }

        public Account toEntity() {
            return Account.builder()
                    .email(this.email)
                    .address2(this.address2)
                    .zip(this.zip)
                    .build();
        }

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class MyAccountReq {
        private String address1;
        private String address2;
        private String zip;

        @Builder
        public MyAccountReq(String address1, String address2, String zip) {
            this.address1 = address1;
            this.address2 = address2;
            this.zip = zip;
        }
    }

    @Getter
    public static class Res {
        private String email;
        private String address2;
        private String zip;

        public Res(Account account) {
            this.email = account.getEmail();
            this.address2 = account.getAddress2();
            this.zip = account.getZip();
        }
    }
}
````


# Querydsl #
Querydsl

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

출처: https://github.com/cheese10yun/spring-jpa-best-practices/blob/master/doc/step-15.md


### 2. QuerydslPredicateExecutor를 이용한 확장 ###
- findById, existsById 같은 유니크 값을 메서드로 표현하는 것이 가독성 및 생산성에 좋다.
- 하지만, 유사한 쿼리가 필요해지면 쿼리 메서드를 지속적으로 추가해야 하는 단점이 있다.
- QuerydslPredicateExecutor를 사용하면 매우 효과적이다.

````java
// springframework-querydsl
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
public interface AccountRepository extends JpaRepository<Account, Long>, AccountSupportRepository, QuerydslPredicateExecutor<Account> {
        ...
}
````

````java
import com.querydsl.core.types.Predicate;

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
