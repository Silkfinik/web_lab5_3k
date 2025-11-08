package org.example.dao.api;

import jakarta.persistence.EntityManager;
import org.example.entity.Subscriber;
import java.util.List;
import java.util.function.Consumer;

public interface SubscriberDao {
    Subscriber findById(int id);
    List<Subscriber> findAll();
    void block(int subscriberId);
    Subscriber add(Subscriber subscriber);
    void deleteAll();
    void runInTransaction(Consumer<EntityManager> block);
}