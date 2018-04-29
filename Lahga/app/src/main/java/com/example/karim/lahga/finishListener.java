package com.example.karim.lahga;

public class finishListener {
    private boolean finish = false;
    private ChangeListener listener;

    public boolean isFinish() {
        return finish;
    }

    public void setFinish(boolean finish) {
        this.finish = finish;
        if (listener != null)
            listener.onChange();
    }

    public void setListener(ChangeListener listener) {
        this.listener = listener;
    }

    public interface ChangeListener {
        void onChange();
    }
}