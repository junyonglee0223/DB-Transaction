# DB-Transaction

# 250319
## [DB2 - 스프링 트랜잭션 전파2 #2]
### Spring Transaction 테스트 (singleTx 설정)

#### 개요
Spring의 트랜잭션 관리 기능을 활용하여 기존 테스트에 `singleTx` 설정을 추가하고, `MemberService`의 `joinV1`에서만 트랜잭션이 실행되도록 변경한 테스트를 수행하였다.

#### 변경 사항
1. 기존에는 각각의 `Repository`에서 `save()` 시 트랜잭션을 진행했으며, AOP가 여러 번 생성됨.
2. 변경 후 `MemberService.joinV1`에서만 트랜잭션을 진행하도록 설정하여 단일 트랜잭션이 수행됨.
3. `MemberService - joinV1`에서만 AOP가 생성되는 것을 로그를 통해 확인함.

#### 테스트 방법
- `memberService.joinV1`을 사용하여 회원을 저장하고 로그를 기록하는 방식으로 테스트 수행.
- `MemberRepository`와 `LogRepository`에서 `find` 시 `isPresent` 상태를 확인하여 데이터가 정상적으로 저장되었는지 검증.

#### 실행 로그 분석

```
INFO  j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
INFO  h.s.propagation.MemberServiceTest        : Started MemberServiceTest

DEBUG  o.s.orm.jpa.JpaTransactionManager        : Creating new transaction with name [hello.springtx.propagation.MemberService.joinV1]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
DEBUG  o.s.orm.jpa.JpaTransactionManager        : Opened new EntityManager [SessionImpl(1670283668<open>)] for JPA transaction
TRACE  o.s.t.i.TransactionInterceptor           : Getting transaction for [hello.springtx.propagation.MemberService.joinV1]

INFO  h.springtx.propagation.MemberRepository  : save member!!!
INFO  h.springtx.propagation.LogRepository     : save log message!!!

DEBUG  org.hibernate.SQL                        : insert into member (username,id) values (?,?)
DEBUG  org.hibernate.SQL                        : insert into log (message,id) values (?,?)

DEBUG  o.s.orm.jpa.JpaTransactionManager        : Initiating transaction commit
DEBUG  o.s.orm.jpa.JpaTransactionManager        : Committing JPA transaction on EntityManager [SessionImpl(1670283668<open>)]
DEBUG  o.s.orm.jpa.JpaTransactionManager        : Closing JPA EntityManager [SessionImpl(1670283668<open>)] after transaction

DEBUG  org.hibernate.SQL                        : select m1_0.id,m1_0.username from member m1_0 where m1_0.username=?
DEBUG  org.hibernate.SQL                        : select l1_0.id,l1_0.message from log l1_0 where l1_0.message=?
```
---
### 트랜잭션 테스트 (Transaction Test)

기존에 주석처리된 repository 트랜잭션을 복구하고, 전체적인 트랜잭션 흐름과 테스트 방법을 정리한 내용입니다.

### 1. 트랜잭션 구성

- 서비스 계층(transaction)이 **물리 트랜잭션**으로 동작
- 하위 `MemberRepository`, `LogRepository`에서는 **논리 트랜잭션**으로 동작
- 기존과 동일하게 물리-논리 트랜잭션 연결 구조 유지

### 2. 테스트 메서드

다음 두 가지 테스트 메서드를 진행합니다.
1. `outerTxOn_success` - 정상적으로 트랜잭션이 커밋되는 경우
2. `outerTxOn_fail` - 내부 트랜잭션에서 예외 발생 시 롤백되는 경우

### 3. 성공 테스트 (`outerTxOn_success`)

- 모든 트랜잭션이 정상적으로 커밋됨을 확인
- 주요 로그:

```
INFO  - Initialized JPA EntityManagerFactory for persistence unit 'default'
DEBUG - Creating new transaction with name [hello.springtx.propagation.MemberService.joinV1]: PROPAGATION_REQUIRED, ISOLATION_DEFAULT
DEBUG - Opened new EntityManager [SessionImpl(996328734<open>)] for JPA transaction
...
DEBUG - Closing JPA EntityManager [SessionImpl(996328734<open>)] after transaction
```

### 4. 실패 테스트 (`outerTxOn_fail`)

- 내부 트랜잭션에서 `RuntimeException` 발생
- 기존 트랜잭션이 `rollback-only` 상태로 변경
- 최종적으로 트랜잭션이 커밋되지 않음

```
INFO  - Initialized JPA EntityManagerFactory for persistence unit 'default'
TRACE - Completing transaction for [hello.springtx.propagation.LogRepository.save] after exception: java.lang.RuntimeException
DEBUG - Participating transaction failed - marking existing transaction as rollback-only
DEBUG - Setting JPA transaction on EntityManager [SessionImpl(241222758<open>)] rollback-only
DEBUG - JDBC transaction marked for rollback-only (exception provided for stack trace)
```

---
### 회원가입 시 로그 저장 실패 허용 설정

회원가입을 진행할 때 로그 저장이 실패하더라도 회원가입이 정상적으로 완료되도록 설정하는 방법에 대해 설명합니다.


### 테스트 방법: `recoverException_fail`

- `joinV1` : 특별한 예외 처리가 없기 때문에 예외가 발생하면 전체 트랜잭션이 롤백됩니다.
- `joinV2` : 예외 발생 시 하위 트랜잭션에서 예외 처리를 수행하여 전체 트랜잭션이 정상적으로 커밋될 수 있도록 설정합니다.

**주의사항**
- `memberService`에서 예외를 `throw`하면 전체 서비스가 예외 상태가 되어 회원가입까지 롤백되므로, 예외를 던지지 않아야 합니다.
- 처음 구현 시 회원가입이 커밋되는 문제가 발생했는데, 이는 `joinV2`에 트랜잭션 설정이 없었기 때문이었습니다.


### 로그 분석 (`joinV2` 적용 전)

```log
DEBUG 7872 : Participating in existing transaction
TRACE 7872 : Completing transaction for [hello.springtx.propagation.LogRepository.save] after exception: `java.lang.RuntimeException`
DEBUG 7872 : JDBC transaction marked for rollback-only
DEBUG 7872 : On commit, transaction was marked for rollback-only, rolling back
```

- `LogRepository.save()`에서 예외가 발생하여 기존 트랜잭션이 롤백됨.
- `memberService`의 회원가입 트랜잭션도 롤백됨.


### 해결 방법: `PROPAGATION_REQUIRES_NEW` 적용

`LogRepository`의 트랜잭션 설정을 `PROPAGATION_REQUIRES_NEW`로 변경하면, 기존 트랜잭션을 별도로 관리할 수 있습니다.

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
```


### 로그 분석 (`PROPAGATION_REQUIRES_NEW` 적용 후)

```log
DEBUG 28560 : Suspending current transaction, creating new transaction with name [hello.springtx.propagation.LogRepository.save]
DEBUG 28560 : Rolling back JPA transaction on EntityManager [SessionImpl(1989877020<open>)]
DEBUG 28560 : Resuming suspended transaction after completion of inner transaction
DEBUG 28560 : Committing JPA transaction on EntityManager [SessionImpl(411382490<open>)]
```

- `LogRepository.save()`에서 새로운 트랜잭션을 생성하여 로그 저장을 별도 처리.
- 로그 저장 트랜잭션이 롤백되더라도, 회원가입 트랜잭션은 정상적으로 커밋됨.

---
# 250319
## [DB2 - 스프링 트랜잭션 이해 #3]
### Spring Transaction Propagation

#### 1. 개요
Spring에서 트랜잭션은 같은 데이터베이스 커넥션을 공유하면서 하나의 논리적인 작업 단위로 묶여 실행됩니다. 하지만 특정한 경우 트랜잭션을 독립적으로 처리해야 할 필요가 있습니다.

이 문서에서는 Spring 트랜잭션 전파(Propagation) 옵션을 설정하여 내부 트랜잭션이 외부 트랜잭션과 분리될 수 있도록 하는 방법을 설명합니다.



#### 2. 트랜잭션 전파(Propagation) 문제
기본적으로 내부 트랜잭션이 존재할 때, Spring은 같은 커넥션을 사용합니다. 이로 인해 아래와 같은 문제가 발생할 수 있습니다:

1. **외부 트랜잭션이 롤백되면 내부 트랜잭션도 롤백됨**
2. **내부 트랜잭션을 롤백해도 외부 트랜잭션이 롤백되지 않음**

이를 해결하기 위해 `PROPAGATION_REQUIRES_NEW`를 설정하여 새로운 커넥션을 할당하는 방식이 필요합니다.



#### 3. `PROPAGATION_REQUIRES_NEW` 설정
기본적으로 Spring의 트랜잭션은 같은 커넥션을 공유하지만, `PROPAGATION_REQUIRES_NEW`를 설정하면 별도의 커넥션을 사용하게 됩니다.

```java
DefaultTransactionAttribute definition = new DefaultTransactionAttribute();
definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
```

위 설정을 적용하면 내부 트랜잭션은 새로운 데이터베이스 커넥션을 사용하므로, 내부 트랜잭션이 롤백되더라도 외부 트랜잭션은 영향을 받지 않습니다.



#### 4. 트랜잭션 로그 분석

실제 실행 시 트랜잭션 로그를 확인하면, 내부 트랜잭션과 외부 트랜잭션이 서로 다른 커넥션을 사용하는 것을 알 수 있습니다:

```
INFO  : outer transaction starts!!!
DEBUG : Creating new transaction with name [null]: PROPAGATION_REQUIRED
DEBUG : Acquired Connection [ConnectionImpl@298263fa] for JDBC transaction
INFO  : inner transaction starts!!!
DEBUG : Suspending current transaction, creating new transaction with name [null]
DEBUG : Acquired Connection [ConnectionImpl@6bf70600] for JDBC transaction
INFO  : inner transaction rollback!!!
DEBUG : Rolling back JDBC transaction on Connection [ConnectionImpl@6bf70600]
DEBUG : Resuming suspended transaction after completion of inner transaction
INFO  : outer transaction commit!!!
DEBUG : Committing JDBC transaction on Connection [ConnectionImpl@298263fa]
```

위 로그를 보면, **외부 트랜잭션과 내부 트랜잭션이 서로 다른 커넥션을 사용하며 독립적으로 처리됨**을 확인할 수 있습니다.



#### 5. 트랜잭션 전파 옵션 정리

| 전파 옵션 | 설명 |
|-----------|-------------------------------------------------|
| `REQUIRED` | 기본 설정. 기존 트랜잭션이 있으면 참여하고, 없으면 생성 |
| `REQUIRES_NEW` | 항상 새로운 트랜잭션을 생성 (새로운 커넥션 할당) |
| `SUPPORTS` | 기존 트랜잭션이 있으면 참여, 없으면 트랜잭션 없이 진행 |
| `NOT_SUPPORTED` | 기존 트랜잭션이 있으면 보류하고 트랜잭션 없이 진행 |
| `MANDATORY` | 기존 트랜잭션이 반드시 있어야 함 (없으면 예외 발생) |
| `NEVER` | 기존 트랜잭션이 있으면 예외 발생 |
| `NESTED` | 기존 트랜잭션이 있으면 중첩 트랜잭션 생성 |



#### 6. 주의할 점
- `PROPAGATION_REQUIRES_NEW`를 사용할 경우 **외부 트랜잭션이 내부 트랜잭션 종료 후 커밋되어야 함**
- `PROPAGATION_REQUIRED`를 사용할 경우 **내부 트랜잭션이 외부 트랜잭션과 같은 커넥션을 공유**
- `NESTED`는 부모 트랜잭션이 롤백되면 함께 롤백되지만, 내부 트랜잭션이 롤백되더라도 부모 트랜잭션에는 영향을 주지 않음

---

## [DB2 - 스프링 트랜잭션 전파2 #1]

### 개요
`springtx.propagation` 패키지 내에서 `Member`, `Log` 엔티티를 직접 구현하고, `EntityManager`를 활용하여 트랜잭션 전파 관련 테스트를 수행합니다.

### 주요 기능
- `Member` 엔티티와 `Log` 엔티티를 생성하여 각각 저장하는 기능 구현
- `EntityManager`를 활용한 데이터 저장 및 조회
- `@Transactional`을 적용하여 트랜잭션 전파 방식 실습
- `PROPAGATION_REQUIRED` 트랜잭션 설정을 통해 예외 발생 시 트랜잭션 롤백 여부 확인

### 테스트 케이스

### 1. `outerTxOff_success`
**검증:**
- `memberRepository.find(username).isPresent()` → `true`
- `logRepository.find(username).isPresent()` → `true`

**실행 로그:**
```
[INFO] save member!!!
[DEBUG] insert into member (username,id) values (?,?)
[DEBUG] Committing JPA transaction
[DEBUG] Closing JPA EntityManager
[INFO] save log message!!!
[DEBUG] insert into log (message,id) values (?,?)
[DEBUG] Committing JPA transaction
[DEBUG] Closing JPA EntityManager
```


### 2. `outerTxOff_fail`

**검증:**
- `memberRepository.find(username).isPresent()` → `true`
- `logRepository.find(username).isPresent()` → `false`

**실행 로그:**
```
[INFO] save member!!!
[DEBUG] insert into member (username,id) values (?,?)
[DEBUG] Committing JPA transaction
[DEBUG] Closing JPA EntityManager
[INFO] save log message!!!
[INFO] log exception 발생
[TRACE] Completing transaction after exception: java.lang.RuntimeException
[DEBUG] Rolling back JPA transaction
[DEBUG] Closing JPA EntityManager
```



---
# 250314
## [DB2 - spring transaction]
### 주문 서비스 (Order Service)

#### 개요
Spring Boot 기반의 주문 서비스로, 결제 프로세스를 포함한 비즈니스 로직을 처리합니다. 시스템 예외 발생 시 전체 롤백하고, 비즈니스 예외(잔액 부족 등) 발생 시 주문 내역을 저장한 후 재설정을 안내하는 구조입니다.


### 1. 패키지 구조
```
main..hello.springtx
├── order
│   ├── Order.java (Entity)
│   ├── OrderRepository.java (JpaRepository)
│   ├── OrderService.java (Service)
│   ├── NotEnoughMoneyException.java (Custom Exception)
```

```
test..hello.springtx
├── order
│   ├── OrderServiceTest.java (Test)

```

### 2. Order Entity
- `@Entity`
- `@Table(name = "orders")`
- 필드: `id (PK)`, `username`, `payStatus`


### 3. 예외 처리 (`NotEnoughMoneyException`)
```java
public class NotEnoughMoneyException extends Exception {
    public NotEnoughMoneyException(String message) {
        super(message);
    }
}
```


### 4. OrderService
- `@Transactional` 적용
- 결제 실패 시 예외 발생 후 주문 상태를 `wait`으로 설정
- 결제 성공 시 `complete`로 변경



### 5. OrderServiceTest
- `@SpringBootTest` 및 `@Transactional` 적용
- 결제 성공 및 실패 케이스 검증


### 6. 실행 로그

```text
#JPA log
logging.level.org.springframework.orm.jpa.JpaTransactionManager = DEBUG
logging.level.org.hibernate.resource.transaction = DEBUG

#JPA SQL
logging.level.org.hibernate.SQL = DEBUG
```
application.properties에 sql debug 모드 추가


#### 주문 완료 테스트
```
[DEBUG] drop table if exists orders
[DEBUG] drop table if exists orders_seq
[DEBUG] create table orders (id bigint not null, pay_status varchar(255), username varchar(255), primary key (id)) engine=InnoDB
[DEBUG] create table orders_seq (next_val bigint) engine=InnoDB
[DEBUG] insert into orders_seq values ( 1 )
[INFO]  Initialized JPA EntityManagerFactory for persistence unit 'default'
[INFO]  Started OrderServiceTest

[DEBUG] Creating new transaction [hello.springtx.order.OrderService.order]
[TRACE] Getting transaction for [hello.springtx.order.OrderService.order]
[INFO]  order process starts!!!!

[DEBUG] Saving order
[DEBUG] select next_val as id_val from orders_seq for update
[DEBUG] update orders_seq set next_val= ? where next_val=?

[INFO]  complete pay process
[INFO]  order process finish!!!!

[DEBUG] Committing JPA transaction
[DEBUG] insert into orders (pay_status, username, id) values (?,?,?)
[DEBUG] update orders set pay_status=?, username=? where id=?
[DEBUG] Closing JPA EntityManager
```
#### 예외 발생 테스트 (잔액 부족)
```
[DEBUG] drop table if exists orders
[DEBUG] drop table if exists orders_seq
[DEBUG] create table orders (id bigint not null, pay_status varchar(255), username varchar(255), primary key (id)) engine=InnoDB
[DEBUG] create table orders_seq (next_val bigint) engine=InnoDB
[DEBUG] insert into orders_seq values ( 1 )
[INFO]  Initialized JPA EntityManagerFactory for persistence unit 'default'
[INFO]  Started OrderServiceTest

[DEBUG] Creating new transaction [hello.springtx.order.OrderService.order]
[TRACE] Getting transaction for [hello.springtx.order.OrderService.order]
[INFO]  order process starts!!!!

[DEBUG] Saving order
[DEBUG] select next_val as id_val from orders_seq for update
[DEBUG] update orders_seq set next_val= ? where next_val=?

[INFO]  not enough money exception
[TRACE] Completing transaction after exception: hello.springtx.order.NotEnoughMoneyException
[DEBUG] Committing JPA transaction
[DEBUG] insert into orders (pay_status, username, id) values (?,?,?)
[DEBUG] update orders set pay_status=?, username=? where id=?
[DEBUG] Closing JPA EntityManager
```

#### 예외 발생 테스트 (시스템 예외)
```
[DEBUG] drop table if exists orders
[DEBUG] drop table if exists orders_seq
[DEBUG] create table orders (id bigint not null, pay_status varchar(255), username varchar(255), primary key (id)) engine=InnoDB
[DEBUG] create table orders_seq (next_val bigint) engine=InnoDB
[DEBUG] insert into orders_seq values ( 1 )
[INFO]  Initialized JPA EntityManagerFactory for persistence unit 'default'
[INFO]  Started OrderServiceTest

[DEBUG] Creating new transaction [hello.springtx.order.OrderService.order]
[TRACE] Getting transaction for [hello.springtx.order.OrderService.order]
[INFO]  order process starts!!!!

[DEBUG] Saving order
[DEBUG] select next_val as id_val from orders_seq for update
[DEBUG] update orders_seq set next_val= ? where next_val=?

[INFO]  exception
[TRACE] Completing transaction after exception: java.lang.RuntimeException
[DEBUG] Rolling back JPA transaction
[DEBUG] Closing JPA EntityManager
```

---
### Spring Transaction Propagation Test

#### 1. 패키지 구성
- **propagation** 패키지 생성
- **BasicTxTest** 클래스 생성
- **PlatformTransactionManager**를 필드로 선언하여 트랜잭션을 관리

#### 2. PlatformTransactionManager 설정
- `DataSource`를 기반으로 `PlatformTransactionManager`를 설정하는 **config 클래스** 생성
- 해당 클래스를 **Bean 등록**하여 트랜잭션을 관리하도록 설정

#### 3. 트랜잭션 테스트
`BasicTxTest`를 이용하여 **commit**과 **rollback**을 테스트합니다.

##### 1) Commit 테스트
- `txManager.getTransaction(new DefaultTransactionAttribute())`를 이용해 트랜잭션 상태 체크
- `txManager.commit(status)`를 실행하여 커밋
- 실행 로그:

```
INFO  BasicTxTest: transaction process starts!!!!
DEBUG DataSourceTransactionManager: Creating new transaction with name [null]: PROPAGATION_REQUIRED, ISOLATION_DEFAULT
DEBUG DataSourceTransactionManager: Acquired Connection
DEBUG DataSourceTransactionManager: Switching JDBC Connection to manual commit
INFO  BasicTxTest: commit starts!!
DEBUG DataSourceTransactionManager: Initiating transaction commit
DEBUG DataSourceTransactionManager: Committing JDBC transaction
DEBUG DataSourceTransactionManager: Releasing JDBC Connection after transaction
INFO  BasicTxTest: commit ends!!
```

##### 2) Rollback 테스트
- `txManager.getTransaction(new DefaultTransactionAttribute())`를 이용해 트랜잭션 상태 체크
- `txManager.rollback(status)`를 실행하여 롤백
- 실행 로그:

```
INFO  BasicTxTest: transaction process starts!!!!
DEBUG DataSourceTransactionManager: Creating new transaction with name [null]: PROPAGATION_REQUIRED, ISOLATION_DEFAULT
DEBUG DataSourceTransactionManager: Acquired Connection
DEBUG DataSourceTransactionManager: Switching JDBC Connection to manual commit
INFO  BasicTxTest: rollback starts!!
DEBUG DataSourceTransactionManager: Initiating transaction rollback
DEBUG DataSourceTransactionManager: Rolling back JDBC transaction
DEBUG DataSourceTransactionManager: Releasing JDBC Connection after transaction
INFO  BasicTxTest: rollback ends!!
```

#### 4. Double Commit 테스트
- 동일한 트랜잭션을 두 번 커밋하는 경우 트랜잭션 매니저가 새로운 커넥션을 획득하는지 확인
- 실행 로그:

```
INFO  BasicTxTest: commit 1 starts!!!
DEBUG DataSourceTransactionManager: Initiating transaction commit
DEBUG DataSourceTransactionManager: Committing JDBC transaction
INFO  BasicTxTest: commit 1 ends!!!
DEBUG DataSourceTransactionManager: Creating new transaction with name [null]
DEBUG DataSourceTransactionManager: Acquired Connection for JDBC transaction
INFO  BasicTxTest: commit 2 starts!!!
DEBUG DataSourceTransactionManager: Initiating transaction commit
DEBUG DataSourceTransactionManager: Committing JDBC transaction
INFO  BasicTxTest: commit 2 ends!!!
```

#### 5. Double Commit 후 Rollback 테스트
- 첫 번째 트랜잭션을 커밋한 후, 두 번째 트랜잭션에서 롤백을 수행할 경우
- 실행 로그:

```
INFO  BasicTxTest: ts 1 commit starts!!!
DEBUG DataSourceTransactionManager: Committing JDBC transaction
INFO  BasicTxTest: ts 1 commit ends!!!
DEBUG DataSourceTransactionManager: Creating new transaction with name [null]
DEBUG DataSourceTransactionManager: Acquired Connection for JDBC transaction
INFO  BasicTxTest: ts 2 rollback starts!!!
DEBUG DataSourceTransactionManager: Initiating transaction rollback
DEBUG DataSourceTransactionManager: Rolling back JDBC transaction
INFO  BasicTxTest: ts 2 rollback ends!!!
```
---
### Spring Transaction Propagation 테스트

Spring의 트랜잭션 전파(Propagation) 동작을 테스트한 내용을 정리한 것입니다.


### 1. 테스트 개요
Spring 트랜잭션의 전파 속성을 확인하기 위해 `TransactionStatus`를 이용하여 외부(outer) 트랜잭션과 내부(inner) 트랜잭션을 설정하고, 생성과 커밋 순서를 교차하여 구현하였습니다.

- `isNewTransaction()` 메서드를 활용하여 신규 트랜잭션 여부를 확인
- 내부 트랜잭션에서 `rollback`이 발생할 경우 전체 트랜잭션에 미치는 영향을 테스트


### 2. 기본 트랜잭션 테스트 (innerCommit test)

#### 테스트 설명
1. **외부 트랜잭션(outer)**을 시작하면 새로운 트랜잭션이 생성됨
2. **내부 트랜잭션(inner)**을 시작하면 기존 트랜잭션에 참여
3. 내부 트랜잭션을 커밋해도 실제로는 아무 동작 없음
4. 외부 트랜잭션을 커밋하면 전체 트랜잭션이 정상적으로 반영됨

#### 로그 분석
```plaintext
outer transaction starts!!!
Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
Acquired Connection [HikariProxyConnection]
Switching JDBC Connection to manual commit
outer.isNewTransaction = true

inner transaction starts!!!
Participating in existing transaction
inner.isNewTransaction = false

inner commit!!!
outer commit!!!
Initiating transaction commit
Committing JDBC transaction on Connection
Releasing JDBC Connection after transaction
```

#### 결론
- 내부 트랜잭션은 별도의 커밋을 실행하지 않고, 외부 트랜잭션의 커밋 시 반영됨


### 3. 외부 롤백 테스트 (outerRollback test)

### 테스트 설명
1. **외부 트랜잭션(outer)**을 시작
2. **내부 트랜잭션(inner)**에서 정상적으로 커밋
3. **외부 트랜잭션(outer)에서 롤백**
4. 전체 트랜잭션이 롤백됨

### 로그 분석
```plaintext
outer transaction starts!!!
Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
Acquired Connection [HikariProxyConnection]
Switching JDBC Connection to manual commit

inner transaction starts!!!
Participating in existing transaction
inner ts commit!!!
outer ts rollback!!!
Initiating transaction rollback
Rolling back JDBC transaction on Connection
Releasing JDBC Connection after transaction
```



### 4. 내부 롤백 테스트 (innerRollback test)

#### 테스트 설명
1. **외부 트랜잭션(outer)**을 시작
2. **내부 트랜잭션(inner)**에서 롤백 발생
3. 내부 트랜잭션이 롤백되면 **외부 트랜잭션이 rollback-only로 변경됨**
4. 외부 트랜잭션이 커밋을 시도할 경우 `UnexpectedRollbackException` 발생

#### 로그 분석
```plaintext
outer transaction starts!!!
Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
Acquired Connection [HikariProxyConnection]
Switching JDBC Connection to manual commit

inner transaction starts!!!
Participating in existing transaction
inner ts rollback!!!
Participating transaction failed - marking existing transaction as rollback-only
Setting JDBC transaction rollback-only

outer ts commit!!!
Global transaction is marked as rollback-only but transactional code requested commit
Initiating transaction rollback
Rolling back JDBC transaction on Connection
Releasing JDBC Connection after transaction
```

#### 예외 처리 코드
```java
Assertions.assertThatThrownBy(() -> txManager.commit(outerTs))
          .isInstanceOf(UnexpectedRollbackException.class);
```




---
# 250313
## [DB2 - spring transaction]
### Spring Transaction Rollback 테스트

---

### 1. 개요
Spring의 트랜잭션 관리에서 예외 타입에 따라 롤백 여부가 결정된다. 기본적으로 `unchecked 예외`는 롤백이 발생하고, `checked 예외`는 커밋이 수행된다. 특정 예외에 대해 롤백을 강제하거나, 특정 예외에 대해 롤백을 막는 방법도 존재한다. 본 테스트에서는 다음과 같은 트랜잭션 롤백 동작을 확인한다.

### 2. 트랜잭션 롤백 규칙

- **Unchecked 예외 (`RuntimeException`, `Error`)**: 기본적으로 롤백 발생
- **Checked 예외 (`Exception`, 그 하위 클래스)**: 기본적으로 커밋 진행
- **`rollbackFor` 속성 사용**: 특정 checked 예외라도 롤백 가능
- **`noRollbackFor` 속성 사용**: 특정 unchecked 예외라도 롤백 방지 가능



### 3. 트랜잭션 로그 확인

#### `runtimeException` 실행 로그
```plaintext
DEBUG JpaTransactionManager : Creating new transaction with name [RollbackService.runtimeException]
INFO  RollbackService : runtime exception method
TRACE TransactionInterceptor : Completing transaction after exception: java.lang.RuntimeException
DEBUG JpaTransactionManager : Initiating transaction rollback
DEBUG JpaTransactionManager : Rolling back JPA transaction
```

➡ `RuntimeException` 발생 시 자동으로 롤백됨.

#### `checkedException` 실행 로그
```plaintext
DEBUG JpaTransactionManager : Creating new transaction with name [RollbackService.checkedException]
INFO  RollbackService : checked exception
TRACE TransactionInterceptor : Completing transaction after exception: MyException
DEBUG JpaTransactionManager : Initiating transaction commit
DEBUG JpaTransactionManager : Committing JPA transaction
```

➡ `CheckedException` 발생 시 기본적으로 커밋됨.

#### `rollbackFor` 실행 로그
```plaintext
DEBUG JpaTransactionManager : Creating new transaction with name [RollbackService.rollbackFor]
INFO  RollbackService : rollback for
TRACE TransactionInterceptor : Completing transaction after exception: MyException
DEBUG JpaTransactionManager : Initiating transaction rollback
DEBUG JpaTransactionManager : Rolling back JPA transaction
```

➡ `rollbackFor = MyException.class` 설정을 통해 checked 예외이지만 롤백이 발생함.


---
# 250306
## [DB2 - spring transaction]
### InitTxTest 클래스 설정 및 테스트

#### 1. InitTxTest 개요
InitTxTest 클래스는 Spring의 트랜잭션(@Transactional) 동작을 이해하기 위해 초기화 메서드(initV1, initV2)를 활용하여 테스트하는 목적을 가지고 있습니다. 각 초기화 메서드는 서로 다른 방식(PostConstruct vs EventListener)으로 실행되며, 트랜잭션 활성 여부를 확인합니다.

#### 2. InitTxTest 클래스 설정

##### 2.1 TestConfiguration 설정

- InitTxTestConfig는 테스트 환경에서 `InitClass` 빈을 등록하기 위한 설정 클래스입니다.
- `@Bean` 어노테이션을 사용하여 `InitClass` 객체를 생성하고 스프링 컨테이너에 등록합니다.

##### 2.2 InitClass 내부 구현

- `initV1()`: `@PostConstruct`를 사용하여 빈이 생성된 후 실행되지만, 트랜잭션 프록시가 적용되기 전에 실행되므로 트랜잭션이 활성화되지 않습니다.
- `initV2()`: `@EventListener(ApplicationReadyEvent.class)`를 사용하여 스프링 컨텍스트가 완전히 초기화된 후 실행되며, 트랜잭션이 정상적으로 적용됩니다.

#### 3. 테스트 코드

##### 3.1 개별 테스트 메서드
- `initTestV1()`: `initV1()`을 직접 호출하여 실행한 결과, 트랜잭션이 비활성 상태(false)로 출력됩니다.
- `initTestV2()`: `initV2()`를 직접 호출하면 트랜잭션이 활성 상태(true)로 실행됩니다.

##### 3.2 테스트 실행 로그
```
2025-03-06T09:36:55.519+09:00  INFO 95276 --- [springtx] [    Test worker] h.springtx.apply.InitTxTest$InitClass    : init V1 transaction result = false
2025-03-06T09:36:56.231+09:00  INFO 95276 --- [springtx] [    Test worker] hello.springtx.apply.InitTxTest          : Started InitTxTest in 8.061 seconds (process running for 11.629)
2025-03-06T09:36:56.631+09:00 TRACE 95276 --- [springtx] [    Test worker] o.s.t.i.TransactionInterceptor           : Getting transaction for [hello.springtx.apply.InitTxTest$InitClass.initV2]
2025-03-06T09:36:56.633+09:00  INFO 95276 --- [springtx] [    Test worker] h.springtx.apply.InitTxTest$InitClass    : init V2 transaction result = true
```
- `initV1()`은 트랜잭션이 활성화되지 않은 상태에서 실행되었습니다.
- `initV2()`는 트랜잭션이 정상적으로 활성화된 상태에서 실행되었습니다.

#### 4. PostConstruct와 EventListener의 차이점

| 항목 | @PostConstruct | @EventListener(ApplicationReadyEvent.class) |
|------|---------------|-----------------------------------|
| 실행 시점 | 빈이 생성된 후 즉시 실행됨 | 애플리케이션이 완전히 실행된 후 실행됨 |
| 트랜잭션 적용 | X (프록시 생성 이전 실행) | O (Spring 컨텍스트 초기화 후 실행) |
| 트랜잭션 활성화 여부 | false | true |

- `@PostConstruct`를 사용하면 스프링 빈이 생성되자마자 실행되므로 트랜잭션이 적용되지 않습니다.
- `@EventListener(ApplicationReadyEvent.class)`는 스프링 컨텍스트가 완전히 로드된 후 실행되므로 트랜잭션이 적용됩니다.




---
# 250305
## [DB2 - spring transaction]
### Spring Transactional 내부 호출 테스트 (InternalCallV1Test)

#### 개요
`@Transactional`을 적용하면 프록시가 생성되어 트랜잭션이 해당 프록시를 통해 호출될 때 정상적으로 작동한다. 하지만 내부 메서드를 직접 호출하면 트랜잭션이 적용되지 않는 문제가 발생한다.

본 테스트(`InternalCallV1Test`)를 통해 이 과정을 확인하고, Spring의 트랜잭션 프록시 동작 방식을 검증한다.



#### 테스트 환경
- **SpringBootTest 적용**
- **Slf4j 적용**
- `CallService` 필드 선언 및 `@Autowired` 적용
- `InternalCallV1TestConfig` → static 선언 후 `@TestConfiguration` 적용
- 내부에서 `CallService` Bean으로 등록하는 로직 구현
- `CallService`를 static으로 선언

#### `CallService` 구현
세 개의 메서드 구현:
1. **`external`**: 로그 출력 → `printTxInfo` 호출 → `internal` 호출
2. **`internal`** (`@Transactional` 적용): 로그 출력 → `printTxInfo` 호출
3. **`printTxInfo`**: 트랜잭션 매니저(`TransactionSynchronizationManager`)를 활용하여 적용 여부 확인
    - 트랜잭션 활성화 여부 (`isActualTransactionActive`)
    - `readOnly` 여부 (`isCurrentTransactionReadOnly`)


#### 테스트 시나리오
세 가지 테스트를 수행:
1. **printProxy** → `log callService.class`
2. **internalCall** → `callService.internal()`
3. **externalCall** → `callService.external()`


#### 테스트 결과

##### 1. printProxy 결과
```
2025-03-05T10:09:31.189+09:00  INFO 40632 --- [springtx] [    Test worker] h.springtx.apply.InternalCallV1Test      : callService class = class hello.springtx.apply.InternalCallV1Test$CallService$$SpringCGLIB$$0
Java HotSpot(TM) 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
2025-03-05T10:09:31.249+09:00  INFO 40632 --- [springtx] [ionShutdownHook] j.LocalContainerEntityManagerFactoryBean : Closing JPA EntityManagerFactory for persistence unit 'default'
```
- `SpringCGLIB` 프록시가 적용됨을 확인할 수 있음.

##### 2. externalCall 결과
```
2025-03-05T10:06:55.433+09:00  INFO 91372 --- [springtx] [    Test worker] h.springtx.apply.InternalCallV1Test      : Started InternalCallV1Test in 7.969 seconds (process running for 11.578)
2025-03-05T10:06:57.478+09:00  INFO 91372 --- [springtx] [    Test worker] h.s.a.InternalCallV1Test$CallService     : external call
2025-03-05T10:06:57.481+09:00  INFO 91372 --- [springtx] [    Test worker] h.s.a.InternalCallV1Test$CallService     : trace active result = false
2025-03-05T10:06:57.482+09:00  INFO 91372 --- [springtx] [    Test worker] h.s.a.InternalCallV1Test$CallService     : read only result = false
2025-03-05T10:06:57.482+09:00  INFO 91372 --- [springtx] [    Test worker] h.s.a.InternalCallV1Test$CallService     : internal call
2025-03-05T10:06:57.482+09:00  INFO 91372 --- [springtx] [    Test worker] h.s.a.InternalCallV1Test$CallService     : trace active result = false
2025-03-05T10:06:57.484+09:00  INFO 91372 --- [springtx] [    Test worker] h.s.a.InternalCallV1Test$CallService     : read only result = false
Java HotSpot(TM) 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
2025-03-05T10:06:57.570+09:00  INFO 91372 --- [springtx] [ionShutdownHook] j.LocalContainerEntityManagerFactoryBean : Closing JPA EntityManagerFactory for persistence unit 'default'
```
- `externalCall`에서 `internal`을 호출했으나 트랜잭션이 적용되지 않음.
- 내부 호출 시 `trace active result = false`, `read only result = false` 확인됨.

##### 3. internalCall 결과
```
2025-03-05T10:05:58.752+09:00 TRACE 39684 --- [springtx] [    Test worker] o.s.t.i.TransactionInterceptor           : Getting transaction for [hello.springtx.apply.InternalCallV1Test$CallService.internal]
2025-03-05T10:05:58.754+09:00  INFO 39684 --- [springtx] [    Test worker] h.s.a.InternalCallV1Test$CallService     : internal call
2025-03-05T10:05:58.754+09:00  INFO 39684 --- [springtx] [    Test worker] h.s.a.InternalCallV1Test$CallService     : trace active result = true
2025-03-05T10:05:58.755+09:00  INFO 39684 --- [springtx] [    Test worker] h.s.a.InternalCallV1Test$CallService     : read only result = false
2025-03-05T10:05:58.755+09:00 TRACE 39684 --- [springtx] [    Test worker] o.s.t.i.TransactionInterceptor           : Completing transaction for [hello.springtx.apply.InternalCallV1Test$CallService.internal]
```
- 내부 메서드를 직접 호출한 경우, 트랜잭션이 정상적으로 활성화됨 (`trace active result = true`).
---
### InternalCallV2Test 개요

`InternalCallV2Test`는 내부 서비스(`InternalService`)를 별도로 구현하고, 
`CallService`와 결합하는 방식으로 내부 호출 시 
트랜잭션 프록시 적용 여부를 확인하는 테스트이다.


### 1. 구성 요소

#### InternalCallV2Test
- `CallService` 및 `InternalService` 필드를 가짐.
- `InternalCallV2TestConfig`에서 `CallService` 생성 시 `InternalService`를 주입받음.
- `CallService`가 `InternalService`를 호출하는 방식으로 테스트 수행.

#### InternalCallV2TestConfig
- `CallService`를 빈으로 등록할 때 `InternalService`를 주입.
- `InternalService`를 별도로 빈으로 등록.

#### CallService
- `@Slf4j` 애너테이션 사용.
- `@RequiredArgsConstructor`를 사용하여 `final` 필드에 대한 생성자를 자동 생성.
- `external()` 및 `printTxInfo()` 메서드 구현.
- 내부 필드에 `InternalService`를 가지고 있으며, 트랜잭션 실행 여부를 확인하기 위해 `printTxInfo()`를 호출.

#### InternalService
- `@Slf4j` 애너테이션 사용.
- `@Transactional` 애너테이션을 적용한 `internal()` 메서드 구현.
- `printTxInfo()`를 통해 현재 트랜잭션 활성 여부 확인.

---

### 2. 실행 결과

테스트 실행 시 `CallService`의 `external()` 메서드를 호출하면 다음과 같은 로그가 출력됩니다:

```log
INFO  InternalCallV2Test$CallService     : external call
INFO  InternalCallV2Test$CallService     : trace active result = false
TRACE TransactionInterceptor           : Getting transaction for [InternalService.internal]
INFO  InternalCallV2Test$InternalService : internal call
INFO  InternalCallV2Test$InternalService : trace active result = true
TRACE TransactionInterceptor           : Completing transaction for [InternalService.internal]
```

- `external()` 호출 시 트랜잭션이 활성화되지 않음.
- `internal()` 호출 시 `TransactionInterceptor`가 트랜잭션을 생성하여 적용됨.
- 트랜잭션 활성 여부를 `trace active result = true` 로그를 통해 확인할 수 있음.

---

### 3. 주요 개념

#### 트랜잭션 프록시 적용 원리
- `@Transactional`은 **Spring AOP 프록시 방식**으로 동작.
- 내부 호출 시 프록시를 거치지 않으면 트랜잭션이 적용되지 않음.
- 이를 방지하기 위해 `InternalService`를 별도 빈으로 분리하여 `CallService`에서 주입받아 호출.

#### `@Transactional` 적용 방식
- **public 메서드에만 적용하는 것이 관례**
- 같은 클래스 내에서 메서드를 호출하면 트랜잭션이 적용되지 않을 수 있음 (프록시를 거치지 않음)
- 트랜잭션이 필요한 로직은 별도 서비스 클래스로 분리하는 것이 바람직함.





---
# 250304
## [DB2 - spring transaction]

### Spring Transaction 기본 개념 및 테스트

#### 1. 개요
Spring의 트랜잭션(Transaction) 기능을 테스트하고, `@Transactional`의 동작을 확인하기 위해 기본적인 설정과 테스트 코드를 작성한다.

#### 2. 환경 설정
- **Spring Boot 3.x** 기반
- **JPA 및 HikariCP** 사용
- **MySQL 8.0** 연동
- **JUnit 5**를 이용한 테스트

#### 3. 트랜잭션 로그 설정
Spring 트랜잭션 인터셉터의 동작을 추적하기 위해 `application.properties`에 다음 설정을 추가한다.

```properties
logging.level.org.springframework.transaction.interceptor = TRACE
```

이 설정을 통해 트랜잭션의 시작과 완료에 대한 자세한 로그를 확인할 수 있다.

#### 4. 테스트 코드 구현
테스트 코드를 작성하여 `@Transactional`이 적용된 메서드와 그렇지 않은 메서드의 차이를 확인한다.


#### 5. 실행 결과 분석

트랜잭션 인터셉터 로그를 확인하여 `@Transactional`이 적용된 메서드(`tx`)와 그렇지 않은 메서드(`nonTx`)의 동작 차이를 분석한다.

```plaintext
2025-03-04T12:19:07.916+09:00 TRACE 61352 --- [springtx] [    Test worker] o.s.t.i.TransactionInterceptor           : Getting transaction for [hello.springtx.apply.TestBasicTest$BasicService.tx]
2025-03-04T12:19:07.917+09:00  INFO 61352 --- [springtx] [    Test worker] hello.springtx.apply.TestBasicTest       : tx method starts
2025-03-04T12:19:07.917+09:00  INFO 61352 --- [springtx] [    Test worker] hello.springtx.apply.TestBasicTest       : trace active result = true
2025-03-04T12:19:07.918+09:00 TRACE 61352 --- [springtx] [    Test worker] o.s.t.i.TransactionInterceptor           : Completing transaction for [hello.springtx.apply.TestBasicTest$BasicService.tx]
```

이 로그를 통해 `tx()` 메서드에서는 트랜잭션이 활성화된 것을 확인할 수 있다.

반면, `nonTx()` 메서드는 트랜잭션이 활성화되지 않는다.

```plaintext
2025-03-04T12:10:45.165+09:00  INFO 86728 --- [springtx] [    Test worker] hello.springtx.apply.TestBasicTest       : nonTx method starts
2025-03-04T12:10:45.165+09:00  INFO 86728 --- [springtx] [    Test worker] hello.springtx.apply.TestBasicTest       : trace active result = false
```
---
추가
readonly를 활용한 우선순위 테스트
```text
2025-03-04T14:13:25.400+09:00 TRACE 90140 --- [springtx] [    Test worker] o.s.t.i.TransactionInterceptor           : Getting transaction for [hello.springtx.apply.TxLevelTest$LevelService.write]
2025-03-04T14:13:25.401+09:00  INFO 90140 --- [springtx] [    Test worker] h.s.apply.TxLevelTest$LevelService       : write call
2025-03-04T14:13:25.401+09:00  INFO 90140 --- [springtx] [    Test worker] h.s.apply.TxLevelTest$LevelService       : transactional active = true
2025-03-04T14:13:25.402+09:00  INFO 90140 --- [springtx] [    Test worker] h.s.apply.TxLevelTest$LevelService       : transaction readonly = false
2025-03-04T14:13:25.403+09:00 TRACE 90140 --- [springtx] [    Test worker] o.s.t.i.TransactionInterceptor           : Completing transaction for [hello.springtx.apply.TxLevelTest$LevelService.write]
2025-03-04T14:13:25.418+09:00 TRACE 90140 --- [springtx] [    Test worker] o.s.t.i.TransactionInterceptor           : Getting transaction for [hello.springtx.apply.TxLevelTest$LevelService.read]
2025-03-04T14:13:25.419+09:00  INFO 90140 --- [springtx] [    Test worker] h.s.apply.TxLevelTest$LevelService       : read call
2025-03-04T14:13:25.419+09:00  INFO 90140 --- [springtx] [    Test worker] h.s.apply.TxLevelTest$LevelService       : transactional active = true
2025-03-04T14:13:25.419+09:00  INFO 90140 --- [springtx] [    Test worker] h.s.apply.TxLevelTest$LevelService       : transaction readonly = true
2025-03-04T14:13:25.420+09:00 TRACE 90140 --- [springtx] [    Test worker] o.s.t.i.TransactionInterceptor           : Completing transaction for [hello.springtx.apply.TxLevelTest$LevelService.read]
```
