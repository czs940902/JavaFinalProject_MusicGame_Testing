package com.rhythmgame.core;

import com.rhythmgame.core.JudgeSystem.JudgmentResult;

public class ScoreSystem {
    private int score = 0;
    private int combo = 0;
    private int maxCombo = 0;
    private int totalJudgements = 0;
    private int totalPossible = 0; // for accuracy
    private int judgedCount = 0;
    private int perfectCount = 0;
    private int greatCount = 0;
    private int goodCount = 0;
    private int missCount = 0;
    private int holdTailSuccessCount = 0;

    public void registerTotalJudgements(int n) {
        totalJudgements = n;
        totalPossible = n * JudgeSystem.weight(JudgmentResult.PERFECT);
    }

    public void applyJudgement(JudgmentResult jr) {
        int w = JudgeSystem.weight(jr);
        score += w;
        judgedCount++;
        switch (jr) {
            case PERFECT -> perfectCount++;
            case GREAT -> greatCount++;
            case GOOD -> goodCount++;
            case MISS -> missCount++;
        }
        if (jr == JudgmentResult.MISS) {
            combo = 0;
        } else {
            combo++;
            if (combo > maxCombo) maxCombo = combo;
        }
    }

    public void applyHoldTailSuccess() {
        score += JudgeSystem.holdTailWeight();
        judgedCount++;
        holdTailSuccessCount++;
        // holding success continues the combo but does not increment it again.
    }

    public int getScore() { return score; }
    public int getCombo() { return combo; }
    public int getMaxCombo() { return maxCombo; }
    public int getJudgedCount() { return judgedCount; }
    public int getTotalJudgements() { return totalJudgements; }
    public int getPerfectCount() { return perfectCount; }
    public int getGreatCount() { return greatCount; }
    public int getGoodCount() { return goodCount; }
    public int getMissCount() { return missCount; }
    public int getHoldTailSuccessCount() { return holdTailSuccessCount; }

    public float getAccuracyPercent() {
        if (totalPossible == 0) return 100f;
        return (score / (float) totalPossible) * 100f;
    }
}
