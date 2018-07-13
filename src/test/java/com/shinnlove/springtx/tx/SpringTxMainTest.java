/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2018 All Rights Reserved.
 */
package com.shinnlove.springtx.tx;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.UnexpectedRollbackException;

import com.shinnlove.springtx.calabash.tx.test.SpringTxTest;

/**
 * @author shinnlove.jinsheng
 * @version $Id: SpringTxMainTest.java, v 0.1 2018-07-13 下午6:16 shinnlove.jinsheng Exp $$
 */
@RunWith(JUnit4.class)
public class SpringTxMainTest {

    /** spring事务测试类 */
    private SpringTxTest springTxTest;

    @Before
    public void startUp() {
        // 从src/resource下加载配置文件
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
            "applicationContext.xml");
        springTxTest = applicationContext.getBean("springTxTest", SpringTxTest.class);
        // 先插入一条测试数据
        springTxTest.before();
    }

    @After
    public void deleteInsertion() {
        // 最后删除插入的所有数据
        springTxTest.cleanUp();
    }

    @Test
    public void testDummy() {
        Assert.assertEquals("Hello, world!", springTxTest.helloWorld());
    }

    @Test
    public void testMySqlConnection() {
        Assert.assertEquals(1, springTxTest.mysqlConnectionTest());
    }

    @Test
    public void testSimpleTx() {
        Assert.assertEquals(1, springTxTest.simpleTxTest());
    }

    /**
     * PROPAGATION_REQUIRED：支持当前事务，如果当前没有事务，就新建一个事务(默认级别)。
     * 场景：在一个事务中抛错。
     * 效果：事务中抛错事务回滚。
     * 除非配置对哪些错误免疫回滚，事务才不会回滚。
     */
    @Test
    public void 测试在事务中主动抛出错误_事务回滚_提交的数据不在() {
        try {
            springTxTest.txRollbackTest();
        } catch (Exception e) {
            // Do nothing at all.
            e.printStackTrace();
        } finally {
            // 验证数据库只剩下一条记录
            Assert.assertEquals(1, springTxTest.mysqlConnectionTest());
        }
    }

    /**
     * PROPAGATION_REQUIRED：支持当前事务，如果当前没有事务，就新建一个事务(默认级别)。
     * 场景：外部事务已经开启，内部事务使用PROPAGATION_REQUIRED的模板去提交新事务。
     * 效果：当内部事务设置了 {@link org.springframework.transaction.TransactionStatus#setRollbackOnly()} 来触发回滚，
     * 外部事务接受到了一个 {@link UnexpectedRollbackException} 也被回滚。
     * PROPAGATION_REQUIRED事务模板开启的事务，rollback会传播到外层事务中、接收到内部事务抛出的rollback也会回滚。
     * 除非配置对内部回滚引起的错误不回滚，则外部事务不受影响。
     */
    @Test
    public void testTxRollbackInnerTxRollbackPropagationRequires_内外事务提交的数据都不在() {
        try {
            springTxTest.txRollbackInnerTxRollbackPropagationRequires();
        } catch (UnexpectedRollbackException e) {
            // Do nothing at all.
            e.printStackTrace();
        } finally {
            // 验证数据库只剩下一条记录
            Assert.assertEquals(1, springTxTest.mysqlConnectionTest());
        }
    }

    /**
     * 解读：PROPAGATION_REQUIRES_NEW因为开启了一个新的事务，而且在内部，因此上下文中的rollback它不会感知。
     * PROPAGATION_REQUIRES_NEW：新建事务，如果当前存在事务，把当前事务挂起。
     * 场景：外部事务已经开启，内部事务使用PROPAGATION_REQUIRES_NEW的模板去提交新事务。
     * 效果：PROPAGATION_REQUIRES_NEW事务模板开启的事务，不受外部事务rollback的影响，照常提交。
     */
    @Test
    public void testTxRollbackInnerTxRollbackPropagationRequiresNew() {
        try {
            springTxTest.txRollbackInnerTxRollbackPropagationRequiresNew();
        } catch (Exception e) {
            // Do nothing at all.
        } finally {
            // 验证数据库有两条记录，一条为内部事务提交的数据。
            Assert.assertEquals(2, springTxTest.mysqlConnectionTest());
        }
    }

    /**
     * 解读：PROPAGATION_REQUIRES_NEW产生的rollback异常不会渗透出去。
     * PROPAGATION_REQUIRES_NEW：新建事务，如果当前存在事务，把当前事务挂起。
     * 场景：外部事务已经开启，内部事务使用PROPAGATION_REQUIRES_NEW的模板去提交新事务，
     * 而后通过设置 {@link org.springframework.transaction.TransactionStatus#setRollbackOnly()} 来触发回滚，
     * 外部事务依旧可以不受影响，正常提交。
     * 效果：PROPAGATION_REQUIRES_NEW事务模板的rollback不会影响到原来已有的事务，反正是新的事务。
     */
    @Test
    public void testTxRollbackInnerTxRollbackPropagationRequiresNew2() {
        springTxTest.txRollbackInnerTxRollbackPropagationRequiresNew2();
        Assert.assertEquals(2, springTxTest.mysqlConnectionTest());
    }

    /**
     * 解读：PROPAGATION_REQUIRES_NEW产生的非rollback的Exception会渗透出去。
     * PROPAGATION_REQUIRES_NEW：新建事务，如果当前存在事务，把当前事务挂起。
     * 场景：外部事务已经开启，内部事务使用PROPAGATION_REQUIRES_NEW的模板去提交新事务，
     * 而后内部事务抛出了 {@link RuntimeException} 异常发生了回滚，外部事务接收到这个异常也会发生回滚。
     * 效果：PROPAGATION_REQUIRES_NEW事务模板抛出的错误会渗透到外部事务中，因为出现错误，因此两个事务都回滚。
     */
    @Test
    public void testTxRollbackInnerTxRollbackPropagationRequiresNew3() {
        try {
            springTxTest.txRollbackInnerTxRollbackPropagationRequiresNew3();
        } catch (RuntimeException e) {
            // Do nothing at all.
        } finally {
            Assert.assertEquals(1, springTxTest.mysqlConnectionTest());
        }

    }

    /**
     * PROPAGATION_NESTED：如果当前存在事务，则在嵌套事务内执行。如果当前没有事务，则进行与PROPAGATION_REQUIRED类似的操作。
     * 场景：外部事务已经开启，内部事务使用PROPAGATION_NESTED的模板去提交新事务，
     * 而后内部事务通过设置 {@link org.springframework.transaction.TransactionStatus#setRollbackOnly()} 来触发回滚，
     * 外部事务依旧可以不受影响，正常提交。
     * 效果：PROPAGATION_NESTED事务模板的rollback不会影响到原来已有的事务。
     */
    @Test
    public void testTxRollbackInnerTxRollbackPropagationNested() {
        springTxTest.txRollbackInnerTxRollbackPropagationNested();
        Assert.assertEquals(2, springTxTest.mysqlConnectionTest());
    }

    /**
     * 测试mysql select for update查询更新锁。
     */
    @Test
    public void testNestRequiredTxRollback() {
        ExecutorService es = Executors.newFixedThreadPool(2);

        es.execute(new Runnable() {
            @Override
            public void run() {
                springTxTest.testNestRequiredTxRollback();
            }
        });

        es.execute(new Runnable() {
            @Override
            public void run() {
                springTxTest.testNestRequiredTxRollbackMulti();
            }
        });

        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     * PROPAGATION_NESTED：外部事务通过设置 {@link org.springframework.transaction.TransactionStatus#setRollbackOnly()} 来触发回滚，
     * 由于savepoint 在外部事务的开头，所以内部事务应该也会被一起回滚掉
     * 效果：PROPAGATION_NESTED事务模板提交的事务是有savepoint的，因此内部的事务会一并被回滚。
     */
    @Test
    public void testTxRollbackInnerTxRollbackPropagationNested2() {
        springTxTest.txRollbackInnerTxRollbackPropagationNested2();
        Assert.assertEquals(1, springTxTest.mysqlConnectionTest());
    }

    /**
     * PROPAGATION_NESTED：虽然在事务开启的时候有savepoint，但是因为requiresNew使用的是新事物提交，
     * 因此上下文中的事务回滚，不会影响内部事务的提交。
     */
    @Test
    public void testTxContextNestedRollbackPropagationNew() {
        springTxTest.txContextNestedRollbackPropagationNew();
        Assert.assertEquals(2, springTxTest.mysqlConnectionTest());
    }

    /**
     * PROPAGATION_MANDATORY：强制性的事务，当前的事务上下文中不存在事务的话，会抛出 {@link IllegalTransactionStateException}
     * 效果：PROPAGATION_MANDATORY事务模板提交事务必须要求当前上下文中已经存在一个事务。
     */
    @Test(expected = IllegalTransactionStateException.class)
    public void testTxRollbackInnerTxRollbackPropagationMandatory() {
        springTxTest.txRollbackInnerTxRollbackPropagationMandatory();
    }

    /**
     * PROPAGATION_MANDATORY：强制性的事务，内部的事务发生回滚，那么外围的事务也会发生回滚，
     * 表现：和 {@link org.springframework.transaction.TransactionDefinition#PROPAGATION_REQUIRED} 一样，本质：也会抛出 {@link UnexpectedRollbackException}
     * 效果：PROPAGATION_MANDATORY事务模板提交事务如果回滚，也会抛出 {@link UnexpectedRollbackException}
     */
    @Test(expected = UnexpectedRollbackException.class)
    public void testTxRollbackInnerTxRollbackPropagationMandatory2() {
        springTxTest.txRollbackInnerTxRollbackPropagationMandatory2();
    }

    /**
     * PROPAGATION_NEVER：不允许当前事务上下文中存在事务，如果没有，就正常执行
     * 效果：使用PROPAGATION_NEVER事务模板提交事务必须要求上下文中没有任何事务，只允许我自己提交事务。
     */
    @Test
    public void testTxRollbackInnerTxRollbackPropagationNever() {
        springTxTest.txRollbackInnerTxRollbackPropagationNever();
        Assert.assertEquals(2, springTxTest.mysqlConnectionTest());
    }

    /**
     * PROPAGATION_NEVER：不允许当前事务上下文中存在事务，如果有，则抛出 {@link IllegalTransactionStateException}
     * 抛出错误上下文的事务肯定也会回滚。
     */
    @Test(expected = IllegalTransactionStateException.class)
    public void testTxRollbackInnerTxRollbackPropagationNever2() {
        springTxTest.txRollbackInnerTxRollbackPropagationNever2();
    }

    /**
     * PROPAGATION_NEVER：不允许当前事务上下文中存在事务，唯我独尊模板可以嵌套使用。
     * 特别注意：当两个 NEVER 的嵌套在一起的时候，应该也是能够执行成功的。
     */
    @Test
    public void testTxRollbackInnerTxRollbackPropagationNever3() {
        springTxTest.txRollbackInnerTxRollbackPropagationNever3();
        Assert.assertEquals(3, springTxTest.mysqlConnectionTest());
    }

    /**
     * PROPAGATION_NOT_SUPPORTED：不支持事务，外围的事务回滚不会导致它包含的内容回滚。
     * 隔离出一块区域，自己做自己的，外围回滚跟它没关系。
     * 效果：使用PROPAGATION_NOT_SUPPORTED事务模板提交事务，上下文中的事务rollback它不会回滚。
     */
    @Test
    public void testTxRollbackInnerTxRollbackPropagationNotSupport() {
        springTxTest.txRollbackInnerTxRollbackPropagationNotSupport();
        Assert.assertEquals(2, springTxTest.mysqlConnectionTest());
    }

    /**
     * PROPAGATION_NOT_SUPPORTED：不支持事务，内部发生异常，外部捕获，都不会发生回滚
     * 效果：使用PROPAGATION_NOT_SUPPORTED事务模板提交事务，内部异常不会回滚自己；如果外部捕获这个错误，外部事务可以不回滚；如果外部抛错，同事务抛错回滚论。
     */
    @Test
    public void testTxRollbackInnerTxRollbackPropagationNotSupport2() {
        try {
            springTxTest.txRollbackInnerTxRollbackPropagationNotSupport2();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertEquals(3, springTxTest.mysqlConnectionTest());
    }

    /**
     * PROPAGATION_SUPPORTS：如果当前事务上下文中没有事务，那么就按照没有事务的方式执行代码。
     * 效果：使用PROPAGATION_SUPPORTS事务模板提交事务，如果上下文中没有事务，
     * 对自己：按照没有事务方式执行，就算内部异常不会回滚自己这个事务。
     * 对上下文：
     */
    @Test
    public void testTxRollbackInnerTxRollbackPropagationSupports() {
        try {
            springTxTest.txRollbackInnerTxRollbackPropagationSupports();
        } catch (RuntimeException e) {
            // Do nothing
            e.printStackTrace();
        } finally {
            Assert.assertEquals(2, springTxTest.mysqlConnectionTest());
        }
    }

    /**
     * PROPAGATION_SUPPORTS：如果当前事务上下文中存在事务，那么合并到当前上下文的事务中去。
     * 表现地和 {@link org.springframework.transaction.TransactionDefinition#PROPAGATION_REQUIRED} 一样
     * 效果：使用PROPAGATION_SUPPORTS事务模板提交事务，如果上下文中有事务，它抛出的回滚会影响到上下文的事务。
     * 对自己：因为自己被合并到上下文中，因此抛错肯定会回滚。
     * 对上下文：上下文接受rollback的影响，接收内部抛错。
     */
    @Test(expected = UnexpectedRollbackException.class)
    public void testTxRollbackInnerTxRollbackPropagationSupports2() {
        springTxTest.txRollbackInnerTxRollbackPropagationSupports2();
    }

}