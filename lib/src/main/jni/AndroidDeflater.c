/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#include <stdio.h>
#include <stdlib.h>
#include <jni.h>
#include <zlib.h>

#define DEF_MEM_LEVEL 8

#define jlong_zero ((jlong) 0)
#define jlong_to_ptr(a) ((void*)(intptr_t)(a))
#define ptr_to_jlong(a) ((jlong) (intptr_t)(a))

static jfieldID levelID;
static jfieldID strategyID;
static jfieldID setParamsID;
static jfieldID finishID;
static jfieldID finishedID;
static jfieldID bufID, offID, lenID;

JNIEXPORT void JNICALL ThrowByName(JNIEnv *env, const char *name, const char *msg) {
    jclass cls = (*env)->FindClass(env, name);
    if (cls != 0)
        (*env)->ThrowNew(env, cls, msg);
}

JNIEXPORT void JNICALL
Java_com_eidu_zip_AndroidDeflater_initIDs(JNIEnv *env, jclass cls) {
    levelID = (*env)->GetFieldID(env, cls, "level", "I");
    strategyID = (*env)->GetFieldID(env, cls, "strategy", "I");
    setParamsID = (*env)->GetFieldID(env, cls, "setParams", "Z");
    finishID = (*env)->GetFieldID(env, cls, "finish", "Z");
    finishedID = (*env)->GetFieldID(env, cls, "finished", "Z");
    bufID = (*env)->GetFieldID(env, cls, "buf", "[B");
    offID = (*env)->GetFieldID(env, cls, "off", "I");
    lenID = (*env)->GetFieldID(env, cls, "len", "I");
}

JNIEXPORT jlong JNICALL
Java_com_eidu_zip_AndroidDeflater_init(JNIEnv *env, jclass cls, jint level,
                                      jint strategy, jboolean nowrap) {
    z_stream *strm = calloc(1, sizeof(z_stream));

    if (strm == 0) {
        ThrowByName(env, "java/lang/OutOfMemoryError", 0);
        return jlong_zero;
    } else {
        char *msg;
        switch (deflateInit2(strm, level, Z_DEFLATED,
                             nowrap ? -MAX_WBITS : MAX_WBITS,
                             DEF_MEM_LEVEL, strategy)) {
            case Z_OK:
                return ptr_to_jlong(strm);
            case Z_MEM_ERROR:
                free(strm);
                ThrowByName(env, "java/lang/OutOfMemoryError", 0);
                return jlong_zero;
            case Z_STREAM_ERROR:
                free(strm);
                ThrowByName(env, "java/lang/IllegalArgumentException", 0);
                return jlong_zero;
            default:
                msg = strm->msg;
                free(strm);
                ThrowByName(env, "java/lang/InternalError", msg);
                return jlong_zero;
        }
    }
}

JNIEXPORT void JNICALL
Java_com_eidu_zip_AndroidDeflater_setDictionary(JNIEnv *env, jclass cls, jlong addr,
                                               jbyteArray b, jint off, jint len) {
    Bytef *buf = (*env)->GetPrimitiveArrayCritical(env, b, 0);
    int res;
    if (buf == 0) {/* out of memory */
        return;
    }
    res = deflateSetDictionary((z_stream *) jlong_to_ptr(addr), buf + off, len);
    (*env)->ReleasePrimitiveArrayCritical(env, b, buf, 0);
    switch (res) {
        case Z_OK:
            break;
        case Z_STREAM_ERROR:
            ThrowByName(env, "java/lang/IllegalArgumentException", 0);
            break;
        default:
            ThrowByName(env, "java/lang/InternalError", ((z_stream *) jlong_to_ptr(addr))->msg);
            break;
    }
}

JNIEXPORT jint JNICALL
Java_com_eidu_zip_AndroidDeflater_deflateBytes(JNIEnv *env, jobject this, jlong addr,
                                              jbyteArray b, jint off, jint len, jint flush) {
    z_stream *strm = jlong_to_ptr(addr);

    jarray this_buf = (*env)->GetObjectField(env, this, bufID);
    jint this_off = (*env)->GetIntField(env, this, offID);
    jint this_len = (*env)->GetIntField(env, this, lenID);
    jbyte *in_buf;
    jbyte *out_buf;
    int res;
    if ((*env)->GetBooleanField(env, this, setParamsID)) {
        int level = (*env)->GetIntField(env, this, levelID);
        int strategy = (*env)->GetIntField(env, this, strategyID);
        in_buf = (*env)->GetPrimitiveArrayCritical(env, this_buf, 0);
        if (in_buf == NULL) {
            // Throw OOME only when length is not zero
            if (this_len != 0)
                ThrowByName(env, "java/lang/OutOfMemoryError", 0);
            return 0;
        }
        out_buf = (*env)->GetPrimitiveArrayCritical(env, b, 0);
        if (out_buf == NULL) {
            (*env)->ReleasePrimitiveArrayCritical(env, this_buf, in_buf, 0);
            if (len != 0)
                ThrowByName(env, "java/lang/OutOfMemoryError", 0);
            return 0;
        }

        strm->next_in = (Bytef *) (in_buf + this_off);
        strm->next_out = (Bytef *) (out_buf + off);
        strm->avail_in = this_len;
        strm->avail_out = len;
        res = deflateParams(strm, level, strategy);
        (*env)->ReleasePrimitiveArrayCritical(env, b, out_buf, 0);
        (*env)->ReleasePrimitiveArrayCritical(env, this_buf, in_buf, 0);

        switch (res) {
            case Z_OK:
                (*env)->SetBooleanField(env, this, setParamsID, JNI_FALSE);
                this_off += this_len - strm->avail_in;
                (*env)->SetIntField(env, this, offID, this_off);
                (*env)->SetIntField(env, this, lenID, (int) strm->avail_in);
                return len - strm->avail_out;
            case Z_BUF_ERROR:
                (*env)->SetBooleanField(env, this, setParamsID, JNI_FALSE);
                return 0;
            default:
                ThrowByName(env, "java/lang/InternalError", strm->msg);
                return 0;
        }
    } else {
        jboolean finish = (*env)->GetBooleanField(env, this, finishID);
        in_buf = (*env)->GetPrimitiveArrayCritical(env, this_buf, 0);
        if (in_buf == NULL) {
            if (this_len != 0)
                ThrowByName(env, "java/lang/OutOfMemoryError", 0);
            return 0;
        }
        out_buf = (*env)->GetPrimitiveArrayCritical(env, b, 0);
        if (out_buf == NULL) {
            (*env)->ReleasePrimitiveArrayCritical(env, this_buf, in_buf, 0);
            if (len != 0)
                ThrowByName(env, "java/lang/OutOfMemoryError", 0);

            return 0;
        }

        strm->next_in = (Bytef *) (in_buf + this_off);
        strm->next_out = (Bytef *) (out_buf + off);
        strm->avail_in = this_len;
        strm->avail_out = len;
        res = deflate(strm, finish ? Z_FINISH : flush);
        (*env)->ReleasePrimitiveArrayCritical(env, b, out_buf, 0);
        (*env)->ReleasePrimitiveArrayCritical(env, this_buf, in_buf, 0);

        switch (res) {
            case Z_STREAM_END:
                (*env)->SetBooleanField(env, this, finishedID, JNI_TRUE);
/* fall through */
            case Z_OK:
                this_off += this_len - strm->avail_in;
                (*env)->SetIntField(env, this, offID, this_off);
                (*env)->SetIntField(env, this, lenID, (int) strm->avail_in);
                return len - strm->avail_out;
            case Z_BUF_ERROR:
                return 0;
            default:
                ThrowByName(env, "java/lang/InternalError", strm->msg);
                return 0;
        }
    }
}

JNIEXPORT jint JNICALL
Java_com_eidu_zip_AndroidDeflater_getAdler(JNIEnv *env, jclass cls, jlong addr) {
    return ((z_stream *) jlong_to_ptr(addr))->adler;
}

JNIEXPORT void JNICALL
Java_com_eidu_zip_AndroidDeflater_reset(JNIEnv *env, jclass cls, jlong addr) {
    if (deflateReset((z_stream *) jlong_to_ptr(addr)) != Z_OK) {
        ThrowByName(env, "java/lang/InternalError", 0);
    }
}

JNIEXPORT void JNICALL
Java_com_eidu_zip_AndroidDeflater_end(JNIEnv *env, jclass cls, jlong addr) {
    if (deflateEnd((z_stream *) jlong_to_ptr(addr)) == Z_STREAM_ERROR) {
        ThrowByName(env, "java/lang/InternalError", 0);
    } else {
        free((z_stream *) jlong_to_ptr(addr));
    }
}
