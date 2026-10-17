package com.artem.notificationservice.repository;

import com.artem.notificationservice.model.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    List<NotificationEntity> findAllByUserUidOrderByCreatedAtDesc(String userUid);

    Optional<NotificationEntity> findByEventId(String eventId);

    boolean existsByEventId(String eventId);
}