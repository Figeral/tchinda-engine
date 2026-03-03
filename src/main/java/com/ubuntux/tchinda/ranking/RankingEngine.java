package com.ubuntux.tchinda.ranking;

import com.ubuntux.tchinda.config.AppProperties;
import com.ubuntux.tchinda.model.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankingEngine {

    private final AppProperties appProperties;

    // keywordWeight = 3
    // recencyWeight = 0-30 days (+10), 30-90 days (+5), past (< 0) -> discard
    // locationWeight = map lookup

    public List<Event> rankAndFilter(List<Event> events) {
        return events.stream()
                .filter(this::isFutureEvent) // discard past events
                .peek(this::scoreEvent)
                .sorted(Comparator.comparingInt(Event::getScore).reversed())
                .limit(20) // Keep top N
                .collect(Collectors.toList());
    }

    private boolean isFutureEvent(Event event) {
        if (event.getDetectedDate() == null) {
            // If we can't detect a date, we generally keep it and rely on content score.
            return true;
        }
        return !event.getDetectedDate().isBefore(LocalDate.now());
    }

    private void scoreEvent(Event event) {
        int score = 0;
        String contentText = (event.getTitle() + " " + event.getSnippet()).toLowerCase();

        // 1. Keyword weight
        for (String keyword : appProperties.getEventKeywords()) {
            if (contentText.contains(keyword.toLowerCase())) {
                score += 3;
            }
        }

        // 2. Location weight
        Map<String, Integer> weights = appProperties.getLocationWeights();
        if (weights != null) {
            for (Map.Entry<String, Integer> entry : weights.entrySet()) {
                if (contentText.contains(entry.getKey().toLowerCase())) {
                    score += entry.getValue();
                    if (event.getLocation() == null) {
                        event.setLocation(entry.getKey());
                    } else if (!event.getLocation().contains(entry.getKey())) {
                        event.setLocation(event.getLocation() + ", " + entry.getKey());
                    }
                }
            }
        }

        // 3. Recency weight
        if (event.getDetectedDate() != null) {
            long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), event.getDetectedDate());
            if (daysUntil >= 0 && daysUntil <= 30) {
                score += 10;
            } else if (daysUntil > 30 && daysUntil <= 90) {
                score += 5;
            }
        }

        event.setScore(score);
    }
}
