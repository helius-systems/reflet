package systems.helius.reflet.fixtures;

public abstract class DataClass {
    private int privateInt;
    protected int b;
    protected String sA;
    protected String sB;
    protected long lC;
    protected Integer integerInstance;
    protected Integer wrapperOfPrivate;

    public DataClass(int privateInt, int b, String sA, String sB, long c, Integer integerInstance, Integer wrapperOfPrivate) {
        this.privateInt = privateInt;
        this.b = b;
        this.sA = sA;
        this.sB = sB;
        this.lC = c;
        this.integerInstance = integerInstance;
        this.wrapperOfPrivate = wrapperOfPrivate;
    }
}
