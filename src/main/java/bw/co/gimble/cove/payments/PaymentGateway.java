package bw.co.gimble.cove.payments;

/**
 * The DPO Pay contract Cove relies on: createToken -> customer pays on the
 * gateway's page -> verifyToken. Swap DemoDpoGateway for a real DPO client
 * (CompanyToken + ServiceType from DPO) without touching PaymentService.
 */
public interface PaymentGateway {

    record CreatedToken(String transToken, String paymentUrl) {}

    /** DPO verifyToken result: "000" paid, "900" not yet paid, "901" declined, "904" cancelled/expired. */
    record Verification(String resultCode, String explanation) {}

    String name();

    CreatedToken createToken(PaymentTransaction tx);

    Verification verifyToken(PaymentTransaction tx);
}
