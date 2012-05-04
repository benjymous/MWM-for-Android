/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.damazio.notifier.event.receivers.mms;

/**
 * The pdu part.
 */
class PduPart {
    /**
     * Well-Known Parameters.
     */
    public static final int P_Q                  = 0x80;
    public static final int P_CHARSET            = 0x81;
    public static final int P_LEVEL              = 0x82;
    public static final int P_TYPE               = 0x83;
    public static final int P_DEP_NAME           = 0x85;
    public static final int P_DEP_FILENAME       = 0x86;
    public static final int P_DIFFERENCES        = 0x87;
    public static final int P_PADDING            = 0x88;
    // This value of "TYPE" s used with Content-Type: multipart/related
    public static final int P_CT_MR_TYPE         = 0x89;
    public static final int P_DEP_START          = 0x8A;
    public static final int P_DEP_START_INFO     = 0x8B;
    public static final int P_DEP_COMMENT        = 0x8C;
    public static final int P_DEP_DOMAIN         = 0x8D;
    public static final int P_MAX_AGE            = 0x8E;
    public static final int P_DEP_PATH           = 0x8F;
    public static final int P_SECURE             = 0x90;
    public static final int P_SEC                = 0x91;
    public static final int P_MAC                = 0x92;
    public static final int P_CREATION_DATE      = 0x93;
    public static final int P_MODIFICATION_DATE  = 0x94;
    public static final int P_READ_DATE          = 0x95;
    public static final int P_SIZE               = 0x96;
    public static final int P_NAME               = 0x97;
    public static final int P_FILENAME           = 0x98;
    public static final int P_START              = 0x99;
    public static final int P_START_INFO         = 0x9A;
    public static final int P_COMMENT            = 0x9B;
    public static final int P_DOMAIN             = 0x9C;
    public static final int P_PATH               = 0x9D;

    /**
     *  Header field names.
     */
     public static final int P_CONTENT_TYPE       = 0x91;
     public static final int P_CONTENT_LOCATION   = 0x8E;
     public static final int P_CONTENT_ID         = 0xC0;
     public static final int P_DEP_CONTENT_DISPOSITION = 0xAE;
     public static final int P_CONTENT_DISPOSITION = 0xC5;
    // The next header is unassigned header, use reserved header(0x48) value.
     public static final int P_CONTENT_TRANSFER_ENCODING = 0xC8;

     /**
      * Content=Transfer-Encoding string.
      */
     public static final String CONTENT_TRANSFER_ENCODING =
             "Content-Transfer-Encoding";

     /**
      * Value of Content-Transfer-Encoding.
      */
     public static final String P_BINARY = "binary";
     public static final String P_7BIT = "7bit";
     public static final String P_8BIT = "8bit";
     public static final String P_BASE64 = "base64";
     public static final String P_QUOTED_PRINTABLE = "quoted-printable";

     /**
      * Value of disposition can be set to PduPart when the value is octet in
      * the PDU.
      * "from-data" instead of Form-data<Octet 128>.
      * "attachment" instead of Attachment<Octet 129>.
      * "inline" instead of Inline<Octet 130>.
      */
     static final byte[] DISPOSITION_FROM_DATA = "from-data".getBytes();
     static final byte[] DISPOSITION_ATTACHMENT = "attachment".getBytes();
     static final byte[] DISPOSITION_INLINE = "inline".getBytes();

     /**
      * Content-Disposition value.
      */
     public static final int P_DISPOSITION_FROM_DATA  = 0x80;
     public static final int P_DISPOSITION_ATTACHMENT = 0x81;
     public static final int P_DISPOSITION_INLINE     = 0x82;

     private PduPart() { }
}

