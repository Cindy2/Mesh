//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.nxp.ble.meshlib.command.base;


public abstract class AbstractCommand {
    private static int sUniqueId = 0;
    private byte[] mCommandValue;
    private ICommandCallback mCommandCallback;
    private int mId;

    private static synchronized int nextId() {
        ++sUniqueId;
        if(sUniqueId > 255) {
            sUniqueId = 0;
        }

        return sUniqueId;
    }

    public AbstractCommand(ICommandCallback callback) {
        this.mCommandCallback = callback;
        if(callback != null) {
            this.mId = nextId();
        }

    }

    public void doResponse(byte[] value) {
    }

    public byte[] getCommandValue() {
        return this.mCommandValue;
    }

    public void setCommandValue(byte[] commandValue) {
        this.mCommandValue = commandValue;
    }

    public ICommandCallback getCommandCallback() {
        return this.mCommandCallback;
    }

    public void setCommandCallback(ICommandCallback commandCallback) {
        this.mCommandCallback = commandCallback;
    }

    public int getId() {
        return this.mId;
    }

    public void setId(int id) {
        this.mId = id;
    }
}
