package de.mel.auth.data;

public class EmptyPayload extends ServicePayload {
    public EmptyPayload() {
    }

    public EmptyPayload(String intent) {
        this.intent = intent;
    }
}
