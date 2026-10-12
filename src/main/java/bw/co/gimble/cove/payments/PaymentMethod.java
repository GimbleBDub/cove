package bw.co.gimble.cove.payments;

/** The options a DPO checkout offers in Botswana. */
public enum PaymentMethod {
    CARD,
    ORANGE_MONEY,
    MASCOM_MYZAKA,
    BTC_SMEGA;

    public String label() {
        return switch (this) {
            case CARD -> "Visa / Mastercard";
            case ORANGE_MONEY -> "Orange Money";
            case MASCOM_MYZAKA -> "Mascom MyZaka";
            case BTC_SMEGA -> "BTC Smega";
        };
    }
}
