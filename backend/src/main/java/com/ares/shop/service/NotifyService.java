package com.ares.shop.service;

import com.ares.shop.model.*;
import com.ares.shop.repo.NotificationRepo;
import org.springframework.stereotype.Service;

@Service
public class NotifyService {
    private final NotificationRepo repo;
    public NotifyService(NotificationRepo repo) { this.repo = repo; }
    public void send(User u, String kind, String title, String body, String link) {
        if (u == null) return;
        Notification n = new Notification(); n.user = u; n.kind = kind; n.title = title; n.body = body; n.link = link;
        repo.save(n);
    }
}
