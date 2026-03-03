package com.ubuntux.tchinda.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    private String title;
    private String url;
    private String snippet;
    private LocalDate detectedDate;
    private String location;
    private int score;
    private String fingerprint;
}
