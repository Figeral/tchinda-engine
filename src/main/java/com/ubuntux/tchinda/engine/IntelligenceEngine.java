package com.ubuntux.tchinda.engine;

import com.ubuntux.tchinda.config.AppProperties;
import com.ubuntux.tchinda.mail.MailService;
import com.ubuntux.tchinda.model.Event;
import com.ubuntux.tchinda.ranking.RankingEngine;
import com.ubuntux.tchinda.redis.IdempotencyService;
import com.ubuntux.tchinda.serpapi.SerpApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntelligenceEngine {

    private final AppProperties appProperties;
    private final SerpApiClient serpApiClient;
    private final RankingEngine rankingEngine;
    private final IdempotencyService idempotencyService;
    private final MailService mailService;

    public void runDailyCycle() {
        log.info("Starting Daily Intelligence Engine Cycle");

        List<String> expandedQueries = expandQueries(appProperties.getQueries(), appProperties.getEventKeywords());
        log.info("Expanded into {} query variations", expandedQueries.size());

        List<CompletableFuture<List<Event>>> futures = expandedQueries.stream()
                .map(serpApiClient::searchEventsAsync)
                .toList();

        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        allOf.thenRun(() -> {
            List<Event> allEvents = futures.stream()
                    .flatMap(f -> f.join().stream())
                    .collect(Collectors.toList());

            log.info("Total organic results raw fetched: {}", allEvents.size());

            // 1. Deduplicate by Redis BEFORE ranking to save cycles and avoid duplicates
            List<Event> newEvents = allEvents.stream()
                    .filter(idempotencyService::isUniqueAndCache)
                    .collect(Collectors.toList());

            log.info("Total unique new events: {}", newEvents.size());

            // 2. Rank and Filter Top 20
            List<Event> topEvents = rankingEngine.rankAndFilter(newEvents);
            log.info("Ranked and distilled down to top {} events", topEvents.size());

            // 3. Send Digest
            mailService.sendDigest(topEvents);
        }).exceptionally(ex -> {
            log.error("Error during intelligence engine cycle", ex);
            return null;
        }).join(); // We wait so the scheduler thread sees completion

        log.info("Finished Daily Intelligence Engine Cycle");
    }

    private List<String> expandQueries(List<String> templates, List<String> keywords) {
        List<String> expanded = new ArrayList<>();
        if (templates == null)
            return expanded;

        for (String template : templates) {
            if (template.contains("${event}")) {
                if (keywords != null) {
                    for (String kw : keywords) {
                        expanded.add(template.replace("${event}", kw));
                    }
                }
            } else {
                expanded.add(template);
            }
        }
        return expanded;
    }
}
