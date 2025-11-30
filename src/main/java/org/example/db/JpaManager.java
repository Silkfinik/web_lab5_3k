package org.example.db;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JpaManager {
    private static final Logger logger = LoggerFactory.getLogger(JpaManager.class);

    private static final String PERSISTENCE_UNIT_NAME = "telecom-pu";

    public static final EntityManagerFactory emf;

    static {
        logger.info("Инициализация JPA EntityManagerFactory...");
        try {
            emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
            logger.info("EntityManagerFactory успешно инициализирована.");
        } catch (Exception e) {
            logger.error("КРИТИЧЕСКАЯ ОШИБКА: Не удалось инициализировать EntityManagerFactory!", e);
            throw new RuntimeException("Ошибка инициализации EntityManagerFactory", e);
        }
    }

    public static EntityManager getEntityManager() {
        return emf.createEntityManager();
    }
}