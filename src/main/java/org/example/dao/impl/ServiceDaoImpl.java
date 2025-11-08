package org.example.dao.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import org.example.dao.api.ServiceDao;
import org.example.db.JpaManager;
import org.example.entity.Service;
import org.example.entity.Subscriber;
import org.example.exception.DataAccessException;
import org.example.exception.DuplicateEntryException;
import org.example.exception.EntryNotFoundException;

import java.util.List;
import java.util.function.Function;

public class ServiceDaoImpl implements ServiceDao {

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
            } else if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        } finally {
            em.close();
        }
    }

    @Override
    public List<Service> findAll() {
        EntityManager em = JpaManager.getEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Service> cq = cb.createQuery(Service.class);
            Root<Service> root = cq.from(Service.class);
            cq.select(root);
            return new java.util.ArrayList<>(em.createQuery(cq).getResultList());
        } catch (Exception e) {
            throw new DataAccessException("Ошибка при получении списка услуг", e);
        } finally {
            em.close();
        }
    }

    @Override
    public List<Service> findBySubscriberId(int subscriberId) {
        EntityManager em = JpaManager.getEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Service> cq = cb.createQuery(Service.class);
            Root<Service> root = cq.from(Service.class);

            Join<Service, Subscriber> subscribersJoin = root.join("subscribers");
            cq.where(cb.equal(subscribersJoin.get("id"), subscriberId));

            return new java.util.ArrayList<>(em.createQuery(cq).getResultList());

        } catch (Exception e) {
            throw new DataAccessException("Ошибка при поиске услуг абонента.", e);
        } finally {
            em.close();
        }
    }

    @Override
    public Service add(Service service) {
        return executeInTransaction(em -> {
            try {
                em.persist(service);
                return service;
            } catch (PersistenceException e) {
                throw new DataAccessException("Ошибка при добавлении услуги.", e);
            }
        });
    }

    @Override
    public void linkServiceToSubscriber(int subscriberId, int serviceId) {
        executeInTransaction(em -> {
            try {
                Subscriber subscriber = em.find(Subscriber.class, subscriberId);
                Service service = em.find(Service.class, serviceId);

                if (subscriber == null) {
                    throw new EntryNotFoundException("Абонент с ID " + subscriberId + " не найден.");
                }
                if (service == null) {
                    throw new EntryNotFoundException("Услуга с ID " + serviceId + " не найдена.");
                }

                if (subscriber.getServices().contains(service)) {
                    throw new DuplicateEntryException("Эта услуга уже подключена абоненту.");
                }

                subscriber.getServices().add(service);
                em.merge(subscriber);

                return null;

            } catch (PersistenceException e) {
                throw new DataAccessException("Ошибка при подключении услуги.", e);
            }
        });
    }

    @Override
    public void deleteAll() {
        executeInTransaction(em -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Service> cq = cb.createQuery(Service.class);
            Root<Service> root = cq.from(Service.class);
            cq.select(root);
            List<Service> allServices = em.createQuery(cq).getResultList();

            for (Service service : allServices) {
                em.remove(service);
            }
            return null;
        });
    }
}