/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2018 All Rights Reserved.
 */
package com.shinnlove.springtx.calabash.tx;

import org.springframework.transaction.annotation.Transactional;

import com.shinnlove.springtx.calabash.CalabashBoy;

/**
 * 使用注解配置事务。
 *
 * @author shinnlove.jinsheng
 * @version $Id: TxCalabashBoy.java, v 0.1 2018-07-13 下午5:10 shinnlove.jinsheng Exp $$
 */
public class TxCalabashBoy implements CalabashBoy {

    /** 火娃 */
    private CalabashBoy fireCalabashBoy;

    /** 水娃 */
    private CalabashBoy waterCalabashBoy;

    @Override
    public String getName() {
        return "Super Calabash";
    }

    @Transactional
    @Override
    public void skill() {
        fireCalabashBoy.skill();
        waterCalabashBoy.skill();
        System.out.println("冰火双重天！");
    }

    @Override
    public int getMana() {
        return 0;
    }

    /**
     * Setter method for property fireCalabashBoy.
     *
     * @param fireCalabashBoy value to be assigned to property fireCalabashBoy
     */
    public void setFireCalabashBoy(CalabashBoy fireCalabashBoy) {
        this.fireCalabashBoy = fireCalabashBoy;
    }

    /**
     * Setter method for property waterCalabashBoy.
     *
     * @param waterCalabashBoy value to be assigned to property waterCalabashBoy
     */
    public void setWaterCalabashBoy(CalabashBoy waterCalabashBoy) {
        this.waterCalabashBoy = waterCalabashBoy;
    }

}