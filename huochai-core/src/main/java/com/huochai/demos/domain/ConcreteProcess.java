package com.huochai.demos.domain;

import lombok.Data;

/**
 *
 *@author peilizhi 
 *@date 2026/3/30 11:03
 **/
@Data
public class ConcreteProcess extends AbstractProcess {


    protected void step1() {
        System.out.println("子类 step1");
    }


    @Override
    protected void step2() {
        System.out.println("子类 step2");
    }

    @Override
    protected void step3() {
        System.out.println("子类 step3");
    }

    @Override
    protected void hook() {
        System.out.println("子类 hook");
    }


    public static void main(String[] args) {
        AbstractProcess process = new ConcreteProcess();
        process.execute();
    }
}
