package com.dristmechanic.dristmechanic.entity;

public interface IAnimatedAttacker {
    void setAttackingState(boolean attacking);
    boolean isAttackingState();
    int getAttackAnimationLength();

    /**
     * Возвращает тик анимации (от 0), на котором должен произойти удар.
     * Например, для Totebot это 9 (пик замаха в BlockBench).
     */
    int getAttackImpactFrame();
}