package com.safipay.webhook.repository;
import com.safipay.webhook.model.WebhookEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, String> {
    List<WebhookEndpoint> findByOwnerIdAndStatus(String ownerId, WebhookEndpoint.EndpointStatus status);

    @Query("SELECT e FROM WebhookEndpoint e WHERE e.status = 'ACTIVE' AND e.subscribedEvents LIKE CONCAT('%', :eventType, '%')")
    List<WebhookEndpoint> findActiveEndpointsForEvent(String eventType);
}
