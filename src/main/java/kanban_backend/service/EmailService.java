package kanban_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void sendInviteEmail(String toEmail, String toName, String inviterName, String token) {
        String inviteLink = frontendUrl + "/invite/accept?token=" + token;

        String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 32px;'>"
            + "<h2 style='color: #1d4ed8; margin-bottom: 8px;'>You're invited to join Kanban Board!</h2>"
            + "<p style='color: #374151;'>Hi <strong>" + toName + "</strong>,</p>"
            + "<p style='color: #374151;'>You've been invited by <strong>" + inviterName + "</strong> to join the team on Kanban Board.</p>"
            + "<a href='" + inviteLink + "' style='display: inline-block; background: #2563eb; color: white; "
            + "padding: 12px 28px; border-radius: 8px; text-decoration: none; font-weight: bold; "
            + "font-size: 15px; margin: 20px 0;'>Set password &amp; join</a>"
            + "<p style='color: #6b7280; font-size: 14px;'>Or copy this link:</p>"
            + "<p style='color: #2563eb; font-size: 13px; word-break: break-all;'>" + inviteLink + "</p>"
            + "<p style='color: #9ca3af; font-size: 13px; margin-top: 24px;'>This link expires in 7 days.</p>"
            + "</div>";

        try {
            // Build request body as a proper Map so Jackson handles all escaping
            Map<String, Object> body = Map.of(
                "sender", Map.of("name", "Kanban Board", "email", "dr.kavi21k@gmail.com"),
                "to", List.of(Map.of("email", toEmail, "name", toName)),
                "subject", "You're invited to join Kanban Board",
                "htmlContent", htmlContent
            );

            String requestBody = objectMapper.writeValueAsString(body);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                .header("Content-Type", "application/json")
                .header("api-key", brevoApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 201) {
                throw new RuntimeException("Brevo API error: " + response.body());
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to send invite email: " + e.getMessage(), e);
        }
    }
}
