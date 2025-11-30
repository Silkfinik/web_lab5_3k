package org.example.dao.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceException;
import org.example.dao.api.UserDao;
import org.example.db.JpaManager;
import org.example.entity.User;
import org.example.exception.DataAccessException;
import org.example.exception.DuplicateEntryException;

import java.util.function.Function;

public class UserDaoImpl implements UserDao {

    private <T> T executeInTransaction(Function<EntityManager, T> block) {
        EntityManager em = JpaManager.getEntityManager();
        try {
            em.getTransaction().begin();
            T result = block.apply(em);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            if (e instanceof PersistenceException) {
                throw new DataAccessException("Ошибка доступа к данным JPA.", e);
            }
            throw new RuntimeException(e);
        } finally {
            em.close();
        }
    }

    @Override
    public User add(User user) {
        return executeInTransaction(em -> {
            try {
                em.persist(user);
                em.flush();
                return user;
            } catch (PersistenceException e) {
                Throwable cause = e.getCause();
                if (cause != null && cause.getMessage() != null && cause.getMessage().contains("Duplicate entry")) {
                    throw new DuplicateEntryException("Пользователь с таким логином уже существует.", e);
                }
                throw e;
            }
        });
    }

    @Override
    public User findByLogin(String login) {
        EntityManager em = JpaManager.getEntityManager();
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.login = :login", User.class)
                    .setParameter("login", login)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } catch (Exception e) {
            throw new DataAccessException("Ошибка поиска пользователя.", e);
        } finally {
            em.close();
        }
    }
}