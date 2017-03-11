package com.nxp.ble.meshlib.command;

import com.nxp.ble.meshlib.command.base.AbstractCommand;


public class NoCallbackCommand extends AbstractCommand {
    public NoCallbackCommand(byte[] cmd) {
        super(null);
        setCommandValue(cmd);
    }
}
