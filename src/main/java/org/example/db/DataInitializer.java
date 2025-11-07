package org.example.db;

import org.example.dao.api.InvoiceDao;
import org.example.dao.api.ServiceDao;
import org.example.dao.api.SubscriberDao;
import org.example.entity.Invoice;
import org.example.entity.Service;
import org.example.entity.Subscriber;
import java.time.LocalDate;

/**
 * Заполняет базу данных тестовыми данными.
 * Трансляция из DataInitializer.kt
 */
public class DataInitializer {

    public static void insertInitialData(
            SubscriberDao subscriberDao,
            ServiceDao serviceDao,
            InvoiceDao invoiceDao) {

        // System.out.println("Заполнение базы данных тестовыми данными...");

        try {
            // Очистка таблиц в правильном порядке
            // (Сначала счета, т.к. они ссылаются на абонентов)
            // (Потом связи абонентов и услуг)

            // В Lab 3 очистка была проще,
            // но в JPA с внешними ключами лучше удалять в явном порядке.
            // Самый простой способ, как в Lab 3 - удалить абонентов и услуги,
            // а JPA (Cascade) удалит всё остальное.
            subscriberDao.deleteAll();
            serviceDao.deleteAll();


            // Создание Абонентов
            Subscriber sub1 = new Subscriber(
                    "Иван Иванов", "+375291234567", 150.50, false
            );
            Subscriber sub2 = new Subscriber(
                    "Петр Петров", "+375337654321", -50.00, true
            );

            sub1 = subscriberDao.add(sub1);
            sub2 = subscriberDao.add(sub2);

            // Создание Услуг
            Service serv1 = serviceDao.add(new Service("Интернет 50 Мбит/с", 450.00));
            Service serv2 = serviceDao.add(new Service("Мобильная связь", 300.00));
            Service serv3 = serviceDao.add(new Service("Антивирус", 100.00));

            // Связывание
            serviceDao.linkServiceToSubscriber(sub1.getId(), serv1.getId());
            serviceDao.linkServiceToSubscriber(sub1.getId(), serv2.getId());
            serviceDao.linkServiceToSubscriber(sub2.getId(), serv2.getId());
            serviceDao.linkServiceToSubscriber(sub2.getId(), serv3.getId());

            // Создание Счетов
            invoiceDao.add(new Invoice(
                    750.00,
                    LocalDate.parse("2025-09-01"),
                    true,
                    sub1
            ));
            invoiceDao.add(new Invoice(
                    400.00,
                    LocalDate.parse("2025-09-05"),
                    false,
                    sub2
            ));

            // System.out.println("Тестовые данные успешно вставлены.");

        } catch (Exception e) {
            // System.err.println("Ошибка при вставке тестовых данных!");
            e.printStackTrace();
            // Оборачиваем в DataAccessException, чтобы сервлет поймал
            throw new org.example.exception.DataAccessException("Ошибка при инициализации данных", e);
        }
    }
}