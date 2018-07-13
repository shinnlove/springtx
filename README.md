# 一、数据库事务前情提要

## 1、多用户带来的问题

mysql数据库和linux操作系统一样支持多用户，不同客户端可能读取相同表。

## 2、不同引擎的锁定机制

**MyISAM引擎使用表级锁定机制，InnoDB可以支持到行级锁定（一个客户端修改一些行，另一个可以读和修改其他行）。**当然，对于同一行数据遵循先来后到原则。

并发访问带来的问题：**一个客户端事务能否看到其他客户端事务所做的修改？**

## 3、并发访问的3个可能

带来的三种结果：

- **脏读**——某个事务A所做的修改尚未提交，其他事务就能看到这些修改。但是有可能这个事务A后来被回滚了，那其他事务读取的数据就是脏数据。
- **不可重复读**——同一个事务A使用同一条select语句在每次读取数据的时候得到的结果都不一样。有可能其他事务在事务A的两次select之间修改、插入了某些行。
- **幻读**——一个事务A突然看到一个以前没有见过的行，常见于刚执行完select后，有另一个事务B插入了新的行。

## 4、隔离级别解决3种情形

这些结果是不太好的，所以mysql数据库提供了**4种事务隔离级别**。

- READ UNCOMMITTED——允许某个事务A看到其他事务未提交的数据，这有可能要承担脏读的风险，不过操作事务A的客户端应该无所谓或者有准备。
- READ COMMITTED——允许某个事务A只能看到其他事务已提交的数据，不会出现脏读，但有可能不可重复读（因为其他事务已经提交了满足条件的行）。
- REPEATABLE READ——可以重复读，在某个事务A中执行同一条select，看到的结果是一样的。哪怕其他事务已经插入了满足条件的行、或者修改了某些满足条件的行变得不满足条件，**由数据库底层机制保证**事务A没结束之前也是看不到这些记录的。
  **即便其他事务修改了记录不满足条件，数据库底层机制（缓存）也保证当前事务可重复读。**
- SERIALIZABLE——串行化执行，隔离事务更彻底。某个事务A正在查看的行只有等到它完成时其他事务才能修改。
  **不可重复读有多行和少行的问题，串行化执行解决少行的问题，会锁定正在查看的行，在上一个基础上杜绝修改。**

## 5、InnoDB的隔离级别表

| 隔离级别             | 脏读   | 不可重复读 | 幻读   |
| :--------------- | ---- | ----- | ---- |
| READ UNCOMMITTED | 是    | 是     | 是    |
| READ COMMITTED   | 否    | 是     | 是    |
| REPEATABLE READ  | 否    | 否     | 否    |
| SERIALIZABLE     | 否    | 否     | 否    |

**MySQL的InnoDB存储引擎默认隔离级别是REPEATABLE READ可重复读，而大多数数据库的隔离级别是READ COMMITTED保证不脏读。**

## 6、Mysql修改隔离级别

- 在服务器启动时添加参数：--transaction-isolation选项
- 在运行时使用SET TRANSACTION语句：
    SET GLOBAL TRANSACTION ISOLATION LEVEL *level*（**拥有SUPER权限**）
    SET SESSION TRANSACTION ISOLATION LEVEL *level*（**对服务器当前会话里所有后续事务起作用**）
    SET TRANSACTION ISOLATION LEVEL *level*（**对下一个事务生效**）

客户端修改自己的隔离级别不需要特殊权限。

# 二、spring中事务配置方法

编程式和声明式都需要传入transactionManager，因此都需要一个数据源，可以用dbcp连接池、也可以用c3p0连接池。
这里先使用dbcp的连接池代替：
```xml
<!--配置数据源-->
<bean id="dataSource" class="org.apache.commons.dbcp.BasicDataSource" destroy-method="close">
    <property name="driverClassName" value="com.mysql.jdbc.Driver"/>
    <property name="url" value="jdbc:mysql://127.0.0.1:3306/springdemo"/>
    <property name="username" value="root"/>
    <property name="password" value=""/>
</bean>
```

## 1、编程式事务

### a) 意义与特点
编程式事务就是在代码中使用显式的事务模板去写代码控制。
特点：完全手动挡，细粒度控制事务。

### b) 使用方式

#### i) 在代码中配置事务模板
配置一个事务模板，传入transactionManager，可以是JDBC的，也可以是hibernate的。
这里演示使用jdbc的事务管理器：
```xml
<bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
        <property name="dataSource" ref="dataSource"/>
    </bean>
```
而后要在代码中配置事务模板：
```xml
<!--配置默认的PROPAGATION_REQUIRED事务模板（传入事务管理器）-->
<bean id="transactionTemplate" class="org.springframework.transaction.support.TransactionTemplate">
    <property name="transactionManager" ref="transactionManager"/>
</bean>
```

#### ii) 在代码中显式开启事务
```java
// 默认事务模板开启一个事务
transactionTemplate.execute(new TransactionCallbackWithoutResult() {
    @Override
    protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
        // 先插入一条数据
        jdbcTemplate.update("insert into user (name, password) values (?, ?)", "Shinnlove",
                "123456");
        // 内层再开启一个事务
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                jdbcTemplate.update("insert into user (name, password) values (?, ?)",
                        "hurui", "234567");
                // 内部事务设置了 setRollbackOnly，
                status.setRollbackOnly();
            }
        });
    }
});
```

### c) 事务控制

## 2、声明式事务

### a) 意义与特点
声明式事务就是在配置文件中指明哪些方法会被事务控制，应该怎么处理等。
特点：手自一体，用xml配置文件实现手动控制，spring托管档位切换。

### b) 使用方式
先理解声明式事务的理念：
**研发人员只关心业务函数调用。如一个不可拆分过程中调用5个子函数，那么这个过程函数的执行应该是一个事务。可以对这个函数名、所在包下所有类或函数进行事务拦截配置。**
spring声明式事务需要使用`<tx:advice>`配合`<aop:config>`和`<aop:advisor>`一起使用。

#### i 说明声明式事务的处理方式和特点
一个`<tx:advice>`是一个声明事务，传入具体的transactionManager做事务控制。
其中`<tx:method>`和`<tx:attributes>`说明哪些方法执行的时候会进行spring事务托管处理，可以使用通配符。
而后定义事务的传播属性等元素。
```xml
<!-- spring声明式事务，使用的manager是上边配置的 -->
<tx:advice id="txAdvice" transaction-manager="transactionManager">
    <tx:attributes>
        <!-- 声明哪些方法参与声明式事务，可以配置method name="save*"之类的方法，REQUIRED是默认的事务传播行为 -->
        <tx:method name="*" propagation="REQUIRED"/>
    </tx:attributes>
</tx:advice>
```
#### ii 说明在哪些地方进行事务拦截处理
使用`<aop:config>`和`<aop:pointcut>`声明切点，`<aop:advisor>`说明关联哪个声明式事务。
```xml
<!-- 配置切点连接声明式事务，aop:advisor可以连接tx:advice、也可以连接拦截器 -->
<aop:config>
    <aop:pointcut id="skill"
                  expression="execution(* com.khotyn.springtx.test.AopTxCalabashBoy.skill(..))"/>
    <aop:advisor advice-ref="txAdvice" pointcut-ref="skill"/>
</aop:config>
```
### c) 事务控制
默认的声明式事务配置如下：
- 事务传播设置是`REQUIRED`
- 隔离级别是`DEFAULT`
- 事务是`读/写`
- 事务超时默认是依赖于事务系统的，或者事务超时没有被支持。
- 任何`RuntimeException`将触发事务回滚，任何配置不关心的Exception将不会触发事务回滚

事务参数表格：
| 属性                | 是否需要 | 默认值      | 描述                                       |
| :---------------- | ---- | -------- | ---------------------------------------- |
| `name`            | 是    |          | 与事务属性关联的方法名。通配符（*）可以用来指定一批关联到相同的事务属性的方法。 如：`get*`、`handle*`、`on*Event`等等。 |
| `propagation`     | 不    | REQUIRED | 事务传播行为                                   |
| `isolation`       | 不    | DEFAULT  | 事务隔离级别                                   |
| `timeout`         | 不    |          | 事务超时的时间（以秒为单位）                           |
| `read-only`       | 不    | false    | 事务是否只读？（典型地，对于只执行查询的事务你会将该属性设为true，如果出现了更新、插入或是删除语句时只读事务就会失败） |
| `rollback-for`    | 不    |          | 将被触发进行回滚的`Exception(s)`；以逗号分开。 如：`com.foo.MyBusinessException,ServletException` |
| `no-rollback-for` |      |          | 不被触发进行回滚的`Exception(s)`；以逗号分开。 如：`com.foo.MyBusinessException,ServletException` |

## 3、注解式事务

### a) 意义与特点
注解式事务就是使用@Transactional注解，将原本配置在xml中的属性都搬到注解的属性中。
特点：手自一体，用注解在代码函数中实现手动控制，spring托管档位切换。

### b) 使用方式

### c) 事务控制

# 三、spring事务的传播级别与隔离级别

在spring应用中，使用声明式事务管理使用率较高，因为比较好控制。

## 1、spring事务的传播级别

在TransactionDefinition接口中定义了事务的传播级别。
(thinking in java 说放入接口中的域都是自动final的，接口用来做Enum类型的枚举不可取，后话，另说)

### b) 传播级别概述

在spring中一共定义了六种事务传播的级别+spring特殊要求传播：

- PROPAGATION\_REQUIRED——支持当前事务，如果当前没有事务，就新建一个事务。这是最常见的选择。
  REQUIRED特点：**我需要，没有就找一个**
- PROPAGATION\_SUPPORTS——支持当前事务，如果当前没有事务，就以非事务方式执行。
  SUPPORTS特点：**支持你，但是没有会更好**
- PROPAGATION\_MANDATORY——支持当前事务，如果当前没有事务，就抛出异常。
  MANDATORY特点：**强制需要，没有则哭爹叫娘**
- PROPAGATION\_REQUIRES_NEW——新建事务，如果当前存在事务，把当前事务挂起。
  REQUIRES_NEW特点：**新独立、挂外部、内外无影响**。
- PROPAGATION\_NOT_SUPPORTED——以非事务方式执行操作，如果当前存在事务，就把当前事务挂起。
  NOT_SUPPORTED特点：**不支持，若存在则靠边站**
- PROPAGATION\_NEVER——以非事务方式执行，如果当前存在事务，则抛出异常。（类似于EJB CMT）
  NEVER特点：**决不妥协上下文中存在事务**
- PROPAGATION\_NESTED——如果当前存在事务，则在嵌套事务内执行。如果当前没有事务，则进行与PROPAGATION_REQUIRED类似的操作。
  (要求事务管理器或者使用JDBC3.0的Savepoint API提供嵌套事务行为，如Spring的DataSourceTransactionManager)
  NESTED特点：**内嵌子事务、存断点做事、自主盈亏不影响老板、随外部老板盈利或亏本**。

事务传播级别表格：

| 事务传播级别                       | 自己开启事务前是否允许上下文存在事务                   | 抛出配置忽略以外的错误是否打断自身事务 | 自己受上下文中已存在事务rollback影响 | 自己的rollback错误向上下文事务渗透 | 自己的非rollback的错误向上下文事务渗透 | 自身回滚是否影响内部新开启事务的回滚                       |
| :--------------------------- | ------------------------------------ | ------------------- | ---------------------- | --------------------- | ----------------------- | ---------------------------------------- |
| PROPAGATION_REQUIRED(**默认**) | 支持，没有则创建                             | 是                   | 是                      | 是                     | 是                       | 是(**REQUIRES_NEW以外**的其他事务都会回滚)           |
| PROPAGATION_REQUIRES_NEW     | 允许                                   | 是                   | 否，甚至免疫nested           | 否                     | 是                       | 是(**REQUIRES_NEW以外**的其他事务都会回滚)           |
| PROPAGATION_NESTED           | 允许                                   | 是                   |                        | 否                     |                         | 是(**REQUIRES_NEW以外**的其他事务都会回滚，savepoint关系) |
| PROPAGATION_MANDATORY        | 必须存在一个事务，否则抛错                        | 是                   |                        | 是                     |                         |                                          |
| PROPAGATION_NEVER            | 上下文存在**非NEVER**的事务就抛错（唯我独尊），只允许自己的事务 | 是                   |                        |                       | 是，且上下文只允许是**NEVER**事务   |                                          |
| PROPAGATION_NOT_SUPPORTED    | 允许                                   | 是                   | 否，免疫                   |                       | 是                       |                                          |
| PROPAGATION_SUPPORTS         | 无所谓，没有就以无事务方式执行                      | 否（如果上下文没有事务）        |                        | 是                     |                         |                                          |




### b) REQUIRES_NEW和NESTED区别

比较容易混淆的是：PROPAGATION_REQUIRES_NEW和PROPAGATION_NESTED的区别。

PROPAGATION_REQUIRES_NEW 启动一个新的, 不依赖于环境的 "内部" 事务. 这个事务将被完全 commited 或 rolled back 而不依赖于外部事务, 它拥有自己的隔离范围, 自己的锁, 等等。当内部事务开始执行时, 外部事务将被挂起, 内务事务结束时, 外部事务将继续执行。

另一方面, PROPAGATION_NESTED 开始一个 "嵌套的" 事务,  它是已经存在事务的一个真正的子事务. 嵌套事务开始执行时,  它将取得一个 savepoint. 如果这个嵌套事务失败, 我们将回滚到此 savepoint. 嵌套事务是外部事务的一部分, 只有外部事务结束后它才会被提交。

由此可见, PROPAGATION_REQUIRES_NEW 和 PROPAGATION_NESTED 的最大区别在于：**PROPAGATION_REQUIRES_NEW 完全是一个新的事务；而PROPAGATION_NESTED则是外部事务的子事务——如果外部事务成功commit, 嵌套事务也会被 commit；如果外部事务被roll back，嵌套事务也会被roll back、即便嵌套事务已经commit成功一样也要roll back**。

## 2、spring事务的隔离级别


# 四、spring事务传播级别的实验

限于时间关系，模拟一个场景：未授权读——READ UNCOMMITTED的spring代码。

## 1、数据库配置
这里我们使用MySQL数据库来做这个实验，以下是DDL为：
```sql
CREATE TABLE `user` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(32) DEFAULT NULL,
  `password` varchar(128) DEFAULT NULL,
  `age` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;
```

## 2、spring配置

### a) 接下来配置数据源
```xml
    <!--配置数据源-->
    <bean id="dataSource" class="org.apache.commons.dbcp.BasicDataSource" destroy-method="close">
        <property name="driverClassName" value="com.mysql.jdbc.Driver"/>
        <property name="url" value="jdbc:mysql://127.0.0.1:3306/springdemo"/>
        <property name="username" value="root"/>
        <property name="password" value=""/>
    </bean>
```

### b) 配置对应的事务管理器
```xml
    <!--在数据源上配置事务管理器-->
    <bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
        <property name="dataSource" ref="dataSource"/>
    </bean>
```

### c) 配置不同隔离级别的事务模板

```xml
    <!--隔离级别为ISOLATION_READ_UNCOMMITTED的事务模板-->
    <bean id="readUncommittedTransactionTemplate" class="org.springframework.transaction.support.TransactionTemplate">
        <property name="transactionManager" ref="transactionManager"/>
        <property name="isolationLevelName" value="ISOLATION_READ_UNCOMMITTED"/>
    </bean>

    <!--隔离级别为ISOLATION_READ_COMMITTED的事务模板-->
    <bean id="readCommittedTransactionTemplate" class="org.springframework.transaction.support.TransactionTemplate">
        <property name="transactionManager" ref="transactionManager"/>
        <property name="isolationLevelName" value="ISOLATION_READ_COMMITTED"/>
    </bean>

    <!--隔离级别为ISOLATION_REPEATABLE_READ的事务模板-->
    <bean id="repeatableReadTransactionTemplate" class="org.springframework.transaction.support.TransactionTemplate">
        <property name="transactionManager" ref="transactionManager"/>
        <property name="isolationLevelName" value="ISOLATION_REPEATABLE_READ"/>
    </bean>

    <!--隔离级别为ISOLATION_SERIALIZABLE的事务模板-->
    <bean id="serializableTransactionTemplate" class="org.springframework.transaction.support.TransactionTemplate">
        <property name="transactionManager" ref="transactionManager"/>
        <property name="isolationLevelName" value="ISOLATION_SERIALIZABLE"/>
    </bean>
```

## 3、测试类的spring注入
在测试类中注入准备的事务模板。
```java
@RunWith(JUnit4.class)
public class IsolationLevelTest {
  /**
   * 事务模板
   */
  private JdbcTemplate jdbcTemplate;

  /**
   * 事务隔离级别为未授权读的事务模板
   */
  private TransactionTemplate readUncommittedTransactionTemplate;
  /**
   * 事务隔离级别为授权读的事务模板
   */
  private TransactionTemplate readCommittedTransactionTemplate;
  /**
   * 事务隔离级别为可重复读的事务模板
   */
  private TransactionTemplate repeatableReadTransactionTemplate;
  /**
   * 事务隔离级别为可序列化的事务模板
   */
  private TransactionTemplate serializableTransactionTemplate;

  @Before
  public void startUp() {
    ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
        "applicationContext.xml");
    serializableTransactionTemplate = applicationContext.getBean(
        "serializableTransactionTemplate", TransactionTemplate.class);
    readUncommittedTransactionTemplate = applicationContext
        .getBean("readUncommittedTransactionTemplate",
            TransactionTemplate.class);
    readCommittedTransactionTemplate = applicationContext.getBean(
        "readCommittedTransactionTemplate", TransactionTemplate.class);
    repeatableReadTransactionTemplate = applicationContext.getBean(
        "repeatableReadTransactionTemplate", TransactionTemplate.class);
    DataSource dataSource = applicationContext.getBean("dataSource",
        DataSource.class);
    jdbcTemplate = new JdbcTemplate(dataSource);
  }
```

## 4、模拟脏读
**原理：先插入一条用户记录，采用一个线程A读取两次相同数据，而另一个线程B在线程A两次读取之间修改数据并回滚的方式产生脏读。**
```java
   /**
   * 模拟脏读现象
   *
   * @param transactionTemplate
   * @return
   * @throws ExecutionException
   * @throws InterruptedException
   */
  private Boolean dirtyRead(final TransactionTemplate transactionTemplate)
      throws ExecutionException, InterruptedException {
    // 1. 插入一条数据
    jdbcTemplate.update("insert into user (name, password) values (?, ?)",
        "Hurui", "123456");
    Future future = Executors.newSingleThreadExecutor().submit(
        new Callable<Boolean>() {

          @Override
          public Boolean call() {
            return transactionTemplate
                .execute(new TransactionCallback<Boolean>() {

                  @Override
                  public Boolean doInTransaction(
                      TransactionStatus status) {
                    // 2. 事务一读取数据
                    String password1 = jdbcTemplate
                        .queryForObject(
                            "select password from user where name = ?",
                            String.class, "Hurui");
                    try {
                      TimeUnit.MILLISECONDS.sleep(500);
                    } catch (InterruptedException e) {
                      e.printStackTrace();
                    }
                    // 4. 事务一再次读取数据
                    String password2 = jdbcTemplate
                        .queryForObject(
                            "select password from user where name = ?",
                            String.class, "Hurui");
                    return password1.equals(password2);
                  }
                });
          }
        });
    Executors.newSingleThreadExecutor().submit(new Runnable() {

      @Override
      public void run() {
        transactionTemplate
            .execute(new TransactionCallbackWithoutResult() {
              @Override
              protected void doInTransactionWithoutResult(
                  TransactionStatus status) {
                try {
                  TimeUnit.MILLISECONDS.sleep(100);
                  // 3. 事务二更新数据
                  jdbcTemplate
                      .update(
                          "update user set password = ? where name = ?",
                          "654321", "Hurui");
                  TimeUnit.MILLISECONDS.sleep(500);
                  // 5. 事务二再次发生回滚
                  status.setRollbackOnly();
                } catch (InterruptedException e) {
                  e.printStackTrace();
                }
              }
            });
      }

    });

    return (Boolean) future.get();
  }
```

## 5、最后上测试用例
```java
    @Test
  public void 测试事务隔离级别为未授权读_会发生脏读() throws ExecutionException,
      InterruptedException {
    Assert.assertFalse(dirtyRead(readUncommittedTransactionTemplate));
  }
```

## 6、运行结果
### a) 开始模拟
运行测试用例，开始debug
![测试开始](leanote://file/getImage?fileId=596b8465ab644153a000141b)
### b) 数据库插入数据
断点到执行插入
![执行插入](leanote://file/getImage?fileId=596b84a1ab644153a0001429)
数据库中已经落库
![数据库已有数据](leanote://file/getImage?fileId=596b84c4ab644153a000142c)
### c) 两个结果不相等
用例通过，说明返回的确实是false，两个值不等，**说明未授权读会出现脏读**。
![用例通过](leanote://file/getImage?fileId=596b8523ab644153a0001439)

# 五、spring事务与传播级别的实验

## 1、spring事务模板
```xml
    <!--配置事务模板（传入事务管理器）-->
    <bean id="transactionTemplate" class="org.springframework.transaction.support.TransactionTemplate">
        <property name="transactionManager" ref="transactionManager"/>
    </bean>
```

## 2、不同传播级别的事务模板配置
```xml
    <!--传播级别为PROPAGATION_REQUIRES_NEW的事务模板-->
    <bean id="requiresNewTransactionTemplate" class="org.springframework.transaction.support.TransactionTemplate">
        <property name="transactionManager" ref="transactionManager"/>
        <property name="propagationBehaviorName" value="PROPAGATION_REQUIRES_NEW"/>
    </bean>

    <!--传播级别为PROPAGATION_NESTED的事务模板-->
    <bean id="nestedTransactionTemplate" class="org.springframework.transaction.support.TransactionTemplate">
        <property name="transactionManager" ref="transactionManager"/>
        <property name="propagationBehaviorName" value="PROPAGATION_NESTED"/>
    </bean>

    <!--传播级别为PROPAGATION_MANDATORY的事务模板-->
    <bean id="mandatoryTransactionTemplate" class="org.springframework.transaction.support.TransactionTemplate">
        <property name="transactionManager" ref="transactionManager"/>
        <property name="propagationBehaviorName" value="PROPAGATION_MANDATORY"/>
    </bean>

    <!--传播级别为PROPAGATION_NEVER的事务模板-->
    <bean id="neverTransactionTemplate" class="org.springframework.transaction.support.TransactionTemplate">
        <property name="transactionManager" ref="transactionManager"/>
        <property name="propagationBehaviorName" value="PROPAGATION_NEVER"/>
    </bean>

    <!--传播级别为PROPAGATION_NOT_SUPPORTED的事务模板-->
    <bean id="notSupportedTransactionTemplate" class="org.springframework.transaction.support.TransactionTemplate">
        <property name="transactionManager" ref="transactionManager"/>
        <property name="propagationBehaviorName" value="PROPAGATION_NOT_SUPPORTED"/>
    </bean>

    <!--传播级别为PROPAGATION_SUPPORTS的事务模板-->
    <bean id="supportsTransactionTemplate" class="org.springframework.transaction.support.TransactionTemplate">
        <property name="transactionManager" ref="transactionManager"/>
        <property name="propagationBehaviorName" value="PROPAGATION_SUPPORTS"/>
    </bean>
```

更新中...

# 六、spring事务在spring中的实现原理及源码分析

更新中...





















