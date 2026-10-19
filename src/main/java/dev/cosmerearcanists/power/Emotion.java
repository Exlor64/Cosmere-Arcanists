package dev.cosmerearcanists.power;

public enum Emotion {
    ANGER, FEAR, TRUST, LOVE, COMPLIANCE;
    public String label(){String s=name().toLowerCase(java.util.Locale.ROOT);return Character.toUpperCase(s.charAt(0))+s.substring(1);}
    public static boolean power(String key){return key.equals("a:zinc")||key.equals("a:brass");}
}
