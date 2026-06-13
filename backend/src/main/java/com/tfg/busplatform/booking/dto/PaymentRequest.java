package com.tfg.busplatform.booking.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Datos del pago simulado de una reserva.
 *
 * <p>Es un pago <strong>ficticio</strong> de prototipo: no se conecta con ninguna
 * pasarela real ni se almacena el número de tarjeta. Solo se valida el formato y
 * se conservan los últimos 4 dígitos para mostrarlos en el ticket.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotBlank(message = "El titular de la tarjeta es obligatorio")
    private String cardHolder;

    /** Número de tarjeta (13-19 dígitos, se admiten espacios que se ignoran). */
    @NotBlank(message = "El número de tarjeta es obligatorio")
    @Pattern(regexp = "[0-9 ]{13,23}", message = "Número de tarjeta no válido")
    private String cardNumber;

    @Min(value = 1, message = "Mes de caducidad no válido")
    @Max(value = 12, message = "Mes de caducidad no válido")
    private int expiryMonth;

    @Min(value = 2024, message = "Año de caducidad no válido")
    @Max(value = 2099, message = "Año de caducidad no válido")
    private int expiryYear;

    @NotBlank(message = "El CVV es obligatorio")
    @Pattern(regexp = "[0-9]{3,4}", message = "CVV no válido")
    private String cvv;
}
