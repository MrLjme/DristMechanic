package com.dristmechanic.dristmechanic.entity;

public interface AnimatedAttacker {
    void setAttackingState(boolean attacking);
    boolean isAttackingState();
    int getAttackAnimationLength();
    int getAttackImpactFrame();
}