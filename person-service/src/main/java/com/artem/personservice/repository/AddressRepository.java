package com.artem.personservice.repository;


import com.artem.personservice.entity.AddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<AddressEntity, UUID>,
        RevisionRepository<AddressEntity, UUID, Long> {
}
