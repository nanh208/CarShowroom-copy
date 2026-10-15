package com.ares.shop.service;

import com.ares.shop.model.*;
import com.ares.shop.repo.NotificationRepo;
import org.springframework.stereotype.Service;

@Service
public class NotifyService {
    private final NotificationRepo repo;
    public NotifyService(NotificationRepo repo) { this.repo = repo; }
    private PushService push;
    @org.springframework.beans.factory.annotation.Autowired(required = false) public void setPush(@org.springframework.context.annotation.Lazy PushService p) { this.push = p; }
    public void send(User u, String kind, String title, String body, String link) {
        if (u == null) return;
        Notification n = new Notification(); n.user = u; n.kind = kind; n.title = title; n.body = body; n.link = link;
        repo.save(n);
        if (push != null) try { push.sendToUserAsync(u.id, title, body, link); } catch (Exception ignored) {}
    }
}
