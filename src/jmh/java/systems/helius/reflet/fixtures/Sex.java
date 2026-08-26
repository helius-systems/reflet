package systems.helius.reflet.fixtures;

public enum Sex {
    MALE('M'), FEMALE('F'), OTHER('O'), UNKNOWN('X');

    char code;

    Sex(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    @Override
    public String toString() {
        return "Sex{" + name() + ": code=" + code + '}';
    }
}
