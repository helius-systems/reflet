package systems.helius.reflet.fixtures;

import jakarta.annotation.Nullable;

public class ChainLink {
    private ChainLink next;
    private ChainLink previous;

    public ChainLink(@Nullable ChainLink previous) {
        this.previous = previous;
        if (previous != null) {
            previous.setNext(this);
        }
    }

    public ChainLink getNext() {
        return next;
    }

    public void setNext(ChainLink next) {
        this.next = next;
    }

    public ChainLink getPrevious() {
        return previous;
    }

    public void setPrevious(ChainLink previous) {
        this.previous = previous;
    }
}
