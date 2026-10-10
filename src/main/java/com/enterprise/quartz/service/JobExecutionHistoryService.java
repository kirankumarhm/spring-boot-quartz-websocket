package com.enterprise.quartz.service;

import com.enterprise.quartz.dto.response.JobExecutionRecord;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * Thread-safe in-memory ring buffer capturing recent job executions for audit and telemetry.
 * Keeps memory overhead bounded (<100KB) while enabling historical visibility.
 */
@Service
public class JobExecutionHistoryService {

    private static final int MAX_HISTORY_SIZE = 250;
    private final ConcurrentLinkedDeque<JobExecutionRecord> history = new ConcurrentLinkedDeque<>();

    public void recordExecution(JobExecutionRecord record) {
        history.addFirst(record);
        while (history.size() > MAX_HISTORY_SIZE) {
            history.pollLast();
        }
    }

    public List<JobExecutionRecord> getHistory(String group, String name, int limit) {
        return history.stream()
                .filter(r -> (group == null || group.isBlank() || r.getJobGroup().equalsIgnoreCase(group))
                          && (name == null || name.isBlank() || r.getJobName().equalsIgnoreCase(name)))
                .limit(limit > 0 ? limit : 100)
                .collect(Collectors.toList());
    }

    public void clearHistory() {
        history.clear();
    }
}
