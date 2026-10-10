package com.enterprise.quartz.service;

import com.enterprise.quartz.dto.event.JobEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes live Quartz lifecycle events over STOMP WebSocket topics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobEventBroadcaster {

    public static final String TOPIC_JOB_EVENTS = "/topic/job-events";

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcast(JobEvent event) {
        log.debug("Broadcasting WebSocket event [{}]: {}.{}",
                event.getEventType(), event.getJobGroup(), event.getJobName());
        try {
            messagingTemplate.convertAndSend(TOPIC_JOB_EVENTS, event);
        } catch (Exception e) {
            log.warn("Failed to broadcast WebSocket event: {}", e.getMessage());
        }
    }
}
