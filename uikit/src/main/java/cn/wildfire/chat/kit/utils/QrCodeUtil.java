package cn.wildfire.chat.kit.utils;

import cn.wildfire.chat.kit.WfcScheme;
import cn.wildfire.chat.kit.qrcode.QRCodeActivity;

/**
 * WfcScheme.QR_CODE_PREFIX_USER
 * WfcScheme.QR_CODE_PREFIX_GROUP
 * WfcScheme.QR_CODE_PREFIX_CHANNEL
 */
public class QrCodeUtil {
    public static String getId(int type, String id) {
        switch (type) {
            case QRCodeActivity.TYPE_PERSON:
                id = WfcScheme.QR_CODE_PREFIX_USER + id;
                break;
            case QRCodeActivity.TYPE_GROUP:
                id = WfcScheme.QR_CODE_PREFIX_GROUP + id;
                break;
            case QRCodeActivity.TYPE_CHANNEL:
                id = WfcScheme.QR_CODE_PREFIX_CHANNEL + id;
                break;
        }
        return id;
    }

    public static int getType(String qrCodeID) {
        if (qrCodeID.contains(WfcScheme.QR_CODE_PREFIX_USER)) {
            return QRCodeActivity.TYPE_PERSON;
        } else if (qrCodeID.contains(WfcScheme.QR_CODE_PREFIX_GROUP)) {
            return QRCodeActivity.TYPE_GROUP;
        } else if (qrCodeID.contains(WfcScheme.QR_CODE_PREFIX_CHANNEL)) {
            return QRCodeActivity.TYPE_CHANNEL;
        } else {
            return QRCodeActivity.TYPE_PERSON;
        }
    }

    public static String spiltId(String qrcode) {
        return qrcode.substring(qrcode.lastIndexOf("/") + 1);
    }
}
