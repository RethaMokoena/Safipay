package com.safipay.webhook.repository;
import com.safipay.webhook.model.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, String> {
    List<WebhookEvent> findByOwnerIdOrderByCreatedAtDesc(String ownerId);
    List<WebhookEvent> findByStatus(WebhookEvent.EventStatus status);
}
