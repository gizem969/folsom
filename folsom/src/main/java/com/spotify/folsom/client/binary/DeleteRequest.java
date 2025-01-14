/*
 * Copyright (c) 2014-2015 Spotify AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.spotify.folsom.client.binary;

import com.spotify.folsom.MemcacheStatus;
import com.spotify.folsom.client.OpCode;
import com.spotify.folsom.client.Request;
import com.spotify.folsom.guava.HostAndPort;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import java.io.IOException;
import java.nio.ByteBuffer;

public class DeleteRequest extends BinaryRequest<MemcacheStatus> {

  private final long cas;

  public DeleteRequest(final byte[] key, final long cas) {
    super(key);

    this.cas = cas;
  }

  @Override
  public ByteBuf writeRequest(final ByteBufAllocator alloc, final ByteBuffer dst) {
    writeHeader(dst, OpCode.DELETE, (byte) 0, 0, cas);
    dst.put(key);
    return toBuffer(alloc, dst);
  }

  @Override
  public Request<MemcacheStatus> duplicate() {
    return new DeleteRequest(key, cas);
  }

  @Override
  public void handle(final BinaryResponse replies, final HostAndPort server) throws IOException {
    final ResponsePacket reply = handleSingleReply(replies);

    succeed(reply.status);
  }
}
