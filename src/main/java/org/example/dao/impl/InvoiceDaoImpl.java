package org.example.dao.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;
import org.example.dao.api.InvoiceDao;
import org.example.db.JpaManager;
import org.example.entity.Invoice;
import org.example.exception.DataAccessException;
import org.example.exception.EntryNotFoundException;

import java.util.List;
import java.util.function.Function;

public class InvoiceDaoImpl implements InvoiceDao {

    // Helper-метод для управления транзакциями
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
    public List<Invoice> findBySubscriberId(int subscriberId) {
        EntityManager em = JpaManager.getEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Invoice> cq = cb.createQuery(Invoice.class);
            Root<Invoice> root = cq.from(Invoice.class);

            // Использование строк "subscriber" и "id"
            cq.where(cb.equal(root.get("subscriber").get("id"), subscriberId));

            return new java.util.ArrayList<>(em.createQuery(cq).getResultList());

        } catch (Exception e) {
            throw new DataAccessException("Ошибка при поиске счетов абонента.", e);
        } finally {
            em.close();
        }
    }

    @Override
    public boolean pay(int invoiceId) {
        return executeInTransaction(em -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaUpdate<Invoice> cu = cb.createCriteriaUpdate(Invoice.class);
            Root<Invoice> root = cu.from(Invoice.class);

            // Использование строк "isPaid" и "id"
            cu.set(root.get("isPaid"), true);
            cu.where(cb.equal(root.get("id"), invoiceId));

            int rowsAffected = em.createQuery(cu).executeUpdate();

            if (rowsAffected == 0) {
                throw new EntryNotFoundException("Счет с ID " + invoiceId + " не найден.");
            }
            return true;
        });
    }

    @Override
    public Integer findSubscriberIdByInvoiceId(int invoiceId) {
        EntityManager em = JpaManager.getEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            // Запрос возвращает Integer (ID)
            CriteriaQuery<Integer> cq = cb.createQuery(Integer.class);
            Root<Invoice> root = cq.from(Invoice.class);

            // SELECT i.subscriber.id FROM Invoice i WHERE i.id = :invoiceId
            cq.select(root.get("subscriber").get("id"));
            cq.where(cb.equal(root.get("id"), invoiceId));

            return em.createQuery(cq).getSingleResult();

        } catch (jakarta.persistence.NoResultException e) {
            throw new EntryNotFoundException("Счет с ID " + invoiceId + " не найден.", e);
        } catch (Exception e) {
            throw new DataAccessException("Ошибка при поиске счета.", e);
        } finally {
            em.close();
        }
    }

    @Override
    public List<Invoice> findUnpaid() {
        EntityManager em = JpaManager.getEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Invoice> cq = cb.createQuery(Invoice.class);
            Root<Invoice> root = cq.from(Invoice.class);

            // Использование строки "isPaid"
            cq.where(cb.equal(root.get("isPaid"), false));

            return new java.util.ArrayList<>(em.createQuery(cq).getResultList());

        } catch (Exception e) {
            throw new DataAccessException("Ошибка при получении списка неоплаченных счетов.", e);
        } finally {
            em.close();
        }
    }

    @Override
    public Invoice add(Invoice invoice) {
        return executeInTransaction(em -> {
            try {
                // Если subscriber не управляется (detached), его нужно смержить
                if (invoice.getSubscriber() != null && !em.contains(invoice.getSubscriber())) {
                    invoice.setSubscriber(em.merge(invoice.getSubscriber()));
                }
                em.persist(invoice);
                return invoice;
            } catch (PersistenceException e) {
                throw new DataAccessException("Ошибка при добавлении счета.", e);
            }
        });
    }
}