package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.dev.log-emails:false}")
    private boolean devLogEmails;

    public void sendVerificationCode(String toEmail, String code) {
        if (devLogEmails) {
            log.debug("Təsdiq kodu {} ünvanına göndərildi (dev rejimi)", toEmail);
        }

        String html = """
            <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto; padding: 32px; background: #f9f9f9; border-radius: 12px; border: 1px solid #eee;">
              <h2 style="color: #1a1a1a; margin-bottom: 8px;">Emaili təsdiqləyin</h2>
              <p style="color: #555; font-size: 15px; line-height: 1.6;">
                NaxEv.az tətbiqinə qeydiyyatı tamamlamaq üçün aşağıdakı təsdiq kodunu daxil edin:
              </p>
              <div style="margin: 24px 0; padding: 16px; background: #fff; border: 2px dashed #d97706; 
                          border-radius: 8px; text-align: center; font-size: 32px; font-weight: 800; 
                          letter-spacing: 8px; color: #d97706;">
                %s
              </div>
              <p style="color: #999; font-size: 13px;">
                Əgər siz qeydiyyatdan keçməmisinizsə, bu emaili nəzərə almayın.
              </p>
            </div>
            """.formatted(code);

        sendHtml(toEmail, "Təsdiq kodu — NaxEv.az", html);
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;

        if (devLogEmails) {
            log.debug("Şifrə sıfırlama linki {} ünvanına göndərildi (dev rejimi)", toEmail);
        }

        String html = """
            <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto; padding: 32px; background: #f9f9f9; border-radius: 12px;">
              <h2 style="color: #1a1a1a; margin-bottom: 8px;">Şifrə sıfırlama</h2>
              <p style="color: #555; font-size: 15px; line-height: 1.6;">
                Şifrənizi sıfırlamaq üçün aşağıdakı düyməyə klikləyin.<br>
                Bu link <strong>30 dəqiqə</strong> ərzində etibarlıdır.
              </p>
              <a href="%s"
                 style="display: inline-block; margin: 24px 0; padding: 12px 28px;
                        background: #2563eb; color: #fff; text-decoration: none;
                        border-radius: 8px; font-size: 15px; font-weight: 600;">
                Şifrəni sıfırla
              </a>
              <p style="color: #999; font-size: 13px;">
                Əgər bu sorğu sizdən gəlməyibsə, bu emaili nəzərə almayın.
              </p>
            </div>
            """.formatted(resetLink);

        sendHtml(toEmail, "Şifrə sıfırlama — Nax Emlak", html);
    }

    public void sendAdvertiseInquiry(String name, String phone, String email, String company, String plan, String message) {
        String html = """
            <div style="font-family:Arial,sans-serif;max-width:560px;margin:0 auto;padding:24px;">
              <h2>Yeni reklam müraciəti</h2>
              <p><strong>Ad:</strong> %s</p>
              <p><strong>Şirkət:</strong> %s</p>
              <p><strong>Telefon:</strong> %s</p>
              <p><strong>Email:</strong> %s</p>
              <p><strong>Paket:</strong> %s</p>
              <p><strong>Qeyd:</strong> %s</p>
            </div>
            """.formatted(
                escapeHtml(name),
                escapeHtml(company),
                escapeHtml(phone),
                escapeHtml(email),
                escapeHtml(plan),
                escapeHtml(message)
        );
        notifyAdmin("Reklam müraciəti — NaxEv.az", html);
    }

    public void sendAgencyApplication(String name, String phone, String email, String company, String note) {
        String html = """
            <div style="font-family:Arial,sans-serif;max-width:560px;margin:0 auto;padding:24px;">
              <h2>Yeni agentlik müraciəti</h2>
              <p><strong>Ad:</strong> %s</p>
              <p><strong>Şirkət:</strong> %s</p>
              <p><strong>Telefon:</strong> %s</p>
              <p><strong>Email:</strong> %s</p>
              <p><strong>Qeyd:</strong> %s</p>
            </div>
            """.formatted(
                escapeHtml(name),
                escapeHtml(company),
                escapeHtml(phone),
                escapeHtml(email),
                escapeHtml(note)
        );
        notifyAdmin("Agentlik müraciəti — NaxEv.az", html);
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }

    private void notifyAdmin(String subject, String html) {
        if (fromEmail != null && !fromEmail.isBlank()) {
            sendHtml(fromEmail, subject, html);
        }
    }

    private void sendHtml(String toEmail, String subject, String html) {
        try {
            if (fromEmail != null && !fromEmail.isBlank()) {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromEmail);
                helper.setTo(toEmail);
                helper.setSubject(subject);
                helper.setText(html, true);
                mailSender.send(message);
                log.info("Email uğurla göndərildi: {}", toEmail);
            }
        } catch (Exception e) {
            log.warn("Email göndərilmədi: {}", e.getMessage());
        }
    }
}
