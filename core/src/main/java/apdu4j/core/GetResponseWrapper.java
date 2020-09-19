/*
 * Copyright (c) 2019-2020 Martin Paljak
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package apdu4j.core;

import java.util.concurrent.CompletableFuture;

public final class GetResponseWrapper implements AsynchronousBIBO {
    private final AsynchronousBIBO wrapped;

    public static GetResponseWrapper wrap(AsynchronousBIBO AsynchronousBIBO) {
        return new GetResponseWrapper(AsynchronousBIBO);
    }

    public GetResponseWrapper(AsynchronousBIBO AsynchronousBIBO) {
        this.wrapped = AsynchronousBIBO;
    }

    @Override
    public CompletableFuture<byte[]> transmit(byte[] command)  {
        return transmitCombining(new byte[0], wrapped, command);
    }

    static CompletableFuture<byte[]> transmitCombining(final byte[] current, final AsynchronousBIBO bibo, final byte[] command) {
        return bibo.transmit(command).thenComposeAsync((response) -> {
            ResponseAPDU res = new ResponseAPDU(response);
            if (res.getSW1() == 0x61) {
                // XXX: dependence on CommandAPDU for 256
                final byte[] cmd = new CommandAPDU(command[0], 0xC0, 0x00, 0x00, res.getSW2() == 0x00 ? 256 : res.getSW2()).getBytes();
                return transmitCombining(concatenate(current, res.getData()), bibo, cmd);
            } else {
                return CompletableFuture.completedFuture(concatenate(current, response, res.getSWBytes()));
            }
        });
    }

    static byte[] concatenate(byte[]... args) {
        int length = 0, pos = 0;
        for (byte[] arg : args) {
            length += arg.length;
        }
        byte[] result = new byte[length];
        for (byte[] arg : args) {
            System.arraycopy(arg, 0, result, pos, arg.length);
            pos += arg.length;
        }
        return result;
    }
}
