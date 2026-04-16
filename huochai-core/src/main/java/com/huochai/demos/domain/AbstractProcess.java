package com.huochai.demos.domain;

public abstract class AbstractProcess {

    // 模板方法（核心流程，不允许子类改顺序）
    public final void execute() {
        step1();
        step2();
        hook();
        step3();
    }

    protected void step1() {
        System.out.println("父类 step1");
    }

    protected abstract void step2(); // 子类必须实现

    protected void hook() {
        // 钩子方法（可选重写）
    }

    protected abstract void step3(); // 子类必须实现
}