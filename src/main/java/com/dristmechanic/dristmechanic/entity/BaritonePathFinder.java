package com.dristmechanic.dristmechanic.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;

import java.util.*;

public class BaritonePathFinder {
    private final Level level;
    private final BlockPos start;
    private final BlockPos goal;
    private final double mobHeight;
    private final Map<Long, PathNode> nodeMap = new HashMap<>();
    private final PriorityQueue<PathNode> openSet = new PriorityQueue<>(
            Comparator.comparingDouble(node -> node.fScore)
    );

    private static final double WALK_COST = 1.0;
    private static final double BREAK_COST_MULTIPLIER = 3.0;
    private static final double FALL_COST_PER_BLOCK = 0.5;
    private static final double JUMP_COST = 1.5;
    private static final int MAX_FALL_HEIGHT = 3;
    private static final Direction[] HORIZONTALS = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    private final Map<BlockPos, List<BlockPos>> blocksToBreakMap = new HashMap<>();

    public BaritonePathFinder(Level level, BlockPos start, BlockPos goal, double mobHeight) {
        this.level = level;
        this.start = start;
        this.goal = goal;
        this.mobHeight = mobHeight;
    }

    public Path findPath(int maxNodes) {
        PathNode startNode = new PathNode(start.getX(), start.getY(), start.getZ());
        startNode.gScore = 0;
        startNode.fScore = heuristic(startNode);
        nodeMap.put(BlockPos.asLong(start.getX(), start.getY(), start.getZ()), startNode);
        openSet.add(startNode);

        int nodesVisited = 0;

        while (!openSet.isEmpty() && nodesVisited < maxNodes) {
            PathNode current = openSet.poll();
            nodesVisited++;

            if (isGoal(current)) {
                return reconstructPath(current);
            }

            for (Movement move : getAllMoves()) {
                List<MovementResult> results = move.apply(level, current.x, current.y, current.z, mobHeight);

                for (MovementResult result : results) {
                    PathNode neighbor = getOrCreateNode(result.x, result.y, result.z);
                    double tentativeGScore = current.gScore + result.cost;

                    if (tentativeGScore < neighbor.gScore) {
                        neighbor.cameFrom = current;
                        neighbor.gScore = tentativeGScore;
                        neighbor.fScore = tentativeGScore + heuristic(neighbor);
                        neighbor.blocksToBreak.clear();
                        neighbor.blocksToBreak.addAll(result.blocksToBreak);

                        if (!openSet.contains(neighbor)) {
                            openSet.add(neighbor);
                        }
                    }
                }
            }
        }

        return null;
    }

    private List<Movement> getAllMoves() {
        return Arrays.asList(
                new TraverseMove(),
                new DescendMove(),
                new AscendMove(),
                new FallMove()
        );
    }

    private double heuristic(PathNode node) {
        double dx = Math.abs(node.x - goal.getX());
        double dz = Math.abs(node.z - goal.getZ());
        double dy = Math.abs(node.y - goal.getY());
        return (dx + dz + dy) * WALK_COST;
    }

    private boolean isGoal(PathNode node) {
        return node.x == goal.getX() &&
                node.z == goal.getZ() &&
                Math.abs(node.y - goal.getY()) <= 1;
    }

    private PathNode getOrCreateNode(int x, int y, int z) {
        long hash = BlockPos.asLong(x, y, z);
        return nodeMap.computeIfAbsent(hash, k -> {
            PathNode node = new PathNode(x, y, z);
            node.gScore = Double.MAX_VALUE;
            node.fScore = Double.MAX_VALUE;
            return node;
        });
    }

    private Path reconstructPath(PathNode endNode) {
        List<Node> pathNodes = new ArrayList<>();
        blocksToBreakMap.clear();
        PathNode current = endNode;

        while (current != null) {
            Node node = new Node(current.x, current.y, current.z);
            node.type = PathType.WALKABLE;
            pathNodes.add(0, node);

            BlockPos nodePos = new BlockPos(current.x, current.y, current.z);
            if (!current.blocksToBreak.isEmpty()) {
                blocksToBreakMap.put(nodePos, new ArrayList<>(current.blocksToBreak));
            }

            current = current.cameFrom;
        }

        Path path = new Path(pathNodes, goal, false);
        return path;
    }

    public Map<BlockPos, List<BlockPos>> getBlocksToBreakMap() {
        return new HashMap<>(blocksToBreakMap);
    }

    private static class PathNode {
        final int x, y, z;
        double gScore = Double.MAX_VALUE;
        double fScore = Double.MAX_VALUE;
        PathNode cameFrom;
        List<BlockPos> blocksToBreak = new ArrayList<>();

        PathNode(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public int hashCode() {
            return (int) BlockPos.asLong(x, y, z);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof PathNode other)) return false;
            return x == other.x && y == other.y && z == other.z;
        }
    }

    private static class MovementResult {
        final int x, y, z;
        final double cost;
        final List<BlockPos> blocksToBreak;

        MovementResult(int x, int y, int z, double cost, List<BlockPos> blocksToBreak) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.cost = cost;
            this.blocksToBreak = blocksToBreak;
        }
    }

    private interface Movement {
        List<MovementResult> apply(Level level, int x, int y, int z, double mobHeight);
    }

    private static boolean canBreakBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        float hardness = state.getDestroySpeed(level, pos);
        return hardness >= 0 && hardness <= 5.0f;
    }

    private static class TraverseMove implements Movement {
        @Override
        public List<MovementResult> apply(Level level, int x, int y, int z, double mobHeight) {
            List<MovementResult> results = new ArrayList<>();

            for (Direction dir : HORIZONTALS) {
                int nx = x + dir.getStepX();
                int nz = z + dir.getStepZ();

                BlockPos below = new BlockPos(nx, y - 1, nz);
                BlockState belowState = level.getBlockState(below);
                if (!belowState.blocksMotion()) continue;

                List<BlockPos> toBreak = new ArrayList<>();
                double cost = WALK_COST;
                boolean canMove = true;

                for (int i = 0; i < Math.ceil(mobHeight); i++) {
                    BlockPos checkPos = new BlockPos(nx, y + i, nz);
                    BlockState state = level.getBlockState(checkPos);

                    if (!state.isAir() && state.blocksMotion()) {
                        if (!canBreakBlock(level, checkPos)) {
                            canMove = false;
                            break;
                        }
                        toBreak.add(checkPos);
                        cost += state.getDestroySpeed(level, checkPos) * BREAK_COST_MULTIPLIER;
                    }
                }

                if (canMove) {
                    results.add(new MovementResult(nx, y, nz, cost, toBreak));
                }
            }

            return results;
        }
    }

    private static class DescendMove implements Movement {
        @Override
        public List<MovementResult> apply(Level level, int x, int y, int z, double mobHeight) {
            List<MovementResult> results = new ArrayList<>();

            for (Direction dir : HORIZONTALS) {
                int nx = x + dir.getStepX();
                int nz = z + dir.getStepZ();
                int ny = y - 1;

                BlockPos landing = new BlockPos(nx, ny - 1, nz);
                BlockState landingState = level.getBlockState(landing);

                if (!landingState.blocksMotion()) continue;

                List<BlockPos> toBreak = new ArrayList<>();
                double cost = WALK_COST + FALL_COST_PER_BLOCK;
                boolean canMove = true;

                BlockPos headPos = new BlockPos(nx, ny, nz);
                BlockState headState = level.getBlockState(headPos);
                if (!headState.isAir() && headState.blocksMotion()) {
                    if (canBreakBlock(level, headPos)) {
                        toBreak.add(headPos);
                        cost += headState.getDestroySpeed(level, headPos) * BREAK_COST_MULTIPLIER;
                    } else {
                        canMove = false;
                    }
                }

                BlockPos aboveHead = new BlockPos(nx, y, nz);
                BlockState aboveHeadState = level.getBlockState(aboveHead);
                if (!aboveHeadState.isAir() && aboveHeadState.blocksMotion()) {
                    if (canBreakBlock(level, aboveHead)) {
                        toBreak.add(aboveHead);
                        cost += aboveHeadState.getDestroySpeed(level, aboveHead) * BREAK_COST_MULTIPLIER;
                    } else {
                        canMove = false;
                    }
                }

                if (canMove) {
                    results.add(new MovementResult(nx, ny, nz, cost, toBreak));
                }
            }
            return results;
        }
    }

    private static class AscendMove implements Movement {
        @Override
        public List<MovementResult> apply(Level level, int x, int y, int z, double mobHeight) {
            List<MovementResult> results = new ArrayList<>();

            for (Direction dir : HORIZONTALS) {
                int nx = x + dir.getStepX();
                int nz = z + dir.getStepZ();
                int ny = y + 1;

                BlockPos step = new BlockPos(nx, y, nz);
                BlockState stepState = level.getBlockState(step);
                if (!stepState.blocksMotion()) continue;

                List<BlockPos> toBreak = new ArrayList<>();
                double cost = WALK_COST + JUMP_COST;
                boolean canMove = true;

                BlockPos above = new BlockPos(nx, ny, nz);
                BlockState aboveState = level.getBlockState(above);
                if (!aboveState.isAir() && aboveState.blocksMotion()) {
                    if (canBreakBlock(level, above)) {
                        toBreak.add(above);
                        cost += aboveState.getDestroySpeed(level, above) * BREAK_COST_MULTIPLIER;
                    } else {
                        canMove = false;
                    }
                }

                BlockPos aboveHead = new BlockPos(nx, y + (int)Math.ceil(mobHeight), nz);
                BlockState aboveHeadState = level.getBlockState(aboveHead);
                if (canMove && !aboveHeadState.isAir() && aboveHeadState.blocksMotion()) {
                    if (canBreakBlock(level, aboveHead)) {
                        toBreak.add(aboveHead);
                        cost += aboveHeadState.getDestroySpeed(level, aboveHead) * BREAK_COST_MULTIPLIER;
                    } else {
                        canMove = false;
                    }
                }

                if (canMove) {
                    results.add(new MovementResult(nx, ny, nz, cost, toBreak));
                }
            }
            return results;
        }
    }

    private static class FallMove implements Movement {
        @Override
        public List<MovementResult> apply(Level level, int x, int y, int z, double mobHeight) {
            List<MovementResult> results = new ArrayList<>();

            for (Direction dir : HORIZONTALS) {
                int nx = x + dir.getStepX();
                int nz = z + dir.getStepZ();

                for (int fallHeight = 1; fallHeight <= MAX_FALL_HEIGHT; fallHeight++) {
                    int ny = y - fallHeight;
                    BlockPos landing = new BlockPos(nx, ny - 1, nz);
                    BlockState landingState = level.getBlockState(landing);

                    if (landingState.blocksMotion()) {
                        BlockPos headPos = new BlockPos(nx, ny, nz);
                        BlockState headState = level.getBlockState(headPos);

                        if (headState.isAir() || !headState.blocksMotion()) {
                            results.add(new MovementResult(nx, ny, nz,
                                    WALK_COST + fallHeight * FALL_COST_PER_BLOCK,
                                    new ArrayList<>()));
                        }
                    }
                }
            }
            return results;
        }
    }
}