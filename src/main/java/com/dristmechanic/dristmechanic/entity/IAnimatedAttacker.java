package com.dristmechanic.dristmechanic.entity;

public interface IAnimatedAttacker {
    void setAttackingState(boolean attacking);
    boolean isAttackingState();
    int getAttackAnimationLength();
}