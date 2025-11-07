package org.example.dao.api;

import org.example.entity.Service;
import java.util.List;

public interface ServiceDao {
    List<Service> findAll();
    List<Service> findBySubscriberId(int subscriberId);
    void linkServiceToSubscriber(int subscriberId, int serviceId);
    Service add(Service service);
    void deleteAll();
}