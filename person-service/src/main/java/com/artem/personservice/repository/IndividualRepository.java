package com.artem.personservice.repository;


import com.artem.personservice.entity.IndividualEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IndividualRepository extends JpaRepository<IndividualEntity, UUID> {
}