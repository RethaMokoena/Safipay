package com.safipay.stokvel.repository;
import com.safipay.stokvel.model.Stokvel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface StokvelRepository extends JpaRepository<Stokvel, Long> {
    boolean existsByName(String name);
    List<Stokvel> findByAdminUserId(String adminUserId);
    List<Stokvel> findByStatus(Stokvel.StokvelStatus status);
    @Query("SELECT s FROM Stokvel s JOIN s.members m WHERE m.userId=:userId AND m.status='ACTIVE'")
    List<Stokvel> findByMemberUserId(String userId);
}
