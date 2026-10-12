package com.artem.webhookcollectorservice.repository;

import com.artem.webhookcollectorservice.entity.UnknownCallbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UnknownCallbackRepository
        extends JpaRepository<UnknownCallbackEntity, UUID> {
}