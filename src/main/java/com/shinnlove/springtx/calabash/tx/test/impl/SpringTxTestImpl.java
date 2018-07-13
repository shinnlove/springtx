/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2018 All Rights Reserved.
 */
package com.shinnlove.springtx.calabash.tx.test.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import com.shinnlove.springtx.calabash.tx.test.SpringTxTest;

import javax.sql.DataSource;

/**
 * spring事务测试类。
 *
 * @author shinnlove.jinsheng
 * @version $Id: SpringTxTestImpl.java, v 0.1 2018-07-13 下午6:18 shinnlove.jinsheng Exp $$
 */
public class SpringTxTestImpl implements SpringTxTest {

    private JdbcTemplate        jdbcTemplate;
    private TransactionTemplate transactionTemplate;
    private TransactionTemplate requiresNewTransactionTemplate;
    private TransactionTemplate nestedTransactionTemplate;
    private TransactionTemplate mandatoryTransactionTemplate;
    private TransactionTemplate neverTransactionTemplate;
    private TransactionTemplate notSupportedTransactionTemplate;
    private TransactionTemplate supportsTransactionTemplate;

    @Override
    public void before() {
        jdbcTemplate.update("insert into user (name, password) values (?, ?)", "shinnlove",
            "123456");
    }

    @Override
    public String helloWorld() {
        return "Hello, world!";
    }

    @Override
    public int mysqlConnectionTest() {
        return countUser();
    }

    @Override
    public int simpleTxTest() {
        return (Integer) transactionTemplate.execute(new TransactionCallback<Object>() {
            @Override
            public Object doInTransaction(TransactionStatus transactionStatus) {
                return countUser();
            }
        });
    }

    /**
     * 使用默认事务模板，在事务中抛出错误，事务被打断并且回滚，数据库记录不会提交。
     */
    @Override
    public void txRollbackTest() {

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                jdbcTemplate.update("insert into user (name, password) values (?, ?)", "Shinnlove",
                    "123456");
                // 主动抛出错误打断事务
                throw new RuntimeException("Rollback!");
            }
        });
    }

    /**
     * 在事务级别为默认的PROPAGATION_REQUIRED情况下，
     * 内部事务使用status.setRollbackOnly()回滚，外部事务也会接收到UnexpectedRollbackException的错误回滚。
     */
    @Override
    public void txRollbackInnerTxRollbackPropagationRequires() {
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

                    }
                });

                transactionStatus.setRollbackOnly();
            }
        });
    }

    /**
     * 新建事务，如果当前存在事务，把当前事务挂起。
     */
    @Override
    public void txRollbackInnerTxRollbackPropagationRequiresNew() {
        // 外部开启一个事务
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {

                // 内部继续提交一个事务，策略：新建事务，如果当前存在事务，把当前事务挂起。
                requiresNewTransactionTemplate.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        jdbcTemplate.update("insert into user (name, password) values (?, ?)",
                            "Shinnlove", "123456");
                    }
                });

                // 外部事务发生回滚，内部事务应该不受影响还是能够提交
                throw new RuntimeException("主动让外部事务产生错误");
            }
        });
    }

    @Override
    public void txRollbackInnerTxRollbackPropagationRequiresNew2() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                jdbcTemplate.update("insert into user (name, password) values (?, ?)", "Shinnlove",
                    "123456");
                // 内部事务使用requires_new来提交
                requiresNewTransactionTemplate.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        jdbcTemplate.update("insert into user (name, password) values (?, ?)",
                            "Wanlukang", "345678");
                        // 内部事务发生回滚，但是外部事务不应该发生回滚
                        status.setRollbackOnly();
                    }
                });
            }
        });
    }

    @Override
    public void txRollbackInnerTxRollbackPropagationRequiresNew3() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                jdbcTemplate.update("insert into user (name, password) values (?, ?)", "Huang",
                    "1111112");

                requiresNewTransactionTemplate.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        jdbcTemplate.update("insert into user (name, password) values (?, ?)",
                            "Huang", "1111112");
                        // 内部事务抛出 RuntimeException，外部事务接收到异常，依旧会发生回滚
                        throw new RuntimeException();
                    }
                });
            }
        });
    }

    @Override
    public void txRollbackInnerTxRollbackPropagationNested() {
        nestedTransactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                jdbcTemplate.update("insert into user (name, password) values (?, ?)", "Shinnlove",
                    "123456");

                nestedTransactionTemplate.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        jdbcTemplate.update("insert into user (name, password) values (?, ?)",
                            "Huang", "1111112");
                        // 内部事务设置了 rollbackOnly，外部事务应该不受影响，可以继续提交
                        status.setRollbackOnly();
                    }
                });
            }
        });
    }

    @Override
    public void testNestRequiredTxRollback() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                // 先查询
                String sql = "select password from user where name='shinnlove' for update";
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
                Iterator ite = rows.iterator();
                while (ite.hasNext()) {
                    Map<String, Object> map = (Map<String, Object>) ite.next();
                    System.out.println("第一个线程读到的数据" + map);
                }

                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // 改表
                jdbcTemplate.update("update user set password = ? where name = ?", "modifypwd",
                    "shinnlove");
            }
        });
    }

    @Override
    public void testNestRequiredTxRollbackMulti() {
        try {
            // 睡睡更健康
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                // 再查询
                String sql = "select password from user where name='shinnlove' for update";
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
                Iterator ite = rows.iterator();
                while (ite.hasNext()) {
                    Map<String, Object> map = (Map<String, Object>) ite.next();
                    System.out.println("第二个线程读到的数据是：map=" + map);
                }
            }
        });
    }

    @Override
    public void txRollbackInnerTxRollbackPropagationNested2() {
        nestedTransactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                jdbcTemplate.update("insert into user (name, password) values (?, ?)", "Shinnlove",
                    "123456");

                nestedTransactionTemplate.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        jdbcTemplate.update("insert into user (name, password) values (?, ?)",
                            "Huang", "1111112");
                    }
                });
                // 外部事务设置了 rollbackOnly，内部事务应该也被回滚掉
                transactionStatus.setRollbackOnly();
            }
        });
    }

    @Override
    public void txContextNestedRollbackPropagationNew() {
        nestedTransactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                jdbcTemplate.update("insert into user (name, password) values (?, ?)", "Shinnlove",
                    "123456");

                requiresNewTransactionTemplate.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        jdbcTemplate.update("insert into user (name, password) values (?, ?)",
                            "Wanlukang", "345678");
                    }
                });

                // requiresNew免疫nested的回滚
                transactionStatus.setRollbackOnly();
            }
        });
    }

    @Override
    public void txRollbackInnerTxRollbackPropagationMandatory() {
        mandatoryTransactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                jdbcTemplate.update("insert into user (name, password) values (?, ?)", "Huang",
                    "1111112");
            }
        });
    }

    @Override
    public void txRollbackInnerTxRollbackPropagationMandatory2() {
        nestedTransactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                jdbcTemplate.update("insert into user (name, password) values (?, ?)", "Huang",
                    "1111112");

                mandatoryTransactionTemplate.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        jdbcTemplate.update("insert into user (name, password) values (?, ?)",
                            "Huang", "1111112");
                        // 内部事务回滚了，外部事务也跟着回滚
                        status.setRollbackOnly();
                    }
                });
            }
        });
    }

    @Override
    public void txRollbackInnerTxRollbackPropagationNever() {
        neverTransactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                jdbcTemplate.update("insert into user (name, password) values (?, ?)", "Huang",
                    "1111112");
            }
        });
    }

    @Override
    public void txRollbackInnerTxRollbackPropagationNever2() {
        // 模拟上下文中存在一个事务
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                jdbcTemplate.update("insert into user (name, password) values (?, ?)", "Huang",
                    "1111112");
                // 使用唯我独尊模板提交，马上抛错
                neverTransactionTemplate.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        jdbcTemplate.update("insert into user (name, password) values (?, ?)",
                            "Huang", "1111112");
                    }
                });
            }
        });
    }

    @Override
    public void txRollbackInnerTxRollbackPropagationNever3() {
        neverTransactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                jdbcTemplate.update("insert into user (name, password) values (?, ?)", "Huang",
                    "1111112");
                neverTransactionTemplate.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        jdbcTemplate.update("insert into user (name, password) values (?, ?)",
                            "Huang", "1111112");
                    }
                });
            }
        });
    }

    @Override
    public void txRollbackInnerTxRollbackPropagationNotSupport() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                jdbcTemplate.update("insert into user (name, password) values (?, ?)", "Huang",
                    "1111112");
                notSupportedTransactionTemplate.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        jdbcTemplate.update("insert into user (name, password) values (?, ?)",
                            "Huang", "1111112");
                    }
                });
                // 外部事务回滚，不会把内部的也连着回滚
                transactionStatus.setRollbackOnly();
            }
        });
    }

    @Override
    public void txRollbackInnerTxRollbackPropagationNotSupport2() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                jdbcTemplate.update("insert into user (name, password) values (?, ?)", "Huang",
                    "1111112");

                try {
                    notSupportedTransactionTemplate.execute(new TransactionCallbackWithoutResult() {
                        @Override
                        protected void doInTransactionWithoutResult(TransactionStatus status) {
                            jdbcTemplate.update("insert into user (name, password) values (?, ?)",
                                "Huang", "1111112");
                            throw new RuntimeException();
                        }
                    });
                } catch (RuntimeException e) {
                    // Do nothing.
                }
            }
        });
    }

    @Override
    public void txRollbackInnerTxRollbackPropagationSupports() {
        supportsTransactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                jdbcTemplate.update("insert into user (name, password) values (?, ?)", "Huang",
                    "1111112");
                throw new RuntimeException();
            }
        });
    }

    @Override
    public void txRollbackInnerTxRollbackPropagationSupports2() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                jdbcTemplate.update("insert into user (name, password) values (?, ?)", "Shinnlove",
                    "123456");
                supportsTransactionTemplate.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        jdbcTemplate.update("insert into user (name, password) values (?, ?)",
                            "Hurui", "234567");
                        status.setRollbackOnly();
                    }
                });
            }
        });
    }

    @Override
    public void cleanUp() {
        jdbcTemplate.update("delete from user");
    }

    private int countUser() {
        return jdbcTemplate.queryForObject("select count(*) from user", Integer.class);
    }

    // ~ Setters
    public void setDataSource(DataSource dataSource) {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    public void setRequiresNewTransactionTemplate(TransactionTemplate nestedTransactionTemplate) {
        this.requiresNewTransactionTemplate = nestedTransactionTemplate;
    }

    public void setNestedTransactionTemplate(TransactionTemplate nestedTransactionTemplate) {
        this.nestedTransactionTemplate = nestedTransactionTemplate;
    }

    public void setMandatoryTransactionTemplate(TransactionTemplate mandatoryTransactionTemplate) {
        this.mandatoryTransactionTemplate = mandatoryTransactionTemplate;
    }

    public void setNeverTransactionTemplate(TransactionTemplate neverTransactionTemplate) {
        this.neverTransactionTemplate = neverTransactionTemplate;
    }

    public void setNotSupportedTransactionTemplate(TransactionTemplate notSupportedTransactionTemplate) {
        this.notSupportedTransactionTemplate = notSupportedTransactionTemplate;
    }

    public void setSupportsTransactionTemplate(TransactionTemplate supportsTransactionTemplate) {
        this.supportsTransactionTemplate = supportsTransactionTemplate;
    }

}