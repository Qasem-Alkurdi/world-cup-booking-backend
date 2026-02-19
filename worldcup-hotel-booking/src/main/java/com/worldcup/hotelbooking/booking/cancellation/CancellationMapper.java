package com.worldcup.hotelbooking.booking.cancellation;

public class CancellationMapper {
    public static CancellationPolicyResponse toDto(CancellationResult result) {
        return new CancellationPolicyResponse(
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
