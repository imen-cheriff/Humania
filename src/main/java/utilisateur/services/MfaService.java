package utilisateur.services;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * TOTP-based MFA service (RFC 6238 / Google Authenticator compatible).
 * Pure Java — no external library needed.
 *
 * How it works:
 *  1. A random Base32 secret is generated once per user and stored in DB.
 *  2. The secret is shared with Google Authenticator via a QR code URI.
 *  3. Every 30 seconds both Google Authenticator and this service compute
 *     HMAC-SHA1(secret, floor(currentTime / 30)) and display the last 6 digits.
 *  4. We verify the user-entered code against the current window ± 1 step
 *     (±30 s tolerance for clock drift).
 */
public class MfaService {

    private static final int    TOTP_DIGITS   = 6;
    private static final int    TIME_STEP     = 30;       // seconds
    private static final int    WINDOW        = 1;        // steps tolerance either side
    private static final String ISSUER        = "Humania";
    private static final String BASE32_CHARS  = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    // ─── Secret generation ────────────────────────────────────────────────────

    /**
     * Generates a random 20-byte Base32-encoded secret key.
     * Store this in utilisateur.mfa_secret.
     */
    public String generateSecret() {
        SecureRandom rng = new SecureRandom();
        byte[] bytes = new byte[20];
        rng.nextBytes(bytes);
        return base32Encode(bytes);
    }

    // ─── QR code URI ─────────────────────────────────────────────────────────

    /**
     * Returns the otpauth:// URI used to generate a QR code.
     * Scan this with Google Authenticator / Authy.
     *
     * @param secret   the user's mfa_secret
     * @param email    used as the account label in the authenticator app
     */
    public String getOtpAuthUri(String secret, String email) {
        return "otpauth://totp/"
                + urlEncode(ISSUER + ":" + email)
                + "?secret=" + secret
                + "&issuer=" + urlEncode(ISSUER)
                + "&algorithm=SHA1"
                + "&digits=" + TOTP_DIGITS
                + "&period=" + TIME_STEP;
    }

    // ─── TOTP verification ────────────────────────────────────────────────────

    /**
     * Verifies that the 6-digit code entered by the user is valid.
     * Checks the current time-step and ±WINDOW steps for clock drift tolerance.
     *
     * @param secret     stored mfa_secret for the user
     * @param userCode   the 6-digit code the user typed
     * @return true if valid
     */
    public boolean verifyCode(String secret, String userCode) {
        if (secret == null || userCode == null) return false;
        userCode = userCode.trim().replaceAll("\\s", "");
        if (!userCode.matches("\\d{6}")) return false;

        int entered;
        try { entered = Integer.parseInt(userCode); }
        catch (NumberFormatException e) { return false; }

        long currentStep = System.currentTimeMillis() / 1000L / TIME_STEP;

        for (int i = -WINDOW; i <= WINDOW; i++) {
            if (generateTotp(secret, currentStep + i) == entered) return true;
        }
        return false;
    }

    // ─── Internal TOTP computation (RFC 6238) ────────────────────────────────

    private int generateTotp(String secret, long timeStep) {
        byte[] key     = base32Decode(secret);
        byte[] message = longToBytes(timeStep);

        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(message);

            // Dynamic truncation
            int offset = hash[hash.length - 1] & 0x0F;
            int truncated = ((hash[offset]     & 0x7F) << 24)
                          | ((hash[offset + 1] & 0xFF) << 16)
                          | ((hash[offset + 2] & 0xFF) << 8)
                          |  (hash[offset + 3] & 0xFF);

            int mod = 1;
            for (int i = 0; i < TOTP_DIGITS; i++) mod *= 10;
            return truncated % mod;

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("TOTP computation failed", e);
        }
    }

    // ─── Base32 helpers ──────────────────────────────────────────────────────

    private String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0, bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                bitsLeft -= 5;
                sb.append(BASE32_CHARS.charAt((buffer >> bitsLeft) & 0x1F));
            }
        }
        if (bitsLeft > 0) sb.append(BASE32_CHARS.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        return sb.toString();
    }

    private byte[] base32Decode(String encoded) {
        encoded = encoded.toUpperCase().replaceAll("[^A-Z2-7]", "");
        byte[] result = new byte[encoded.length() * 5 / 8];
        int buffer = 0, bitsLeft = 0, idx = 0;
        for (char c : encoded.toCharArray()) {
            int val = BASE32_CHARS.indexOf(c);
            if (val < 0) continue;
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bitsLeft -= 8;
                result[idx++] = (byte) (buffer >> bitsLeft);
            }
        }
        return result;
    }

    // ─── Utility ─────────────────────────────────────────────────────────────

    private byte[] longToBytes(long value) {
        byte[] b = new byte[8];
        for (int i = 7; i >= 0; i--) { b[i] = (byte) (value & 0xFF); value >>= 8; }
        return b;
    }

    private String urlEncode(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20"); }
        catch (Exception e) { return s; }
    }
}
