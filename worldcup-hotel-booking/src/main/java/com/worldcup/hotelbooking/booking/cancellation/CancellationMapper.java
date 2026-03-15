package com.worldcup.hotelbooking.booking.cancellation;

public class CancellationMapper {
    public static CancellationPolicyResponseDto toDto(CancellationResponseDto result) {
        return new CancellationPolicyResponseDto(
                result.isCanCancel(),
                result.getRefundAmount(),
                result.getCancellationFee(),
                result.getRefundPercentage(),
                result.getPolicyMessage(),
                result.getDaysUntilCheckIn(),
                result.getSummary()
        );
    }
}
