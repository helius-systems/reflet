package systems.helius.reflet.fixtures;

/**
 * A deliberately deep class hierarchy used to reproduce the recursive-update
 * bug of ConcurrentHashMap.computeIfAbsent when caching hierarchical fields.
 * The leaf to inspect is {@link L1}.
 */
public final class DeepHierarchy {
    private DeepHierarchy() {}

    public static class L20 { int f20; }
    public static class L19 extends L20 { int f19; }
    public static class L18 extends L19 { int f18; }
    public static class L17 extends L18 { int f17; }
    public static class L16 extends L17 { int f16; }
    public static class L15 extends L16 { int f15; }
    public static class L14 extends L15 { int f14; }
    public static class L13 extends L14 { int f13; }
    public static class L12 extends L13 { int f12; }
    public static class L11 extends L12 { int f11; }
    public static class L10 extends L11 { int f10; }
    public static class L9 extends L10 { int f9; }
    public static class L8 extends L9 { int f8; }
    public static class L7 extends L8 { int f7; }
    public static class L6 extends L7 { int f6; }
    public static class L5 extends L6 { int f5; }
    public static class L4 extends L5 { int f4; }
    public static class L3 extends L4 { int f3; }
    public static class L2 extends L3 { int f2; }
    public static class L1 extends L2 { int f1; }
}
