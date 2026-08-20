package com.dristmechanic.dristmechanic.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
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
    @SuppressWarnings({"NullableProblems", "ConstantConditions"})
    public Path createPath(@NotNull BlockPos pos, int distance) {
        Path path = super.createPath(pos, distance);
        return path != null ? smoothPath(path) : null;
    }

    private Path smoothPath(Path path) {
        if (path == null || path.getNodeCount() < 3) return path;

        int currentIdx = path.getNextNodeIndex();
        int maxLookahead = 5; // Сглаживаем только 5 нод вперед
        int endSmoothIdx = Math.min(path.getNodeCount() - 1, currentIdx + maxLookahead);

        List<Node> newNodes = new ArrayList<>();

        for (int i = 0; i < currentIdx; i++) {
            newNodes.add(path.getNode(i));
        }

        if (currentIdx < path.getNodeCount()) {
            newNodes.add(path.getNode(currentIdx));
            Node lastNode = path.getNode(currentIdx);

            for (int i = currentIdx + 1; i <= endSmoothIdx; i++) {
                Node current = path.getNode(i);

                if (i == path.getNodeCount() - 1) {
                    newNodes.add(current);
                    break;
                }

                Node next = path.getNode(i + 1);

                if (i == endSmoothIdx) {
                    newNodes.add(current);
                    break;
                }

                if (!hasLineOfSight(lastNode, next)) {
                    newNodes.add(current);
                    lastNode = current;
                }
            }

            for (int i = endSmoothIdx + 1; i < path.getNodeCount(); i++) {
                newNodes.add(path.getNode(i));
            }
        }

        return new Path(newNodes, path.getTarget(), path.canReach());
    }

    private boolean hasLineOfSight(Node start, Node end) {
        double dx = end.x - start.x;
        double dy = end.y - start.y;
        double dz = end.z - start.z;

        // Ограничиваем длину сглаживания (максимум 10 блоков).
        // Это спасает от лагов физики (collideBoundingBox), когда моб пытается
        // идти по длинной диагонали и "скребет" хитбоксом углы блоков.
        if (dx * dx + dy * dy + dz * dz > 100.0) {
            return false;
        }

        double x0 = start.x + 0.5;
        double y0 = start.y + 0.5;
        double z0 = start.z + 0.5;

        double x1 = end.x + 0.5;
        double y1 = end.y + 0.5;
        double z1 = end.z + 0.5;

        int x = (int) Math.floor(x0);
        int y = (int) Math.floor(y0);
        int z = (int) Math.floor(z0);

        int endX = (int) Math.floor(x1);
        int endY = (int) Math.floor(y1);
        int endZ = (int) Math.floor(z1);

        int stepX = (int) Math.signum(dx);
        int stepY = (int) Math.signum(dy);
        int stepZ = (int) Math.signum(dz);

        double tMaxX = stepX != 0 ? ((stepX > 0 ? (x + 1 - x0) : (x0 - x)) / Math.abs(dx)) : Double.MAX_VALUE;
        double tMaxY = stepY != 0 ? ((stepY > 0 ? (y + 1 - y0) : (y0 - y)) / Math.abs(dy)) : Double.MAX_VALUE;
        double tMaxZ = stepZ != 0 ? ((stepZ > 0 ? (z + 1 - z0) : (z0 - z)) / Math.abs(dz)) : Double.MAX_VALUE;

        double tDeltaX = stepX != 0 ? (1.0 / Math.abs(dx)) : Double.MAX_VALUE;
        double tDeltaY = stepY != 0 ? (1.0 / Math.abs(dy)) : Double.MAX_VALUE;
        double tDeltaZ = stepZ != 0 ? (1.0 / Math.abs(dz)) : Double.MAX_VALUE;

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        while (true) {
            if (x == endX && y == endY && z == endZ) break;

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    x += stepX;
                    tMaxX += tDeltaX;
                } else {
                    z += stepZ;
                    tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    y += stepY;
                    tMaxY += tDeltaY;
                } else {
                    z += stepZ;
                    tMaxZ += tDeltaZ;
                }
            }

            mutable.set(x, y, z);
            BlockState stateFeet = this.level.getBlockState(mutable);

            mutable.set(x, y + 1, z);
            BlockState stateHead = this.level.getBlockState(mutable);

            if (stateFeet.blocksMotion() || stateHead.blocksMotion() ||
                    stateFeet.getFluidState().is(FluidTags.LAVA) ||
                    stateHead.getFluidState().is(FluidTags.LAVA)) {
                return false;
            }

            mutable.set(x, y - 1, z);
            if (this.level.getBlockState(mutable).getFluidState().is(FluidTags.LAVA)) {
                return false;
            }
        }

        return true;
    }
}