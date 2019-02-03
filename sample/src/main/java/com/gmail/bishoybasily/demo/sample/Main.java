package com.gmail.bishoybasily.demo.sample;


import com.gmail.bishoybasily.demo.generated.Graph;
import com.gmail.bishoybasily.demo.sample.repos.RepositoryUsers;

public class Main {

    public static void main(String[] args) {
        RepositoryUsers repositoryUsers = Graph.getInstance().repositoryUsers();

        repositoryUsers.findAll();
    }

}
