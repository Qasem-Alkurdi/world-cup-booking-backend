package com.worldcup.hotelbooking.payment;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPaymentRequestDto {

    @NotNull(message = "Payment intent ID is required")
    private String paymentIntentId;

    // For mock: true = success, false = failure
    private Boolean simulateSuccess = true;//in the real word we would not have this field, it's just for testing different scenarios , because any one can send that process done from teh frontend and we want to make sure that we can test both success and failure scenarios and the right way is to sure from stripe or any other payment gateway that the payment is done and we can not trust the frontend to send us that information because it can be easily manipulated, so we need to have a way to simulate both scenarios for testing purposes

    // Credit card details (for mock, we just validate format)
    private String cardNumber;
    private String cardHolderName;
    private String expiryDate;
    private String cvv;
}
