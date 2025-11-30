package org.example.dao.api;

import org.example.entity.User;

public interface UserDao {
    User add(User user);
    User findByLogin(String login);
}