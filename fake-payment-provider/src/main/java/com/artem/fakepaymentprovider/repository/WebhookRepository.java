package com.artem.fakepaymentprovider.repository;



import com.artem.fakepaymentprovider.model.WebhookEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebhookRepository extends JpaRepository<WebhookEntity, Long> {

    List<WebhookEntity> findByEventTypeAndEntityId(String eventType, Long entityId);
}