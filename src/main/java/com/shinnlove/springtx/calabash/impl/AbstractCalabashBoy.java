/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2018 All Rights Reserved.
 */
package com.shinnlove.springtx.calabash.impl;

import com.shinnlove.springtx.calabash.CalabashBoy;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 抽象的葫芦娃，提供法力计算的逻辑。
 *
 * @author shinnlove.jinsheng
 * @version $Id: AbstractCalabashBoy.java, v 0.1 2018-07-13 下午4:57 shinnlove.jinsheng Exp $$
 */
public abstract class AbstractCalabashBoy implements CalabashBoy, InitializingBean {

    /** JDBC 模板 */
    private JdbcTemplate jdbcTemplate;

    /** 数据库模板 */
    private DataSource   dataSource;

    /** 葫芦娃名字 */
    protected String     name;

    /** 释放技能需要的法力值 */
    protected int        manaConsume;

    @Override
    public void skill() {
        if (getManaConsume() > getMana()) {
            throw new RuntimeException("法力不够，求给力啊！");
        }

        // 从数据库中先扣减法力值
        jdbcTemplate.update("update calabash_boy set mana = ? where name = ?", getMana()
                                                                               - getManaConsume(),
            getName());

        // 而后释放技能
        doSkill();
    }

    @Override
    public int getMana() {
        return jdbcTemplate.queryForObject("select mana from calabash_boy where name = ?",
            Integer.class, getName());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * 具体的技能，抽象方法，留给各个葫芦娃去实现。
     */
    public abstract void doSkill();

    /**
     * Getter method for property name.
     *
     * @return property value of name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Setter method for property name.
     *
     * @param name value to be assigned to property name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Getter method for property manaConsume.
     *
     * @return property value of manaConsume
     */
    public int getManaConsume() {
        return manaConsume;
    }

    /**
     * Setter method for property manaConsume.
     *
     * @param manaConsume value to be assigned to property manaConsume
     */
    public void setManaConsume(int manaConsume) {
        this.manaConsume = manaConsume;
    }

    /**
     * Setter method for property dataSource.
     *
     * @param dataSource value to be assigned to property dataSource
     */
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

}