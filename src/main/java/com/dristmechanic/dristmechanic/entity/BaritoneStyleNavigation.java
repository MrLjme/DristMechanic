package com.dristmechanic.dristmechanic.entity;

import com.dristmechanic.dristmechanic.entity.AnimatedAttacker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaritoneStyleNavigation extends GroundPathNavigation {
    private Map<BlockPos, List<BlockPos>> blocksToBreakMap = new HashMap<>();
    private boolean usingBaritonePath = false;

    public BaritoneStyleNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new WalkNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        this.nodeEvaluator.setCanFloat(true);
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }

    @Override
    public Path createPath(BlockPos target, int distance) {
        BaritonePathFinder baritoneFinder = new BaritonePathFinder(
                this.level,
                this.mob.blockPosition(),
                target,
                this.mob.getBbHeight()
        );

        Path baritonePath = baritoneFinder.findPath(distance);
        if (baritonePath != null) {
            this.blocksToBreakMap = baritoneFinder.getBlocksToBreakMap();
            this.usingBaritonePath = true;
            return baritonePath;
        }

        this.usingBaritonePath = false;
        return super.createPath(target, distance);
    }

    @Override
    public void tick() {
        if (!usingBaritonePath) {
            super.tick();
            return;
        }

        if (!(this.mob instanceof AnimatedAttacker attacker)) {
            super.tick();
            return;
        }

        if (!(this.mob instanceof PathfinderMob pathfinderMob)) {
            super.tick();
            return;
        }

        if (attacker.getBreakingBlock() != null) {
            boolean continueBreaking = attacker.tickBreakingBlock(pathfinderMob, true);
            if (!continueBreaking) {
                if (this.getPath() != null && !this.isDone()) {
                    Node nextNode = this.getPath().getNextNode();
                    if (nextNode != null) {
                        this.mob.getNavigation().moveTo(
                                nextNode.x + 0.5,
                                nextNode.y,
                                nextNode.z + 0.5,
                                1.0
                        );
                    }
                }
            }
            return;
        }

        if (this.getPath() != null && !this.isDone()) {
            int nextIndex = this.getPath().getNextNodeIndex();
            if (nextIndex < this.getPath().getNodeCount()) {
                Node nextNode = this.getPath().getNode(nextIndex);
                BlockPos nextPos = nextNode.asBlockPos();

                List<BlockPos> blocksForNode = blocksToBreakMap.get(nextPos);
                if (blocksForNode != null && !blocksForNode.isEmpty()) {
                    BlockPos blockToBreak = blocksForNode.get(0);
                    double distance = this.mob.blockPosition().distSqr(blockToBreak);

                    if (distance < 9.0) {
                        attacker.startBreakingBlock(pathfinderMob, blockToBreak);
                        blocksForNode.remove(0);
                        return;
                    }
                }
            }
        }

        super.tick();
    }

    @Override
    public void stop() {
        super.stop();
        blocksToBreakMap.clear();
        usingBaritonePath = false;
    }
}