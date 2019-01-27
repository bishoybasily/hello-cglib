package com.gmail.bishoybasily.demo.sample.aspects;

import com.gmail.bishoybasily.demo.annotations.After;
import com.gmail.bishoybasily.demo.annotations.Aspect;
import com.gmail.bishoybasily.demo.annotations.Before;

@Aspect
public class AspctFindAll {

    @Before("public void com.gmail.bishoybasily.demo.sample.repos.RepositoryUsers.findAll()")
    public void beforeFindingAll() {
        System.out.println("Before findAll");
    }

    @After("public void com.gmail.bishoybasily.demo.sample.repos.RepositoryUsers.findAll()")
    public void afterFindingAll() {
        System.out.println("After findAll");
    }

}
