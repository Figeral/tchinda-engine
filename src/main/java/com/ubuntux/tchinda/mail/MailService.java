package com.ubuntux.tchinda.mail;

import com.ubuntux.tchinda.config.AppProperties;
import com.ubuntux.tchinda.model.Event;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final AppProperties appProperties;

    public void sendDigest(List<Event> events) {
        if (events == null || events.isEmpty()) {
            log.info("No new events to send in digest. Skipping email.");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(appProperties.getEmail().getSender());
            helper.setTo(appProperties.getEmail().getRecipient());
            helper.setSubject("Daily Event Radar - " + LocalDate.now());

            String htmlContent = buildHtmlContent(events);
            helper.setText(htmlContent, true); // true = isHtml

            mailSender.send(message);
            log.info("Successfully sent Event Digest with {} events to {}", events.size(),
                    appProperties.getEmail().getRecipient());

        } catch (MessagingException e) {
            log.error("Failed to send Event Digest email", e);
        }
    }

    private String buildHtmlContent(List<Event> events) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif;'>");
        sb.append("<h2>Daily Event Radar</h2>");
        sb.append("<p>Found <strong>").append(events.size()).append("</strong> new relevant events.</p>");
        sb.append("<hr/>");

        for (Event e : events) {
            sb.append("<div style='margin-bottom: 20px;'>");
            sb.append("<h3 style='margin: 0 0 5px 0;'><a href='").append(e.getUrl()).append("' target='_blank'>")
                    .append(e.getTitle()).append("</a></h3>");
            sb.append("<div style='color: #555; font-size: 0.9em; margin-bottom: 5px;'>");
            sb.append("<strong>Score:</strong> ").append(e.getScore()).append(" | ");
            if (e.getDetectedDate() != null) {
                sb.append("<strong>Date:</strong> ").append(e.getDetectedDate()).append(" | ");
            }
            if (e.getLocation() != null && !e.getLocation().isEmpty()) {
                sb.append("<strong>Location:</strong> ").append(e.getLocation());
            }
            sb.append("</div>");
            sb.append("<p style='margin: 0; color: #333;'>").append(e.getSnippet()).append("</p>");
            sb.append("</div><hr/>");
        }

        sb.append("</body></html>");
        return sb.toString();
    }
}
