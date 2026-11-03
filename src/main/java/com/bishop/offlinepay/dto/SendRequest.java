package com.bishop.offlinepay.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class SendRequest {
    @NotBlank(message = "Receiver email is required")
    @Email(message = "Enter a valid email")
    private String receiverEmail;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "^\\d{4}$", message = "PIN must be exactly 4 digits")
    private String pin;

    @Size(max = 64)
    @Pattern(regexp = "^[A-Za-z0-9-]*$", message = "Bad reference")
    private String clientRef;

    public String getReceiverEmail() {
        return receiverEmail;
    }

    public void setReceiverEmail(String receiverEmail) {
        this.receiverEmail = receiverEmail;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getClientRef() {
        return clientRef;
    }

    public void setClientRef(String clientRef) {
        this.clientRef = clientRef;
    }
}
