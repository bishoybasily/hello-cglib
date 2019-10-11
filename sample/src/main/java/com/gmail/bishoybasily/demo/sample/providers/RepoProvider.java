package com.gmail.bishoybasily.demo.sample.providers;

import com.gmail.bishoybasily.demo.annotations.Bean;
import com.gmail.bishoybasily.demo.annotations.Configuration;
import com.gmail.bishoybasily.demo.sample.repos.RepositoryUsers;

@Configuration
public class RepoProvider {

    @Bean
    public RepositoryUsers repositoryUsers() {
        return new RepositoryUsers();
    }

}
