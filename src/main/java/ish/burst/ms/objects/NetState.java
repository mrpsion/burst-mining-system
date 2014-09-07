package ish.burst.ms.objects;

import java.math.BigInteger;

/**
 * Created by ihartney on 9/2/14.
 */
public class NetState {

    public static final BigInteger two64 = new BigInteger("18446744073709551616");

    private String height;
    private String generationSignature;
    private String baseTarget;
    private String targetDeadline;
    private byte[] gensig;
    private long heightL;
    private long baseTargetL;
    private long targetDeadlineL;

    public String getTargetDeadline() {
        return targetDeadline;
    }

    public void setTargetDeadline(String targetDeadline) {
        this.targetDeadlineL = Long.valueOf(targetDeadline);
        this.targetDeadline = targetDeadline;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        heightL = parseUnsignedLong(height);
        this.height = height;
    }

    public String getGenerationSignature() {
        return generationSignature;
    }

    public void setGenerationSignature(String generationSignature) {
        gensig = NetState.parseHexString(generationSignature);
        this.generationSignature = generationSignature;
    }

    public String getBaseTarget() {
        return baseTarget;
    }

    public void setBaseTarget(String baseTarget) {
        this.baseTargetL = Long.valueOf(baseTarget);
        this.baseTarget = baseTarget;
    }

    public String getGenerationSignatureForDisplay(){
        return generationSignature.substring(0,10)+"..."+generationSignature.substring(generationSignature.length()-10);
    }

    public byte[] getGensig(){
        return gensig;
    }

    public long getHeightL(){
        return heightL;
    }

    public long getBaseTargetL(){
        return baseTargetL;

    }

    public long getTargetDeadlineL(){
        return targetDeadlineL;
    }

    public static byte[] parseHexString(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int char1 = hex.charAt(i * 2);
            char1 = char1 > 0x60 ? char1 - 0x57 : char1 - 0x30;
            int char2 = hex.charAt(i * 2 + 1);
            char2 = char2 > 0x60 ? char2 - 0x57 : char2 - 0x30;
            if (char1 < 0 || char2 < 0 || char1 > 15 || char2 > 15) {
                throw new NumberFormatException("Invalid hex number: " + hex);
            }
            bytes[i] = (byte)((char1 << 4) + char2);
        }
        return bytes;
    }

    public static Long parseUnsignedLong(String number) {
        if (number == null) {
            return null;
        }
        BigInteger bigInt = new BigInteger(number.trim());
        if (bigInt.signum() < 0 || bigInt.compareTo(two64) != -1) {
            throw new IllegalArgumentException("overflow: " + number);
        }
        return zeroToNull(bigInt.longValue());
    }


    public static Long zeroToNull(long l) {
        return l == 0 ? null : l;
    }

}
