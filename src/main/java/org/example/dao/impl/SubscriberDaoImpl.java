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

public class SubscriberDaoImpl implements SubscriberDao {

    // Helper-метод для управления транзакциями (трансляция из Kotlin)
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
            // System.err.println("JPA transaction failed: " + e.getMessage());

            // Преобразование исключений JPA
            Exception exceptionToThrow;
            if (e instanceof PersistenceException) {
                exceptionToThrow = new DataAccessException("Ошибка доступа к данным JPA.", e);
            } else {
                exceptionToThrow = e;
            }

            // Мы должны выбросить RuntimeException, т.к. Function.apply не позволяет
            // выбрасывать проверяемые исключения, но наши кастомные исключения
            // и так являются RuntimeException.
            if (exceptionToThrow instanceof RuntimeException) {
                throw (RuntimeException) exceptionToThrow;
            } else {
                throw new RuntimeException(exceptionToThrow);
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

            // Использование строки "id" вместо Subscriber_.id
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

            // Использование строк "isBlocked" и "id"
            cu.set(root.get("isBlocked"), true);
            cu.where(cb.equal(root.get("id"), subscriberId));

            int rowsAffected = em.createQuery(cu).executeUpdate();

            if (rowsAffected == 0) {
                throw new EntryNotFoundException("Абонент с ID " + subscriberId + " не найден.");
            }
            return null; // т.к. executeInTransaction ожидает возврат
        });
    }

    @Override
    public Subscriber add(Subscriber subscriber) {
        return executeInTransaction(em -> {
            try {
                em.persist(subscriber);
                return subscriber;
            } catch (PersistenceException e) {
                // Проверка на нарушение UNIQUE constraint
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
            // JPA не поддерживает CriteriaDelete с каскадом,
            // поэтому находим и удаляем вручную, как в Lab 3
            List<Subscriber> allSubscribers = findAll(); // Используем существующий метод
            for (Subscriber subscriber : allSubscribers) {
                em.remove(em.contains(subscriber) ? subscriber : em.merge(subscriber));
            }
            return null;
        });
    }
}