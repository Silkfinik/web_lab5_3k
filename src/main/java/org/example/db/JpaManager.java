package org.example.db;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * Управляет EntityManagerFactory (синглтон).
 * Трансляция из JpaManager.kt
 */
public class JpaManager {

    // Имя <persistence-unit> из persistence.xml
    private static final String PERSISTENCE_UNIT_NAME = "telecom-pu";

    public static final EntityManagerFactory emf; // Фабрика Entity Manager

    static {
        // System.out.println("Инициализация JPA EntityManagerFactory...");
        try {
            emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
            // System.out.println("EntityManagerFactory успешно инициализирована.");
        } catch (Exception e) {
            // System.err.println("КРИТИЧЕСКАЯ ОШИБКА: Не удалось инициализировать EntityManagerFactory!");
            e.printStackTrace();
            throw new RuntimeException("Ошибка инициализации EntityManagerFactory", e);
        }
    }

    /**
     * Создает новый EntityManager для выполнения операций
     */
    public static EntityManager getEntityManager() {
        return emf.createEntityManager();
    }
}