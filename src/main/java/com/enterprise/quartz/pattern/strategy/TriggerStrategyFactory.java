package com.enterprise.quartz.pattern.strategy;

import com.enterprise.quartz.dto.request.TriggerType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Strategy Registry providing lookup of TriggerStrategy by TriggerType.
 */
@Component
public class TriggerStrategyFactory {

    private final Map<TriggerType, TriggerStrategy> strategies = new EnumMap<>(TriggerType.class);

    public TriggerStrategyFactory(List<TriggerStrategy> strategyList) {
        for (TriggerStrategy strategy : strategyList) {
            strategies.put(strategy.getTriggerType(), strategy);
        }
    }

    public TriggerStrategy getStrategy(TriggerType triggerType) {
        TriggerStrategy strategy = strategies.get(triggerType);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported trigger strategy type: " + triggerType);
        }
        return strategy;
    }
}
