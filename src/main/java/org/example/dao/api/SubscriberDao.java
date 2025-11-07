package org.example.dao.api;

import org.example.entity.Subscriber;
import java.util.List;

public interface SubscriberDao {
    Subscriber findById(int id);
    List<Subscriber> findAll();
    void block(int subscriberId);
    Subscriber add(Subscriber subscriber);
    void deleteAll();
}