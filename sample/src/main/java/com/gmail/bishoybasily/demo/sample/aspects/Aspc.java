package com.gmail.bishoybasily.demo.sample.aspects;

import com.gmail.bishoybasily.demo.annotations.Aspect;
import com.gmail.bishoybasily.demo.annotations.Before;

@Aspect
public class Aspc {

    @Before("com.gmail.bishoybasily.demo.sample.repos.RepositoryUsers.findAll")
    public void beforeFindingAllUsers() {

    }

}
