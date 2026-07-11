package com.dristmechanic.dristmechanic.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SmoothPathNavigation extends GroundPathNavigation {

    public SmoothPathNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    @Nullable
    public Path createPath(@NotNull BlockPos pos, int distance) {
        Path path = super.createPath(pos, distance);

        return path != null ? smoothPath(path) : null;
    }

    private Path smoothPath(Path path) {
        if (path.getNodeCount() < 3) return path;

        List<Node> smoothedNodes = new ArrayList<>();
        smoothedNodes.add(path.getNode(0));
        Node lastNode = path.getNode(0);

        for (int i = 1; i < path.getNodeCount(); i++) {
            Node current = path.getNode(i);

            if (i == path.getNodeCount() - 1) {
                smoothedNodes.add(current);
                continue;
            }

            Node next = path.getNode(i + 1);

            if (!hasLineOfSight(lastNode, next)) {
                smoothedNodes.add(current);
                lastNode = current;
            }
        }

        return new Path(smoothedNodes, path.getTarget(), path.canReach());
    }

    private boolean hasLineOfSight(Node start, Node end) {
        Vec3 startPos = new Vec3(start.x + 0.5, start.y + 0.5, start.z + 0.5);
        Vec3 endPos = new Vec3(end.x + 0.5, end.y + 0.5, end.z + 0.5);

        BlockHitResult result = this.level.clip(new ClipContext(
                startPos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.mob
        ));

        return result.getType() == HitResult.Type.MISS;
    }
}