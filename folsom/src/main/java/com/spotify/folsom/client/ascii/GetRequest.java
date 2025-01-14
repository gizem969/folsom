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

package com.spotify.folsom.client.ascii;

import com.spotify.folsom.GetResult;
import com.spotify.folsom.client.Request;
import com.spotify.folsom.guava.HostAndPort;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class GetRequest extends AsciiRequest<GetResult<byte[]>>
    implements com.spotify.folsom.client.GetRequest {

  private static final byte[] GET = "get ".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] CAS_GET = "gets ".getBytes(StandardCharsets.US_ASCII);

  private final byte[] cmd;

  public GetRequest(final byte[] key, boolean withCas) {
    super(key);
    this.cmd = withCas ? CAS_GET : GET;
  }

  @Override
  public ByteBuf writeRequest(final ByteBufAllocator alloc, final ByteBuffer dst) {
    dst.put(cmd);
    dst.put(key);
    dst.put(NEWLINE_BYTES);
    return toBuffer(alloc, dst);
  }

  @Override
  public Request<GetResult<byte[]>> duplicate() {
    return new GetRequest(key, this.cmd == CAS_GET);
  }

  @Override
  public void handle(final AsciiResponse response, final HostAndPort server) throws IOException {
    if (response.type == AsciiResponse.Type.EMPTY_LIST) {
      succeed(null);
      return;
    }

    if (!(response instanceof ValueAsciiResponse)) {
      throw new IOException("Unexpected response type: " + response.type);
    }

    List<ValueResponse> values = ((ValueAsciiResponse) response).values;

    if (values.size() > 1) {
      throw new IOException("Too many responses, expected 1 but got " + values.size());
    }

    ValueResponse valueResponse = values.get(0);
    if (!Arrays.equals(valueResponse.key, key)) {
      String message =
          "Expected key " + decodeKey(key) + " but got " + decodeKey(valueResponse.key);
      throw new IOException(message);
    }

    succeed(GetResult.success(valueResponse.value, valueResponse.cas, valueResponse.flags));
  }

  private String decodeKey(byte[] key1) {
    // TODO: use charset from request object
    return new String(key1, StandardCharsets.US_ASCII);
  }
}
