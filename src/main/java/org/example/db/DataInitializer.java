package org.example.db;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.example.dao.api.InvoiceDao;
import org.example.dao.api.ServiceDao;
import org.example.dao.api.SubscriberDao;
import org.example.entity.Invoice;
import org.example.entity.Service;
import org.example.entity.Subscriber;

import java.time.LocalDate;
import java.util.List;

public class DataInitializer {

    public static void insertInitialData(
            SubscriberDao subscriberDao,
            ServiceDao serviceDao,
            InvoiceDao invoiceDao) {

        try {
            subscriberDao.runInTransaction(em -> {
                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaQuery<Subscriber> cqSub = cb.createQuery(Subscriber.class);
                Root<Subscriber> rootSub = cqSub.from(Subscriber.class);
                cqSub.select(rootSub);
                List<Subscriber> allSubscribers = em.createQuery(cqSub).getResultList();
                for (Subscriber sub : allSubscribers) {
                    em.remove(sub);
                }

                CriteriaQuery<Service> cqSrv = cb.createQuery(Service.class);
                Root<Service> rootSrv = cqSrv.from(Service.class);
                cqSrv.select(rootSrv);
                List<Service> allServices = em.createQuery(cqSrv).getResultList();
                for (Service srv : allServices) {
                    em.remove(srv);
                }

                em.flush();

                Subscriber sub1 = new Subscriber(
                        "Иван Иванов", "+375291234567", 150.50, false
                );
                Subscriber sub2 = new Subscriber(
                        "Петр Петров", "+375337654321", -50.00, true
                );
                em.persist(sub1);
                em.persist(sub2);

                Service serv1 = new Service("Интернет 50 Мбит/с", 450.00);
                Service serv2 = new Service("Мобильная связь", 300.00);
                Service serv3 = new Service("Антивирус", 100.00);
                em.persist(serv1);
                em.persist(serv2);
                em.persist(serv3);

                sub1.getServices().add(serv1);
                sub1.getServices().add(serv2);

                sub2.getServices().add(serv2);
                sub2.getServices().add(serv3);

                Invoice inv1 = new Invoice(
                        750.00, LocalDate.parse("2025-09-01"), true, sub1
                );
                Invoice inv2 = new Invoice(
                        400.00, LocalDate.parse("2025-09-05"), false, sub2
                );
                em.persist(inv1);
                em.persist(inv2);

            });

        } catch (Exception e) {
            e.printStackTrace();
            throw new org.example.exception.DataAccessException("Ошибка при инициализации данных", e);
        }
    }
}