package com.tfg.busplatform.booking.service;

import com.tfg.busplatform.booking.dto.PaymentRequest;
import org.springframework.stereotype.Service;

import java.time.YearMonth;

/**
 * Pasarela de pago <strong>simulada</strong> para el prototipo.
 *
 * <p>No realiza ningún cargo real ni se conecta con un proveedor externo: aplica
 * unas validaciones básicas (algoritmo de Luhn y fecha de caducidad) y autoriza
 * la operación. Para poder demostrar también el camino de error en la memoria,
 * cualquier tarjeta cuyos cuatro últimos dígitos sean {@code 0000} se rechaza.</p>
 */
@Service
public class SimulatedPaymentGateway {

    /** Resultado de la autorización del pago simulado. */
    public record PaymentResult(boolean approved, String declineReason, String cardLast4) {
        static PaymentResult approved(String last4) {
            return new PaymentResult(true, null, last4);
        }
        static PaymentResult declined(String reason) {
            return new PaymentResult(false, reason, null);
        }
    }

    public PaymentResult authorize(PaymentRequest req) {
        String digits = req.getCardNumber().replaceAll("\\s", "");

        if (!digits.matches("[0-9]{13,19}")) {
            return PaymentResult.declined("El número de tarjeta no tiene un formato válido.");
        }
        if (!passesLuhn(digits)) {
            return PaymentResult.declined("El número de tarjeta no es válido.");
        }
        if (isExpired(req.getExpiryMonth(), req.getExpiryYear())) {
            return PaymentResult.declined("La tarjeta está caducada.");
        }

        String last4 = digits.substring(digits.length() - 4);

        // Regla de simulación: tarjetas terminadas en 0000 simulan un rechazo bancario.
        if ("0000".equals(last4)) {
            return PaymentResult.declined("Pago rechazado por la entidad emisora.");
        }

        return PaymentResult.approved(last4);
    }

    /** Validación de número de tarjeta mediante el algoritmo de Luhn. */
    private boolean passesLuhn(String digits) {
        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    private boolean isExpired(int month, int year) {
        YearMonth expiry = YearMonth.of(year, month);
        return expiry.isBefore(YearMonth.now());
    }
}
