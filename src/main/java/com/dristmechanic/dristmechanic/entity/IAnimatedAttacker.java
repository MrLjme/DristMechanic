package com.dristmechanic.dristmechanic.entity;

public interface IAnimatedAttacker {
    void setAttackingState(boolean attacking);

    /**
     * Позволяет другим Goal проверить, не проигрывается ли сейчас анимация атаки.
     */
    boolean isAttackingState();

    int getAttackAnimationLength();
}