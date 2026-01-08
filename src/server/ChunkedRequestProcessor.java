package server;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public final class ChunkedRequestProcessor {

    private ChunkedRequestProcessor() {
    }

    public static Result process(ByteArrayOutputStream buffer, byte[] data, int headerEndIndex, long maxBodyBytes) {
        if (data != null && data.length > 0) {
            buffer.writeBytes(data);
        }

        byte[] raw = buffer.toByteArray();
        ChunkedTransferDecoder.DecodeResult decoded = ChunkedTransferDecoder.decode(raw, headerEndIndex);
        if (!decoded.complete) {
            return Result.incomplete();
        }

        if (decoded.body.length > maxBodyBytes) {
            return Result.payloadTooLarge();
        }

        String headerSection = new String(raw, 0, headerEndIndex, StandardCharsets.ISO_8859_1);
        String normalizedHeaders =
                ChunkedTransferDecoder.normalizeHeaders(headerSection, decoded.body.length);

        ByteArrayOutputStream complete = new ByteArrayOutputStream();
        complete.writeBytes(normalizedHeaders.getBytes(StandardCharsets.ISO_8859_1));
        complete.writeBytes(decoded.body);
        return Result.complete(complete.toByteArray());
    }

    public static final class Result {
        public enum Status {
            COMPLETE,
            INCOMPLETE,
            PAYLOAD_TOO_LARGE
        }

        public final Status status;
        public final byte[] normalizedRequest;

        private Result(Status status, byte[] normalizedRequest) {
            this.status = status;
            this.normalizedRequest = normalizedRequest;
        }

        public static Result complete(byte[] normalizedRequest) {
            return new Result(Status.COMPLETE, normalizedRequest);
        }

        public static Result incomplete() {
            return new Result(Status.INCOMPLETE, null);
        }

        public static Result payloadTooLarge() {
            return new Result(Status.PAYLOAD_TOO_LARGE, null);
        }
    }
}
