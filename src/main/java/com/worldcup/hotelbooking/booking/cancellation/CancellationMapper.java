package com.worldcup.hotelbooking.booking.cancellation;

public class CancellationMapper {
    public static CancellationPolicyResponseDto toDto(CancellationResponse result) {
        return new CancellationPolicyResponseDto(
                result.isCanCancel(),
                result.getRefundAmount(),
                result.getCancellationFee(),
                result.getRefundPercentage(),
                result.getPolicyMessage(),
                result.getDaysUntilCheckIn(),
                result.getSummary(),
                result.getBonusAmount(),
                result.getBonusTierDescription(),
                result.getTotalPayout()
        );
    }
}
