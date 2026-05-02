package com.safipay.webhook.repository;
import com.safipay.webhook.model.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, String> {
    List<WebhookDelivery> findByEventId(String eventId);
    List<WebhookDelivery> findByStatusAndNextRetryAtBefore(WebhookDelivery.DeliveryStatus status, LocalDateTime now);
    long countByEndpointIdAndStatus(String endpointId, WebhookDelivery.DeliveryStatus status);
}
