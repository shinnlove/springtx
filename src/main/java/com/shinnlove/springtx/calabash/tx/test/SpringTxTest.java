/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2018 All Rights Reserved.
 */
package com.shinnlove.springtx.calabash.tx.test;

/**
 * 事务测试接口。
 *
 * @author shinnlove.jinsheng
 * @version $Id: SpringTxTest.java, v 0.1 2018-07-13 下午6:12 shinnlove.jinsheng Exp $$
 */
public interface SpringTxTest {

    void before();

    String helloWorld();

    int mysqlConnectionTest();

    int simpleTxTest();

    void txRollbackTest();

    void txRollbackInnerTxRollbackPropagationRequires();

    void txRollbackInnerTxRollbackPropagationRequiresNew();

    void txRollbackInnerTxRollbackPropagationRequiresNew2();

    void txRollbackInnerTxRollbackPropagationRequiresNew3();

    void txRollbackInnerTxRollbackPropagationNested();

    void testNestRequiredTxRollback();

    void testNestRequiredTxRollbackMulti();

    void txRollbackInnerTxRollbackPropagationNested2();

    void txContextNestedRollbackPropagationNew();

    void txRollbackInnerTxRollbackPropagationMandatory();

    void txRollbackInnerTxRollbackPropagationMandatory2();

    void txRollbackInnerTxRollbackPropagationNever();

    void txRollbackInnerTxRollbackPropagationNever2();

    void txRollbackInnerTxRollbackPropagationNever3();

    void txRollbackInnerTxRollbackPropagationNotSupport();

    void txRollbackInnerTxRollbackPropagationNotSupport2();

    void txRollbackInnerTxRollbackPropagationSupports();

    void txRollbackInnerTxRollbackPropagationSupports2();

    void cleanUp();

}
