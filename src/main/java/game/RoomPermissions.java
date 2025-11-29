package game;

import extension.logger.Logger;
import gearth.extensions.IExtension;
import gearth.protocol.HMessage;
import utils.Callback;

public class RoomPermissions {
    private final IExtension extension;
    private final Logger logger;
    private final Callback stateChangeCallback;

    private boolean canModifyWired;
    private boolean canMoveFurni;

    public RoomPermissions(IExtension extension, Logger logger, Callback stateChangeCallback) {
        this.extension = extension;
        this.logger = logger;
        this.stateChangeCallback = stateChangeCallback;

        extension.intercept(
                HMessage.Direction.TOCLIENT, "WiredPermissions", this::onWiredPermissions);
        extension.intercept(
                HMessage.Direction.TOCLIENT, "YouAreController", this::onYouAreController);
        extension.intercept(
                HMessage.Direction.TOCLIENT, "YouAreNotController", this::onYouAreNotController);

        extension.intercept(HMessage.Direction.TOCLIENT, "CloseConnection", m -> clear());
        extension.intercept(HMessage.Direction.TOSERVER, "Quit", m -> clear());
        extension.intercept(HMessage.Direction.TOCLIENT, "RoomReady", m -> clear());
    }

    private void onWiredPermissions(HMessage msg) {
        canModifyWired = msg.getPacket().readBoolean();
        stateChangeCallback.call();
    }

    private void onYouAreController(HMessage msg) {
        canMoveFurni = true;
        stateChangeCallback.call();
    }

    private void onYouAreNotController(HMessage msg) {
        canMoveFurni = false;
        stateChangeCallback.call();
    }

    public boolean canModifyWired() {
        return canModifyWired;
    }

    public boolean canMoveFurni() {
        return canMoveFurni;
    }

    public void clear() {
        canModifyWired = false;
        canMoveFurni = false;
        stateChangeCallback.call();
    }
}
