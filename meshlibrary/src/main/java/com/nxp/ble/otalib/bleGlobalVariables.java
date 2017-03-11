package com.nxp.ble.otalib;

import java.util.UUID;

/**
 * Created by afan on 2017/2/13.
 */

public class bleGlobalVariables {
    public static final String QuinticOtaService = "0000fee8-0000-1000-8000-00805f9b34fb";
    public static final String QuinticQppService = "0000fee9-0000-1000-8000-00805f9b34fb";
    public static final String otaWriteCharacteristic = "013784cf-f7e3-55b4-6c4c-9fd140100a16";
    public static final String otaNotifyCharacteristic = "003784cf-f7e3-55b4-6c4c-9fd140100a16";
    public static final String qppWriteCharacteristic = "d44bc439-abfd-45a2-b575-925416129600";
    public static final String qppDescripter = "00002902-0000-1000-8000-00805f9b34fb";
    public static final UUID UUID_QUINTIC_OTA_SERVICE = UUID.fromString("0000fee8-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_QUINTIC_QPP_SERVICE = UUID.fromString("0000fee9-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_OTA_WRITE_CHARACTERISTIC = UUID.fromString("013784cf-f7e3-55b4-6c4c-9fd140100a16");
    public static final UUID UUID_OTA_NOTIFY_CHARACTERISTIC = UUID.fromString("003784cf-f7e3-55b4-6c4c-9fd140100a16");
    public static final UUID UUID_QPP_WRITE_CHARACTERISTIC = UUID.fromString("d44bc439-abfd-45a2-b575-925416129600");
    public static final UUID UUID_QPP_DESCRIPTER = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public bleGlobalVariables() {
    }

    protected static enum OtaNotiDataPkg {
        OTA_NOTI_LENGTH_L,
        OTA_NOTI_LENGTH_H,
        OTA_NOTI_CMD,
        OTA_NOTI_RESULT,
        OTA_NOTI_RCVED_LENGTH_L,
        OTA_NOTI_RCVED_LENGTH_H,
        OTA_NOTI_RCVED_CS_L,
        OTA_NOTI_RCVED_CS_H;

        private OtaNotiDataPkg() {
        }
    }

    protected static enum otaCmd {
        OTA_CMD_META_DATA,
        OTA_CMD_BRICK_DATA,
        OTA_CMD_DATA_VERIFY,
        OTA_CMD_EXECUTION_NEW_CODE;

        private otaCmd() {
        }
    }

    public static enum otaResult {
        OTA_RESULT_SUCCESS,
        OTA_RESULT_PKT_CHECKSUM_ERROR,
        OTA_RESULT_PKT_LEN_ERROR,
        OTA_RESULT_DEVICE_NOT_SUPPORT_OTA,
        OTA_RESULT_FW_SIZE_ERROR,
        OTA_RESULT_FW_VERIFY_ERROR,
        OTA_RESULT_INVALID_ARGUMENT,
        OTA_RESULT_OPEN_FIRMWAREFILE_ERROR,
        OTA_RESULT_SEND_META_ERROR,
        OTA_RESULT_RECEIVED_INVALID_PACKET,
        OTA_RESULT_META_RESPONSE_TIMEOUT,
        OTA_RESULT_DATA_RESPONSE_TIMEOUT;

        private otaResult() {
        }
    }
}
