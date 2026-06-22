package com.rhythmgame.core;

public class JudgeSystem {
    public enum JudgmentResult { PERFECT, GREAT, GOOD, MISS }

    public static JudgmentResult judge(long deltaMs) {
        long abs = Math.abs(deltaMs);
        if (abs <= 20) return JudgmentResult.PERFECT;
        if (abs <= 50) return JudgmentResult.GREAT;
        if (abs <= 90) return JudgmentResult.GOOD;
        return JudgmentResult.MISS;
    }

    public static int weight(JudgmentResult r) {
        return switch (r) {
            case PERFECT -> 100;
            case GREAT -> 80;
            case GOOD -> 50;
            default -> 0;
        };
    }

    public static int holdTailWeight() {
        return 50;
    }
}
