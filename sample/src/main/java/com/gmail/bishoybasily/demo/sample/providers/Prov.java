package com.gmail.bishoybasily.demo.sample.providers;

import com.gmail.bishoybasily.demo.annotations.Bean;
import com.gmail.bishoybasily.demo.annotations.Provider;
import com.gmail.bishoybasily.demo.sample.repos.RepositoryUsers;

@Provider
public class Prov {

    @Bean
    public RepositoryUsers repositoryUsers() {
        return new RepositoryUsers();
    }

}
