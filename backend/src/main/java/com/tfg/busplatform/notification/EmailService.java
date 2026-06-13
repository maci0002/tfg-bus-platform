package com.tfg.busplatform.notification;

import com.tfg.busplatform.booking.model.Reservation;
import com.tfg.busplatform.notification.dto.ContactRequest;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * Servicio de notificaciones por correo electrónico.
 *
 * <p>En desarrollo se apoya en Mailpit (servidor SMTP de pruebas que captura los
 * correos sin enviarlos a destinatarios reales). El envío es <strong>tolerante a
 * fallos</strong>: si está deshabilitado o el servidor SMTP no responde, se registra
 * un aviso y la operación de negocio (pago de la reserva, contacto) continúa con
 * normalidad.</p>
 */
@Service
@Slf4j
public class EmailService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;
    private final String supportInbox;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.mail.enabled:true}") boolean enabled,
                        @Value("${app.mail.from}") String from,
                        @Value("${app.mail.support-inbox}") String supportInbox) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
        this.supportInbox = supportInbox;
    }

    /**
     * Envía al viajero el correo de confirmación de su reserva con el resumen del
     * viaje y el código QR del billete incrustado en el cuerpo del mensaje.
     */
    public void sendBookingConfirmation(Reservation reservation, byte[] qrPng) {
        if (!enabled) {
            log.debug("Notificaciones por correo deshabilitadas: se omite la confirmación de {}",
                    reservation.getCode());
            return;
        }
        String to = reservation.getUser().getEmail();
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("Tu billete BusJaén · " + reservation.getCode());
            helper.setText(buildConfirmationHtml(reservation), true);
            if (qrPng != null && qrPng.length > 0) {
                helper.addInline("ticketQr", new ByteArrayResource(qrPng), "image/png");
            }
            mailSender.send(message);
            log.info("Correo de confirmación enviado a {} para la reserva {}", to, reservation.getCode());
        } catch (Exception e) {
            log.warn("No se pudo enviar el correo de confirmación de {} a {}: {}",
                    reservation.getCode(), to, e.getMessage());
        }
    }

    /**
     * Reenvía al buzón de soporte un mensaje recibido desde el formulario de
     * contacto, con la dirección del remitente en {@code Reply-To}.
     */
    public void sendContactMessage(ContactRequest req) {
        if (!enabled) {
            log.debug("Notificaciones por correo deshabilitadas: se omite el contacto de {}", req.getEmail());
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(supportInbox);
            helper.setReplyTo(req.getEmail());
            helper.setSubject("[Contacto] " + req.getSubject());
            helper.setText(buildContactBody(req), false);
            mailSender.send(message);
            log.info("Mensaje de contacto de {} reenviado a soporte", req.getEmail());
        } catch (Exception e) {
            log.warn("No se pudo reenviar el mensaje de contacto de {}: {}", req.getEmail(), e.getMessage());
        }
    }

    // ── Plantillas ───────────────────────────────────────────────────────────

    private String buildConfirmationHtml(Reservation r) {
        String color = r.getLine().getColor() == null ? "#1565C0" : r.getLine().getColor();
        return """
            <div style="font-family:Arial,Helvetica,sans-serif;max-width:560px;margin:0 auto;color:#263238;">
              <h2 style="color:%s;margin-bottom:4px;">Reserva confirmada</h2>
              <p>Hola %s, tu billete está listo. Presenta el código QR al subir al autobús.</p>
              <table style="width:100%%;border-collapse:collapse;margin:16px 0;font-size:14px;">
                <tr><td style="padding:6px 0;color:#607D8B;">Localizador</td><td style="text-align:right;font-weight:bold;">%s</td></tr>
                <tr><td style="padding:6px 0;color:#607D8B;">Línea</td><td style="text-align:right;">%s · %s</td></tr>
                <tr><td style="padding:6px 0;color:#607D8B;">Trayecto</td><td style="text-align:right;">%s → %s</td></tr>
                <tr><td style="padding:6px 0;color:#607D8B;">Fecha</td><td style="text-align:right;">%s</td></tr>
                <tr><td style="padding:6px 0;color:#607D8B;">Horario</td><td style="text-align:right;">%s - %s</td></tr>
                <tr><td style="padding:6px 0;color:#607D8B;">Asiento</td><td style="text-align:right;">%s</td></tr>
                <tr><td style="padding:6px 0;color:#607D8B;">Importe</td><td style="text-align:right;font-weight:bold;">%.2f €</td></tr>
              </table>
              <div style="text-align:center;margin:20px 0;">
                <img src="cid:ticketQr" alt="Código QR del billete" style="width:220px;height:220px;" />
              </div>
              <p style="font-size:12px;color:#90A4AE;text-align:center;">
                BusJaén · Transporte interurbano de la provincia de Jaén.<br/>
                Este es un correo automático de un entorno de pruebas; no respondas a este mensaje.
              </p>
            </div>
            """.formatted(
                color,
                r.getUser().getFirstName(),
                r.getCode(),
                r.getLine().getCode(), r.getLine().getName(),
                r.getOriginStop().getName(), r.getDestinationStop().getName(),
                r.getTravelDate().format(DATE),
                r.getDepartureTime().format(HHMM), r.getArrivalTime().format(HHMM),
                r.getSeatCode(),
                r.getPrice());
    }

    private String buildContactBody(ContactRequest req) {
        return """
            Nuevo mensaje desde el formulario de contacto de BusJaén.

            Nombre:  %s
            Email:   %s
            Asunto:  %s

            Mensaje:
            %s
            """.formatted(req.getName(), req.getEmail(), req.getSubject(), req.getMessage());
    }
}
