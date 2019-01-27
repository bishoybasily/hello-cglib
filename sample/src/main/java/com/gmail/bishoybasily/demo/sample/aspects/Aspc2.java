package com.gmail.bishoybasily.demo.sample.aspects;

import com.gmail.bishoybasily.demo.annotations.After;
import com.gmail.bishoybasily.demo.annotations.Aspect;
import com.gmail.bishoybasily.demo.annotations.Before;

@Aspect
public class Aspc2 {

    @Before("public void com.gmail.bishoybasily.demo.sample.repos.RepositoryUsers.findSingle()")
    public void beforeFindingSingle() {
        System.out.println("Before findSingle");
    }

    @After("public void com.gmail.bishoybasily.demo.sample.repos.RepositoryUsers.findSingle()")
    public void afterFindingSingle() {
        System.out.println("After findSingle");
    }

}
