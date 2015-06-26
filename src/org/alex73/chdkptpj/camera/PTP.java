/**************************************************************************
 chdkptpJ - Java CHDK PTP framework.

 Copyright (C) 2015 Aleś Bułojčyk (alex73mail@gmail.com)

 This file is part of chdkptpJ.

 chdkptpJ is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 chdkptpJ is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/
package org.alex73.chdkptpj.camera;

/**
 * You can find some constants and flow description in the ISO 15740:2008 standard. Some old versions can be
 * found by Google.
 */
public class PTP {
    public static final short USB_CONTAINER_COMMAND = 1;
    public static final short USB_CONTAINER_DATA = 2;
    public static final short USB_CONTAINER_RESPONSE = 3;
    public static final short USB_CONTAINER_EVENT = 4;

    // Commands
    public static final short OPERATION_OpenSession = (short) 0x1002;
    public static final short OPERATION_CHDK = (short) 0x9999;

    // CHDK Operation Codes
    public static final int CHDK_Version = 0;
    public static final int CHDK_GetMemory = 1;
    public static final int CHDK_SetMemory = 2;
    public static final int CHDK_CallFunction = 3;
    public static final int CHDK_TempData = 4;
    public static final int CHDK_UploadFile = 5;
    public static final int CHDK_DownloadFile = 6;
    public static final int CHDK_ExecuteScript = 7;
    public static final int CHDK_ScriptStatus = 8;
    public static final int CHDK_ScriptSupport = 9;
    public static final int CHDK_ReadScriptMsg = 10;
    public static final int CHDK_WriteScriptData = 11;
    public static final int CHDK_GetDisplayData = 12;
    public static final int CHDK_RemoteCaptureIsReady = 13;
    public static final int CHDK_RemoteCaptureGetData = 14;

    public static final short RESPONSE_CODE_OK = (short) 0x2001;
    public static final short RESPONSE_CODE_GeneralError = (short) 0x2002;

    public static final int LV_TFR_VIEWPORT = 0x01;
    public static final int LV_TFR_BITMAP = 0x04;
    public static final int LV_TFR_PALETTE = 0x08;
}
