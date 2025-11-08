package org.example.dao.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;
import org.example.dao.api.SubscriberDao;
import org.example.db.JpaManager;
import org.example.entity.Subscriber;
import org.example.exception.DataAccessException;
import org.example.exception.DuplicateEntryException;
import org.example.exception.EntryNotFoundException;

import java.util.List;
import java.util.function.Function;
import java.util.function.Consumer;


public class SubscriberDaoImpl implements SubscriberDao {

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

            Exception exceptionToThrow;
            if (e instanceof PersistenceException) {
                exceptionToThrow = new DataAccessException("Ошибка доступа к данным JPA.", e);
            } else {
                exceptionToThrow = e;
            }

            if (exceptionToThrow instanceof RuntimeException) {
                throw (RuntimeException) exceptionToThrow;
            } else {
                throw new RuntimeException(exceptionToThrow);
            }

        } finally {
            em.close();
        }
    }

    private void executeInTransaction(Consumer<EntityManager> block) {
        EntityManager em = JpaManager.getEntityManager();
        try {
            em.getTransaction().begin();
            block.accept(em);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        } finally {
            em.close();
        }
    }

    @Override
    public Subscriber findById(int id) {
        EntityManager em = JpaManager.getEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Subscriber> cq = cb.createQuery(Subscriber.class);
            Root<Subscriber> root = cq.from(Subscriber.class);

            cq.where(cb.equal(root.get("id"), id));

            return em.createQuery(cq).getSingleResult();

        } catch (jakarta.persistence.NoResultException e) {
            return null;
        } catch (Exception e) {
            throw new DataAccessException("Ошибка при поиске абонента.", e);
        } finally {
            em.close();
        }
    }

    @Override
    public List<Subscriber> findAll() {
        EntityManager em = JpaManager.getEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Subscriber> cq = cb.createQuery(Subscriber.class);
            Root<Subscriber> root = cq.from(Subscriber.class);
            cq.select(root);
            return new java.util.ArrayList<>(em.createQuery(cq).getResultList());
        } catch (Exception e) {
            throw new DataAccessException("Ошибка при получении списка абонентов.", e);
        } finally {
            em.close();
        }
    }

    @Override
    public void block(int subscriberId) {
        executeInTransaction(em -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaUpdate<Subscriber> cu = cb.createCriteriaUpdate(Subscriber.class);
            Root<Subscriber> root = cu.from(Subscriber.class);

            cu.set(root.get("isBlocked"), true);
            cu.where(cb.equal(root.get("id"), subscriberId));

            int rowsAffected = em.createQuery(cu).executeUpdate();

            if (rowsAffected == 0) {
                throw new EntryNotFoundException("Абонент с ID " + subscriberId + " не найден.");
            }
            return null;
        });
    }

    @Override
    public Subscriber add(Subscriber subscriber) {
        return executeInTransaction(em -> {
            try {
                em.persist(subscriber);
                return subscriber;
            } catch (PersistenceException e) {
                if (e.getMessage() != null && e.getMessage().contains("UNIQUE_")) {
                    throw new DuplicateEntryException("Абонент с номером " + subscriber.getPhoneNumber() + " уже существует.", e);
                }
                throw new DataAccessException("Ошибка при добавлении абонента.", e);
            }
        });
    }

    @Override
    public void deleteAll() {
        executeInTransaction(em -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Subscriber> cq = cb.createQuery(Subscriber.class);
            Root<Subscriber> root = cq.from(Subscriber.class);
            cq.select(root);
            List<Subscriber> allSubscribers = em.createQuery(cq).getResultList();

            for (Subscriber subscriber : allSubscribers) {
                em.remove(subscriber);
            }
            return null;
        });
    }

    @Override
    public void runInTransaction(Consumer<EntityManager> block) {
        this.executeInTransaction(block);
    }
}