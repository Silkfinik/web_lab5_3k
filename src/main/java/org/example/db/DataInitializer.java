package org.example.db;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.example.dao.api.InvoiceDao;
import org.example.dao.api.ServiceDao;
import org.example.dao.api.SubscriberDao;
import org.example.dao.api.UserDao;
import org.example.entity.*;

import java.time.LocalDate;
import java.util.List;

public class DataInitializer {

    public static void insertInitialData(
            SubscriberDao subscriberDao,
            ServiceDao serviceDao,
            InvoiceDao invoiceDao,
            UserDao userDao) { // Добавили UserDao

        try {
            subscriberDao.runInTransaction(em -> {
                // Очистка таблиц
                CriteriaBuilder cb = em.getCriteriaBuilder();

                // Удаляем Invoice
                CriteriaQuery<Invoice> cqInv = cb.createQuery(Invoice.class);
                cqInv.select(cqInv.from(Invoice.class));
                for (Invoice i : em.createQuery(cqInv).getResultList()) em.remove(i);

                // Удаляем Subscriber
                CriteriaQuery<Subscriber> cqSub = cb.createQuery(Subscriber.class);
                cqSub.select(cqSub.from(Subscriber.class));
                for (Subscriber s : em.createQuery(cqSub).getResultList()) em.remove(s);

                // Удаляем Service
                CriteriaQuery<Service> cqSrv = cb.createQuery(Service.class);
                cqSrv.select(cqSrv.from(Service.class));
                for (Service s : em.createQuery(cqSrv).getResultList()) em.remove(s);

                // Удаляем User
                CriteriaQuery<User> cqUser = cb.createQuery(User.class);
                cqUser.select(cqUser.from(User.class));
                for (User u : em.createQuery(cqUser).getResultList()) em.remove(u);

                em.flush();

                // 1. Создаем Админа
                User admin = new User("admin", "admin", Role.ADMIN);
                em.persist(admin);

                // 2. Создаем тестовые данные абонентов
                Subscriber sub1 = new Subscriber("Иван Иванов", "+375291234567", 150.50, false);
                Subscriber sub2 = new Subscriber("Петр Петров", "+375337654321", -50.00, true);
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

                Invoice inv1 = new Invoice(750.00, LocalDate.parse("2025-09-01"), true, sub1);
                Invoice inv2 = new Invoice(400.00, LocalDate.parse("2025-09-05"), false, sub2);
                em.persist(inv1);
                em.persist(inv2);
            });

        } catch (Exception e) {
            e.printStackTrace();
            throw new org.example.exception.DataAccessException("Ошибка при инициализации данных", e);
        }
    }
}