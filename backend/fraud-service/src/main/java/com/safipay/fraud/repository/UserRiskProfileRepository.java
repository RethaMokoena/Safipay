package com.safipay.fraud.repository;

import com.safipay.fraud.model.UserRiskProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRiskProfileRepository extends JpaRepository<UserRiskProfile, String> {}
