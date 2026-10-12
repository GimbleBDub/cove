package bw.co.gimble.cove.payments;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * A stand-in for DPO Pay that never moves money. Tokens look like DPO's, the
 * payment URL opens Cove's own demo checkout page, and verifyToken reports
 * whatever the tester chose there (approve or decline).
 */
@Component
public class DemoDpoGateway implements PaymentGateway {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${app.frontend-url:http://localhost:64045}")
    private String frontendUrl;

    @Override
    public String name() {
        return "demo";
    }

    @Override
    public CreatedToken createToken(PaymentTransaction tx) {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        String hex = HexFormat.of().withUpperCase().formatHex(bytes);
        // Same shape as a DPO TransToken: 8-4-4-4-12 upper-case hex.
        String token = hex.substring(0, 8) + "-" + hex.substring(8, 12) + "-" + hex.substring(12, 16) + "-"
                + hex.substring(16, 20) + "-" + hex.substring(20);
        return new CreatedToken(token, frontendUrl + "/#/pay/" + token);
    }

    @Override
    public Verification verifyToken(PaymentTransaction tx) {
        return switch (tx.getStatus()) {
            case PAID -> new Verification("000", "Transaction paid");
            case PENDING -> new Verification("900", "Transaction not paid yet");
            case DECLINED -> new Verification("901", "Transaction declined: " + tx.getFailureReason());
            case CANCELLED -> new Verification("904", "Transaction cancelled");
            case EXPIRED -> new Verification("904", "Transaction expired");
        };
    }
}
