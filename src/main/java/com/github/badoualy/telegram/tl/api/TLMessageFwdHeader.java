package com.github.badoualy.telegram.tl.api;

import com.github.badoualy.telegram.tl.StreamUtils;
import com.github.badoualy.telegram.tl.TLContext;
import com.github.badoualy.telegram.tl.core.TLObject;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.SIZE_CONSTRUCTOR_ID;
import static com.github.badoualy.telegram.tl.TLObjectUtils.SIZE_INT32;

/**
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public class TLMessageFwdHeader extends TLObject {

    public static final int CONSTRUCTOR_ID = 0xc786ddcb;

    protected int flags;

    protected Integer fromId;

    protected int date;

    protected Integer channelId;

    protected Integer channelPost;

    protected String filename;
    protected String channel;

    private final String _constructor = "messageFwdHeader#c786ddcb";

    public TLMessageFwdHeader() {
    }

    public TLMessageFwdHeader(Integer fromId, int date, Integer channelId, Integer channelPost) {
        this.fromId = fromId;
        this.date = date;
        this.channelId = channelId;
        this.channelPost = channelPost;
    }

    private void computeFlags() {
        flags = 0;
        flags = fromId != null ? (flags | 1) : (flags & ~1);
        flags = channelId != null ? (flags | 2) : (flags & ~2);
        flags = channelPost != null ? (flags | 4) : (flags & ~4);
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        computeFlags();

        writeInt(flags, stream);
        if ((flags & 1) != 0) {
            if (fromId == null) throwNullFieldException("fromId", flags);
            writeInt(fromId, stream);
        }
        writeInt(date, stream);
        if ((flags & 2) != 0) {
            if (channelId == null) throwNullFieldException("channelId", flags);
            writeInt(channelId, stream);
        }
        if ((flags & 4) != 0) {
            if (channelPost == null) throwNullFieldException("channelPost", flags);
            writeInt(channelPost, stream);
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        byte[] is = stream.readAllBytes();
        stream = new ByteArrayInputStream(is);

        flags = readInt(stream);
        fromId = (flags & 1) != 0 ? readInt(stream) : null;
        date = readInt(stream);
        channelId = (flags & 2) != 0 ? readInt(stream) : null;
        channelPost = (flags & 4) != 0 ? readInt(stream) : null;

        Tuple2<String, String> filenameAndChannelT = decodeFileNameAndChannel(is);
        filename = filenameAndChannelT.getT1();
        channel = filenameAndChannelT.getT2();
        System.out.println();
    }

    // TODO OPTIMIZE ME
    private Tuple2<String, String> decodeFileNameAndChannel(byte[] is) {
        List<List<Byte>> arrs = new ArrayList<>();
        List<Byte> actual = new ArrayList<>();
        arrs.add(actual);
        for (int i = 0; i < is.length; i++) {
            if(is[i] != 21) {
                arrs.get(arrs.size() - 1).add(is[i]);
            } else {
                arrs.add(new ArrayList<>());
            }
        }
        List<String> strings = arrs.stream()
                .map(b -> b.toArray(Byte[]::new))
                .map(bytes -> {
                    byte[] bs = new byte[bytes.length];
                    for (int i = 0; i < bytes.length; i++) {
                        bs[i] = bytes[i];
                    }
                    return new String(bs);
                })
                .collect(Collectors.toList());
        return Tuples.of(strings.get(2), strings.get(strings.size()-1));
    }

    @Override
    public int computeSerializedSize() {
        computeFlags();

        int size = SIZE_CONSTRUCTOR_ID;
        size += SIZE_INT32;
        if ((flags & 1) != 0) {
            if (fromId == null) throwNullFieldException("fromId", flags);
            size += SIZE_INT32;
        }
        size += SIZE_INT32;
        if ((flags & 2) != 0) {
            if (channelId == null) throwNullFieldException("channelId", flags);
            size += SIZE_INT32;
        }
        if ((flags & 4) != 0) {
            if (channelPost == null) throwNullFieldException("channelPost", flags);
            size += SIZE_INT32;
        }
        return size;
    }

    @Override
    public String toString() {
        return _constructor;
    }

    @Override
    public int getConstructorId() {
        return CONSTRUCTOR_ID;
    }

    public Integer getFromId() {
        return fromId;
    }

    public void setFromId(Integer fromId) {
        this.fromId = fromId;
    }

    public int getDate() {
        return date;
    }

    public void setDate(int date) {
        this.date = date;
    }

    public Integer getChannelId() {
        return channelId;
    }

    public void setChannelId(Integer channelId) {
        this.channelId = channelId;
    }

    public Integer getChannelPost() {
        return channelPost;
    }

    public void setChannelPost(Integer channelPost) {
        this.channelPost = channelPost;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return this.filename;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getChannel() {
        return this.channel;
    }

}
